package com.dsh.harness.data.repository

import com.dsh.harness.data.local.Mappers.toEntity
import com.dsh.harness.data.local.Mappers.toModel
import com.dsh.harness.data.local.MessageDao
import com.dsh.harness.data.local.MemoryDao
import com.dsh.harness.data.local.ModelDao
import com.dsh.harness.data.local.PluginDao
import com.dsh.harness.data.local.ProviderDao
import com.dsh.harness.data.local.SessionDao
import com.dsh.harness.data.local.SideCardDao
import com.dsh.harness.data.local.SkillDao
import com.dsh.harness.data.local.TodoDao
import com.dsh.harness.data.local.WorkspaceDao
import com.dsh.harness.data.model.AccessMode
import com.dsh.harness.data.model.BackgroundTaskStatus
import com.dsh.harness.data.model.ChatMessage
import com.dsh.harness.data.model.Commands
import com.dsh.harness.data.model.MarketPlugin
import com.dsh.harness.data.model.MemoryItem
import com.dsh.harness.data.model.MessageRole
import com.dsh.harness.data.model.ModelInfo
import com.dsh.harness.data.model.ModelProvider
import com.dsh.harness.data.model.RetryInfo
import com.dsh.harness.data.model.Session
import com.dsh.harness.data.model.SideCard
import com.dsh.harness.data.model.SideCardGroup
import com.dsh.harness.data.model.SkillItem
import com.dsh.harness.data.model.TodoItem
import com.dsh.harness.data.model.ToolCall
import com.dsh.harness.data.model.ToolKind
import com.dsh.harness.data.model.ToolStatus
import com.dsh.harness.data.model.Workspace
import com.dsh.harness.data.remote.BackgroundTaskDto
import com.dsh.harness.data.remote.CreateSessionReq
import com.dsh.harness.data.remote.CreateWorkspaceReq
import com.dsh.harness.data.remote.DshRpcClient
import com.dsh.harness.data.remote.HarnessApi
import com.dsh.harness.data.remote.HarnessSettings
import com.dsh.harness.data.remote.ProviderReq
import com.dsh.harness.data.remote.RenameSessionReq
import com.dsh.harness.data.remote.SendMessageReq
import com.dsh.harness.data.remote.ServerTransport
import com.dsh.harness.data.remote.UpdateAccessReq
import com.dsh.harness.data.remote.UpdateModelReq
import com.dsh.harness.data.remote.UpdatePresetReq
import com.dsh.harness.data.remote.WebViewServerClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEmpty
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 统一数据访问层：
 * - 远程 API 优先；网络失败回退本地缓存；
 * - 列表写入本地缓存，UI 通过 Flow 订阅本地数据，保证离线可读。
 */
@Singleton
class HarnessRepository @Inject constructor(
    private val api: HarnessApi,
    private val dsh: DshRpcClient,
    private val web: WebViewServerClient,
    private val workspaceDao: WorkspaceDao,
    private val sessionDao: SessionDao,
    private val messageDao: MessageDao,
    private val providerDao: ProviderDao,
    private val modelDao: ModelDao,
    private val pluginDao: PluginDao,
    private val sideCardDao: SideCardDao,
    private val todoDao: TodoDao,
    private val memoryDao: MemoryDao,
    private val skillDao: SkillDao
) {
    // ---------- Workspaces ----------
    fun observeWorkspaces(): Flow<List<Workspace>> =
        workspaceDao.observeAll().map { list -> list.map { it.toModel() } }

    /**
     * 统一真实宿主 RPC：按 ServerTransport.useWebView 选择 传输层（WebView 或 OkHttp），
     * 响应解析一律复用 DshRpcClient.valueFromBody，保证两种传输结果一致。
     */
    private suspend fun rpcValue(method: String, payload: JsonObject = buildJsonObject {}): JsonElement {
        val frame = dsh.buildFrame(method, payload)
        val body = if (ServerTransport.useWebView) {
            web.setOrigin(dsh.baseUrl())
            web.post(method, frame)
        } else {
            dsh.postBody(method, frame)
        }
        return dsh.valueFromBody(method, body)
    }

    /** 真实宿主协议拉取工作区；成功返回 null，失败返回用户可读的错误信息（供 UI 展示）。 */
    suspend fun refreshWorkspaces(): String? {
        val dshError = try {
            val value = rpcValue(DshRpcClient.Rpcs.workspaceList)
            val list = dsh.parseWorkspaces(value)
            if (list.isNotEmpty()) {
                workspaceDao.upsertAll(list.map {
                    com.dsh.harness.data.local.WorkspaceEntity(
                        id = it.workspaceId,
                        name = it.title ?: it.path ?: "",
                        parentId = it.parentWorkspaceId,
                        createdAt = 0L
                    )
                })
            }
            null
        } catch (e: Exception) {
            e.message ?: "网络错误"
        }
        if (dshError == null) return null
        // 回退：旧式接口兜底 + 向 UI 报告真实失败原因
        safe {
            val list = api.listWorkspaces()
            if (list.isNotEmpty()) {
                workspaceDao.upsertAll(list.map {
                    com.dsh.harness.data.local.WorkspaceEntity(it.id, it.name, it.parentId, it.createdAt)
                })
            }
        }
        return dshError
    }

    /** 切换真实宿主服务器地址（设置页改地址时调用），同步到两种传输，并返回当前生效地址。 */
    fun applyServerUrl(url: String?) {
        dsh.setBaseUrl(url)
        web.setOrigin(url)
    }
    fun currentServerUrl(): String = dsh.baseUrl()

    suspend fun createWorkspace(name: String, parentId: String? = null): Workspace? = safe {
        val ws = api.createWorkspace(CreateWorkspaceReq(name, parentId))
        workspaceDao.upsertAll(
            listOf(com.dsh.harness.data.local.WorkspaceEntity(ws.id, ws.name, ws.parentId, ws.createdAt))
        )
        ws
    }

    suspend fun deleteWorkspace(id: String) {
        workspaceDao.delete(id)
        safe { /* api.deleteWorkspace(id) */ }
    }

    // ---------- Sessions ----------
    fun observeSessions(): Flow<List<Session>> =
        sessionDao.observeAll().map { list -> list.map { it.toModel() } }

    fun observeSessionsByWorkspace(workspaceId: String): Flow<List<Session>> =
        sessionDao.observeByWorkspace(workspaceId).map { list -> list.map { it.toModel() } }

    fun searchSessions(query: String): Flow<List<Session>> =
        sessionDao.search("%${query}%").map { list -> list.map { it.toModel() } }

    suspend fun refreshSessions(workspaceId: String? = null) = safe {
        val list = api.listSessions(workspaceId)
        sessionDao.upsertAll(list.map { it.toEntity() })
    }

    suspend fun createSession(
        workspaceId: String,
        title: String? = null,
        preset: String = "standard",
        access: AccessMode = AccessMode.FULL_ACCESS,
        modelId: String = "doubao-seed-2-1-pro"
    ): Session? = safe {
        val s = api.createSession(CreateSessionReq(workspaceId, title, preset, access, modelId))
        sessionDao.upsert(s.toEntity())
        s
    } ?: createLocalSession(workspaceId, title, preset, access, modelId)

    /** 本地兜底创建：网络不可用时仍可打开会话。 */
    private suspend fun createLocalSession(
        workspaceId: String, title: String?, preset: String,
        access: AccessMode, modelId: String
    ): Session {
        val now = System.currentTimeMillis()
        val s = Session(
            id = UUID.randomUUID().toString(),
            title = title ?: "新会话",
            workspaceId = workspaceId,
            agentPreset = preset, accessMode = access, modelId = modelId,
            backgroundTaskCount = 0, updatedAt = now, createdAt = now
        )
        sessionDao.upsert(s.toEntity())
        return s
    }

    suspend fun renameSession(id: String, alias: String) {
        sessionDao.alias(id, alias)
        safe { api.renameSession(id, RenameSessionReq(alias)) }
    }

    suspend fun updatePreset(id: String, preset: String) {
        sessionDao.preset(id, preset)
        safe { api.updatePreset(id, UpdatePresetReq(preset)) }
    }

    suspend fun updateAccess(id: String, mode: AccessMode) {
        sessionDao.access(id, mode.key)
        safe { api.updateAccess(id, UpdateAccessReq(mode)) }
    }

    suspend fun updateModel(id: String, modelId: String) {
        sessionDao.model(id, modelId)
        safe { api.updateModel(id, UpdateModelReq(modelId)) }
    }

    suspend fun pinSession(id: String, pinned: Boolean) {
        sessionDao.pin(id, pinned)
    }

    suspend fun deleteSession(id: String) {
        messageDao.clear(id)
        sessionDao.delete(id)
        safe { /* api.deleteSession(id) */ }
    }

    suspend fun branchSession(id: String): Session? = safe { api.branchSession(id) }

    // ---------- Messages ----------
    fun observeMessages(sessionId: String): Flow<List<ChatMessage>> =
        messageDao.observeBySession(sessionId).map { list -> list.map { it.toModel() } }

    suspend fun loadMoreMessages(sessionId: String, before: Long, limit: Int = 50): List<ChatMessage> = safe {
        val page = api.listMessages(sessionId, before.toString(), limit)
        messageDao.upsertAll(page.messages.map { it.toEntity() })
        page.messages
    } ?: messageDao.page(sessionId, before, limit).map { it.toModel() }

    suspend fun sendMessage(sessionId: String, content: String, command: String? = null): ChatMessage {
        val now = System.currentTimeMillis()
        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            role = MessageRole.USER,
            content = content,
            createdAt = now
        )
        messageDao.upsert(userMsg.toEntity())
        // 创建占位助手消息
        val assistantId = UUID.randomUUID().toString()
        val placeholder = ChatMessage(
            id = assistantId,
            sessionId = sessionId,
            role = MessageRole.ASSISTANT,
            content = "",
            toolCalls = emptyList(),
            createdAt = System.currentTimeMillis(),
            streaming = true
        )
        messageDao.upsert(placeholder.toEntity())
        safe {
            val resp = api.sendMessage(sessionId, SendMessageReq(content, command))
            // 流式响应由 streamChannel 单独推送
            if (!resp.streaming) {
                messageDao.finalize(assistantId, "")
            }
        }
        return placeholder
    }

    /** SSE/轮询回写流式增量。StreamService 直接通过 DAO 写入累积文本。 */
    suspend fun appendAssistantChunk(messageId: String, chunk: String) {
        // 占位：流式由 StreamService 直接更新 DAO 完成
    }

    suspend fun finalizeAssistant(messageId: String, content: String) {
        messageDao.finalize(messageId, content)
    }

    suspend fun stopSession(sessionId: String) {
        safe { api.stopSession(sessionId) }
    }

    suspend fun exportSession(sessionId: String): String? = safe {
        api.exportSession(sessionId).url
    }

    // ---------- Models ----------
    fun observeProviders(): Flow<List<ModelProvider>> =
        providerDao.observeAll().map { list -> list.map { it.toModel(emptyList()) } }

    suspend fun refreshProviders() = safe {
        val list = api.listProviders()
        providerDao.upsertAll(list.map {
            com.dsh.harness.data.local.ProviderEntity(
                it.id, it.name, it.code, it.custom, it.apiKeyConfigured, it.baseUrl
            )
        })
        list.forEach { p ->
            if (p.models.isNotEmpty()) {
                modelDao.deleteByProvider(p.id)
                modelDao.upsertAll(p.models.map {
                    com.dsh.harness.data.local.ModelEntity(
                        it.id, it.name, it.providerId, it.family, it.routing, it.fallback, it.enabled
                    )
                })
            }
        }
    }

    fun observeModels(): Flow<List<ModelInfo>> =
        modelDao.observeAll().map { list -> list.map { it.toModel() } }

    suspend fun addProvider(req: ProviderReq): ModelProvider? = safe {
        val p = api.addProvider(req)
        providerDao.upsert(
            com.dsh.harness.data.local.ProviderEntity(
                p.id, p.name, p.code, p.custom, p.apiKeyConfigured, p.baseUrl
            )
        )
        p
    }

    suspend fun updateProvider(id: String, req: ProviderReq) = safe {
        val p = api.updateProvider(id, req)
        providerDao.upsert(
            com.dsh.harness.data.local.ProviderEntity(
                p.id, p.name, p.code, p.custom, p.apiKeyConfigured, p.baseUrl
            )
        )
    }

    suspend fun deleteProvider(id: String) {
        providerDao.delete(id)
        modelDao.deleteByProvider(id)
        safe { /* api.deleteProvider(id) */ }
    }

    // ---------- Plugins ----------
    fun observePlugins(): Flow<List<MarketPlugin>> =
        pluginDao.observeAll().map { list -> list.map { it.toModel() } }

    fun observeInstalledPlugins(): Flow<List<MarketPlugin>> =
        pluginDao.observeInstalled().map { list -> list.map { it.toModel() } }

    fun observeFavoritePlugins(): Flow<List<MarketPlugin>> =
        pluginDao.observeFavorites().map { list -> list.map { it.toModel() } }

    fun searchPlugins(q: String): Flow<List<MarketPlugin>> =
        pluginDao.search("%${q}%").map { list -> list.map { it.toModel() } }

    suspend fun refreshMarket(tab: String, q: String? = null, rookie: Boolean = true) = safe {
        val list = api.listMarket(tab, q, if (rookie) "rookie" else "personal")
        pluginDao.upsertAll(list.map { it.toEntity() })
    }

    suspend fun installPlugin(id: String) {
        pluginDao.setInstalled(id, true)
        safe { api.installPlugin(id) }
    }

    suspend fun favoritePlugin(id: String, fav: Boolean) {
        pluginDao.setFavorite(id, fav)
        safe { api.favoritePlugin(id, com.dsh.harness.data.remote.FavoriteReq(fav)) }
    }

    suspend fun ignorePluginUpdate(id: String) {
        pluginDao.clearUpdate(id)
    }

    // ---------- Settings ----------
    suspend fun getSettings(): HarnessSettings? = safe { api.getSettings() }

    suspend fun updateSettings(settings: HarnessSettings) = safe { api.updateSettings(settings) }

    // ---------- Side cards ----------
    fun observeSideCards(): Flow<List<SideCard>> =
        sideCardDao.observeAll().map { list -> list.map { it.toModel() } }

    suspend fun refreshSideCards() = safe {
        val list = api.listSideCards()
        sideCardDao.upsertAll(list.map {
            com.dsh.harness.data.local.SideCardEntity(
                it.id, it.label, it.tag, it.description, it.enabled, it.group
            )
        })
    }

    suspend fun setSideCardEnabled(id: String, enabled: Boolean) {
        sideCardDao.setEnabled(id, enabled)
        safe { api.updateSideCard(id, com.dsh.harness.data.remote.SideCardUpdateReq(enabled)) }
    }

    /** 默认侧边卡片清单（与 Web 端一致）。 */
    suspend fun seedSideCardsIfEmpty() {
        // 直接 upsert 全部，幂等
        val defaults = listOf(
            com.dsh.harness.data.local.SideCardEntity("scm", "源代码管理", "git", "Git 提交/分支/Diff", true, SideCardGroup.SIDEBAR_CONTENT.name),
            com.dsh.harness.data.local.SideCardEntity("task", "任务管理", "subagent", "子代理任务执行", true, SideCardGroup.SIDEBAR_CONTENT.name),
            com.dsh.harness.data.local.SideCardEntity("terminal", "终端", "terminal", "持久 Shell 终端", true, SideCardGroup.SIDEBAR_CONTENT.name),
            com.dsh.harness.data.local.SideCardEntity("browser", "浏览器", "browser", "Agent 浏览器视图", true, SideCardGroup.SIDEBAR_CONTENT.name),
            com.dsh.harness.data.local.SideCardEntity("editor", "文件", "editor", "代码与文本编辑器", true, SideCardGroup.SIDEBAR_CONTENT.name),
            com.dsh.harness.data.local.SideCardEntity("diff", "源代码管理", "diff", "Diff 比对视图", true, SideCardGroup.SIDEBAR_CONTENT.name),
            com.dsh.harness.data.local.SideCardEntity("img", "图片", "png · jpg · jpeg · gif · webp · svg · bmp · ico · avif", "图片预览", true, SideCardGroup.FILE_PREVIEW.name),
            com.dsh.harness.data.local.SideCardEntity("pdf", "PDF", "pdf", "PDF 文档预览", true, SideCardGroup.FILE_PREVIEW.name),
            com.dsh.harness.data.local.SideCardEntity("md", "Markdown", "md · markdown", "Markdown 渲染预览", true, SideCardGroup.FILE_PREVIEW.name),
            com.dsh.harness.data.local.SideCardEntity("html", "HTML", "html · htm", "HTML 网页预览", true, SideCardGroup.FILE_PREVIEW.name),
            com.dsh.harness.data.local.SideCardEntity("bin", "二进制下载", "doc · xls · ppt", "二进制文件下载", true, SideCardGroup.FILE_PREVIEW.name),
            com.dsh.harness.data.local.SideCardEntity("code", "代码", "兜底：任意文件", "任意代码文件预览", true, SideCardGroup.FILE_PREVIEW.name)
        )
        sideCardDao.upsertAll(defaults)
    }

    // ---------- Background tasks ----------
    suspend fun listBackgroundTasks(sessionId: String): List<BackgroundTaskDto> = safe {
        api.listBackgroundTasks(sessionId)
    } ?: emptyList()

    // ---------- Todos ----------
    fun observeTodos(sessionId: String): Flow<List<TodoItem>> =
        todoDao.observe(sessionId).map { list -> list.map { it.toModel() } }

    // ---------- Memories ----------
    fun observeMemories(sessionId: String): Flow<List<MemoryItem>> =
        memoryDao.observe(sessionId).map { list -> list.map { it.toModel() } }

    // ---------- Skills ----------
    fun observeSkills(sessionId: String): Flow<List<SkillItem>> =
        skillDao.observe(sessionId).map { list -> list.map { it.toModel() } }

    // ---------- Commands ----------
    fun listCommands() = Commands.ALL

    // ---------- 默认内置提供方/模型/插件（首次启动种子）----------
    suspend fun seedDefaultsIfEmpty() {
        seedProvidersIfEmpty()
        seedSideCardsIfEmpty()
    }

    private suspend fun seedProvidersIfEmpty() {
        // 默认清单（幂等 upsert）
        val defaults = listOf(
            com.dsh.harness.data.local.ProviderEntity("deepseek", "DeepSeek", "deepseek-official", false, true, null),
            com.dsh.harness.data.local.ProviderEntity("opencode", "opencode", null, false, true, null),
            com.dsh.harness.data.local.ProviderEntity("zhipu", "智普", "zhipu", true, true, null),
            com.dsh.harness.data.local.ProviderEntity("freellmapi", "汇流箱", "freellmapi", true, true, null),
            com.dsh.harness.data.local.ProviderEntity("guijiliudong", "硅基流动", "guijiliudong", true, true, null),
            com.dsh.harness.data.local.ProviderEntity("qianwen", "千问", "qianwen", true, true, null),
            com.dsh.harness.data.local.ProviderEntity("huoshan", "火山方舟", "huoshan", true, true, null)
        )
        providerDao.upsertAll(defaults)
        val models = listOf(
            com.dsh.harness.data.local.ModelEntity("doubao-seed-2-1-pro", "doubao-seed-2-1-pro", "huoshan", "doubao", null, null, true),
            com.dsh.harness.data.local.ModelEntity("deepseek-chat", "deepseek-chat", "deepseek", "deepseek", null, null, true),
            com.dsh.harness.data.local.ModelEntity("glm-4", "glm-4", "zhipu", "glm", null, null, true),
            com.dsh.harness.data.local.ModelEntity("moonshot-v1-8k", "moonshot-v1-8k", "freellmapi", "kimi", null, null, true),
            com.dsh.harness.data.local.ModelEntity("qwen-max", "qwen-max", "qianwen", "qwen", null, null, true),
            com.dsh.harness.data.local.ModelEntity("DeepSeek-V4-Flash", "DeepSeek-V4-Flash", "guijiliudong", "deepseek", null, null, true),
            com.dsh.harness.data.local.ModelEntity("GLM-5.2", "GLM-5.2", "guijiliudong", "glm", null, null, true),
            com.dsh.harness.data.local.ModelEntity("Kimi-K2.7-Code", "Kimi-K2.7-Code", "guijiliudong", "kimi", null, null, true)
        )
        modelDao.upsertAll(models)
    }

    /** 安全包裹：失败返回 null，避免网络异常阻断 UI。 */
    private suspend fun <T> safe(block: suspend () -> T): T? = try {
        block()
    } catch (e: Throwable) {
        null
    }
}

/** Flow 扩展：空时返回空列表。 */
fun <T> Flow<List<T>>.onEmptyDefault(): Flow<List<T>> = onEmpty { emit(emptyList()) }
