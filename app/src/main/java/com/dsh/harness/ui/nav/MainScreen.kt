package com.dsh.harness.ui.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.dsh.harness.ui.vm.AppUiState
import com.dsh.harness.ui.theme.harnessColors
import com.dsh.harness.ui.components.Sidebar
import com.dsh.harness.ui.components.ConversationArea
import com.dsh.harness.ui.components.DetailPanel

/**
 * 主屏：侧边栏 + 会话主区 + 详情面板。
 * 在窄屏上侧边栏以抽屉形式展示（由 [Sidebar] 自身处理），主区与详情面板按比例共存。
 */
@Composable
fun MainScreen(
    uiState: AppUiState,
    onToggleSidebar: () -> Unit,
    onSelectWorkspace: (String) -> Unit,
    onSelectSession: (String) -> Unit,
    onNewSession: () -> Unit,
    onOpenSearch: (Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onOpenMarket: () -> Unit,
    onOpenSettings: () -> Unit,
    onNewWorkspace: (String) -> Unit,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
    onSelectPreset: (String) -> Unit,
    onSelectAccess: (com.dsh.harness.data.model.AccessMode) -> Unit,
    onSelectModel: (String) -> Unit,
    onSelectTab: (com.dsh.harness.data.model.SessionTab) -> Unit,
    onBranchSession: () -> Unit,
    onCommand: (com.dsh.harness.data.model.CommandItem) -> Unit,
    onTogglePinned: (String, Boolean) -> Unit,
    onDeleteSession: (String) -> Unit,
    navController: NavHostController
) {
    Surface(modifier = Modifier.fillMaxSize(), color = harnessColors().background) {
        Row(modifier = Modifier.fillMaxSize()) {
            // 侧边栏
            AnimatedVisibility(
                visible = uiState.sidebarOpen,
                enter = expandHorizontally(animationSpec = tween(200)) { fullWidth -> fullWidth } + fadeIn(tween(200)),
                exit = shrinkHorizontally(animationSpec = tween(200)) { fullWidth -> fullWidth } + fadeOut(tween(200))
            ) {
                Sidebar(
                    uiState = uiState,
                    onToggleSidebar = onToggleSidebar,
                    onSelectWorkspace = onSelectWorkspace,
                    onSelectSession = onSelectSession,
                    onNewSession = onNewSession,
                    onOpenSearch = onOpenSearch,
                    onSearchQueryChange = onSearchQueryChange,
                    onOpenMarket = onOpenMarket,
                    onOpenSettings = onOpenSettings,
                    onNewWorkspace = onNewWorkspace,
                    onTogglePinned = onTogglePinned,
                    onDeleteSession = onDeleteSession,
                    modifier = Modifier
                        .background(harnessColors().sidebar)
                        .width(280.dp)
                        .fillMaxSize()
                )
            }

            // 会话主区
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                ConversationArea(
                    uiState = uiState,
                    onToggleSidebar = onToggleSidebar,
                    onNewSession = onNewSession,
                    onOpenSettings = onOpenSettings,
                    onOpenMarket = onOpenMarket,
                    onSend = onSend,
                    onStop = onStop,
                    onSelectPreset = onSelectPreset,
                    onSelectAccess = onSelectAccess,
                    onSelectModel = onSelectModel,
                    onSelectTab = onSelectTab,
                    onBranchSession = onBranchSession,
                    onCommand = onCommand,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 详情面板（仅当有会话时展示）
            AnimatedVisibility(
                visible = uiState.currentSessionId != null,
                enter = expandHorizontally(animationSpec = tween(200)) { fullWidth -> fullWidth } + fadeIn(tween(200)),
                exit = shrinkHorizontally(animationSpec = tween(200)) { fullWidth -> fullWidth } + fadeOut(tween(200))
            ) {
                DetailPanel(
                    uiState = uiState,
                    onClose = { /* 切换 tab 或关闭由详情面板内部处理 */ },
                    modifier = Modifier
                        .background(harnessColors().surface)
                        .width(360.dp)
                        .fillMaxSize()
                )
            }
        }
    }
}
