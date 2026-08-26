package com.dsh.harness.ui.screens.market

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Source
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dsh.harness.data.model.MarketPlugin
import com.dsh.harness.data.model.MarketTab
import com.dsh.harness.ui.theme.harnessColors
import com.dsh.harness.ui.vm.PluginMarketViewModel

/**
 * 插件市场：与 Web 端 DSH Market 一致。
 * 顶部 Tab、模式切换、场景推荐、猜你喜欢、最近更新、搜索、收藏、已装、设置。
 */
@Composable
fun PluginMarketScreen(onClose: () -> Unit) {
    val vm: PluginMarketViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.bootstrap() }

    Surface(modifier = Modifier.fillMaxSize(), color = harnessColors().background) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部条
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.Extension, null, tint = harnessColors().brand, modifier = Modifier.size(28.dp))
                Text("插件市场", style = MaterialTheme.typography.titleLarge, color = harnessColors().primaryText)
                Text("${state.total} 个插件", style = MaterialTheme.typography.labelSmall, color = harnessColors().tertiaryText)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, "关闭", tint = harnessColors().tertiaryText)
                }
            }
            // Tab 行
            MarketTabBar(active = state.tab, onSelect = vm::setTab)
            // 更新提示条
            state.updating?.let { p ->
                UpdateBar(
                    pluginName = p.name,
                    from = p.fromVersion ?: "",
                    to = p.toVersion ?: "",
                    onIgnore = { vm.ignoreUpdate(p.id) },
                    onUpdate = { vm.install(p.id) }
                )
            }
            // 模式切换提示
            if (state.tab == MarketTab.RECOMMEND) {
                ModeBanner(rookie = state.rookieMode, onToggle = vm::toggleMode)
                // 适合当前场景
                SceneSuggestionBanner(loading = state.sceneLoading, onFetch = vm::fetchSceneSuggestion)
                // 猜你喜欢
                SectionTitle("猜你喜欢")
                // 网格 + 列表
            }
            // 搜索框（仅 Search Tab）
            if (state.tab == MarketTab.SEARCH) {
                SearchBar(value = state.query, onChange = vm::setQuery, onSearch = vm::search)
            }
            // 内容列表
            val list = when (state.tab) {
                MarketTab.RECOMMEND -> state.recommend
                MarketTab.SEARCH -> if (state.query.isNotBlank()) state.recommend.filter { it.name.contains(state.query, true) } else state.recent
                MarketTab.BUNDLE -> state.bundles
                MarketTab.FAVORITE -> state.favorites
                MarketTab.INSTALLED -> state.installed
                MarketTab.SETTINGS -> emptyList()
            }
            if (state.tab == MarketTab.SETTINGS) {
                SettingsPane()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (state.tab == MarketTab.RECOMMEND) {
                        item { SectionTitle("最近更新") }
                    }
                    items(list, key = { it.id }) { p ->
                        PluginCard(plugin = p, onInstall = { vm.install(p.id) }, onFavorite = { vm.favorite(p.id, !p.favorited) })
                    }
                    if (state.tab == MarketTab.RECOMMEND) {
                        item {
                            Text("已排除 ${state.excludedInstalledCount} 个已安装插件", style = MaterialTheme.typography.labelSmall, color = harnessColors().tertiaryText)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarketTabBar(active: MarketTab, onSelect: (MarketTab) -> Unit) {
    val tabs = MarketTab.values().toList()
    androidx.compose.foundation.lazy.LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(tabs) { tab ->
            val selected = tab == active
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) harnessColors().brand else harnessColors().surfaceVariant)
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(tab.label, style = MaterialTheme.typography.labelMedium,
                    color = if (selected) harnessColors().onBrand else harnessColors().secondaryText)
            }
        }
    }
}

@Composable
private fun UpdateBar(pluginName: String, from: String, to: String, onIgnore: () -> Unit, onUpdate: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(harnessColors().surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("插件有新版本 $from → $to", style = MaterialTheme.typography.labelMedium, color = harnessColors().primaryText)
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onIgnore) { Text("忽略") }
        Button(onClick = onUpdate, colors = ButtonDefaults.buttonColors(containerColor = harnessColors().brand)) {
            Text("更新")
        }
    }
}

@Composable
private fun ModeBanner(rookie: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(harnessColors().brandContainer)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            if (rookie) "当前为 新手模式 ，推荐更稳妥通用。可切换到 个性化模式 获取贴合你工作流的建议。"
            else "当前为 个性化模式 ，推荐贴合你的工作流。可切换到 新手模式 获取通用建议。",
            style = MaterialTheme.typography.labelSmall,
            color = harnessColors().secondaryText,
            modifier = Modifier.weight(1f)
        )
        OutlinedButton(onClick = onToggle) {
            Text(if (rookie) "切换到 · 个性化模式" else "切换到 · 新手模式")
        }
    }
}

@Composable
private fun SceneSuggestionBanner(loading: Boolean, onFetch: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Column {
                Text("适合当前场景", style = MaterialTheme.typography.titleMedium, color = harnessColors().primaryText)
                Text("根据你当前正在做的事推荐插件（点击获取，基于会话内容）", style = MaterialTheme.typography.labelSmall, color = harnessColors().tertiaryText)
                Spacer(Modifier.size(8.dp))
                Button(onClick = onFetch, enabled = !loading) {
                    if (loading) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(14.dp), color = harnessColors().onBrand)
                        Spacer(Modifier.size(6.dp))
                        Text("获取中…")
                    } else {
                        Text("获取场景推荐")
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = harnessColors().primaryText,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp))
}

@Composable
private fun SearchBar(value: String, onChange: (String) -> Unit, onSearch: () -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange, singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        placeholder = { Text("搜索插件名称、关键词…") },
        leadingIcon = { Icon(Icons.Outlined.Search, null, tint = harnessColors().tertiaryText) },
        trailingIcon = {
            IconButton(onClick = { onChange(""); onSearch() }) { Icon(Icons.Outlined.Clear, null, tint = harnessColors().tertiaryText) }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
    )
}

@Composable
private fun PluginCard(
    plugin: MarketPlugin,
    onInstall: () -> Unit,
    onFavorite: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(harnessColors().surface)
            .border(1.dp, harnessColors().outline, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(harnessColors().brandContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Extension, null, tint = harnessColors().brand, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(plugin.name, style = MaterialTheme.typography.labelLarge, color = harnessColors().primaryText)
                Text(plugin.kind, style = MaterialTheme.typography.labelSmall, color = harnessColors().tertiaryText)
                Text("${plugin.stars}", style = MaterialTheme.typography.labelSmall, color = harnessColors().tertiaryText)
                if (plugin.recentlyUpdated) Text("近 30 天更新活跃", style = MaterialTheme.typography.labelSmall, color = harnessColors().success)
            }
            Text(plugin.description, style = MaterialTheme.typography.bodySmall, color = harnessColors().secondaryText, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (plugin.tags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    plugin.tags.take(4).forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(harnessColors().surfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(tag, style = MaterialTheme.typography.labelSmall, color = harnessColors().tertiaryText)
                        }
                    }
                }
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onFavorite, modifier = Modifier.size(28.dp)) {
                Icon(
                    if (plugin.favorited) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                    "收藏", tint = harnessColors().secondaryText, modifier = Modifier.size(16.dp)
                )
            }
            IconButton(onClick = { /* 仓库 */ }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Outlined.Source, "仓库", tint = harnessColors().secondaryText, modifier = Modifier.size(16.dp))
            }
            Button(
                onClick = onInstall,
                enabled = !plugin.installed,
                colors = ButtonDefaults.buttonColors(containerColor = harnessColors().brand),
                modifier = Modifier.heightIn(min = 28.dp)
            ) {
                Text(if (plugin.installed) "已安装" else "安装")
            }
        }
    }
}

@Composable
private fun SettingsPane() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("插件设置", style = MaterialTheme.typography.titleMedium, color = harnessColors().primaryText)
        Spacer(Modifier.size(8.dp))
        Text("在此管理 GitHub 绑定、缓存目录、镜像源等高级选项。", style = MaterialTheme.typography.bodySmall, color = harnessColors().tertiaryText)
    }
}
