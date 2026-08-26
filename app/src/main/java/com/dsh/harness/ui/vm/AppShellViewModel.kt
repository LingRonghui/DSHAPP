package com.dsh.harness.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsh.harness.data.local.PrefsRepository
import com.dsh.harness.data.model.AccessMode
import com.dsh.harness.data.model.AgentPreset
import com.dsh.harness.data.model.ChatMessage
import com.dsh.harness.data.model.CommandItem
import com.dsh.harness.data.model.MarketPlugin
import com.dsh.harness.data.model.MessageRole
import com.dsh.harness.data.model.ModelInfo
import com.dsh.harness.data.model.ModelProvider
import com.dsh.harness.data.model.Session
import com.dsh.harness.data.model.SessionTab
import com.dsh.harness.data.model.SideCard
import com.dsh.harness.data.model.Workspace
import com.dsh.harness.data.repository.HarnessRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/** 全局 UI 状态。 */
data class AppUiState(
    val bootstrapped: Boolean = false,
    val sidebarOpen: Boolean = true,
    val searchOpen: Boolean = false,
    val searchQuery: String = "",
    val marketOpen: Boolean = false,
    val settingsOpen: Boolean = false,
    val workspaces: List<Workspace> = emptyList(),
    val currentWorkspaceId: String? = null,
    val sessions: List<Session> = emptyList(),
    val currentSessionId: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val activeTab: SessionTab = SessionTab.DIALOG,
    val providers: List<ModelProvider> = emptyList(),
    val models: List<ModelInfo> = emptyList(),
    val sideCards: List<SideCard> = emptyList(),
    val marketPlugins: List<MarketPlugin> = emptyList(),
    val installedPlugins: List<MarketPlugin> = emptyList(),
    val favoritePlugins: List<MarketPlugin> = emptyList(),
    val busyEnterQueue: Boolean = true,
    val sideCardDefaultOpen: Boolean = true,
    val sideCardWidthPercent: Int = 35,
    val sending: Boolean = false,
    val error: String? = null,
    val syncError: String? = null,
    val serverUrl: String? = null
) {
    val currentSession: Session? get() = sessions.firstOrNull { it.id == currentSessionId }
    val currentWorkspace: Workspace? get() = workspaces.firstOrNull { it.id == currentWorkspaceId }
    val visibleSessions: List<Session> get() = if (searchQuery.isNotBlank()) {
        sessions.filter {
            it.title.contains(searchQuery, true) ||
                (it.alias?.contains(searchQuery, true) == true)
        }
    } else {
        currentWorkspaceId?.let { wid -> sessions.filter { it.workspaceId == wid } } ?: sessions
    }
}

@HiltViewModel
class AppShellViewModel @Inject constructor(
    private val repo: HarnessRepository,
    private val prefs: PrefsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    val firstRun: StateFlow<Boolean> = prefs.firstRun
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    init {
        // 服务器地址切换时同步到真实宿主客户端
        viewModelScope.launch {
            prefs.baseUrl.collect { repo.applyServerUrl(it) }
        }
        // 订阅工作区、会话、提供方、模型、侧边卡片
        viewModelScope.launch {
            combine(
                repo.observeWorkspaces(),
                repo.observeSessions(),
                repo.observeProviders(),
                repo.observeModels(),
                repo.observeSideCards()
            ) { ws, sess, prov, models, cards ->
                _uiState.update {
                    it.copy(
                        workspaces = ws, sessions = sess, providers = prov,
                        models = models, sideCards = cards
                    )
                }
            }.collect {}
        }
        // 当前会话的消息订阅
        viewModelScope.launch {
            _uiState.flatMapLatest { state ->
                if (state.currentSessionId != null) repo.observeMessages(state.currentSessionId)
                else kotlinx.coroutines.flow.flowOf(emptyList())
            }.collect { msgs ->
                _uiState.update { it.copy(messages = msgs) }
            }
        }
        // 最近工作区记忆
        viewModelScope.launch {
            prefs.lastWorkspace.collect { wid ->
                _uiState.update { it.copy(currentWorkspaceId = wid ?: _uiState.value.workspaces.firstOrNull()?.id) }
            }
        }
        // 最近会话记忆
        viewModelScope.launch {
            prefs.lastSession.collect { sid ->
                _uiState.update { it.copy(currentSessionId = sid) }
            }
        }
    }

    /** 首次启动种子 + 数据刷新。 */
    suspend fun bootstrap() {
        if (_uiState.value.bootstrapped) return
        runCatching { repo.seedDefaultsIfEmpty() }
        val syncErr = repo.refreshWorkspaces()
        if (syncErr != null) {
            _uiState.update {
                it.copy(syncError = syncErr, serverUrl = repo.currentServerUrl())
            }
        }
        runCatching { repo.refreshSessions() }
        runCatching { repo.refreshProviders() }
        runCatching { repo.refreshSideCards() }
        // 没有任何工作区时创建一个默认工作区
        val wss = repo.observeWorkspaces().first()
        if (wss.isEmpty()) {
            val ws = repo.createWorkspace("足球AI网站")
            if (ws != null) {
                prefs.setLastWorkspace(ws.id)
                val s = repo.createSession(ws.id, "新会话")
                if (s != null) prefs.setLastSession(s.id)
            }
        }
        _uiState.update { it.copy(bootstrapped = true) }
    }

    fun markFirstRunDone() {
        viewModelScope.launch { prefs.setFirstRunDone() }
    }

    fun toggleSidebar() = _uiState.update { it.copy(sidebarOpen = !it.sidebarOpen) }

    fun selectWorkspace(id: String) {
        viewModelScope.launch { prefs.setLastWorkspace(id) }
    }

    fun selectSession(id: String) {
        _uiState.update { it.copy(currentSessionId = id) }
        viewModelScope.launch { prefs.setLastSession(id) }
    }

    fun createNewSession() {
        val wid = _uiState.value.currentWorkspaceId ?: return
        viewModelScope.launch {
            val s = repo.createSession(wid, "新会话")
            if (s != null) {
                prefs.setLastSession(s.id)
                _uiState.update { it.copy(searchOpen = false) }
            }
        }
    }

    fun openSearch(open: Boolean) = _uiState.update { it.copy(searchOpen = open, searchQuery = "") }

    fun setSearchQuery(q: String) = _uiState.update { it.copy(searchQuery = q) }

    fun openMarket() = _uiState.update { it.copy(marketOpen = true) }
    fun closeMarket() = _uiState.update { it.copy(marketOpen = false) }

    fun openSettings() = _uiState.update { it.copy(settingsOpen = true) }
    fun closeSettings() = _uiState.update { it.copy(settingsOpen = false) }

    fun createWorkspace(name: String) {
        viewModelScope.launch {
            val ws = repo.createWorkspace(name)
            if (ws != null) prefs.setLastWorkspace(ws.id)
        }
    }

    fun sendMessage(content: String) {
        val sid = _uiState.value.currentSessionId ?: return
        if (content.isBlank()) return
        val isCommand = content.startsWith("/")
        val cmd = if (isCommand) content.removePrefix("/").split(" ").firstOrNull() else null
        val text = if (isCommand) content.substringAfter(" ", "").ifBlank { null } ?: content.removePrefix("/") else content
        _uiState.update { it.copy(sending = true) }
        viewModelScope.launch {
            repo.sendMessage(sid, text, cmd)
            _uiState.update { it.copy(sending = false) }
        }
    }

    fun stopSession() {
        val sid = _uiState.value.currentSessionId ?: return
        viewModelScope.launch { repo.stopSession(sid) }
    }

    fun updatePreset(preset: String) {
        val sid = _uiState.value.currentSessionId ?: return
        viewModelScope.launch { repo.updatePreset(sid, preset) }
    }

    fun updateAccess(mode: AccessMode) {
        val sid = _uiState.value.currentSessionId ?: return
        viewModelScope.launch { repo.updateAccess(sid, mode) }
    }

    fun updateModel(modelId: String) {
        val sid = _uiState.value.currentSessionId ?: return
        viewModelScope.launch { repo.updateModel(sid, modelId) }
    }

    fun selectTab(tab: SessionTab) = _uiState.update { it.copy(activeTab = tab) }

    fun branchSession() {
        val sid = _uiState.value.currentSessionId ?: return
        viewModelScope.launch {
            repo.branchSession(sid)?.let { newS -> prefs.setLastSession(newS.id) }
        }
    }

    fun runCommand(command: CommandItem) {
        sendMessage("/${command.name}")
    }

    fun togglePinned(id: String, pinned: Boolean) {
        viewModelScope.launch { repo.pinSession(id, pinned) }
    }

    fun deleteSession(id: String) {
        viewModelScope.launch {
            repo.deleteSession(id)
            if (_uiState.value.currentSessionId == id) {
                prefs.setLastSession(null)
            }
        }
    }
}
