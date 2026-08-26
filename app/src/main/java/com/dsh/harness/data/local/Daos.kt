package com.dsh.harness.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDao {
    @Query("SELECT * FROM workspaces ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<WorkspaceEntity>>

    @Query("SELECT * FROM workspaces ORDER BY createdAt ASC")
    suspend fun all(): List<WorkspaceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<WorkspaceEntity>)

    @Query("DELETE FROM workspaces WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE workspaceId = :workspaceId ORDER BY updatedAt DESC")
    fun observeByWorkspace(workspaceId: String): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun get(id: String): SessionEntity?

    @Query("SELECT * FROM sessions WHERE title LIKE :q OR alias LIKE :q ORDER BY updatedAt DESC")
    fun search(q: String): Flow<List<SessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: SessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<SessionEntity>)

    @Query("UPDATE sessions SET title = :title WHERE id = :id")
    suspend fun rename(id: String, title: String)

    @Query("UPDATE sessions SET alias = :alias WHERE id = :id")
    suspend fun alias(id: String, alias: String?)

    @Query("UPDATE sessions SET agentPreset = :preset WHERE id = :id")
    suspend fun preset(id: String, preset: String)

    @Query("UPDATE sessions SET accessMode = :mode WHERE id = :id")
    suspend fun access(id: String, mode: String)

    @Query("UPDATE sessions SET modelId = :model WHERE id = :id")
    suspend fun model(id: String, model: String)

    @Query("UPDATE sessions SET pinned = :pinned WHERE id = :id")
    suspend fun pin(id: String, pinned: Boolean)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY createdAt ASC LIMIT :limit")
    fun observeBySession(sessionId: String, limit: Int = 200): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId AND createdAt < :before ORDER BY createdAt DESC LIMIT :limit")
    suspend fun page(sessionId: String, before: Long, limit: Int = 50): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<MessageEntity>)

    @Query("UPDATE messages SET streaming = 0, content = :content WHERE id = :id")
    suspend fun finalize(id: String, content: String)

    @Query("DELETE FROM messages WHERE sessionId = :sessionId")
    suspend fun clear(sessionId: String)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface ProviderDao {
    @Query("SELECT * FROM providers")
    fun observeAll(): Flow<List<ProviderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ProviderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ProviderEntity)

    @Query("DELETE FROM providers WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface ModelDao {
    @Query("SELECT * FROM models")
    fun observeAll(): Flow<List<ModelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ModelEntity>)

    @Query("DELETE FROM models WHERE providerId = :providerId")
    suspend fun deleteByProvider(providerId: String)
}

@Dao
interface PluginDao {
    @Query("SELECT * FROM plugins")
    fun observeAll(): Flow<List<PluginEntity>>

    @Query("SELECT * FROM plugins WHERE installed = 1")
    fun observeInstalled(): Flow<List<PluginEntity>>

    @Query("SELECT * FROM plugins WHERE favorited = 1")
    fun observeFavorites(): Flow<List<PluginEntity>>

    @Query("SELECT * FROM plugins WHERE name LIKE :q OR description LIKE :q")
    fun search(q: String): Flow<List<PluginEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<PluginEntity>)

    @Query("UPDATE plugins SET installed = :installed WHERE id = :id")
    suspend fun setInstalled(id: String, installed: Boolean)

    @Query("UPDATE plugins SET favorited = :fav WHERE id = :id")
    suspend fun setFavorite(id: String, fav: Boolean)

    @Query("UPDATE plugins SET hasUpdate = 0, fromVersion = NULL, toVersion = NULL WHERE id = :id")
    suspend fun clearUpdate(id: String)
}

@Dao
interface SideCardDao {
    @Query("SELECT * FROM side_cards")
    fun observeAll(): Flow<List<SideCardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<SideCardEntity>)

    @Query("UPDATE side_cards SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)
}

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos WHERE sessionId = :sessionId")
    fun observe(sessionId: String): Flow<List<TodoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<TodoEntity>)

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories WHERE sessionId = :sessionId ORDER BY createdAt DESC")
    fun observe(sessionId: String): Flow<List<MemoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<MemoryEntity>)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface SkillDao {
    @Query("SELECT * FROM skills WHERE sessionId = :sessionId")
    fun observe(sessionId: String): Flow<List<SkillEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<SkillEntity>)
}
