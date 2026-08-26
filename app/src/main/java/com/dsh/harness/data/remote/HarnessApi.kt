package com.dsh.harness.data.remote

import com.dsh.harness.data.model.AccessMode
import com.dsh.harness.data.model.MarketPlugin
import com.dsh.harness.data.model.ModelInfo
import com.dsh.harness.data.model.ModelProvider
import com.dsh.harness.data.model.Session
import com.dsh.harness.data.model.Workspace
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 后端 API 契约（与 Web 端共享同一后端）。
 * 走 HTTPS + JSON，所有 DTO 使用 kotlinx.serialization。
 */
interface HarnessApi {

    // ---------- Workspace ----------
    @GET("api/workspaces")
    suspend fun listWorkspaces(): List<Workspace>

    @POST("api/workspaces")
    suspend fun createWorkspace(@Body req: CreateWorkspaceReq): Workspace

    // ---------- Sessions ----------
    @GET("api/sessions")
    suspend fun listSessions(@Query("workspace") workspace: String? = null): List<Session>

    @POST("api/sessions")
    suspend fun createSession(@Body req: CreateSessionReq): Session

    @GET("api/sessions/{id}")
    suspend fun getSession(@Path("id") id: String): Session

    @POST("api/sessions/{id}/alias")
    suspend fun renameSession(@Path("id") id: String, @Body req: RenameSessionReq): Session

    @POST("api/sessions/{id}/preset")
    suspend fun updatePreset(@Path("id") id: String, @Body req: UpdatePresetReq): Session

    @POST("api/sessions/{id}/access")
    suspend fun updateAccess(@Path("id") id: String, @Body req: UpdateAccessReq): Session

    @POST("api/sessions/{id}/model")
    suspend fun updateModel(@Path("id") id: String, @Body req: UpdateModelReq): Session

    // ---------- Messages ----------
    @GET("api/sessions/{id}/messages")
    suspend fun listMessages(
        @Path("id") id: String,
        @Query("before") before: String? = null,
        @Query("limit") limit: Int = 50
    ): MessagesPage

    @POST("api/sessions/{id}/messages")
    suspend fun sendMessage(
        @Path("id") id: String,
        @Body req: SendMessageReq,
        @Header("X-Stream") stream: String = "true"
    ): SendMessageResp

    @POST("api/sessions/{id}/stop")
    suspend fun stopSession(@Path("id") id: String)

    @POST("api/sessions/{id}/branch")
    suspend fun branchSession(@Path("id") id: String): Session

    @GET("api/sessions/{id}/export")
    suspend fun exportSession(@Path("id") id: String): ExportResp

    // ---------- Models ----------
    @GET("api/models/providers")
    suspend fun listProviders(): List<ModelProvider>

    @POST("api/models/providers")
    suspend fun addProvider(@Body req: ProviderReq): ModelProvider

    @POST("api/models/providers/{id}")
    suspend fun updateProvider(@Path("id") id: String, @Body req: ProviderReq): ModelProvider

    @DELETE("api/models/providers/{id}")
    suspend fun deleteProvider(@Path("id") id: String)

    @GET("api/models")
    suspend fun listModels(@Query("provider") provider: String? = null): List<ModelInfo>

    // ---------- Plugin Market ----------
    @GET("api/plugins/market")
    suspend fun listMarket(
        @Query("tab") tab: String,
        @Query("q") q: String? = null,
        @Query("mode") mode: String = "rookie"
    ): List<MarketPlugin>

    @POST("api/plugins/market/{id}/install")
    suspend fun installPlugin(@Path("id") id: String)

    @POST("api/plugins/market/{id}/favorite")
    suspend fun favoritePlugin(@Path("id") id: String, @Body req: FavoriteReq)

    @GET("api/plugins/installed")
    suspend fun listInstalledPlugins(): List<MarketPlugin>

    @GET("api/plugins/config")
    suspend fun getPluginConfig(): PluginConfig

    @POST("api/plugins/config/{id}")
    suspend fun updatePluginConfig(@Path("id") id: String, @Body req: PluginConfigItem)

    // ---------- Settings ----------
    @GET("api/settings")
    suspend fun getSettings(): HarnessSettings

    @POST("api/settings")
    suspend fun updateSettings(@Body req: HarnessSettings)

    @GET("api/sidecards")
    suspend fun listSideCards(): List<SideCardDto>

    @POST("api/sidecards/{id}")
    suspend fun updateSideCard(@Path("id") id: String, @Body req: SideCardUpdateReq)

    // ---------- Background tasks ----------
    @GET("api/sessions/{id}/tasks")
    suspend fun listBackgroundTasks(@Path("id") id: String): List<BackgroundTaskDto>
}

@Serializable
data class CreateWorkspaceReq(val name: String, val parentId: String? = null)

@Serializable
data class CreateSessionReq(
    val workspaceId: String,
    val title: String? = null,
    val preset: String = "standard",
    val accessMode: AccessMode = AccessMode.FULL_ACCESS,
    val modelId: String = "doubao-seed-2-1-pro"
)

@Serializable
data class RenameSessionReq(val alias: String)

@Serializable
data class UpdatePresetReq(val preset: String)

@Serializable
data class UpdateAccessReq(val accessMode: AccessMode)

@Serializable
data class UpdateModelReq(val modelId: String)

@Serializable
data class SendMessageReq(
    val content: String,
    @SerialName("command") val command: String? = null,
    @SerialName("attachments") val attachments: List<String> = emptyList()
)

@Serializable
data class SendMessageResp(val messageId: String, val streaming: Boolean = true)

@Serializable
data class MessagesPage(
    val messages: List<com.dsh.harness.data.model.ChatMessage> = emptyList(),
    val hasMore: Boolean = false,
    val nextCursor: String? = null
)

@Serializable
data class ExportResp(val url: String, val size: Long)

@Serializable
data class ProviderReq(
    val name: String,
    val code: String? = null,
    val baseUrl: String? = null,
    val apiKey: String? = null,
    val custom: Boolean = false
)

@Serializable
data class FavoriteReq(val favorited: Boolean)

@Serializable
data class PluginConfig(
    val items: List<PluginConfigItem> = emptyList()
)

@Serializable
data class PluginConfigItem(
    val id: String,
    val enabled: Boolean = true,
    val settings: Map<String, String> = emptyMap()
)

@Serializable
data class HarnessSettings(
    val language: String = "zh-CN",
    val theme: String = "system",
    val defaultPreset: String = "standard",
    val defaultAccess: AccessMode = AccessMode.FULL_ACCESS,
    val busyEnterBehavior: String = "queue",
    val sideCardDefaultOpen: Boolean = true,
    val sideCardWidthPercent: Int = 35,
    val openFileInSideCard: Boolean = true,
    val positionCompatMode: Boolean = false
)

@Serializable
data class SideCardDto(
    val id: String,
    val label: String,
    val tag: String,
    val description: String,
    val enabled: Boolean,
    val group: String
)

@Serializable
data class SideCardUpdateReq(val enabled: Boolean)

@Serializable
data class BackgroundTaskDto(
    val id: String,
    val title: String,
    val status: String,
    val progress: Float = 0f
)
