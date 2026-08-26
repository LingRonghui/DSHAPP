package com.dsh.harness.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsh.harness.data.local.PrefsRepository
import com.dsh.harness.data.model.AccessMode
import com.dsh.harness.data.model.ModelProvider
import com.dsh.harness.data.model.SideCard
import com.dsh.harness.data.remote.ProviderReq
import com.dsh.harness.data.repository.HarnessRepository
import com.dsh.harness.ui.screens.settings.SettingsTab
import com.dsh.harness.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 提供方对话框状态。 */
data class ProviderDialogState(
    val editing: String?,        // null = 新建；非空 = 编辑该 id
    val initial: ModelProvider?  // 编辑模式下的现有值
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: HarnessRepository,
    private val prefs: PrefsRepository
) : ViewModel() {

    private val _tab = MutableStateFlow(SettingsTab.GENERAL)
    val tab: StateFlow<SettingsTab> = _tab.asStateFlow()

    val themeMode: StateFlow<ThemeMode> = prefs.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.System)
    val language: StateFlow<String> = prefs.language
        .stateIn(viewModelScope, SharingStarted.Eagerly, "zh-CN")
    val defaultPreset: StateFlow<String> = prefs.defaultPreset
        .stateIn(viewModelScope, SharingStarted.Eagerly, "standard")
    val defaultAccess: StateFlow<AccessMode> = prefs.defaultAccess
        .stateIn(viewModelScope, SharingStarted.Eagerly, AccessMode.FULL_ACCESS)
    val busyEnterBehavior: StateFlow<String> = prefs.busyEnterBehavior
        .stateIn(viewModelScope, SharingStarted.Eagerly, "queue")
    val sideCardDefaultOpen: StateFlow<Boolean> = prefs.sideCardDefaultOpen
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val sideCardWidthPercent: StateFlow<Int> = prefs.sideCardWidthPercent
        .stateIn(viewModelScope, SharingStarted.Eagerly, 35)
    val openFileInSideCard: StateFlow<Boolean> = prefs.openFileInSideCard
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val positionCompatMode: StateFlow<Boolean> = prefs.positionCompatMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val providers: StateFlow<List<ModelProvider>> = repo.observeProviders()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val sideCards: StateFlow<List<SideCard>> = repo.observeSideCards()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var _providerDialog = MutableStateFlow<ProviderDialogState?>(null)
    val providerDialog get() = _providerDialog.value

    fun setTab(tab: SettingsTab) { _tab.value = tab }
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { prefs.setThemeMode(mode) }
    fun setLanguage(lang: String) = viewModelScope.launch { prefs.setLanguage(lang) }
    fun setDefaultPreset(preset: String) = viewModelScope.launch { prefs.setDefaultPreset(preset) }
    fun setDefaultAccess(mode: AccessMode) = viewModelScope.launch { prefs.setDefaultAccess(mode) }
    fun setBusyEnterBehavior(value: String) = viewModelScope.launch { prefs.setBusyEnterBehavior(value) }
    fun setSideCardDefaultOpen(open: Boolean) = viewModelScope.launch { prefs.setSideCardDefaultOpen(open) }
    fun setSideCardWidthPercent(percent: Int) = viewModelScope.launch { prefs.setSideCardWidthPercent(percent) }
    fun setOpenFileInSideCard(open: Boolean) = viewModelScope.launch { prefs.setOpenFileInSideCard(open) }
    fun setPositionCompatMode(enabled: Boolean) = viewModelScope.launch { prefs.setPositionCompatMode(enabled) }
    fun toggleSideCard(id: String, enabled: Boolean) = viewModelScope.launch { repo.setSideCardEnabled(id, enabled) }

    fun refresh() {
        viewModelScope.launch {
            runCatching { repo.refreshProviders() }
            runCatching { repo.refreshSideCards() }
        }
    }

    fun showAddProvider() {
        _providerDialog.value = ProviderDialogState(editing = null, initial = null)
    }

    fun showEditProvider(provider: ModelProvider) {
        _providerDialog.value = ProviderDialogState(editing = provider.id, initial = provider)
    }

    fun dismissProviderDialog() {
        _providerDialog.value = null
    }

    fun submitProvider(req: ProviderReq) {
        val state = _providerDialog.value ?: return
        viewModelScope.launch {
            if (state.editing == null) repo.addProvider(req)
            else repo.updateProvider(state.editing, req)
            _providerDialog.value = null
        }
    }

    fun deleteProvider(id: String) {
        viewModelScope.launch { repo.deleteProvider(id) }
    }
}
