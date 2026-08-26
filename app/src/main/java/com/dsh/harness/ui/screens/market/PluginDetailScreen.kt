package com.dsh.harness.ui.screens.market

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Source
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dsh.harness.data.model.MarketPlugin
import com.dsh.harness.data.model.MarketTab
import com.dsh.harness.ui.theme.harnessColors
import com.dsh.harness.ui.vm.PluginMarketViewModel

/**
 * 插件详情页：展示完整元数据、安装/卸载、收藏、版本、仓库链接。
 * 路由独立，从市场卡片点入。
 */
@Composable
fun PluginDetailScreen(pluginId: String, onBack: () -> Unit) {
    val vm: PluginMarketViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(pluginId) { vm.ensureLoaded(pluginId) }

    val plugin: MarketPlugin? = remember(state) {
        val all = state.recommend + state.recent + state.installed +
            state.favorites + state.bundles + state.sceneSuggestion
        all.firstOrNull { it.id == pluginId }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = harnessColors().background) {
        if (plugin == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(strokeWidth = 2.dp, color = harnessColors().brand)
            }
            return@Surface
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { DetailHeader(plugin = plugin, onBack = onBack) }
            item { ActionRow(plugin = plugin, vm = vm, context = context) }
            item { MetaCard(plugin = plugin) }
            if (plugin.tags.isNotEmpty()) {
                item { TagsRow(tags = plugin.tags) }
            }
            item { DescriptionCard(description = plugin.description) }
            item { RepoCard(plugin = plugin, context = context) }
            item {
                Text(
                    "插件 ID：${plugin.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = harnessColors().tertiaryText
                )
            }
        }
    }
}

@Composable
private fun DetailHeader(plugin: MarketPlugin, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                "返回",
                tint = harnessColors().secondaryText
            )
        }
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(harnessColors().brandContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Extension, null,
                tint = harnessColors().brand,
                modifier = Modifier.size(28.dp)
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(plugin.name, style = MaterialTheme.typography.titleLarge, color = harnessColors().primaryText)
            Text(
                "作者：${plugin.author ?: "未知"} · 类型 ${plugin.kind}",
                style = MaterialTheme.typography.labelSmall,
                color = harnessColors().tertiaryText
            )
        }
    }
}

@Composable
private fun ActionRow(
    plugin: MarketPlugin,
    vm: PluginMarketViewModel,
    context: android.content.Context
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = { vm.install(plugin.id) },
            enabled = !plugin.installed,
            colors = ButtonDefaults.buttonColors(containerColor = harnessColors().brand),
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Outlined.Download, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (plugin.installed) "已安装" else "安装")
        }
        OutlinedButton(
            onClick = { vm.favorite(plugin.id, !plugin.favorited) },
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                if (plugin.favorited) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(if (plugin.favorited) "取消收藏" else "收藏")
        }
        if (plugin.repoUrl != null) {
            IconButton(onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(plugin.repoUrl))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(intent) }
            }) {
                Icon(Icons.Outlined.OpenInNew, "打开仓库", tint = harnessColors().secondaryText)
            }
        }
    }
}

@Composable
private fun MetaCard(plugin: MarketPlugin) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(harnessColors().surface)
            .border(1.dp, harnessColors().outline, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        MetaItem(label = "Stars", value = "${plugin.stars}", icon = Icons.Outlined.Star)
        MetaItem(label = "安装量", value = "${plugin.installs}", icon = Icons.Outlined.Storefront)
        MetaItem(
            label = "更新",
            value = if (plugin.recentlyUpdated) "近30天活跃" else "无",
            icon = Icons.Outlined.Verified
        )
        MetaItem(
            label = "状态",
            value = if (plugin.installed) "已安装" else "未安装",
            icon = Icons.Outlined.Source
        )
    }
}

@Composable
private fun MetaItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, tint = harnessColors().brand, modifier = Modifier.size(18.dp))
        Text(value, style = MaterialTheme.typography.labelMedium, color = harnessColors().primaryText)
        Text(label, style = MaterialTheme.typography.labelSmall, color = harnessColors().tertiaryText)
    }
}

@Composable
private fun TagsRow(tags: List<String>) {
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 0.dp)
    ) {
        items(tags) { tag ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(harnessColors().surfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(tag, style = MaterialTheme.typography.labelSmall, color = harnessColors().tertiaryText)
            }
        }
    }
}

@Composable
private fun DescriptionCard(description: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(harnessColors().surface)
            .border(1.dp, harnessColors().outline, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("插件介绍", style = MaterialTheme.typography.titleSmall, color = harnessColors().primaryText)
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = harnessColors().secondaryText
        )
    }
}

@Composable
private fun RepoCard(plugin: MarketPlugin, context: android.content.Context) {
    if (plugin.repoUrl == null) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(harnessColors().brandContainer)
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(plugin.repoUrl))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(intent) }
            }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Outlined.Source, null, tint = harnessColors().brand, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("仓库地址", style = MaterialTheme.typography.labelSmall, color = harnessColors().tertiaryText)
            Text(
                plugin.repoUrl,
                style = MaterialTheme.typography.bodySmall,
                color = harnessColors().brand,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(Icons.Outlined.OpenInNew, null, tint = harnessColors().brand, modifier = Modifier.size(16.dp))
    }
}
