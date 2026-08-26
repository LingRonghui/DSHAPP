package com.dsh.harness.data.local

import com.dsh.harness.data.model.AccessMode
import com.dsh.harness.data.model.BackgroundTaskStatus
import com.dsh.harness.data.model.ChatMessage
import com.dsh.harness.data.model.MarketPlugin
import com.dsh.harness.data.model.MemoryItem
import com.dsh.harness.data.model.ModelInfo
import com.dsh.harness.data.model.ModelProvider
import com.dsh.harness.data.model.RetryInfo
import com.dsh.harness.data.model.Session
import com.dsh.harness.data.model.SideCard
import com.dsh.harness.data.model.SideCardGroup
import com.dsh.harness.data.model.SkillItem
import com.dsh.harness.data.model.TodoItem
import com.dsh.harness.data.model.TodoStatus
import com.dsh.harness.data.model.ToolCall
import com.dsh.harness.data.model.ToolKind
import com.dsh.harness.data.model.ToolStatus
import com.dsh.harness.data.model.Workspace
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Entity ↔ Model 转换 + 工具调用 JSON 序列化。 */
object Mappers {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }
    private val toolListSerializer = ListSerializer(ToolCall.serializer())
    private val retrySerializer = RetryInfo.serializer()

    fun WorkspaceEntity.toModel(): Workspace = Workspace(
        id = id, name = name, parentId = parentId, createdAt = createdAt
    )

    fun SessionEntity.toModel(): Session = Session(
        id = id, title = title, workspaceId = workspaceId, alias = alias,
        agentPreset = agentPreset, accessMode = AccessMode.fromKey(accessMode),
        modelId = modelId, backgroundTaskCount = backgroundTaskCount,
        updatedAt = updatedAt, createdAt = createdAt, pinned = pinned
    )

    fun Session.toEntity(): SessionEntity = SessionEntity(
        id = id, title = title, workspaceId = workspaceId, alias = alias,
        agentPreset = agentPreset, accessMode = accessMode.key,
        modelId = modelId, backgroundTaskCount = backgroundTaskCount,
        updatedAt = updatedAt, createdAt = createdAt, pinned = pinned
    )

    fun MessageEntity.toModel(): ChatMessage = ChatMessage(
        id = id, sessionId = sessionId, role = role.let {
            when (it.uppercase()) {
                "USER" -> com.dsh.harness.data.model.MessageRole.USER
                "ASSISTANT" -> com.dsh.harness.data.model.MessageRole.ASSISTANT
                "SYSTEM" -> com.dsh.harness.data.model.MessageRole.SYSTEM
                else -> com.dsh.harness.data.model.MessageRole.TOOL
            }
        },
        content = content,
        toolCalls = runCatching { json.decodeFromString(toolListSerializer, toolCallsJson) }.getOrDefault(emptyList()),
        retryInfo = retryJson?.let { runCatching { json.decodeFromString(retrySerializer, it) }.getOrNull() },
        createdAt = createdAt, streaming = streaming, stopped = stopped
    )

    fun ChatMessage.toEntity(): MessageEntity = MessageEntity(
        id = id, sessionId = sessionId, role = role.name.lowercase(),
        content = content,
        toolCallsJson = json.encodeToString(toolListSerializer, toolCalls),
        retryJson = retryInfo?.let { json.encodeToString(retrySerializer, it) },
        createdAt = createdAt, streaming = streaming, stopped = stopped
    )

    fun ProviderEntity.toModel(models: List<ModelEntity>): ModelProvider = ModelProvider(
        id = id, name = name, code = code, custom = custom,
        apiKeyConfigured = apiKeyConfigured, baseUrl = baseUrl,
        models = models.map { it.toModel() }
    )

    fun ModelEntity.toModel(): ModelInfo = ModelInfo(
        id = id, name = name, providerId = providerId,
        family = family, routing = routing, fallback = fallback, enabled = enabled
    )

    fun PluginEntity.toModel(): MarketPlugin = MarketPlugin(
        id = id, name = name, author = author, kind = kind, stars = stars,
        installs = installs, description = description,
        tags = runCatching { json.decodeFromString<List<String>>(tagsJson) }.getOrDefault(emptyList()),
        recentlyUpdated = recentlyUpdated, installed = installed, favorited = favorited,
        hasUpdate = hasUpdate, fromVersion = fromVersion, toVersion = toVersion,
        repoUrl = repoUrl, githubBound = githubBound
    )

    fun MarketPlugin.toEntity(): PluginEntity = PluginEntity(
        id = id, name = name, author = author, kind = kind, stars = stars,
        installs = installs, description = description,
        tagsJson = json.encodeToString(tags),
        recentlyUpdated = recentlyUpdated, installed = installed, favorited = favorited,
        hasUpdate = hasUpdate, fromVersion = fromVersion, toVersion = toVersion,
        repoUrl = repoUrl, githubBound = githubBound
    )

    fun SideCardEntity.toModel(): SideCard = SideCard(
        id = id, label = label, tag = tag, description = description, enabled = enabled,
        group = SideCardGroup.valueOf(group)
    )

    fun TodoEntity.toModel(): TodoItem = TodoItem(
        id = id, sessionId = sessionId, content = content,
        status = TodoStatus.valueOf(status), priority = priority
    )

    fun MemoryEntity.toModel(): MemoryItem = MemoryItem(
        id = id, sessionId = sessionId, content = content,
        kind = kind, createdAt = createdAt, archived = archived
    )

    fun SkillEntity.toModel(): SkillItem = SkillItem(
        id = id, name = name, description = description, enabled = enabled
    )

    /** 根据 Web 端约定解析工具种类。 */
    fun parseToolKind(text: String): ToolKind = when (text.lowercase()) {
        "edit" -> ToolKind.EDIT
        "read" -> ToolKind.READ
        "bash" -> ToolKind.BASH
        "think" -> ToolKind.THINK
        "tool call" -> ToolKind.TOOL_CALL
        "memory · add", "memory" -> ToolKind.MEMORY
        "agent-teams" -> ToolKind.AGENT_TEAMS
        "compact" -> ToolKind.COMPACT
        "export" -> ToolKind.EXPORT
        "feedback" -> ToolKind.FEEDBACK
        "goal" -> ToolKind.GOAL
        "permission" -> ToolKind.PERMISSION
        "plan" -> ToolKind.PLAN
        "model" -> ToolKind.MODEL
        "advisor" -> ToolKind.ADVISOR
        else -> ToolKind.UNKNOWN
    }

    fun parseTaskStatus(text: String): BackgroundTaskStatus = when (text.lowercase()) {
        "pending" -> BackgroundTaskStatus.PENDING
        "running" -> BackgroundTaskStatus.RUNNING
        "completed" -> BackgroundTaskStatus.COMPLETED
        "failed" -> BackgroundTaskStatus.FAILED
        else -> BackgroundTaskStatus.CANCELLED
    }

    fun parseTodoStatus(text: String): TodoStatus = when (text.lowercase()) {
        "pending" -> TodoStatus.PENDING
        "in_progress" -> TodoStatus.IN_PROGRESS
        else -> TodoStatus.COMPLETED
    }
}
