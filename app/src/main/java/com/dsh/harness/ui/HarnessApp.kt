package com.dsh.harness.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.dsh.harness.ui.nav.HarnessNavHost
import com.dsh.harness.ui.nav.MainScreen
import com.dsh.harness.ui.vm.AppShellViewModel

/**
 * 应用根组合。承担：
 * - 首次启动种子数据
 * - 内测声明弹窗
 * - 主屏（侧边栏 + 主区 + 详情面板）
 * - 路由（设置详情、插件市场详情等独立页面）
 */
@Composable
fun HarnessApp() {
    val navController = rememberNavController()
    val shellVm: AppShellViewModel = hiltViewModel()
    val uiState by shellVm.uiState.collectAsStateWithLifecycle()
    val firstRun by shellVm.firstRun.collectAsStateWithLifecycle(initialValue = true)

    LaunchedEffect(Unit) {
        shellVm.bootstrap()
    }

    // 内测声明（仅首次启动展示）
    if (firstRun) {
        BetaNoticeDialog(onDismiss = { shellVm.markFirstRunDone() })
    }

    // 全屏覆盖：插件市场
    if (uiState.marketOpen) {
        com.dsh.harness.ui.screens.market.PluginMarketScreen(onClose = { shellVm.closeMarket() })
        return
    }

    // 全屏覆盖：设置
    if (uiState.settingsOpen) {
        com.dsh.harness.ui.screens.settings.SettingsScreen(onClose = { shellVm.closeSettings() })
        return
    }

    // 主屏
    MainScreen(
        uiState = uiState,
        onToggleSidebar = shellVm::toggleSidebar,
        onSelectWorkspace = shellVm::selectWorkspace,
        onSelectSession = shellVm::selectSession,
        onNewSession = shellVm::createNewSession,
        onOpenSearch = shellVm::openSearch,
        onSearchQueryChange = shellVm::setSearchQuery,
        onOpenMarket = shellVm::openMarket,
        onOpenSettings = shellVm::openSettings,
        onNewWorkspace = shellVm::createWorkspace,
        onSend = shellVm::sendMessage,
        onStop = shellVm::stopSession,
        onSelectPreset = shellVm::updatePreset,
        onSelectAccess = shellVm::updateAccess,
        onSelectModel = shellVm::updateModel,
        onSelectTab = shellVm::selectTab,
        onBranchSession = shellVm::branchSession,
        onCommand = shellVm::runCommand,
        onTogglePinned = shellVm::togglePinned,
        onDeleteSession = shellVm::deleteSession,
        onLoadMoreMessages = shellVm::loadMoreMessages,
        navController = navController
    )

    // 路由层（详情页面等，独立路由）
    HarnessNavHost(navController = navController)
}
