package com.dsh.harness.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dsh.harness.data.model.AccessMode
import com.dsh.harness.ui.theme.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "harness_prefs")

/**
 * 应用偏好持久化（主题、语言、默认预设/访问、Enter 行为、侧边卡片宽度、是否首次启动等）。
 * 与 Web 端"通用设置"对应。
 */
@Singleton
class PrefsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val themeMode: Flow<ThemeMode> = context.dataStore.data.map {
        ThemeMode.valueOf(it[KEY_THEME] ?: ThemeMode.System.name)
    }
    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[KEY_THEME] = mode.name }
    }

    val language: Flow<String> = context.dataStore.data.map { it[KEY_LANG] ?: "zh-CN" }
    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[KEY_LANG] = lang }
    }

    val defaultPreset: Flow<String> = context.dataStore.data.map { it[KEY_PRESET] ?: "standard" }
    suspend fun setDefaultPreset(preset: String) {
        context.dataStore.edit { it[KEY_PRESET] = preset }
    }

    val defaultAccess: Flow<AccessMode> = context.dataStore.data.map {
        AccessMode.fromKey(it[KEY_ACCESS])
    }
    suspend fun setDefaultAccess(mode: AccessMode) {
        context.dataStore.edit { it[KEY_ACCESS] = mode.key }
    }

    val busyEnterBehavior: Flow<String> = context.dataStore.data.map { it[KEY_BUSY_ENTER] ?: "queue" }
    suspend fun setBusyEnterBehavior(value: String) {
        context.dataStore.edit { it[KEY_BUSY_ENTER] = value }
    }

    val sideCardDefaultOpen: Flow<Boolean> = context.dataStore.data.map { it[KEY_SIDE_OPEN] ?: true }
    suspend fun setSideCardDefaultOpen(open: Boolean) {
        context.dataStore.edit { it[KEY_SIDE_OPEN] = open }
    }

    val sideCardWidthPercent: Flow<Int> = context.dataStore.data.map { it[KEY_SIDE_WIDTH] ?: 35 }
    suspend fun setSideCardWidthPercent(percent: Int) {
        context.dataStore.edit {
            val safe = percent.coerceIn(20, 60)
            it[KEY_SIDE_WIDTH] = safe
        }
    }

    val openFileInSideCard: Flow<Boolean> = context.dataStore.data.map { it[KEY_FILE_IN_SIDE] ?: true }
    suspend fun setOpenFileInSideCard(open: Boolean) {
        context.dataStore.edit { it[KEY_FILE_IN_SIDE] = open }
    }

    val positionCompatMode: Flow<Boolean> = context.dataStore.data.map { it[KEY_POS_COMPAT] ?: false }
    suspend fun setPositionCompatMode(enabled: Boolean) {
        context.dataStore.edit { it[KEY_POS_COMPAT] = enabled }
    }

    val baseUrl: Flow<String> = context.dataStore.data.map { it[KEY_BASE_URL] ?: "https://47.110.78.97.sslip.io/" }
    suspend fun setBaseUrl(url: String) {
        context.dataStore.edit { it[KEY_BASE_URL] = url }
    }

    val firstRun: Flow<Boolean> = context.dataStore.data.map { it[KEY_FIRST_RUN] ?: true }
    suspend fun setFirstRunDone() {
        context.dataStore.edit { it[KEY_FIRST_RUN] = false }
    }

    val marketRookieMode: Flow<Boolean> = context.dataStore.data.map { it[KEY_MARKET_ROOKIE] ?: true }
    suspend fun setMarketRookieMode(rookie: Boolean) {
        context.dataStore.edit { it[KEY_MARKET_ROOKIE] = rookie }
    }

    val lastWorkspace: Flow<String?> = context.dataStore.data.map { it[KEY_LAST_WS] }
    suspend fun setLastWorkspace(id: String?) {
        context.dataStore.edit {
            if (id == null) it.remove(KEY_LAST_WS) else it[KEY_LAST_WS] = id
        }
    }

    val lastSession: Flow<String?> = context.dataStore.data.map { it[KEY_LAST_SESSION] }
    suspend fun setLastSession(id: String?) {
        context.dataStore.edit {
            if (id == null) it.remove(KEY_LAST_SESSION) else it[KEY_LAST_SESSION] = id
        }
    }

    companion object {
        private val KEY_THEME = stringPreferencesKey("theme_mode")
        private val KEY_LANG = stringPreferencesKey("language")
        private val KEY_PRESET = stringPreferencesKey("default_preset")
        private val KEY_ACCESS = stringPreferencesKey("default_access")
        private val KEY_BUSY_ENTER = stringPreferencesKey("busy_enter")
        private val KEY_SIDE_OPEN = booleanPreferencesKey("side_open")
        private val KEY_SIDE_WIDTH = intPreferencesKey("side_width")
        private val KEY_FILE_IN_SIDE = booleanPreferencesKey("file_in_side")
        private val KEY_POS_COMPAT = booleanPreferencesKey("pos_compat")
        private val KEY_BASE_URL = stringPreferencesKey("base_url")
        private val KEY_FIRST_RUN = booleanPreferencesKey("first_run")
        private val KEY_MARKET_ROOKIE = booleanPreferencesKey("market_rookie")
        private val KEY_LAST_WS = stringPreferencesKey("last_workspace")
        private val KEY_LAST_SESSION = stringPreferencesKey("last_session")
    }
}
