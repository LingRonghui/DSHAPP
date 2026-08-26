package com.dsh.harness.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.dsh.harness.data.model.AccessMode
import com.dsh.harness.data.model.AgentPreset
import com.dsh.harness.data.model.ModelProvider
import com.dsh.harness.data.model.SideCard
import com.dsh.harness.data.model.SideCardGroup
import com.dsh.harness.data.remote.ProviderReq
import com.dsh.harness.data.remote.ServerDiagnostic
import com.dsh.harness.ui.theme.ThemeMode
import com.dsh.harness.ui.theme.harnessColors

/** 通用设置。 */
@Composable
fun GeneralSettings(
    themeMode: ThemeMode,
    language: String,
    defaultPreset: String,
    defaultAccess: AccessMode,
    busyEnter: String,
    serverBaseUrl: String,
    diagRunning: Boolean,
    diagResults: List<ServerDiagnostic.Step>,
    onThemeMode: (ThemeMode) -> Unit,
    onLanguage: (String) -> Unit,
    onPreset: (String) -> Unit,
    onAccess: (AccessMode) -> Unit,
    onBusyEnter: (String) -> Unit,
    onServerBaseUrl: (String) -> Unit,
    onRunDiag: () -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Agent 预设", style = MaterialTheme.typography.labelMedium, color = harnessColors().tertiaryText) }
        item {
            SegmentedRow(
                options = AgentPreset.BUILT_IN.map { it.code to it.name },
                selected = defaultPreset,
                onSelect = onPreset
            )
        }
        item { Divider() }
        item { Text("权限 选择新会话的默认权限模式", style = MaterialTheme.typography.labelMedium, color = harnessColors().tertiaryText) }
        item {
            SegmentedRow(
                options = AccessMode.values().map { it.key to it.label },
                selected = defaultAccess.key,
                onSelect = { key -> onAccess(AccessMode.fromKey(key)) }
            )
        }
        item { Divider() }
        item { Text("语言", style = MaterialTheme.typography.labelMedium, color = harnessColors().tertiaryText) }
        item {
            SegmentedRow(
                options = listOf("zh-CN" to "中文", "en" to "English"),
                selected = language,
                onSelect = onLanguage
            )
        }
        item { Divider() }
        item { Text("外观", style = MaterialTheme.typography.labelMedium, color = harnessColors().tertiaryText) }
        item {
            SegmentedRow(
                options = listOf(
                    ThemeMode.Light.name to "浅色",
                    ThemeMode.Dark.name to "深色",
                    ThemeMode.System.name to "跟随系统"
                ),
                selected = themeMode.name,
                onSelect = { name -> onThemeMode(ThemeMode.valueOf(name)) }
            )
        }
        item { Divider() }
        item {
            Text(
                "繁忙时 Enter 键行为 仅在智能体运行时生效；Cmd/Ctrl+Enter 使用另一行为",
                style = MaterialTheme.typography.labelSmall,
                color = harnessColors().tertiaryText
            )
        }
        item {
            SegmentedRow(
                options = listOf("queue" to "排队发送", "send" to "立即发送", "ignore" to "忽略"),
                selected = busyEnter,
                onSelect = onBusyEnter
            )
        }
        item { Divider() }
        item { Text("服务器地址", style = MaterialTheme.typography.labelMedium, color = harnessColors().tertiaryText) }
        item {
            OutlinedTextField(
                value = serverBaseUrl,
                onValueChange = onServerBaseUrl,
                singleLine = true,
                label = { Text("服务器地址") },
                supportingText = { Text("例如 https://你的服务器域名或IP/（含 http(s):// 和结尾斜杠）") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item { Divider() }
        item { Text("连接自检", style = MaterialTheme.typography.labelMedium, color = harnessColors().tertiaryText) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onRunDiag,
                    enabled = !diagRunning,
                    colors = ButtonDefaults.buttonColors(containerColor = harnessColors().brand)
                ) {
                    Text(if (diagRunning) "检测中…" else "开始检测")
                }
            }
        }
        if (diagResults.isNotEmpty()) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(harnessColors().sidebar)
                        .padding(12.dp)
                ) {
                    diagResults.forEach { s ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(if (s.ok) "✅" else "❌", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "${s.label}：${s.detail}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (s.ok) harnessColors().primaryText else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 模型设置：提供方列表 + 添加/编辑/删除。 */
@Composable
fun ModelsSettings(
    providers: List<ModelProvider>,
    onAdd: () -> Unit,
    onEdit: (ModelProvider) -> Unit,
    onDelete: (String) -> Unit
) {
    Column {
        Text("填入各提供方的 API 密钥即可使用其模型。", style = MaterialTheme.typography.bodySmall, color = harnessColors().tertiaryText)
        Spacer(Modifier.size(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            items(providers, key = { it.id }) { p ->
                ProviderRow(provider = p, onEdit = { onEdit(p) }, onDelete = { onDelete(p.id) })
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = harnessColors().brand)) {
                        Icon(Icons.Outlined.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("添加提供方")
                    }
                    OutlinedButton(onClick = onAdd) { Text("添加自定义提供方") }
                }
            }
        }
    }
}

@Composable
private fun ProviderRow(
    provider: ModelProvider,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(harnessColors().surface)
            .border(1.dp, harnessColors().outline, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(provider.name, style = MaterialTheme.typography.labelLarge, color = harnessColors().primaryText)
                if (provider.custom) Text("自定义", style = MaterialTheme.typography.labelSmall, color = harnessColors().tertiaryText)
                if (provider.apiKeyConfigured) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(harnessColors().success)
                    )
                    Text("API 密钥已配置", style = MaterialTheme.typography.labelSmall, color = harnessColors().secondaryText)
                }
            }
            provider.code?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = harnessColors().tertiaryText)
            }
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Outlined.Edit, "编辑", tint = harnessColors().secondaryText, modifier = Modifier.size(16.dp))
        }
        if (provider.custom) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, "删除", tint = harnessColors().danger, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/** 插件设置：3 个 Tab。 */
@Composable
fun PluginsSettings() {
    var tab by remember { mutableStateOf(0) }
    val tabs = listOf("插件配置", "插件列表", "IM机器人")
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            tabs.forEachIndexed { idx, label ->
                val selected = idx == tab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) harnessColors().brand else harnessColors().surfaceVariant)
                        .clickable { tab = idx }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = if (selected) harnessColors().onBrand else harnessColors().secondaryText)
                }
            }
        }
        Spacer(Modifier.size(12.dp))
        when (tab) {
            0 -> Text("视觉引擎（ModLens） 视觉引擎提供商配置。", style = MaterialTheme.typography.bodySmall, color = harnessColors().secondaryText)
            1 -> Text("已安装插件：可在此启用/禁用。", style = MaterialTheme.typography.bodySmall, color = harnessColors().secondaryText)
            2 -> Text("IM 机器人：将 DSH 接入 IM 平台。", style = MaterialTheme.typography.bodySmall, color = harnessColors().secondaryText)
        }
    }
}

/** Agent 预设：内置 + 自定义。 */
@Composable
fun AgentPresetsSettings(current: String, onSelect: (String) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("预设即一个会话的 Agent 所运行的插件组装 —— 它的工具、提示词与能力。复制一份既有预设改成自己的，或用「创造模式」让 Agent 帮你创建。",
                style = MaterialTheme.typography.bodySmall, color = harnessColors().secondaryText)
        }
        item { Text("内置", style = MaterialTheme.typography.labelMedium, color = harnessColors().tertiaryText) }
        items(AgentPreset.BUILT_IN, key = { it.id }) { preset ->
            PresetRow(preset, inUse = preset.code == current, onSelect = { onSelect(preset.code) })
        }
        item { Text("自定义", style = MaterialTheme.typography.labelMedium, color = harnessColors().tertiaryText) }
        item {
            OutlinedButton(onClick = { /* 创造模式 */ }, modifier = Modifier.fillMaxWidth()) {
                Text("用「创造模式」创作自定义预设")
            }
        }
    }
}

@Composable
private fun PresetRow(preset: AgentPreset, inUse: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(harnessColors().surface)
            .border(1.dp, harnessColors().outline, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(preset.name, style = MaterialTheme.typography.labelLarge, color = harnessColors().primaryText)
                Text("内置", style = MaterialTheme.typography.labelSmall, color = harnessColors().tertiaryText)
                if (inUse) Text("当前使用", style = MaterialTheme.typography.labelSmall, color = harnessColors().brand)
            }
            Text(preset.description, style = MaterialTheme.typography.bodySmall, color = harnessColors().secondaryText, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text(preset.code, style = MaterialTheme.typography.labelSmall, color = harnessColors().tertiaryText)
        }
        if (!inUse) {
            TextButton(onClick = onSelect) { Text("设为默认") }
        } else {
            TextButton(onClick = { /* 查看 */ }) { Text("查看") }
        }
    }
}

/** 侧边卡片设置。 */
@Composable
fun SideCardsSettings(
    sideOpen: Boolean,
    sideWidth: Int,
    fileInSide: Boolean,
    posCompat: Boolean,
    cards: List<SideCard>,
    onSideOpen: (Boolean) -> Unit,
    onSideWidth: (Int) -> Unit,
    onFileInSide: (Boolean) -> Unit,
    onPosCompat: (Boolean) -> Unit,
    onToggleCard: (String, Boolean) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("管理侧边卡片的显示内容与默认行为", style = MaterialTheme.typography.bodySmall, color = harnessColors().secondaryText) }
        item { Text("常规", style = MaterialTheme.typography.labelMedium, color = harnessColors().tertiaryText) }
        item {
            ToggleRow(
                title = "新会话默认打开",
                subtitle = "新建会话时自动展开侧边卡片；已存在的会话保持各自布局",
                checked = sideOpen, onToggle = onSideOpen
            )
        }
        item {
            NumberRow(
                title = "默认宽度占比",
                subtitle = "新建会话时侧边卡片占窗口宽度的百分比 (20–60)",
                value = sideWidth,
                onChange = onSideWidth
            )
        }
        item {
            ToggleRow(
                title = "聊天区文件在侧边栏打开",
                subtitle = "在聊天里点击文件链接（工具行、产物列表、文件提及）时，在侧边栏编辑器中打开，不再调用系统默认应用",
                checked = fileInSide, onToggle = onFileInSide
            )
        }
        item {
            ToggleRow(
                title = "位置兼容模式",
                subtitle = "为 Windows 右上角的原生标题栏预留空间：侧边栏按钮与侧边栏内容整体下移，避免被标题栏遮挡",
                checked = posCompat, onToggle = onPosCompat
            )
        }
        item { Divider() }
        item { Text("侧边栏内容 ${cards.count { it.group == SideCardGroup.SIDEBAR_CONTENT }}", style = MaterialTheme.typography.labelMedium, color = harnessColors().tertiaryText) }
        items(cards.filter { it.group == SideCardGroup.SIDEBAR_CONTENT }) { card ->
            CardToggleRow(card = card, onToggle = { onToggleCard(card.id, it) })
        }
        item { Text("文件预览 ${cards.count { it.group == SideCardGroup.FILE_PREVIEW }}", style = MaterialTheme.typography.labelMedium, color = harnessColors().tertiaryText) }
        items(cards.filter { it.group == SideCardGroup.FILE_PREVIEW }) { card ->
            CardToggleRow(card = card, onToggle = { onToggleCard(card.id, it) })
        }
    }
}

@Composable
private fun CardToggleRow(card: SideCard, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(card.label, style = MaterialTheme.typography.labelLarge, color = harnessColors().primaryText)
            Text(card.tag, style = MaterialTheme.typography.labelSmall, color = harnessColors().tertiaryText)
        }
        Switch(checked = card.enabled, onCheckedChange = onToggle)
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = harnessColors().primaryText)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = harnessColors().tertiaryText)
        }
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

@Composable
private fun NumberRow(title: String, subtitle: String, value: Int, onChange: (Int) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    Column {
        Text(title, style = MaterialTheme.typography.labelLarge, color = harnessColors().primaryText)
        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = harnessColors().tertiaryText)
        OutlinedTextField(
            value = text,
            onValueChange = { text = it; it.toIntOrNull()?.let(onChange) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun SegmentedRow(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { (key, label) ->
            val isSelected = key == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) harnessColors().brand else harnessColors().surfaceVariant)
                    .border(1.dp, if (isSelected) harnessColors().brand else harnessColors().outline, RoundedCornerShape(8.dp))
                    .clickable { onSelect(key) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(label, style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) harnessColors().onBrand else harnessColors().secondaryText)
            }
        }
    }
}

@Composable
private fun Divider() {
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(harnessColors().outline))
}

/** 添加/编辑提供方弹窗。 */
@Composable
fun ProviderDialog(
    editing: String?,
    initial: ModelProvider?,
    onConfirm: (ProviderReq) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var code by remember(initial) { mutableStateOf(initial?.code ?: "") }
    var baseUrl by remember(initial) { mutableStateOf(initial?.baseUrl ?: "") }
    var apiKey by remember { mutableStateOf("") }
    val custom = initial?.custom ?: true

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editing == null) "添加提供方" else "编辑 ${initial?.name ?: ""}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text("名称") })
                OutlinedTextField(value = code, onValueChange = { code = it }, singleLine = true, label = { Text("代码 (可选)") })
                OutlinedTextField(value = baseUrl, onValueChange = { baseUrl = it }, singleLine = true, label = { Text("Base URL (可选)") })
                OutlinedTextField(
                    value = apiKey, onValueChange = { apiKey = it }, singleLine = true,
                    label = { Text("API Key") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(ProviderReq(name = name.ifBlank { "未命名" }, code = code.ifBlank { null }, baseUrl = baseUrl.ifBlank { null }, apiKey = apiKey.ifBlank { null }, custom = custom))
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
