package com.dsh.harness.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrowserUpdated
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DeviceHub
import androidx.compose.material.icons.outlined.EmojiObjects
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Source
import androidx.compose.material.icons.outlined.SyncAlt
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dsh.harness.data.model.MemoryItem
import com.dsh.harness.data.model.SessionTab
import com.dsh.harness.data.model.SideCard
import com.dsh.harness.data.model.SideCardGroup
import com.dsh.harness.data.model.SkillItem
import com.dsh.harness.data.model.TodoItem
import com.dsh.harness.ui.theme.harnessColors
import com.dsh.harness.ui.vm.AppUiState

/**
 * 详情面板：根据 activeTab 渲染对应内容。
 * 默认（DIALOG）展示侧边卡片列表（与 Web 端"详情/侧边卡片"一致）。
 */
@Composable
fun DetailPanel(
    uiState: AppUiState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 标题
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("详情", style = MaterialTheme.typography.titleMedium, color = harnessColors().primaryText)
        }
        Text(
            "点击消息流中的工具行查看详情",
            style = MaterialTheme.typography.labelSmall,
            color = harnessColors().tertiaryText,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        Spacer(Modifier.height(8.dp))
        when (uiState.activeTab) {
            SessionTab.DIALOG -> SideCardsList(cards = uiState.sideCards)
            SessionTab.TRACE -> TraceList(items = emptyList())
            SessionTab.MEMORY -> MemoryList(items = emptyList())
            SessionTab.SKILLS -> SkillsList(items = emptyList())
            SessionTab.TODOS -> TodosList(items = emptyList())
            SessionTab.MEMORY_SYNC -> MemorySyncPanel()
            SessionTab.CANVAS -> CanvasPanel()
            SessionTab.MODEL_SETTINGS -> ModelSettingsPanel()
            SessionTab.MEMORY_EVOLVE -> MemoryEvolveSettingsPanel()
        }
    }
}

@Composable
private fun SideCardsList(cards: List<SideCard>) {
    val sidebarContent = cards.filter { it.group == SideCardGroup.SIDEBAR_CONTENT }
    val filePreview = cards.filter { it.group == SideCardGroup.FILE_PREVIEW }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SectionHeader("侧边栏内容 ${sidebarContent.size}") }
        items(sidebarContent) { SideCardRow(it) }
        item { SectionHeader("文件预览 ${filePreview.size}") }
        items(filePreview) { SideCardRow(it) }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = harnessColors().tertiaryText)
}

@Composable
private fun SideCardRow(card: SideCard) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(harnessColors().brandContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(sideCardIcon(card.id), null, tint = harnessColors().brand, modifier = Modifier.size(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(card.label, style = MaterialTheme.typography.labelLarge, color = harnessColors().primaryText)
            Text(card.tag, style = MaterialTheme.typography.labelSmall, color = harnessColors().tertiaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (card.enabled) harnessColors().brand else harnessColors().outline)
        )
    }
}

private fun sideCardIcon(id: String) = when (id) {
    "scm" -> Icons.Outlined.Source
    "task" -> Icons.Outlined.TaskAlt
    "terminal" -> Icons.Outlined.Terminal
    "browser" -> Icons.Outlined.BrowserUpdated
    "editor" -> Icons.Outlined.Folder
    "diff" -> Icons.Outlined.SyncAlt
    "img" -> Icons.Outlined.GraphicEq
    "pdf" -> Icons.Outlined.Code
    "md" -> Icons.Outlined.Code
    "html" -> Icons.Outlined.Code
    "bin" -> Icons.Outlined.Folder
    "code" -> Icons.Outlined.Code
    else -> Icons.Outlined.Extension
}

@Composable
private fun TraceList(items: List<String>) {
    EmptyDetail(icon = Icons.Outlined.History, text = "暂无轨迹数据")
}

@Composable
private fun MemoryList(items: List<MemoryItem>) {
    EmptyDetail(icon = Icons.Outlined.Memory, text = "记忆会在会话进行中累积")
}

@Composable
private fun SkillsList(items: List<SkillItem>) {
    EmptyDetail(icon = Icons.Outlined.Psychology, text = "Agent 技能列表")
}

@Composable
private fun TodosList(items: List<TodoItem>) {
    EmptyDetail(icon = Icons.Outlined.Checklist, text = "Agent 任务清单")
}

@Composable
private fun MemorySyncPanel() {
    EmptyDetail(icon = Icons.Outlined.SyncAlt, text = "跨设备记忆同步")
}

@Composable
private fun CanvasPanel() {
    EmptyDetail(icon = Icons.Outlined.DeviceHub, text = "画板暂未实现")
}

@Composable
private fun ModelSettingsPanel() {
    EmptyDetail(icon = Icons.Outlined.Settings, text = "本会话模型设置")
}

@Composable
private fun MemoryEvolveSettingsPanel() {
    EmptyDetail(icon = Icons.Outlined.EmojiObjects, text = "Memory Evolve 设置")
}

@Composable
private fun EmptyDetail(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = harnessColors().tertiaryText, modifier = Modifier.size(48.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, color = harnessColors().tertiaryText)
        }
    }
}
