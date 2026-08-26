package com.dsh.harness.ui.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.dsh.harness.ui.vm.AppUiState
import com.dsh.harness.ui.theme.harnessColors
import com.dsh.harness.ui.components.Sidebar
import com.dsh.harness.ui.components.ConversationArea
import com.dsh.harness.ui.components.DetailPanel
import com.dsh.harness.data.model.AccessMode
import com.dsh.harness.data.model.CommandItem
import com.dsh.harness.data.model.SessionTab

/** 手机竖屏断点：宽度低于该值时使用单列 + 抽屉 + 底部导航布局。 */
private val CompactWidthDp = 600

/**
 * 主屏：侧边栏 + 会话主区 + 详情面板。
 * - 宽屏（平板/横屏）：三栏并排。
 * - 手机竖屏：会话全屏单列，侧边栏以抽屉浮层、详情面板以右侧覆盖层展开，
 *   底部用导航栏承载 新建会话 / 插件市场 / 设置。
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
    onSelectAccess: (AccessMode) -> Unit,
    onSelectModel: (String) -> Unit,
    onSelectTab: (SessionTab) -> Unit,
    onBranchSession: () -> Unit,
    onCommand: (CommandItem) -> Unit,
    onTogglePinned: (String, Boolean) -> Unit,
    onDeleteSession: (String) -> Unit,
    navController: NavHostController
) {
    val colors = harnessColors()
    val isCompact = LocalConfiguration.current.screenWidthDp < CompactWidthDp
    var detailDismissed by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.currentSessionId) { detailDismissed = false }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        if (isCompact) {
            CompactLayout(
                uiState = uiState,
                colors = colors,
                detailDismissed = detailDismissed,
                onDismissDetail = { detailDismissed = true },
                onToggleSidebar = onToggleSidebar,
                onSelectWorkspace = onSelectWorkspace,
                onSelectSession = onSelectSession,
                onNewSession = onNewSession,
                onOpenSearch = onOpenSearch,
                onSearchQueryChange = onSearchQueryChange,
                onOpenMarket = onOpenMarket,
                onOpenSettings = onOpenSettings,
                onNewWorkspace = onNewWorkspace,
                onSend = onSend,
                onStop = onStop,
                onSelectPreset = onSelectPreset,
                onSelectAccess = onSelectAccess,
                onSelectModel = onSelectModel,
                onSelectTab = onSelectTab,
                onBranchSession = onBranchSession,
                onCommand = onCommand,
                onTogglePinned = onTogglePinned,
                onDeleteSession = onDeleteSession
            )
        } else {
            WideLayout(
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
                onSend = onSend,
                onStop = onStop,
                onSelectPreset = onSelectPreset,
                onSelectAccess = onSelectAccess,
                onSelectModel = onSelectModel,
                onSelectTab = onSelectTab,
                onBranchSession = onBranchSession,
                onCommand = onCommand,
                onTogglePinned = onTogglePinned,
                onDeleteSession = onDeleteSession
            )
        }
    }
}

/** 手机竖屏：会话全屏单列 + 抽屉侧栏 + 右侧详情覆盖层 + 底部导航栏。 */
@Composable
private fun CompactLayout(
    uiState: AppUiState,
    colors: com.dsh.harness.ui.theme.HarnessColors,
    detailDismissed: Boolean,
    onDismissDetail: () -> Unit,
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
    onSelectAccess: (AccessMode) -> Unit,
    onSelectModel: (String) -> Unit,
    onSelectTab: (SessionTab) -> Unit,
    onBranchSession: () -> Unit,
    onCommand: (CommandItem) -> Unit,
    onTogglePinned: (String, Boolean) -> Unit,
    onDeleteSession: (String) -> Unit
) {
    val scrim = Color.Black.copy(alpha = 0.48f)

    Column(modifier = Modifier.fillMaxSize()) {
        // 会话主区（顶部避开状态栏）
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
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

        // 底部导航（避开手势区）
        NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars),
            containerColor = colors.surface,
            contentColor = colors.primaryText
        ) {
            NavigationBarItem(
                selected = false,
                onClick = onNewSession,
                icon = { Icon(Icons.Outlined.Add, null) },
                label = { Text("新建") }
            )
            NavigationBarItem(
                selected = false,
                onClick = onOpenMarket,
                icon = { Icon(Icons.Outlined.Extension, null) },
                label = { Text("插件") }
            )
            NavigationBarItem(
                selected = false,
                onClick = onOpenSettings,
                icon = { Icon(Icons.Outlined.Settings, null) },
                label = { Text("设置") }
            )
        }
    }

    // 侧边栏抽屉浮层
    AnimatedVisibility(
        visible = uiState.sidebarOpen,
        enter = expandHorizontally(animationSpec = tween(220)) { it } + fadeIn(tween(220)),
        exit = shrinkHorizontally(animationSpec = tween(220)) { it } + fadeOut(tween(220))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrim)
                    .clickable(onClick = onToggleSidebar)
            )
            Surface(
                color = colors.sidebar,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(300.dp)
                    .align(Alignment.CenterStart)
                    .windowInsetsPadding(WindowInsets.statusBars)
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
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // 详情面板右侧覆盖层
    AnimatedVisibility(
        visible = uiState.currentSessionId != null && !detailDismissed,
        enter = expandHorizontally(animationSpec = tween(220)) { it } + fadeIn(tween(220)),
        exit = shrinkHorizontally(animationSpec = tween(220)) { it } + fadeOut(tween(220))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrim)
                    .clickable(onClick = onDismissDetail)
            )
            Surface(
                color = colors.surface,
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 380.dp)
                    .fillMaxWidth(0.9f)
                    .align(Alignment.CenterEnd)
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("详情", style = MaterialTheme.typography.titleMedium, color = colors.primaryText)
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = onDismissDetail) {
                            Icon(Icons.Filled.Close, "收起详情", tint = colors.secondaryText)
                        }
                    }
                    DetailPanel(
                        uiState = uiState,
                        onClose = onDismissDetail,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                }
            }
        }
    }
}

/** 宽屏（平板/横屏）：侧边栏 + 会话主区 + 详情面板三栏并排，适配安全区。 */
@Composable
private fun WideLayout(
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
    onSelectAccess: (AccessMode) -> Unit,
    onSelectModel: (String) -> Unit,
    onSelectTab: (SessionTab) -> Unit,
    onBranchSession: () -> Unit,
    onCommand: (CommandItem) -> Unit,
    onTogglePinned: (String, Boolean) -> Unit,
    onDeleteSession: (String) -> Unit
) {
    Row(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
        AnimatedVisibility(
            visible = uiState.sidebarOpen,
            enter = expandHorizontally(animationSpec = tween(200)) { it } + fadeIn(tween(200)),
            exit = shrinkHorizontally(animationSpec = tween(200)) { it } + fadeOut(tween(200))
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

        AnimatedVisibility(
            visible = uiState.currentSessionId != null,
            enter = expandHorizontally(animationSpec = tween(200)) { it } + fadeIn(tween(200)),
            exit = shrinkHorizontally(animationSpec = tween(200)) { it } + fadeOut(tween(200))
        ) {
            DetailPanel(
                uiState = uiState,
                onClose = { },
                modifier = Modifier
                    .background(harnessColors().surface)
                    .width(360.dp)
                    .fillMaxSize()
            )
        }
    }
}