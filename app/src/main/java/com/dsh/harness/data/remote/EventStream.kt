package com.dsh.harness.data.remote

import com.dsh.harness.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.Volatile

/**
 * 官方 MuxStream 持久连接增强版（对齐 Persistent Connection Systems 规范）：
 *
 *  - 下行：WebSocket {base}/api/events.mux，文本帧 = ServerRequest JSON
 *  - 心跳：应用层 25s 定时 ping（文本帧 "ping"），服务端不回复也不影响；
 *          超过 70s 无任何消息视为静默断开 → 主动关闭并重连
 *  - 重连 & 恢复：指数退避（2s→30s），每次重连成功后发出 `RECONNECTED` 控制事件，
 *                  ViewModel 收到后做一次全量刷新（等价于 resume cursor=0，保证无缺口）
 *  - 背压：SharedFlow extraBufferCapacity=512，onBufferOverflow=DROP_OLDEST
 *          （慢消费时优先保新事件，旧事件靠重连后全量刷新兜底）
 *  - 扇出：把原始 method 归一化为 [StreamEvent.Kind]，便于上层精确路由
 */
@Singleton
class EventStream @Inject constructor(
    private val client: OkHttpClient
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile private var base: String = BuildConfig.HARNESS_BASE_URL.trimEnd('/')
    @Volatile private var running = false
    @Volatile private var socket: WebSocket? = null
    @Volatile private var lastRxAt = 0L

    // -------- 背压：512 缓冲 + 丢最旧 --------
    private val _events = MutableSharedFlow<StreamEvent>(
        extraBufferCapacity = 512,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<StreamEvent> = _events.asSharedFlow()

    // 连接状态暴露给 UI（可选展示）
    private val _connState = MutableStateFlow(ConnState.DISCONNECTED)
    val connState: StateFlow<ConnState> = _connState.asStateFlow()

    fun setBase(url: String?) {
        val u = url?.trim()?.trimEnd('/')
        if (!u.isNullOrBlank()) {
            val changed = base != u
            base = u
            if (changed && running) {
                // 地址变更 → 主动断连让 runLoop 用新地址重连
                socket?.close(1000, "base-url-changed")
                socket = null
            }
        }
    }

    fun start() {
        if (running) return
        running = true
        scope.launch { runLoop() }
        scope.launch { watchDogLoop() }
    }

    fun stop() {
        running = false
        socket?.close(1000, "app-stop")
        socket = null
        _connState.value = ConnState.DISCONNECTED
    }

    // ---------------- 主循环：指数退避 ----------------
    private suspend fun runLoop() {
        var backoffMs = 2000L
        while (running) {
            _connState.value = ConnState.CONNECTING
            val ws = openWs()
            if (ws == null) {
                _connState.value = ConnState.DISCONNECTED
                delay(backoffMs)
                backoffMs = (backoffMs * 2).coerceAtMost(30_000L)
                continue
            }
            backoffMs = 2000L
            _connState.value = ConnState.CONNECTED
            lastRxAt = System.currentTimeMillis()
            // 重连后立刻发出控制事件，上层执行一次全量刷新（resume 语义）
            _events.tryEmit(StreamEvent(kind = StreamEvent.Kind.RECONNECTED))
            // 心跳：每 25s 发送一个 ping 文本帧，防止中间设备静默断连
            val pingJob = scope.launch { pingLoop(ws) }
            // 阻塞直到 socket 置空或 running=false
            while (running && ws === socket) {
                delay(1000)
            }
            pingJob.cancel()
            _connState.value = ConnState.DISCONNECTED
            delay(1500)
        }
    }

    // ---------------- 看门狗：70s 无接收 → 判定静默断线 ----------------
    private suspend fun watchDogLoop() {
        while (running) {
            delay(5000)
            val s = socket ?: continue
            val idle = System.currentTimeMillis() - lastRxAt
            if (idle > 70_000L) {
                runCatching { s.close(1001, "idle-timeout-$idle") }
                socket = null
            }
        }
    }

    // ---------------- 心跳 ----------------
    private suspend fun pingLoop(ws: WebSocket) {
        try {
            while (running && ws === socket) {
                delay(25_000L)
                if (ws === socket) {
                    runCatching { ws.send("ping") }
                }
            }
        } catch (_: Throwable) { /* ping 失败会由看门狗/监听器发现 */ }
    }

    private fun openWs(): WebSocket? {
        val url = wsUrl()
        socket = null
        val req = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("X-Client", "DSH mobile/Android")
            .header("User-Agent", "DSH mobile/Android (EventStream)")
            .build()
        val ws = runCatching { client.newWebSocket(req, listener()) }
            .getOrNull() ?: return null
        socket = ws
        return ws
    }

    private fun wsUrl(): String {
        val host = base
            .removePrefix("https://")
            .removePrefix("http://")
        return if (base.startsWith("https")) "wss://$host/api/events.mux" else "ws://$host/api/events.mux"
    }

    private fun listener() = object : WebSocketListener() {
        override fun onMessage(ws: WebSocket, text: String) {
            lastRxAt = System.currentTimeMillis()
            if (text.isBlank() || text == "pong" || text == "ping") return
            val frame = try {
                json.parseToJsonElement(text).jsonObject
            } catch (e: Exception) {
                JsonObject(emptyMap())
            }
            val rawMethod = frame["method"]?.jsonPrimitive?.contentOrNull
            val kind = classify(rawMethod)
            val evt = StreamEvent(
                kind = kind,
                rawMethod = rawMethod,
                type = frame["type"]?.jsonPrimitive?.contentOrNull,
                payload = frame["payload"] as? JsonObject
            )
            _events.tryEmit(evt)
        }

        override fun onClosed(ws: WebSocket, code: Int, reason: String) {
            if (socket === ws) socket = null
        }

        override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
            if (socket === ws) socket = null
        }
    }

    /** 归一化事件分类（上层路由用 Kind 匹配即可，不关心具体 method 字符串）。 */
    private fun classify(rawMethod: String?): StreamEvent.Kind = when {
        rawMethod.isNullOrBlank() -> StreamEvent.Kind.UNKNOWN
        rawMethod.contains("workspace", true) -> StreamEvent.Kind.WORKSPACE
        rawMethod.contains("session", true) &&
            !rawMethod.contains("message", true) -> StreamEvent.Kind.SESSION
        rawMethod.contains("message", true) -> StreamEvent.Kind.MESSAGE
        rawMethod.contains("model", true) || rawMethod.contains("provider", true) -> StreamEvent.Kind.MODEL
        rawMethod.contains("plugin", true) || rawMethod.contains("market", true) -> StreamEvent.Kind.PLUGIN
        rawMethod.contains("settings", true) || rawMethod.contains("sidecard", true) -> StreamEvent.Kind.SETTINGS
        rawMethod.contains("task", true) -> StreamEvent.Kind.TASK
        rawMethod.contains("host", true) && rawMethod.contains("describe", true) -> StreamEvent.Kind.HOST_DESCRIBE
        else -> StreamEvent.Kind.UNKNOWN
    }
}

/** 连接状态（UI 可选展示在线状态徽标）。 */
enum class ConnState { CONNECTING, CONNECTED, DISCONNECTED }

/**
 * 归一化后的流事件：
 * - 控制事件：RECONNECTED（上层据此执行全量 refresh 恢复缺口）
 * - 业务事件：WORKSPACE / SESSION / MESSAGE / MODEL / PLUGIN / SETTINGS / TASK
 */
data class StreamEvent(
    val kind: Kind,
    val rawMethod: String? = null,
    val type: String? = null,
    val payload: JsonObject? = null
) {
    enum class Kind {
        RECONNECTED,      // 控制：刚重连成功，需要全量刷新
        WORKSPACE,        // 工作区变更（增删改）
        SESSION,          // 会话列表/会话元数据变更
        MESSAGE,          // 会话中消息追加/更新（流式增量）
        MODEL,            // 模型/提供方变更
        PLUGIN,           // 插件市场/安装变更
        SETTINGS,         // 设置/侧边卡片变更
        TASK,             // 后台任务进度
        HOST_DESCRIBE,    // 宿主描述
        UNKNOWN           // 无法识别，上层忽略
    }
}

/** 兼容旧代码：仍可按"方法名是否包含 XX"读取。 */
@Deprecated("Use StreamEvent.kind", ReplaceWith("StreamEvent.kind"))
data class ServerRequestFrame(
    val type: String? = null,
    val method: String? = null,
    val payload: JsonElement? = null
)
