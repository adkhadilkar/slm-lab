package com.slmlab.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.slmlab.app.ui.theme.MonoFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Renders the Markdown subset from DESIGN.md 6.1. Does not scroll; the parent scrolls.
 */
@Composable
fun MarkdownView(markdown: String, modifier: Modifier = Modifier) {
    val blocks = remember(markdown) { parseMarkdown(markdown) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        blocks.forEach { block ->
            RenderBlock(block)
        }
    }
}

@Composable
private fun RenderBlock(block: MdBlock) {
    val scheme = MaterialTheme.colorScheme
    val codeBackground = scheme.surfaceVariant
    when (block) {
        is MdBlock.Heading -> {
            val style = when (block.level) {
                1 -> MaterialTheme.typography.headlineSmall
                2 -> MaterialTheme.typography.titleLarge
                else -> MaterialTheme.typography.titleMedium
            }
            Text(
                text = inlineText(block.text, codeBackground),
                style = style,
                color = scheme.onSurface,
            )
        }

        is MdBlock.Paragraph -> {
            Text(
                text = inlineText(block.text, codeBackground),
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurface,
            )
        }

        is MdBlock.Bullets -> {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                block.items.forEach { item ->
                    Row {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyLarge,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.width(20.dp),
                        )
                        Text(
                            text = inlineText(item, codeBackground),
                            style = MaterialTheme.typography.bodyLarge,
                            color = scheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        is MdBlock.Numbered -> {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                block.items.forEachIndexed { index, item ->
                    Row {
                        Text(
                            text = "${index + 1}.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.width(28.dp),
                        )
                        Text(
                            text = inlineText(item, codeBackground),
                            style = MaterialTheme.typography.bodyLarge,
                            color = scheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        is MdBlock.Code -> CodeBlock(block.code)

        is MdBlock.Callout -> CalloutBlock(block.paragraphs, codeBackground)
    }
}

@Composable
private fun CodeBlock(code: String) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1500)
            copied = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(scheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    copyToClipboard(context, code)
                    copied = true
                },
            ) {
                Icon(
                    imageVector = if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                    contentDescription = if (copied) "Copied" else "Copy code",
                    tint = scheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = code,
            style = TextStyle(
                fontFamily = MonoFamily,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            ),
            color = scheme.onSurface,
            softWrap = false,
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
        )
    }
}

@Composable
private fun CalloutBlock(paragraphs: List<String>, codeBackground: Color) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(14.dp))
            .background(scheme.surface),
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(scheme.primary),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            paragraphs.forEach { paragraph ->
                Text(
                    text = inlineText(paragraph, codeBackground),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurface,
                )
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    manager.setPrimaryClip(ClipData.newPlainText("code", text))
}

/** Bold (**text**) and inline code (`code`). An unterminated marker is kept as plain text. */
private fun inlineText(text: String, codeBackground: Color): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        if (text.startsWith("**", i)) {
            val end = text.indexOf("**", i + 2)
            if (end > i + 2) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(text.substring(i + 2, end))
                }
                i = end + 2
            } else {
                append("**")
                i += 2
            }
        } else if (text[i] == '`') {
            val end = text.indexOf('`', i + 1)
            if (end > i + 1) {
                withStyle(SpanStyle(fontFamily = MonoFamily, background = codeBackground)) {
                    append(text.substring(i + 1, end))
                }
                i = end + 1
            } else {
                append("`")
                i++
            }
        } else {
            append(text[i].toString())
            i++
        }
    }
}

private sealed class MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock()
    data class Paragraph(val text: String) : MdBlock()
    data class Bullets(val items: List<String>) : MdBlock()
    data class Numbered(val items: List<String>) : MdBlock()
    data class Code(val code: String) : MdBlock()
    data class Callout(val paragraphs: List<String>) : MdBlock()
}

private val NUMBERED_ITEM = Regex("""^\d+\.\s+(.*)$""")

/** True for a line that starts a block other than a paragraph. Must match the branches in parseMarkdown. */
private fun startsBlock(trimmed: String): Boolean =
    trimmed.startsWith("```") ||
        trimmed.startsWith("# ") ||
        trimmed.startsWith("## ") ||
        trimmed.startsWith("### ") ||
        trimmed.startsWith("- ") ||
        trimmed.startsWith(">") ||
        NUMBERED_ITEM.matches(trimmed)

private fun parseMarkdown(source: String): List<MdBlock> {
    val lines = source.replace("\r\n", "\n").split("\n")
    val blocks = mutableListOf<MdBlock>()
    var i = 0
    while (i < lines.size) {
        val trimmed = lines[i].trim()
        when {
            trimmed.isEmpty() -> i++

            trimmed.startsWith("```") -> {
                i++
                val code = mutableListOf<String>()
                while (i < lines.size && !lines[i].trim().startsWith("```")) {
                    code += lines[i]
                    i++
                }
                if (i < lines.size) i++ // closing fence
                blocks += MdBlock.Code(code.joinToString("\n"))
            }

            trimmed.startsWith("### ") -> {
                blocks += MdBlock.Heading(3, trimmed.substring(4).trim())
                i++
            }

            trimmed.startsWith("## ") -> {
                blocks += MdBlock.Heading(2, trimmed.substring(3).trim())
                i++
            }

            trimmed.startsWith("# ") -> {
                blocks += MdBlock.Heading(1, trimmed.substring(2).trim())
                i++
            }

            trimmed.startsWith("- ") -> {
                val items = mutableListOf<String>()
                while (i < lines.size && lines[i].trim().startsWith("- ")) {
                    items += lines[i].trim().substring(2).trim()
                    i++
                }
                blocks += MdBlock.Bullets(items)
            }

            NUMBERED_ITEM.matches(trimmed) -> {
                val items = mutableListOf<String>()
                while (i < lines.size) {
                    val match = NUMBERED_ITEM.matchEntire(lines[i].trim()) ?: break
                    items += match.groupValues[1].trim()
                    i++
                }
                blocks += MdBlock.Numbered(items)
            }

            trimmed.startsWith(">") -> {
                val paragraphs = mutableListOf<String>()
                val current = mutableListOf<String>()
                while (i < lines.size && lines[i].trim().startsWith(">")) {
                    val content = lines[i].trim().removePrefix(">").trim()
                    if (content.isEmpty()) {
                        if (current.isNotEmpty()) {
                            paragraphs += current.joinToString(" ")
                            current.clear()
                        }
                    } else {
                        current += content
                    }
                    i++
                }
                if (current.isNotEmpty()) paragraphs += current.joinToString(" ")
                blocks += MdBlock.Callout(paragraphs)
            }

            else -> {
                // The first line here never starts a block (checked above), so this always consumes it.
                val buffer = mutableListOf<String>()
                while (i < lines.size) {
                    val t = lines[i].trim()
                    if (t.isEmpty() || startsBlock(t)) break
                    buffer += t
                    i++
                }
                blocks += MdBlock.Paragraph(buffer.joinToString(" "))
            }
        }
    }
    return blocks
}
