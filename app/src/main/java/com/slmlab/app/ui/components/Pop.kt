package com.slmlab.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.slmlab.app.ui.theme.LocalPop

/**
 * Building blocks of the "Pop Paper" look: a filled shape with an ink outline and an optional
 * hard offset shadow. The shadow sits inside this element's own bounds (space is reserved on
 * the end and bottom edges), so it is never clipped by a scrolling parent.
 */
fun Modifier.popBox(
    fill: Color,
    ink: Color,
    radius: Dp = 16.dp,
    border: Dp = 2.dp,
    shadow: Dp = 0.dp,
    shadowColor: Color = ink,
): Modifier = this
    .padding(end = shadow, bottom = shadow)
    .drawBehind {
        if (shadow > 0.dp) {
            drawRoundRect(
                color = shadowColor,
                topLeft = Offset(shadow.toPx(), shadow.toPx()),
                size = size,
                cornerRadius = CornerRadius(radius.toPx(), radius.toPx()),
            )
        }
    }
    .clip(RoundedCornerShape(radius))
    .background(fill)
    .border(border, ink, RoundedCornerShape(radius))

/** A card: outlined block, optionally clickable, optionally with a hard shadow. */
@Composable
fun PopCard(
    modifier: Modifier = Modifier,
    fill: Color = MaterialTheme.colorScheme.surface,
    radius: Dp = 18.dp,
    border: Dp = 2.dp,
    shadow: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val pop = LocalPop.current
    var box = modifier.popBox(fill = fill, ink = pop.ink, radius = radius, border = border, shadow = shadow)
    if (onClick != null) {
        box = box.clickable(onClick = onClick)
    }
    Column(modifier = box.padding(contentPadding), content = content)
}

/** The main button style: a bright block with a hard shadow. At least 52dp tall. */
@Composable
fun PopButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fill: Color = LocalPop.current.blue,
    contentColor: Color = LocalPop.current.onBlue,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    shadow: Dp = 4.dp,
    minHeight: Dp = 52.dp,
) {
    val pop = LocalPop.current
    val scheme = MaterialTheme.colorScheme
    val background = if (enabled) fill else scheme.surfaceVariant
    val foreground = if (enabled) contentColor else scheme.onSurfaceVariant
    Row(
        modifier = modifier
            .popBox(
                fill = background,
                ink = pop.ink,
                radius = 14.dp,
                shadow = shadow,
                shadowColor = if (enabled) pop.ink else Color.Transparent,
            )
            .heightIn(min = minHeight)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = foreground,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = foreground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A 44dp outlined square button holding one icon. */
@Composable
fun PopIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fill: Color = MaterialTheme.colorScheme.surface,
) {
    val pop = LocalPop.current
    Box(
        modifier = modifier
            .size(44.dp)
            .popBox(fill = fill, ink = pop.ink, radius = 14.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** A small outlined label in the monospace face, such as "WEEKS 1–2". */
@Composable
fun PopTag(
    text: String,
    modifier: Modifier = Modifier,
    fill: Color = LocalPop.current.yellow,
    contentColor: Color = LocalPop.current.onBlock,
) {
    val pop = LocalPop.current
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = contentColor,
        maxLines = 1,
        modifier = modifier
            .popBox(fill = fill, ink = pop.ink, radius = 7.dp)
            .padding(horizontal = 7.dp, vertical = 4.dp),
    )
}

/**
 * Screen header used in place of a Material top bar. Without [onBack] the title is drawn large
 * (tab screens); with it, a back button is shown and the title is one line.
 */
@Composable
fun PopTopBar(
    title: String,
    modifier: Modifier = Modifier,
    overline: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            PopIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onBack,
            )
            Spacer(Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            if (overline != null) {
                Text(
                    text = overline.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
            }
            Text(
                text = title,
                style = if (onBack == null) {
                    MaterialTheme.typography.headlineLarge
                } else {
                    MaterialTheme.typography.headlineSmall
                },
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = if (onBack == null) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        actions()
    }
}

/** A row of [total] blocks with the first [done] filled: chunky progress for the path. */
@Composable
fun SegmentedProgress(
    done: Int,
    total: Int,
    modifier: Modifier = Modifier,
    filled: Color = LocalPop.current.onBlock,
    empty: Color = Color.White,
    outline: Color = LocalPop.current.onBlock,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$done of $total lessons complete" },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(total.coerceAtLeast(0)) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(12.dp)
                    .popBox(fill = if (index < done) filled else empty, ink = outline, radius = 3.dp),
            )
        }
    }
}
