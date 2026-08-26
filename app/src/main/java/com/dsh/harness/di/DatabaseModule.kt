package com.dsh.harness.di

import android.content.Context
import androidx.room.Room
import com.dsh.harness.data.local.HarnessDatabase
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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Room 数据库与各 DAO 提供。 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HarnessDatabase =
        Room.databaseBuilder(context, HarnessDatabase::class.java, "harness.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun workspaceDao(db: HarnessDatabase): WorkspaceDao = db.workspaceDao()
    @Provides fun sessionDao(db: HarnessDatabase): SessionDao = db.sessionDao()
    @Provides fun messageDao(db: HarnessDatabase): MessageDao = db.messageDao()
    @Provides fun providerDao(db: HarnessDatabase): ProviderDao = db.providerDao()
    @Provides fun modelDao(db: HarnessDatabase): ModelDao = db.modelDao()
    @Provides fun pluginDao(db: HarnessDatabase): PluginDao = db.pluginDao()
    @Provides fun sideCardDao(db: HarnessDatabase): SideCardDao = db.sideCardDao()
    @Provides fun todoDao(db: HarnessDatabase): TodoDao = db.todoDao()
    @Provides fun memoryDao(db: HarnessDatabase): MemoryDao = db.memoryDao()
    @Provides fun skillDao(db: HarnessDatabase): SkillDao = db.skillDao()
}
