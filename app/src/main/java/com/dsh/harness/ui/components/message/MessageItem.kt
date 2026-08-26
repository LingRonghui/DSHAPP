package com.dsh.harness.ui.components.message

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dsh.harness.data.model.ChatMessage
import com.dsh.harness.data.model.MessageRole
import com.dsh.harness.data.model.ToolCall
import com.dsh.harness.ui.theme.harnessColors

/**
 * 单条消息渲染。支持：用户消息、助手回复、工具调用、失败重试、富文本。
 */
@Composable
fun MessageItem(message: ChatMessage) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 工具调用先于正文展示（与 Web 端顺序一致）
        message.toolCalls.forEach { tool ->
            ToolCallItem(tool = tool)
        }
        message.retryInfo?.let { RetryItem(info = it) }
        when (message.role) {
            MessageRole.USER -> UserMessage(content = message.content)
            MessageRole.ASSISTANT -> AssistantMessage(message = message)
            MessageRole.SYSTEM, MessageRole.TOOL -> SystemMessage(content = message.content)
        }
    }
}

@Composable
private fun UserMessage(content: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(harnessColors().brand)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(content, style = MaterialTheme.typography.bodyMedium, color = harnessColors().onBrand)
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            Icons.Outlined.Person, null,
            tint = harnessColors().tertiaryText,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun AssistantMessage(message: ChatMessage) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Icon(
            Icons.Outlined.SmartToy, null,
            tint = harnessColors().brand,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.widthIn(max = 720.dp)) {
            if (message.content.isBlank() && message.streaming) {
                StreamingPlaceholder()
            } else {
                RichContent(content = message.content, stopped = message.stopped)
            }
            if (!message.streaming && message.content.isNotBlank()) {
                Spacer(Modifier.size(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = { /* 复制 */ }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Outlined.ContentCopy, "复制",
                            tint = harnessColors().tertiaryText,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    if (message.stopped) {
                        Text("已停止", style = MaterialTheme.typography.labelSmall, color = harnessColors().tertiaryText)
                    }
                }
            }
        }
    }
}

@Composable
private fun SystemMessage(content: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(harnessColors().surfaceVariant)
            .padding(8.dp)
    ) {
        Text(content, style = MaterialTheme.typography.labelMedium, color = harnessColors().secondaryText)
    }
}

@Composable
private fun StreamingPlaceholder() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(harnessColors().brand)
        )
        Text("思考中…", style = MaterialTheme.typography.labelMedium, color = harnessColors().tertiaryText)
    }
}
