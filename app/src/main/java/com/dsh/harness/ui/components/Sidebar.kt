package com.dsh.harness.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.NewLabel
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewModule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dsh.harness.data.model.Session
import com.dsh.harness.data.model.Workspace
import com.dsh.harness.ui.theme.harnessColors
import com.dsh.harness.ui.vm.AppUiState
import com.dsh.harness.util.formatRelative
import java.util.Locale

/**
 * 侧边栏：工作区与会话树 + 操作入口。
 * 与 Web 端侧边栏一一对应。
 */
@Composable
fun Sidebar(
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
    onTogglePinned: (String, Boolean) -> Unit,
    onDeleteSession: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var addingWorkspace by remember { mutableStateOf(false) }
    var newWorkspaceName by remember { mutableStateOf("") }

    Column(modifier = modifier) {
        // 顶部操作行
        TopActions(
            onToggleSidebar = onToggleSidebar,
            onNewSession = onNewSession,
            onOpenSearch = { onOpenSearch(true) },
            onViewOptions = { /* 弹出视图选项 */ },
            onAddWorkspace = { addingWorkspace = true }
        )

        // 工作区标题
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text("工作区", style = MaterialTheme.typography.labelMedium, color = harnessColors().tertiaryText)
        }

        // 添加工作区输入
        if (addingWorkspace) {
            AddWorkspaceRow(
                value = newWorkspaceName,
                onChange = { newWorkspaceName = it },
                onConfirm = {
                    if (newWorkspaceName.isNotBlank()) {
                        onNewWorkspace(newWorkspaceName)
                        newWorkspaceName = ""
                    }
                    addingWorkspace = false
                },
                onCancel = { addingWorkspace = false; newWorkspaceName = "" }
            )
        }

        // 搜索框（已展开时）
        if (uiState.searchOpen) {
            SearchRow(
                value = uiState.searchQuery,
                onQueryChange = onSearchQueryChange,
                onClose = { onOpenSearch(false) }
            )
        }

        // 会话列表
        SessionsTree(
            workspaces = uiState.workspaces,
            sessions = uiState.visibleSessions,
            currentWorkspaceId = uiState.currentWorkspaceId,
            currentSessionId = uiState.currentSessionId,
            onSelectWorkspace = onSelectWorkspace,
            onSelectSession = onSelectSession,
            onTogglePinned = onTogglePinned,
            onDeleteSession = onDeleteSession,
            modifier = Modifier.weight(1f)
        )

        // 底部入口
        BottomEntries(
            onOpenMarket = onOpenMarket,
            onOpenSettings = onOpenSettings
        )

        // 品牌口号
        Text(
            "探索未至之境 · 预览版",
            style = MaterialTheme.typography.labelSmall,
            color = harnessColors().tertiaryText,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun TopActions(
    onToggleSidebar: () -> Unit,
    onNewSession: () -> Unit,
    onOpenSearch: () -> Unit,
    onViewOptions: () -> Unit,
    onAddWorkspace: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SidebarIconButton(icon = Icons.Outlined.Close, label = "收起侧边栏", onClick = onToggleSidebar)
        SidebarIconButton(icon = Icons.Outlined.NewLabel, label = "新建会话", onClick = onNewSession)
        SidebarIconButton(icon = Icons.Outlined.Search, label = "搜索会话", onClick = onOpenSearch)
        SidebarIconButton(icon = Icons.Outlined.ViewModule, label = "视图选项", onClick = onViewOptions)
        SidebarIconButton(icon = Icons.Outlined.Add, label = "添加工作区", onClick = onAddWorkspace)
    }
}

@Composable
private fun SidebarIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
        Icon(icon, contentDescription = label, tint = harnessColors().secondaryText, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun AddWorkspaceRow(
    value: String,
    onChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.OutlinedTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            placeholder = { Text("新工作区名", style = MaterialTheme.typography.bodySmall) },
            modifier = Modifier.weight(1f),
            textStyle = MaterialTheme.typography.bodySmall
        )
        IconButton(onClick = onCancel) { Icon(Icons.Outlined.Close, null, modifier = Modifier.size(16.dp)) }
        IconButton(onClick = onConfirm) {
            Icon(
                Icons.Filled.Check,
                null,
                modifier = Modifier.size(18.dp),
                tint = harnessColors().brand
            )
        }
    }
}

@Composable
private fun SearchRow(
    value: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.OutlinedTextField(
            value = value,
            onValueChange = onQueryChange,
            singleLine = true,
            placeholder = { Text("搜索会话", style = MaterialTheme.typography.bodySmall) },
            modifier = Modifier.weight(1f),
            textStyle = MaterialTheme.typography.bodySmall,
            leadingIcon = {
                Icon(
                    Icons.Outlined.Search,
                    null,
                    modifier = Modifier.size(16.dp),
                    tint = harnessColors().tertiaryText
                )
            },
            trailingIcon = {
                IconButton(onClick = onClose) {
                    Icon(Icons.Outlined.Close, null, modifier = Modifier.size(16.dp))
                }
            }
        )
    }
}

@Composable
private fun SessionsTree(
    workspaces: List<Workspace>,
    sessions: List<Session>,
    currentWorkspaceId: String?,
    currentSessionId: String?,
    onSelectWorkspace: (String) -> Unit,
    onSelectSession: (String) -> Unit,
    onTogglePinned: (String, Boolean) -> Unit,
    onDeleteSession: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        // 工作区节点 + 关联会话
        items(workspaces, key = { it.id }) { ws ->
            WorkspaceItem(
                workspace = ws,
                sessions = sessions.filter { it.workspaceId == ws.id },
                currentSessionId = currentSessionId,
                isCurrent = ws.id == currentWorkspaceId,
                onSelectWorkspace = onSelectWorkspace,
                onSelectSession = onSelectSession,
                onTogglePinned = onTogglePinned,
                onDeleteSession = onDeleteSession
            )
        }
        // 未分组会话
        val loose = sessions.filter { s -> workspaces.none { it.id == s.workspaceId } }
        if (loose.isNotEmpty()) {
            item {
                Text(
                    "未分组",
                    style = MaterialTheme.typography.labelMedium,
                    color = harnessColors().tertiaryText,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            items(loose, key = { "loose-${it.id}" }) { s ->
                SessionRow(
                    session = s,
                    selected = s.id == currentSessionId,
                    onSelect = { onSelectSession(s.id) },
                    onTogglePinned = onTogglePinned,
                    onDelete = onDeleteSession
                )
            }
        }
    }
}

@Composable
private fun WorkspaceItem(
    workspace: Workspace,
    sessions: List<Session>,
    currentSessionId: String?,
    isCurrent: Boolean,
    onSelectWorkspace: (String) -> Unit,
    onSelectSession: (String) -> Unit,
    onTogglePinned: (String, Boolean) -> Unit,
    onDeleteSession: (String) -> Unit
) {
    var expanded by remember(workspace.id) { mutableStateOf(true) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded; onSelectWorkspace(workspace.id) }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (expanded) Icons.Outlined.ExpandMore else Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = harnessColors().tertiaryText,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                workspace.name,
                style = MaterialTheme.typography.labelLarge,
                color = if (isCurrent) harnessColors().brand else harnessColors().primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (expanded) {
            sessions.forEach { s ->
                SessionRow(
                    session = s,
                    selected = s.id == currentSessionId,
                    onSelect = { onSelectSession(s.id) },
                    onTogglePinned = onTogglePinned,
                    onDelete = onDeleteSession,
                    indent = 28
                )
            }
        }
    }
}

@Composable
private fun SessionRow(
    session: Session,
    selected: Boolean,
    onSelect: () -> Unit,
    onTogglePinned: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    indent: Int = 0
) {
    var menuOpen by remember(session.id) { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indent.dp)
            .background(if (selected) harnessColors().brandContainer else Color.Transparent)
            .clickable(onClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (selected) harnessColors().brand else harnessColors().outline)
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                session.alias ?: session.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) harnessColors().brand else harnessColors().primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            session.updatedAt.takeIf { it > 0 }?.let {
                Text(
                    formatRelative(it),
                    style = MaterialTheme.typography.labelSmall,
                    color = harnessColors().tertiaryText
                )
            }
        }
        if (session.pinned) {
            Icon(
                Icons.Filled.PushPin,
                contentDescription = "已置顶",
                tint = harnessColors().brand,
                modifier = Modifier.size(14.dp)
            )
        }
        Box {
            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Outlined.MoreHoriz, null, tint = harnessColors().tertiaryText, modifier = Modifier.size(16.dp))
            }
            androidx.compose.material3.DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false }
            ) {
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(if (session.pinned) "取消置顶" else "置顶") },
                    onClick = {
                        onTogglePinned(session.id, !session.pinned)
                        menuOpen = false
                    }
                )
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text("删除会话") },
                    onClick = {
                        onDelete(session.id)
                        menuOpen = false
                    }
                )
            }
        }
    }
}

@Composable
private fun BottomEntries(
    onOpenMarket: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
        BottomEntry(
            icon = Icons.Outlined.Extension,
            label = "插件市场",
            badge = "DSH Market",
            onClick = onOpenMarket
        )
        BottomEntry(
            icon = Icons.Outlined.Settings,
            label = "设置",
            onClick = onOpenSettings
        )
    }
}

@Composable
private fun BottomEntry(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    badge: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = harnessColors().secondaryText, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = harnessColors().primaryText)
        if (badge != null) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .border(1.dp, harnessColors().outline, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(badge, style = MaterialTheme.typography.labelSmall, color = harnessColors().tertiaryText)
            }
        }
    }
}
