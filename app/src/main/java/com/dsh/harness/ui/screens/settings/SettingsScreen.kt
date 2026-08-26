package com.dsh.harness.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ViewSidebar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dsh.harness.data.model.AccessMode
import com.dsh.harness.data.model.AgentPreset
import com.dsh.harness.data.model.ModelProvider
import com.dsh.harness.data.model.SideCardGroup
import com.dsh.harness.ui.theme.ThemeMode
import com.dsh.harness.ui.theme.harnessColors
import com.dsh.harness.ui.vm.SettingsViewModel
import kotlinx.coroutines.launch

/**
 * 设置面板。覆盖 Web 端"设置"弹窗的全部 5 个分类。
 */
@Composable
fun SettingsScreen(onClose: () -> Unit) {
    val vm: SettingsViewModel = hiltViewModel()
    val tab by vm.tab.collectAsStateWithLifecycle()
    val themeMode by vm.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.System)
    val language by vm.language.collectAsStateWithLifecycle(initialValue = "zh-CN")
    val defaultPreset by vm.defaultPreset.collectAsStateWithLifecycle(initialValue = "standard")
    val defaultAccess by vm.defaultAccess.collectAsStateWithLifecycle(initialValue = AccessMode.FULL_ACCESS)
    val busyEnter by vm.busyEnterBehavior.collectAsStateWithLifecycle(initialValue = "queue")
    val sideOpen by vm.sideCardDefaultOpen.collectAsStateWithLifecycle(initialValue = true)
    val sideWidth by vm.sideCardWidthPercent.collectAsStateWithLifecycle(initialValue = 35)
    val fileInSide by vm.openFileInSideCard.collectAsStateWithLifecycle(initialValue = true)
    val posCompat by vm.positionCompatMode.collectAsStateWithLifecycle(initialValue = false)
    val serverBaseUrl by vm.serverBaseUrl.collectAsStateWithLifecycle(initialValue = "https://47.110.78.97.sslip.io/")
    val providers by vm.providers.collectAsStateWithLifecycle()
    val sideCards by vm.sideCards.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.refresh() }

    Surface(modifier = Modifier.fillMaxSize(), color = harnessColors().background) {
        Row(modifier = Modifier.fillMaxSize()) {
            // 左侧导航
            SettingsNav(
                current = tab,
                onSelect = vm::setTab,
                onClose = onClose,
                modifier = Modifier
                    .background(harnessColors().sidebar)
                    .width(220.dp)
                    .fillMaxSize()
            )
            // 右侧内容
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Text(
                        when (tab) {
                            SettingsTab.GENERAL -> "通用设置"
                            SettingsTab.MODELS -> "模型"
                            SettingsTab.PLUGINS -> "插件"
                            SettingsTab.AGENT_PRESETS -> "Agent 预设"
                            SettingsTab.SIDE_CARDS -> "侧边卡片"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        color = harnessColors().primaryText
                    )
                }
                when (tab) {
                    SettingsTab.GENERAL -> GeneralSettings(
                        themeMode = themeMode,
                        language = language,
                        defaultPreset = defaultPreset,
                        defaultAccess = defaultAccess,
                        busyEnter = busyEnter,
                        serverBaseUrl = serverBaseUrl,
                        onThemeMode = vm::setThemeMode,
                        onLanguage = vm::setLanguage,
                        onPreset = vm::setDefaultPreset,
                        onAccess = vm::setDefaultAccess,
                        onBusyEnter = vm::setBusyEnterBehavior,
                        onServerBaseUrl = vm::setServerBaseUrl
                    )
                    SettingsTab.MODELS -> ModelsSettings(
                        providers = providers,
                        onAdd = vm::showAddProvider,
                        onEdit = vm::showEditProvider,
                        onDelete = vm::deleteProvider
                    )
                    SettingsTab.PLUGINS -> PluginsSettings()
                    SettingsTab.AGENT_PRESETS -> AgentPresetsSettings(
                        current = defaultPreset,
                        onSelect = vm::setDefaultPreset
                    )
                    SettingsTab.SIDE_CARDS -> SideCardsSettings(
                        sideOpen = sideOpen,
                        sideWidth = sideWidth,
                        fileInSide = fileInSide,
                        posCompat = posCompat,
                        cards = sideCards,
                        onSideOpen = vm::setSideCardDefaultOpen,
                        onSideWidth = vm::setSideCardWidthPercent,
                        onFileInSide = vm::setOpenFileInSideCard,
                        onPosCompat = vm::setPositionCompatMode,
                        onToggleCard = vm::toggleSideCard
                    )
                }
            }
        }

        // 添加/编辑提供方弹窗
        val dialog = vm.providerDialog
        if (dialog != null) {
            ProviderDialog(
                editing = dialog.editing,
                initial = dialog.initial,
                onConfirm = { req -> vm.submitProvider(req) },
                onDismiss = vm::dismissProviderDialog
            )
        }
    }
}

@Composable
private fun SettingsNav(
    current: SettingsTab,
    onSelect: (SettingsTab) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("设置", style = MaterialTheme.typography.titleMedium, color = harnessColors().primaryText)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "关闭", tint = harnessColors().tertiaryText)
            }
        }
        SettingsTab.values().forEach { tab ->
            val selected = tab == current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) harnessColors().brandContainer else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(navIcon(tab), null, tint = if (selected) harnessColors().brand else harnessColors().secondaryText, modifier = Modifier.size(18.dp))
                Text(tab.label, style = MaterialTheme.typography.labelLarge, color = if (selected) harnessColors().brand else harnessColors().primaryText)
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            "Agent 预设 对此后新建的会话生效。运行中的会话保持它开始时的预设。",
            style = MaterialTheme.typography.labelSmall,
            color = harnessColors().tertiaryText
        )
    }
}

private fun navIcon(tab: SettingsTab) = when (tab) {
    SettingsTab.GENERAL -> Icons.Outlined.Tune
    SettingsTab.MODELS -> Icons.Outlined.Key
    SettingsTab.PLUGINS -> Icons.Outlined.Extension
    SettingsTab.AGENT_PRESETS -> Icons.Outlined.PlayCircle
    SettingsTab.SIDE_CARDS -> Icons.Outlined.ViewSidebar
}

enum class SettingsTab(val label: String) {
    GENERAL("通用设置"), MODELS("模型"), PLUGINS("插件"), AGENT_PRESETS("Agent 预设"), SIDE_CARDS("侧边卡片")
}
