package com.slmlab.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import com.slmlab.app.ui.theme.MonoFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Monospace code editor with a line-number gutter. Lines do not wrap: the text scrolls
 * horizontally and the whole editor (gutter included) scrolls vertically.
 */
@Composable
fun CodeEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    fontSizeSp: Int,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val textStyle = TextStyle(
        fontFamily = MonoFamily,
        fontSize = fontSizeSp.sp,
        lineHeight = (fontSizeSp * 1.4f).sp,
        color = scheme.onSurface,
    )

    val lineCount = value.text.count { it == '\n' } + 1
    val digits = lineCount.toString().length
    val gutter = (1..lineCount).joinToString("\n") { it.toString().padStart(digits) }

    // Monospace glyphs are about 0.6em wide. The field is at least this wide so no line wraps.
    val longest = value.text.split('\n').maxOf { it.length }
    val contentWidth = (longest * fontSizeSp * 0.62f + 32f).dp

    val vScrollState = rememberScrollState()
    val hScrollState = rememberScrollState()

    Row(modifier = modifier.fillMaxSize().verticalScroll(vScrollState)) {
        Box(
            modifier = Modifier
                .background(scheme.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            Text(
                text = gutter,
                style = textStyle,
                color = scheme.onSurfaceVariant,
                softWrap = false,
            )
        }

        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val fieldWidth = maxOf(maxWidth, contentWidth)
            Box(modifier = Modifier.horizontalScroll(hScrollState)) {
                BasicTextField(
                    value = value,
                    onValueChange = { changed -> onValueChange(applyAutoIndent(value, changed)) },
                    modifier = Modifier
                        .width(fieldWidth)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    readOnly = readOnly,
                    textStyle = textStyle,
                    cursorBrush = SolidColor(scheme.primary),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Ascii,
                    ),
                )
            }
        }
    }
}

/**
 * Pressing Enter keeps the previous line's indentation, plus four spaces when that line
 * ends with ':'. Only a single inserted newline is handled; anything else is returned as is.
 */
private fun applyAutoIndent(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
    val newText = new.text
    val caret = new.selection.start
    if (!new.selection.collapsed || caret < 1) return new
    if (newText.length != old.text.length + 1) return new
    if (newText[caret - 1] != '\n') return new
    if (newText.substring(0, caret - 1) != old.text.substring(0, caret - 1)) return new
    if (newText.substring(caret) != old.text.substring(caret - 1)) return new

    val lineStart = newText.lastIndexOf('\n', caret - 2) + 1
    val previous = newText.substring(lineStart, caret - 1)
    val indent = previous.takeWhile { it == ' ' }.length
    val extra = if (previous.trimEnd().endsWith(':')) 4 else 0
    val insert = " ".repeat(indent + extra)

    val updated = newText.substring(0, caret) + insert + newText.substring(caret)
    return TextFieldValue(text = updated, selection = TextRange(caret + insert.length))
}

private val SYMBOL_KEYS = listOf("Tab", "(", ")", "[", "]", ":", "=", "_", "\"", "#", ",", ".", "+", "-", "*")

/** Row of symbol keys shown above the keyboard while editing. Tab inserts four spaces. */
@Composable
fun SymbolBar(onInsert: (String) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(scheme.surfaceVariant)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SYMBOL_KEYS.forEach { key ->
            Box(
                modifier = Modifier
                    .sizeIn(minWidth = 40.dp, minHeight = 44.dp)
                    .clickable(role = Role.Button) {
                        onInsert(if (key == "Tab") "    " else key)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = key,
                    style = TextStyle(fontFamily = MonoFamily, fontSize = 15.sp),
                    color = scheme.onSurface,
                )
            }
        }
    }
}

/** Replaces the selection (or inserts at the caret) and places the caret after the inserted text. */
fun TextFieldValue.insertAtCursor(text: String): TextFieldValue {
    val start = selection.min.coerceIn(0, this.text.length)
    val end = selection.max.coerceIn(start, this.text.length)
    val updated = this.text.substring(0, start) + text + this.text.substring(end)
    return TextFieldValue(text = updated, selection = TextRange(start + text.length))
}
