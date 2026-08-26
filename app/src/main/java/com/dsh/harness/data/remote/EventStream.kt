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
import kotlinx.serialization.json.JsonObject
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
 * 官方 MuxStream：将 `{base}/api/events.mux` 升级为 WebSocket（下行事件流）。
 * 每个文本帧是一个 JSON ServerRequest 帧，payload 携带 host 事件（session/event、host/workspace-* …）。
 *
 * 与官方客户端一致：该 socket 仅下行（不发送客户端消息），掉线自动重连（指数退避）。
 * 用于实现"与网页端实时同步"。请求-响应类调用（workspace.list 等）仍走 POST /api/<method>。
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

    private val _events = MutableSharedFlow<ServerRequestFrame>(extraBufferCapacity = 128)
    val events: SharedFlow<ServerRequestFrame> = _events.asSharedFlow()

    fun setBase(url: String?) {
        val u = url?.trim()?.trimEnd('/')
        if (!u.isNullOrBlank()) base = u
    }

    fun start() {
        if (running) return
        running = true
        scope.launch { runLoop() }
    }

    fun stop() {
        running = false
        socket?.close(1000, "app stop")
        socket = null
    }

    private suspend fun runLoop() {
        var backoffMs = 2000L
        while (running) {
            val ws = openWs()
            if (ws == null) {
                delay(backoffMs)
                backoffMs = (backoffMs * 2).coerceAtMost(30_000L)
                continue
            }
            backoffMs = 2000L
            // 阻塞直到 socket 断开（listener 里把 socket 置空驱动退出）
            while (running && ws == socket) {
                delay(1000)
            }
            delay(1500)
        }
    }

    private fun openWs(): WebSocket? {
        val url = wsUrl()
        socket = null
        val req = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("X-Client", "DSH mobile/Android")
            .build()
        val ws = client.newWebSocket(req, listener())
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
            val frame = try {
                json.parseToJsonElement(text).jsonObject
            } catch (e: Exception) {
                JsonObject(emptyMap())
            }
            _events.tryEmit(
                ServerRequestFrame(
                    type = frame["type"]?.jsonPrimitive?.contentOrNull,
                    method = frame["method"]?.jsonPrimitive?.contentOrNull,
                    payload = frame["payload"] as? JsonObject
                )
            )
        }

        override fun onClosed(ws: WebSocket, code: Int, reason: String) {
            if (socket === ws) socket = null
        }

        override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
            if (socket === ws) socket = null
        }
    }
}

/** 下行 ServerRequest 帧（宽松建模，只读需要字段）。 */
data class ServerRequestFrame(
    val type: String? = null,
    val method: String? = null,
    val payload: JsonObject? = null
)