package com.dsh.harness.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsh.harness.data.local.PrefsRepository
import com.dsh.harness.data.model.MarketPlugin
import com.dsh.harness.data.model.MarketTab
import com.dsh.harness.data.repository.HarnessRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MarketUiState(
    val tab: MarketTab = MarketTab.RECOMMEND,
    val rookieMode: Boolean = true,
    val query: String = "",
    val total: Int = 3414,
    val recommend: List<MarketPlugin> = emptyList(),
    val recent: List<MarketPlugin> = emptyList(),
    val installed: List<MarketPlugin> = emptyList(),
    val favorites: List<MarketPlugin> = emptyList(),
    val bundles: List<MarketPlugin> = emptyList(),
    val sceneSuggestion: List<MarketPlugin> = emptyList(),
    val sceneLoading: Boolean = false,
    val updating: MarketPlugin? = null,
    val excludedInstalledCount: Int = 0
)

@HiltViewModel
class PluginMarketViewModel @Inject constructor(
    private val repo: HarnessRepository,
    private val prefs: PrefsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MarketUiState())
    val state: StateFlow<MarketUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.observePlugins().collect { list ->
                _state.update {
                    it.copy(
                        recommend = list.shuffled().take(8),
                        recent = list.filter { p -> p.recentlyUpdated }.take(8),
                        installed = list.filter { p -> p.installed },
                        favorites = list.filter { p -> p.favorited },
                        bundles = list.filter { p -> p.kind == "bundle" },
                        excludedInstalledCount = list.count { p -> p.installed }
                    )
                }
            }
        }
        viewModelScope.launch {
            prefs.marketRookieMode.collect { r -> _state.update { it.copy(rookieMode = r) } }
        }
    }

    fun bootstrap() {
        viewModelScope.launch {
            runCatching { repo.refreshMarket("recommend") }
            runCatching { repo.refreshMarket("recent") }
        }
    }

    /** 详情页打开时确保目标插件已加载。 */
    fun ensureLoaded(pluginId: String) {
        val exists = listOf(
            state.value.recommend, state.value.recent, state.value.installed,
            state.value.favorites, state.value.bundles, state.value.sceneSuggestion
        ).any { list -> list.any { it.id == pluginId } }
        if (!exists) {
            viewModelScope.launch {
                runCatching { repo.refreshMarket("recommend") }
                runCatching { repo.refreshMarket("installed") }
            }
        }
    }

    fun setTab(tab: MarketTab) {
        _state.update { it.copy(tab = tab) }
        viewModelScope.launch {
            when (tab) {
                MarketTab.SEARCH -> if (state.value.query.isNotBlank()) repo.refreshMarket("search", state.value.query, state.value.rookieMode)
                MarketTab.FAVORITE -> repo.refreshMarket("favorite")
                MarketTab.INSTALLED -> repo.refreshMarket("installed")
                MarketTab.BUNDLE -> repo.refreshMarket("bundle")
                else -> Unit
            }
        }
    }

    fun setQuery(q: String) = _state.update { it.copy(query = q) }

    fun search() {
        viewModelScope.launch {
            runCatching { repo.refreshMarket("search", state.value.query, state.value.rookieMode) }
        }
    }

    fun toggleMode() {
        val next = !state.value.rookieMode
        _state.update { it.copy(rookieMode = next) }
        viewModelScope.launch { prefs.setMarketRookieMode(next) }
    }

    fun install(id: String) {
        viewModelScope.launch { repo.installPlugin(id) }
    }

    fun favorite(id: String, fav: Boolean) {
        viewModelScope.launch { repo.favoritePlugin(id, fav) }
    }

    fun ignoreUpdate(id: String) {
        viewModelScope.launch { repo.ignorePluginUpdate(id) }
        _state.update { it.copy(updating = null) }
    }

    fun fetchSceneSuggestion() {
        _state.update { it.copy(sceneLoading = true) }
        viewModelScope.launch {
            runCatching { repo.refreshMarket("scene") }
            _state.update { it.copy(sceneLoading = false) }
        }
    }
}
