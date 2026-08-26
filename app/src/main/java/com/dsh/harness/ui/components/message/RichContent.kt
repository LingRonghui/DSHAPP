package com.dsh.harness.ui.components.message

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.dsh.harness.data.model.ChatMessage
import com.dsh.harness.ui.theme.harnessColors

/**
 * 助手富文本渲染：标题、代码块、行内代码、列表、引用、表格。
 * 用纯 Compose 解析常见 Markdown 片段，避免引入额外渲染依赖。
 */
@Composable
fun RichContent(content: String, stopped: Boolean = false) {
    val blocks = remember(content) { parseMarkdown(content) }
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { renderBlock(it) }
        if (stopped) Text("（已停止）", style = MaterialTheme.typography.labelSmall, color = harnessColors().tertiaryText)
    }
}

private sealed class MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock()
    data class Code(val language: String?, val code: String) : MdBlock()
    data class Paragraph(val text: String) : MdBlock()
    data class Quote(val text: String) : MdBlock()
    data class ListBlock(val ordered: Boolean, val items: kotlin.collections.List<String>) : MdBlock()
    data class Divider(val ignored: Unit = Unit) : MdBlock()
    data class Table(val header: kotlin.collections.List<String>, val rows: kotlin.collections.List<kotlin.collections.List<String>>) : MdBlock()
}

private fun parseMarkdown(src: String): List<MdBlock> {
    val out = mutableListOf<MdBlock>()
    val lines = src.split("\n")
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        if (line.startsWith("```")) {
            val lang = line.removePrefix("```").trim().takeIf { it.isNotBlank() }
            val sb = StringBuilder()
            i++
            while (i < lines.size && !lines[i].startsWith("```")) {
                sb.appendLine(lines[i])
                i++
            }
            if (i < lines.size) i++ // 闭合 ``` 跳过
            out.add(MdBlock.Code(lang, sb.toString().trimEnd('\n')))
            continue
        }
        if (line.startsWith("# ")) { out.add(MdBlock.Heading(1, line.removePrefix("# ").trim())); i++; continue }
        if (line.startsWith("## ")) { out.add(MdBlock.Heading(2, line.removePrefix("## ").trim())); i++; continue }
        if (line.startsWith("### ")) { out.add(MdBlock.Heading(3, line.removePrefix("### ").trim())); i++; continue }
        if (line.startsWith("#### ")) { out.add(MdBlock.Heading(4, line.removePrefix("#### ").trim())); i++; continue }
        if (line.startsWith("> ")) {
            val sb = StringBuilder()
            while (i < lines.size && lines[i].startsWith("> ")) {
                sb.appendLine(lines[i].removePrefix("> "))
                i++
            }
            out.add(MdBlock.Quote(sb.toString().trimEnd('\n')))
            continue
        }
        if (line.startsWith("- ") || line.startsWith("* ")) {
            val items = mutableListOf<String>()
            while (i < lines.size && (lines[i].startsWith("- ") || lines[i].startsWith("* "))) {
                items.add(lines[i].substring(2))
                i++
            }
            out.add(MdBlock.ListBlock(ordered = false, items = items))
            continue
        }
        if (line.matches(Regex("""\d+\.\s.*"""))) {
            val items = mutableListOf<String>()
            while (i < lines.size && lines[i].matches(Regex("""\d+\.\s.*"""))) {
                items.add(lines[i].substringAfter(". ", ""))
                i++
            }
            out.add(MdBlock.ListBlock(ordered = true, items = items))
            continue
        }
        if (line.startsWith("---") || line.startsWith("***")) {
            out.add(MdBlock.Divider())
            i++
            continue
        }
        // 表格：包含 | 且下一行是分隔行
        if (line.contains("|") && i + 1 < lines.size && lines[i + 1].matches(Regex("""[\s\-|:]+"""))) {
            val header = line.split("|").filter { it.isNotBlank() }.map { it.trim() }
            i += 2
            val rows = mutableListOf<List<String>>()
            while (i < lines.size && lines[i].contains("|")) {
                rows.add(lines[i].split("|").filter { it.isNotBlank() }.map { it.trim() })
                i++
            }
            out.add(MdBlock.Table(header, rows))
            continue
        }
        if (line.isBlank()) { i++; continue }
        // 普通段落（合并连续非空行）
        val sb = StringBuilder()
        while (i < lines.size && lines[i].isNotBlank() &&
            !lines[i].startsWith("#") && !lines[i].startsWith("```") &&
            !lines[i].startsWith(">") && !lines[i].startsWith("- ") &&
            !lines[i].startsWith("* ") && !lines[i].matches(Regex("""\d+\.\s.*"""))
        ) {
            sb.appendLine(lines[i])
            i++
        }
        out.add(MdBlock.Paragraph(sb.toString().trimEnd('\n')))
    }
    return out
}

@Composable
private fun renderBlock(block: MdBlock) {
    when (block) {
        is MdBlock.Heading -> {
            val style = when (block.level) {
                1 -> MaterialTheme.typography.headlineLarge
                2 -> MaterialTheme.typography.headlineMedium
                3 -> MaterialTheme.typography.titleLarge
                else -> MaterialTheme.typography.titleMedium
            }
            Text(block.text, style = style, color = harnessColors().primaryText)
        }
        is MdBlock.Paragraph -> {
            Text(
                text = inlineAnnotated(block.text),
                style = MaterialTheme.typography.bodyMedium,
                color = harnessColors().primaryText
            )
        }
        is MdBlock.Quote -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(harnessColors().surfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(block.text, style = MaterialTheme.typography.bodyMedium, color = harnessColors().secondaryText)
            }
        }
        is MdBlock.ListBlock -> {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                block.items.forEachIndexed { idx, item ->
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            if (block.ordered) "${idx + 1}." else "•",
                            style = MaterialTheme.typography.bodyMedium,
                            color = harnessColors().brand
                        )
                        Text(
                            text = inlineAnnotated(item),
                            style = MaterialTheme.typography.bodyMedium,
                            color = harnessColors().primaryText
                        )
                    }
                }
            }
        }
        is MdBlock.Code -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(harnessColors().codeBlock)
                    .horizontalScroll(rememberScrollState())
                    .padding(10.dp)
            ) {
                Column {
                    block.language?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = harnessColors().tertiaryText)
                        Spacer(Modifier.height(4.dp))
                    }
                    Text(
                        block.code,
                        style = MaterialTheme.typography.bodySmall,
                        color = harnessColors().codeBlockText,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
        is MdBlock.Divider -> {
            Box(
                modifier = Modifier.fillMaxWidth().height(1.dp).background(harnessColors().outline)
            )
        }
        is MdBlock.Table -> {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().background(harnessColors().surfaceVariant)) {
                    block.header.forEach { cell ->
                        Text(
                            cell,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = harnessColors().primaryText,
                            modifier = Modifier.weight(1f).padding(8.dp)
                        )
                    }
                }
                block.rows.forEachIndexed { idx, row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (idx % 2 == 0) harnessColors().surface else harnessColors().surfaceVariant)
                    ) {
                        row.forEach { cell ->
                            Text(
                                cell,
                                style = MaterialTheme.typography.bodySmall,
                                color = harnessColors().primaryText,
                                modifier = Modifier.weight(1f).padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 处理行内 `代码`、**加粗**、*斜体* 等格式。 */
private fun inlineAnnotated(src: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    val s = src
    while (i < s.length) {
        if (s.startsWith("`", i)) {
            val end = s.indexOf('`', i + 1)
            if (end > i) {
                pushStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0x22000000)))
                append(s.substring(i + 1, end))
                pop()
                i = end + 1
                continue
            }
        }
        if (s.startsWith("**", i)) {
            val end = s.indexOf("**", i + 2)
            if (end > i) {
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                append(s.substring(i + 2, end))
                pop()
                i = end + 2
                continue
            }
        }
        if (s.startsWith("*", i)) {
            val end = s.indexOf('*', i + 1)
            if (end > i) {
                pushStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
                append(s.substring(i + 1, end))
                pop()
                i = end + 1
                continue
            }
        }
        append(s[i])
        i++
    }
}
