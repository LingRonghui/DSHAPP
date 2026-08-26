package com.dsh.harness.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        WorkspaceEntity::class,
        SessionEntity::class,
        MessageEntity::class,
        ProviderEntity::class,
        ModelEntity::class,
        PluginEntity::class,
        SideCardEntity::class,
        TodoEntity::class,
        MemoryEntity::class,
        SkillEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class HarnessDatabase : RoomDatabase() {
    abstract fun workspaceDao(): WorkspaceDao
    abstract fun sessionDao(): SessionDao
    abstract fun messageDao(): MessageDao
    abstract fun providerDao(): ProviderDao
    abstract fun modelDao(): ModelDao
    abstract fun pluginDao(): PluginDao
    abstract fun sideCardDao(): SideCardDao
    abstract fun todoDao(): TodoDao
    abstract fun memoryDao(): MemoryDao
    abstract fun skillDao(): SkillDao
}
