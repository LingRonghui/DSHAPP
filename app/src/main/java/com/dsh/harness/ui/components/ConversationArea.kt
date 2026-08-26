package com.dsh.harness.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Subject
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dsh.harness.data.model.AccessMode
import com.dsh.harness.data.model.AgentPreset
import com.dsh.harness.data.model.CommandItem
import com.dsh.harness.data.model.Commands
import com.dsh.harness.data.model.SessionTab
import com.dsh.harness.ui.theme.harnessColors
import com.dsh.harness.ui.vm.AppUiState

/**
 * 会话主区：顶部栏 + Tab 栏 + 消息流 + 输入框。
 * 与 Web 端"会话顶栏 + 标签 + 对话"完全对应。
 */
@Composable
fun ConversationArea(
    uiState: AppUiState,
    onToggleSidebar: () -> Unit,
    onNewSession: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMarket: () -> Unit,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
    onSelectPreset: (String) -> Unit,
    onSelectAccess: (AccessMode) -> Unit,
    onSelectModel: (String) -> Unit,
    onSelectTab: (SessionTab) -> Unit,
    onBranchSession: () -> Unit,
    onCommand: (CommandItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var draft by remember(uiState.currentSessionId) { mutableStateOf("") }
    val session = uiState.currentSession
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    Column(modifier = modifier) {
        // 顶部栏
        TopBar(
            uiState = uiState,
            onToggleSidebar = onToggleSidebar,
            onNewSession = onNewSession,
            onOpenSettings = onOpenSettings,
            onOpenMarket = onOpenMarket,
            onSelectPreset = onSelectPreset,
            onSelectAccess = onSelectAccess,
            onSelectModel = onSelectModel
        )

        // 同步失败提示条（工作区为空时把真实原因显示出来，而不是静默空白）
        if (uiState.syncError != null && uiState.workspaces.isEmpty()) {
            SyncErrorBanner(message = uiState.syncError, serverUrl = uiState.serverUrl)
        }

        // 会话信息栏
        if (session != null) {
            SessionHeader(
                title = session.alias ?: session.title,
                preset = session.agentPreset,
                backgroundTaskCount = session.backgroundTaskCount,
                onBranch = onBranchSession
            )
            // Tab 栏
            SessionTabBar(
                active = uiState.activeTab,
                onSelect = onSelectTab
            )
        }

        // 消息流
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (session == null) {
                EmptyState()
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item { LoadEarlierButton {} }
                    items(uiState.messages, key = { it.id }) { msg ->
                        com.dsh.harness.ui.components.message.MessageItem(message = msg)
                    }
                }
            }
        }

        // 输入框
        Composer(
            draft = draft,
            onChange = { draft = it },
            onSend = {
                if (draft.isNotBlank()) {
                    onSend(draft)
                    draft = ""
                }
            },
            onStop = onStop,
            sending = uiState.sending,
            onCommand = onCommand,
            commands = Commands.ALL
        )
    }
}

/** 同步失败提示条：工作区拉取失败时显示真实原因，避免静默空白。 */
@Composable
private fun SyncErrorBanner(message: String, serverUrl: String?) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("同步失败：$message", style = MaterialTheme.typography.bodyMedium)
                if (!serverUrl.isNullOrBlank()) {
                    Text(
                        "当前服务器：$serverUrl（可在 设置 中修改）",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    uiState: AppUiState,
    onToggleSidebar: () -> Unit,
    onNewSession: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMarket: () -> Unit,
    onSelectPreset: (String) -> Unit,
    onSelectAccess: (AccessMode) -> Unit,
    onSelectModel: (String) -> Unit
) {
    val session = uiState.currentSession
    Surface(
        color = harnessColors().surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (!uiState.sidebarOpen) {
                IconButton(onClick = onToggleSidebar) {
                    Icon(Icons.Outlined.Menu, "展开侧边栏", tint = harnessColors().secondaryText)
                }
            }
            // 工作区选择
            DropdownChip(
                label = uiState.currentWorkspace?.name ?: "选择工作区",
                icon = Icons.Outlined.Subject,
                items = uiState.workspaces.map { it.name to { /* 切换工作区逻辑由会话级回调 */ } }
            )
            // Agent 预设
            DropdownChip(
                label = AgentPreset.BUILT_IN.firstOrNull { it.code == session?.agentPreset }?.name ?: "标准模式",
                icon = Icons.Outlined.PlayCircle,
                items = AgentPreset.BUILT_IN.map { it.name to { onSelectPreset(it.code) } }
            )
            Spacer(Modifier.width(8.dp))
            // 命令按钮（占位，输入框侧也有更细致的命令入口）
            // 访问模式（session.accessMode 为空时给出友好默认，避免显示 "null"）
            DropdownChip(
                label = "访问模式: " + (session?.accessMode?.label ?: "自动"),
                icon = Icons.Outlined.Bolt,
                items = AccessMode.values().map { it.label to { onSelectAccess(it) } }
            )
            // 模型
            DropdownChip(
                label = "选择模型: " + (session?.modelId ?: "doubao-seed-2-1-pro"),
                icon = Icons.Outlined.Terminal,
                items = uiState.models.map { it.name to { onSelectModel(it.id) } }.ifEmpty {
                    listOf("doubao-seed-2-1-pro" to { onSelectModel("doubao-seed-2-1-pro") })
                }
            )
        }
    }
}

@Composable
private fun DropdownChip(
    label: String,
    icon: ImageVector,
    items: List<Pair<String, () -> Unit>>
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(harnessColors().surfaceVariant)
                .clickable { expanded = true }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, tint = harnessColors().secondaryText, modifier = Modifier.size(14.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = harnessColors().primaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Icons.Outlined.ArrowDropDown, null, tint = harnessColors().tertiaryText, modifier = Modifier.size(14.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { (label, action) ->
                DropdownMenuItem(text = { Text(label) }, onClick = { action(); expanded = false })
            }
        }
    }
}

@Composable
private fun SessionHeader(
    title: String,
    preset: String,
    backgroundTaskCount: Int,
    onBranch: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = harnessColors().primaryText)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(harnessColors().brandContainer)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(AgentPreset.BUILT_IN.firstOrNull { it.code == preset }?.name ?: "标准模式",
                style = MaterialTheme.typography.labelSmall, color = harnessColors().brand)
        }
        if (backgroundTaskCount > 0) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Outlined.TaskAlt, null, tint = harnessColors().warning, modifier = Modifier.size(12.dp))
                Text("$backgroundTaskCount 个后台任务", style = MaterialTheme.typography.labelSmall, color = harnessColors().secondaryText)
            }
        }
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = { /* 复制会话 ID */ }) {
            Icon(Icons.Outlined.ContentCopy, "复制会话ID", tint = harnessColors().tertiaryText, modifier = Modifier.size(16.dp))
        }
        IconButton(onClick = { /* 设置别名 */ }) {
            Icon(Icons.Outlined.Edit, "别名", tint = harnessColors().tertiaryText, modifier = Modifier.size(16.dp))
        }
        IconButton(onClick = onBranch, enabled = false) {
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, "分支", tint = harnessColors().tertiaryText, modifier = Modifier.size(16.dp))
        }
        IconButton(onClick = { /* Session log */ }) {
            Icon(Icons.Outlined.Forum, "Session log", tint = harnessColors().tertiaryText, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun SessionTabBar(
    active: SessionTab,
    onSelect: (SessionTab) -> Unit
) {
    val tabs = SessionTab.values().toList()
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(tabs) { tab ->
            TabPill(label = tab.label, active = tab == active) { onSelect(tab) }
        }
    }
}

@Composable
private fun TabPill(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) harnessColors().brandContainer else harnessColors().surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = if (active) harnessColors().brand else harnessColors().secondaryText)
    }
}

@Composable
private fun EmptyState() {
    val c = harnessColors()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        c.brandContainer.copy(alpha = 0.45f),
                        c.background.copy(alpha = 0f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(c.brand, c.purple))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.AutoAwesome,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text("选择或新建一个会话", style = MaterialTheme.typography.titleSmall, color = c.secondaryText)
            Text("DSH mobile · 探索未至之境", style = MaterialTheme.typography.labelMedium, color = c.tertiaryText)
        }
    }
}

@Composable
private fun LoadEarlierButton(onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            "加载更早",
            style = MaterialTheme.typography.labelMedium,
            color = harnessColors().brand,
            modifier = Modifier.clickable(onClick = onClick).padding(8.dp)
        )
    }
}

@Composable
private fun Composer(
    draft: String,
    onChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    sending: Boolean,
    onCommand: (CommandItem) -> Unit,
    commands: List<CommandItem>
) {
    var commandMenuOpen by remember { mutableStateOf(false) }
    Surface(
        color = harnessColors().surface,
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(harnessColors().surfaceVariant)
                            .clickable { commandMenuOpen = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Outlined.Terminal, "命令", tint = harnessColors().secondaryText, modifier = Modifier.size(14.dp))
                        Text("命令", style = MaterialTheme.typography.labelMedium, color = harnessColors().primaryText)
                        Icon(Icons.Outlined.ArrowDropDown, null, tint = harnessColors().tertiaryText, modifier = Modifier.size(14.dp))
                    }
                    DropdownMenu(expanded = commandMenuOpen, onDismissRequest = { commandMenuOpen = false }) {
                        commands.forEach { cmd ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(cmd.name, style = MaterialTheme.typography.labelLarge)
                                        Text(cmd.description, style = MaterialTheme.typography.labelSmall, color = harnessColors().tertiaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                },
                                onClick = { onCommand(cmd); commandMenuOpen = false }
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = draft,
                    onValueChange = onChange,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp, max = 200.dp),
                    placeholder = { Text("描述你想要构建的内容", color = harnessColors().tertiaryText) },
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = harnessColors().surfaceVariant,
                        unfocusedContainerColor = harnessColors().surfaceVariant,
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    textStyle = MaterialTheme.typography.bodyLarge
                )
                IconButton(
                    onClick = { if (sending) onStop() else onSend() },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (sending) harnessColors().danger else if (draft.isBlank()) harnessColors().surfaceVariant else harnessColors().brand)
                ) {
                    Icon(
                        if (sending) Icons.Filled.StopCircle else Icons.Filled.Send,
                        if (sending) "停止" else "发送消息",
                        tint = if (sending || draft.isNotBlank()) harnessColors().onBrand else harnessColors().tertiaryText
                    )
                }
            }
        }
    }
}
