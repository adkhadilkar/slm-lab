package com.slmlab.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.slmlab.app.ui.theme.MonoFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Monospace, selectable console output. Follows the bottom while the text grows, unless the
 * user has scrolled up; following resumes when they scroll back to the bottom.
 */
@Composable
fun ConsolePane(
    text: String,
    fontSizeSp: Int,
    modifier: Modifier = Modifier,
    emptyText: String = "Run the code to see output here.",
) {
    val scheme = MaterialTheme.colorScheme
    val scroll = rememberScrollState()
    var stickToBottom by remember { mutableStateOf(true) }

    LaunchedEffect(scroll) {
        // Only a change of the scroll position counts as the user scrolling. Growing content does not.
        launch {
            snapshotFlow { scroll.value }.collect { position ->
                stickToBottom = position >= scroll.maxValue - 32
            }
        }
        launch {
            snapshotFlow { scroll.maxValue }.collect { max ->
                if (stickToBottom) scroll.scrollTo(max)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.surfaceVariant)
            .verticalScroll(scroll)
            .padding(12.dp),
    ) {
        if (text.isEmpty()) {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
        } else {
            SelectionContainer {
                Text(
                    text = text,
                    style = TextStyle(
                        fontFamily = MonoFamily,
                        fontSize = fontSizeSp.sp,
                        lineHeight = (fontSizeSp * 1.4f).sp,
                    ),
                    color = scheme.onSurface,
                )
            }
        }
    }
}
