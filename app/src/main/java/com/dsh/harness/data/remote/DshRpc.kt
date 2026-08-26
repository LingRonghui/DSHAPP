package com.dsh.harness.data.remote

import com.dsh.harness.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.put
import kotlinx.serialization.json.buildJsonObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DeepSeek Harness 真实宿主协议客户端（对齐官方 deepseek-harness/packages/host/apiproxy）。
 *
 * 线协议（fetch carrier）：
 *  - 客户端调用：POST {base}/api/<method>，body = ClientRequest 帧
 *    { type:'client-request', rpcId, method, payload }
 *  - 响应体    ：ServerResponse 帧 { type:'server-response', rpcId, result:{ ok, value?, error? } }
 *  - 流式下行  ：session.prompt 的响应体为按行分隔的事件帧（ServerRequest，其 payload 为 MuxFrame/HostFrame）。
 *
 * 反序列化采用宽松模式（ignoreUnknownKeys + isLenient + coerceInputValues），
 * 只建模业务真正读取的字段，未知字段自动忽略，兼容官方 schema 的所有扩展。
 */
@Singleton
class DshRpcClient @Inject constructor(
    private val client: OkHttpClient
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
        explicitNulls = false
    }

    private var base = BuildConfig.HARNESS_BASE_URL.trimEnd('/')

    /** 运行期可切换目标实例（设置页）。 */
    fun setBaseUrl(url: String?) {
        val u = url?.trim()?.trimEnd('/')
        if (!u.isNullOrBlank()) base = u
    }
    fun baseUrl(): String = base

    private val jsonType: MediaType = "application/json; charset=utf-8".toMediaType()

    /** 发起一次一元 RPC 调用，返回 ServerResponse.result.value（业务成功值）或抛出异常。 */
    suspend fun callValue(method: String, payload: JsonObject = buildJsonObject {}): JsonElement {
        val body = postBody(method, buildFrame(method, payload))
        return valueFromBody(method, body)
    }

    /** 构造 ClientRequest 帧 JSON 字符串（供原生 OkHttp 与 WebView 两种传输复用）。 */
    fun buildFrame(method: String, payload: JsonObject = buildJsonObject {}): String =
        buildJsonObject {
            put("type", "client-request")
            put("rpcId", RpcId.mint())
            put("method", method)
            put("payload", payload)
        }.toString()

    /** 原生 OkHttp 传输：POST {base}/api/<method>，返回原始响应体字符串。 */
    suspend fun postBody(method: String, frame: String): String =
        withContext(Dispatchers.IO) {
            val req = Request.Builder()
                .url("$base/api/$method")
                .addHeader("Accept", "application/json")
                .addHeader("X-Client", "DSH mobile/Android")
                .post(frame.toRequestBody(jsonType))
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    throw IOException("$method -> HTTP ${resp.code}: ${body.take(200)}")
                }
                body
            }
        }

    /** 解析原始 ServerResponse 帧，返回 result.value 或抛出异常（两种传输共用）。 */
    fun valueFromBody(method: String, body: String): JsonElement {
        val serverResponse = json.parseToJsonElement(body).jsonObject
        val result = serverResponse["result"]?.jsonObject
            ?: throw IOException("$method -> missing result envelope: ${body.take(200)}")
        val ok = result["ok"]?.jsonPrimitive?.content == "true"
        if (!ok) {
            val err = result["error"]
            throw IOException("$method -> rpc error: ${err ?: body.take(200)}")
        }
        return result["value"] ?: JsonObject(emptyMap())
    }

    /** 从 host 解析工作区列表（value 为 {items:[...]} / {workspaces:[...]} / 数组），逐项宽松解码。 */
    fun parseWorkspaces(elem: JsonElement): List<WorkspaceDto> {
        val arr = when {
            elem is JsonArray -> elem
            elem is JsonObject && elem["items"] is JsonArray -> elem["items"] as JsonArray
            elem is JsonObject && elem["workspaces"] is JsonArray -> elem["workspaces"] as JsonArray
            else -> JsonArray(emptyList())
        }
        return arr.mapNotNull { runCatching { json.decodeFromJsonElement<WorkspaceDto>(it) }.getOrNull() }
    }

    /** 从 host 解析会话列表（value 为 {items:[...]} / {sessions:[...]} / 数组），逐项宽松解码。 */
    fun parseSessions(elem: JsonElement): List<SessionDto> {
        val arr = when {
            elem is JsonArray -> elem
            elem is JsonObject && elem["items"] is JsonArray -> elem["items"] as JsonArray
            elem is JsonObject && elem["sessions"] is JsonArray -> elem["sessions"] as JsonArray
            else -> JsonArray(emptyList())
        }
        return arr.mapNotNull { runCatching { json.decodeFromJsonElement<SessionDto>(it) }.getOrNull() }
    }

    // ---------- 官方 RPC 方法名（与 rpc-map.ts 一致） ----------
    object Rpcs {
        const val workspaceList = "workspace.list"
        const val workspaceCreate = "workspace.create"
        const val workspaceRename = "workspace.rename"
        const val workspaceDelete = "workspace.delete"
        const val sessionList = "session.list"
        const val sessionCreate = "session.create"
        const val sessionRename = "session.rename"
        const val sessionHistory = "session.history"
        const val sessionPrompt = "session.prompt"
        const val sessionCancel = "session.cancel"
        const val hostDescribe = "host.describe"
    }
}

object RpcId {
    fun mint(): String = "rpc-" + UUID.randomUUID().toString()
}

// ---------- 业务 DTO（宽松，仅建模读取字段） ----------

@kotlinx.serialization.Serializable
data class WorkspaceDto(
    val workspaceId: String = "",
    val title: String? = null,
    val path: String? = null,
    val parentWorkspaceId: String? = null,
    val sessionIds: List<String> = emptyList()
)

@kotlinx.serialization.Serializable
data class SessionDto(
    val sessionId: String = "",
    val cwd: String? = null,
    val agentPreset: String? = null,
    val running: Boolean = false,
    val blank: Boolean = true,
    val updatedAt: Long? = null
)

@kotlinx.serialization.Serializable
data class SessionListResponse(
    val items: List<SessionDto> = emptyList(),
    val sessions: List<SessionDto> = emptyList()
)

/** session.prompt 入参。 */
fun promptPayload(text: String, attachments: List<String> = emptyList()): JsonObject =
    buildJsonObject {
        put("readOnly", false)
        put("attachments", kotlinx.serialization.json.JsonArray(attachments.map { kotlinx.serialization.json.JsonPrimitive(it) }))
    }