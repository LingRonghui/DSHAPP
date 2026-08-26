package com.dsh.harness.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 工作区表。 */
@Entity(tableName = "workspaces")
data class WorkspaceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val parentId: String?,
    val createdAt: Long
)

/** 会话表。 */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val workspaceId: String,
    val alias: String?,
    val agentPreset: String,
    val accessMode: String,
    val modelId: String,
    val backgroundTaskCount: Int,
    val updatedAt: Long,
    val createdAt: Long,
    val pinned: Boolean,
    /** 标签 JSON：List<String>，旧版本无此列时默认空串。 */
    val tagsJson: String = ""
)

/** 消息表。 */
@Entity(tableName = "messages", indices = [androidx.room.Index("sessionId")])
data class MessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val role: String,
    val content: String,
    val toolCallsJson: String,
    val retryJson: String?,
    val createdAt: Long,
    val streaming: Boolean,
    val stopped: Boolean
)

/** 提供方表。 */
@Entity(tableName = "providers")
data class ProviderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val code: String?,
    val custom: Boolean,
    val apiKeyConfigured: Boolean,
    val baseUrl: String?
)

/** 模型表。 */
@Entity(tableName = "models", indices = [androidx.room.Index("providerId")])
data class ModelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val providerId: String,
    val family: String?,
    val routing: String?,
    val fallback: String?,
    val enabled: Boolean
)

/** 插件表（市场缓存 + 已安装状态）。 */
@Entity(tableName = "plugins")
data class PluginEntity(
    @PrimaryKey val id: String,
    val name: String,
    val author: String?,
    val kind: String,
    val stars: Int,
    val installs: Int,
    val description: String,
    val tagsJson: String,
    val recentlyUpdated: Boolean,
    val installed: Boolean,
    val favorited: Boolean,
    val hasUpdate: Boolean,
    val fromVersion: String?,
    val toVersion: String?,
    val repoUrl: String?,
    val githubBound: Boolean
)

/** 侧边卡片表。 */
@Entity(tableName = "side_cards")
data class SideCardEntity(
    @PrimaryKey val id: String,
    val label: String,
    val tag: String,
    val description: String,
    val enabled: Boolean,
    val group: String
)

/** 待办表。 */
@Entity(tableName = "todos", indices = [androidx.room.Index("sessionId")])
data class TodoEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val content: String,
    val status: String,
    val priority: String
)

/** 记忆表。 */
@Entity(tableName = "memories", indices = [androidx.room.Index("sessionId")])
data class MemoryEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val content: String,
    val kind: String,
    val createdAt: Long,
    val archived: Boolean
)

/** 技能表。 */
@Entity(tableName = "skills")
data class SkillEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val name: String,
    val description: String,
    val enabled: Boolean
)
