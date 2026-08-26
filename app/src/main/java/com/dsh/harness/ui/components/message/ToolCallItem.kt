package com.dsh.harness.ui.components.message

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.CircularProgressIndicator
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
import com.dsh.harness.data.model.ToolCall
import com.dsh.harness.data.model.ToolKind
import com.dsh.harness.data.model.ToolStatus
import com.dsh.harness.ui.theme.harnessColors
import java.util.Locale

/**
 * 工具调用项：Edit/Read/Bash/Think/Tool call 等。
 * 与 Web 端工具行折叠一致：标题 + 状态 + 可展开。
 */
@Composable
fun ToolCallItem(tool: ToolCall) {
    var expanded by remember(tool.id) { mutableStateOf(tool.expanded) }
    val color = when (tool.kind) {
        ToolKind.EDIT -> harnessColors().toolEdit
        ToolKind.READ -> harnessColors().toolRead
        ToolKind.BASH -> harnessColors().toolBash
        ToolKind.THINK -> harnessColors().toolThink
        ToolKind.TOOL_CALL -> harnessColors().brand
        ToolKind.MEMORY -> harnessColors().brand
        ToolKind.AGENT_TEAMS -> harnessColors().purple
        ToolKind.COMPACT -> harnessColors().accent
        ToolKind.EXPORT -> harnessColors().accent
        ToolKind.FEEDBACK -> harnessColors().accent
        ToolKind.GOAL -> harnessColors().warning
        ToolKind.PERMISSION -> harnessColors().warning
        ToolKind.PLAN -> harnessColors().brand
        ToolKind.MODEL -> harnessColors().brand
        ToolKind.ADVISOR -> harnessColors().brand
        ToolKind.UNKNOWN -> harnessColors().secondaryText
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(harnessColors().surfaceVariant)
                .clickable { expanded = !expanded }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 状态指示
            when (tool.status) {
                ToolStatus.RUNNING -> CircularProgressIndicator(
                    modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = color
                )
                ToolStatus.SUCCESS -> Box(
                    modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(color)
                )
                ToolStatus.FAILED -> Icon(Icons.Outlined.Error, null, tint = harnessColors().toolFail, modifier = Modifier.size(14.dp))
            }
            // 失败前缀
            if (tool.status == ToolStatus.FAILED) {
                Text("失败", style = MaterialTheme.typography.labelSmall, color = harnessColors().toolFail)
                Spacer(Modifier.width(4.dp))
            }
            // 工具类型标签
            Text(
                tool.kind.labelName(),
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
            // 标题/目标
            Text(
                tool.title,
                style = MaterialTheme.typography.labelMedium,
                color = harnessColors().secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            // 时长
            tool.durationMs?.takeIf { it > 0 }?.let {
                Text(formatDuration(it), style = MaterialTheme.typography.labelSmall, color = harnessColors().tertiaryText)
            }
            Icon(
                if (expanded) Icons.Outlined.ExpandMore else Icons.Outlined.ChevronRight,
                null,
                tint = harnessColors().tertiaryText,
                modifier = Modifier.size(14.dp)
            )
        }
        AnimatedVisibility(visible = expanded, enter = fadeIn(tween(120)), exit = fadeOut(tween(120))) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, top = 6.dp, bottom = 6.dp, end = 8.dp)
            ) {
                tool.description?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = harnessColors().secondaryText)
                    Spacer(Modifier.size(4.dp))
                }
                tool.target?.let {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(harnessColors().codeBlock)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = harnessColors().codeBlockText)
                    }
                }
                tool.failureReason?.let {
                    Spacer(Modifier.size(4.dp))
                    Text(
                        "失败原因: $it",
                        style = MaterialTheme.typography.labelSmall,
                        color = harnessColors().toolFail
                    )
                }
            }
        }
    }
}

@Composable
fun RetryItem(info: com.dsh.harness.data.model.RetryInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            Icons.Outlined.PlayArrow, null,
            tint = harnessColors().warning,
            modifier = Modifier.size(14.dp)
        )
        Text(
            "已重试模型请求（${info.attempt}/${info.totalAttempts}）· ${info.delayMs / 1000}s",
            style = MaterialTheme.typography.labelSmall,
            color = harnessColors().secondaryText
        )
    }
    info.reason.takeIf { it.isNotBlank() }?.let {
        Text(
            "失败原因: $it",
            style = MaterialTheme.typography.labelSmall,
            color = harnessColors().tertiaryText,
            modifier = Modifier.padding(start = 38.dp, bottom = 4.dp)
        )
    }
}

private fun ToolKind.labelName(): String = when (this) {
    ToolKind.EDIT -> "Edit"
    ToolKind.READ -> "Read"
    ToolKind.BASH -> "Bash"
    ToolKind.THINK -> "Think"
    ToolKind.TOOL_CALL -> "Tool call"
    ToolKind.MEMORY -> "Tool call · memory"
    ToolKind.AGENT_TEAMS -> "agent-teams"
    ToolKind.COMPACT -> "compact"
    ToolKind.EXPORT -> "export"
    ToolKind.FEEDBACK -> "feedback"
    ToolKind.GOAL -> "goal"
    ToolKind.PERMISSION -> "permission"
    ToolKind.PLAN -> "plan"
    ToolKind.MODEL -> "model"
    ToolKind.ADVISOR -> "advisor"
    ToolKind.UNKNOWN -> "Tool"
}

private fun formatDuration(ms: Long): String {
    val sec = ms / 1000.0
    return if (sec < 60) String.format(Locale.US, "%.1fs", sec)
    else "${sec / 60}m ${sec % 60}s"
}
