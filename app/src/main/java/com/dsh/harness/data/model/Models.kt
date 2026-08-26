package com.dsh.harness.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 访问模式。 */
@Serializable
enum class AccessMode(val key: String, val label: String) {
    @SerialName("read_only") READ_ONLY("read_only", "Read Only"),
    @SerialName("workspace_write") WORKSPACE_WRITE("workspace_write", "Workspace Write"),
    @SerialName("full_access") FULL_ACCESS("full_access", "Full access");

    companion object {
        fun fromKey(key: String?): AccessMode =
            values().firstOrNull { it.key == key } ?: FULL_ACCESS
    }
}

/** Agent 预设模式。 */
@Serializable
data class AgentPreset(
    val id: String,
    val name: String,
    val description: String,
    val code: String,
    val builtin: Boolean = true,
    val inUse: Boolean = false,
    val customizable: Boolean = true
) {
    companion object {
        /** 与 Web 端一致的内置预设。 */
        val BUILT_IN = listOf(
            AgentPreset(
                id = "standard",
                name = "标准模式",
                description = "功能完整的编码 Agent，支持文件编辑、Shell、文件与网页检索、Skills、计划、目标、子代理和工作流。",
                code = "standard",
                builtin = true,
                inUse = true
            ),
            AgentPreset(
                id = "code",
                name = "PTC 模式",
                description = "具备标准模式的全部能力，并通过 Code Mode SDK 呈现工具，让模型用一个 TypeScript 程序组合多步操作。",
                code = "code",
                builtin = true
            ),
            AgentPreset(
                id = "minimal",
                name = "极简模式",
                description = "仅提供持久 bash 与 str_replace_editor 的双工具编码 Agent。",
                code = "minimal",
                builtin = true
            ),
            AgentPreset(
                id = "cordis",
                name = "创造模式",
                description = "用于创建自定义 Agent preset：具备标准模式的全部能力，并提供运行时检查、插件实验和 preset 创作指导。",
                code = "cordis",
                builtin = true
            )
        )
    }
}

/** 工作区。 */
@Serializable
data class Workspace(
    val id: String,
    val name: String,
    val parentId: String? = null,
    val children: List<String> = emptyList(),
    val conversationIds: List<String> = emptyList(),
    val createdAt: Long = 0L
)

/** 会话（侧边栏列表项 + 顶部信息）。 */
@Serializable
data class Session(
    val id: String,
    val title: String,
    val workspaceId: String,
    val alias: String? = null,
    val agentPreset: String = "standard",
    val accessMode: AccessMode = AccessMode.FULL_ACCESS,
    val modelId: String = "doubao-seed-2-1-pro",
    val backgroundTaskCount: Int = 0,
    val updatedAt: Long = 0L,
    val createdAt: Long = 0L,
    val pinned: Boolean = false,
    val tags: List<String> = emptyList()
)

/** Tab 标签。 */
@Serializable
enum class SessionTab(val label: String) {
    DIALOG("对话"),
    TRACE("轨迹"),
    MEMORY("记忆"),
    SKILLS("技能"),
    TODOS("待办"),
    MEMORY_SYNC("记忆同步"),
    CANVAS("画板"),
    MODEL_SETTINGS("模型设置"),
    MEMORY_EVOLVE("Memory Evolve 设置")
}

/** 消息角色。 */
@Serializable
enum class MessageRole { USER, ASSISTANT, SYSTEM, TOOL }

/** 工具调用类型。 */
@Serializable
enum class ToolKind {
    EDIT, READ, BASH, THINK, TOOL_CALL, MEMORY, AGENT_TEAMS, COMPACT,
    EXPORT, FEEDBACK, GOAL, PERMISSION, PLAN, MODEL, ADVISOR, UNKNOWN
}

/** 工具调用记录。 */
@Serializable
data class ToolCall(
    val id: String,
    val kind: ToolKind,
    val title: String,
    val target: String? = null,
    val description: String? = null,
    val status: ToolStatus = ToolStatus.RUNNING,
    val durationMs: Long? = null,
    val failureReason: String? = null,
    val expanded: Boolean = false
)

@Serializable
enum class ToolStatus { RUNNING, SUCCESS, FAILED }

/** 会话中的一条消息。 */
@Serializable
data class ChatMessage(
    val id: String,
    val sessionId: String,
    val role: MessageRole,
    val content: String,
    val toolCalls: List<ToolCall> = emptyList(),
    val retryInfo: RetryInfo? = null,
    val createdAt: Long = 0L,
    val streaming: Boolean = false,
    val stopped: Boolean = false
)

/** 重试信息。 */
@Serializable
data class RetryInfo(
    val attempt: Int,
    val totalAttempts: Int,
    val delayMs: Long,
    val reason: String
)

/** 模型提供方。 */
@Serializable
data class ModelProvider(
    val id: String,
    val name: String,
    val code: String? = null,
    val custom: Boolean = false,
    val apiKeyConfigured: Boolean = false,
    val baseUrl: String? = null,
    val models: List<ModelInfo> = emptyList()
)

/** 模型元数据。 */
@Serializable
data class ModelInfo(
    val id: String,
    val name: String,
    val providerId: String,
    val family: String? = null,
    val routing: String? = null,
    val fallback: String? = null,
    val enabled: Boolean = true
)

/** 侧边栏卡片项配置。 */
@Serializable
data class SideCard(
    val id: String,
    val label: String,
    val tag: String,
    val description: String,
    val enabled: Boolean = true,
    val group: SideCardGroup
)

@Serializable
enum class SideCardGroup { SIDEBAR_CONTENT, FILE_PREVIEW }

/** 插件市场条目。 */
@Serializable
data class MarketPlugin(
    val id: String,
    val name: String,
    val author: String? = null,
    val kind: String,
    val stars: Int = 0,
    val installs: Int = 0,
    val description: String,
    val tags: List<String> = emptyList(),
    val recentlyUpdated: Boolean = false,
    val installed: Boolean = false,
    val favorited: Boolean = false,
    val hasUpdate: Boolean = false,
    val fromVersion: String? = null,
    val toVersion: String? = null,
    val repoUrl: String? = null,
    val githubBound: Boolean = false
)

/** 插件市场 Tab。 */
@Serializable
enum class MarketTab(val label: String) {
    RECOMMEND("推荐"),
    SEARCH("搜索"),
    BUNDLE("整合包"),
    FAVORITE("收藏"),
    INSTALLED("已装"),
    SETTINGS("设置")
}

/** 命令菜单项。 */
@Serializable
data class CommandItem(
    val id: String,
    val name: String,
    val description: String
)

object Commands {
    val ALL = listOf(
        CommandItem("advisor", "advisor", "Toggle, enable, disable, inspect, or instruct the per-session advisor"),
        CommandItem("agent-teams", "agent-teams", "run a goal with a multi-agent team (you become the captain)"),
        CommandItem("compact", "compact", "Compact older conversation history"),
        CommandItem("export", "export", "Download this Session log as a ZIP archive"),
        CommandItem("feedback", "feedback", "record feedback about this session"),
        CommandItem("goal", "goal", "set or view the goal for a long-running task"),
        CommandItem("memory_evolve_search_files", "memory_evolve_search_files", "启用/禁用/查看本地文档搜索工具（memory_evolve_search_local_docs）：on 启用，off 禁用，不带参数查看状态"),
        CommandItem("memory_review", "memory_review", "查看和管理记忆审查产生的建议：list 列出，approve <序号> 采纳，archive <序号> 归档（保留备查，可移回主记忆），reject <序号> 拒绝，approve-all / reject-all 批量处理"),
        CommandItem("permission", "permission", "Switch the permission preset (sandbox mode + approval policy)"),
        CommandItem("plan", "plan", "Enter or leave plan mode"),
        CommandItem("model", "model", "选择本会话使用的模型")
    )
}

/** 后台任务。 */
@Serializable
data class BackgroundTask(
    val id: String,
    val sessionId: String,
    val title: String,
    val status: BackgroundTaskStatus,
    val progress: Float = 0f
)

@Serializable
enum class BackgroundTaskStatus { PENDING, RUNNING, COMPLETED, FAILED, CANCELLED }

/** 待办（会话级）。 */
@Serializable
data class TodoItem(
    val id: String,
    val sessionId: String,
    val content: String,
    val status: TodoStatus,
    val priority: String = "medium"
)

@Serializable
enum class TodoStatus { PENDING, IN_PROGRESS, COMPLETED }

/** 记忆条目。 */
@Serializable
data class MemoryItem(
    val id: String,
    val sessionId: String,
    val content: String,
    val kind: String,
    val createdAt: Long = 0L,
    val archived: Boolean = false
)

/** 技能。 */
@Serializable
data class SkillItem(
    val id: String,
    val name: String,
    val description: String,
    val enabled: Boolean = true
)
