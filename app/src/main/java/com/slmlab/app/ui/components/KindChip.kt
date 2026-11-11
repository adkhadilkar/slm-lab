package com.slmlab.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.slmlab.app.data.LessonKind

/** Label and icon for where a lesson runs: "On device" (PhoneAndroid) or "Colab" (Cloud). */
@Composable
fun KindChip(kind: LessonKind, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val onDevice = kind == LessonKind.ON_DEVICE
    val container = if (onDevice) scheme.primaryContainer else scheme.secondaryContainer
    val foreground = if (onDevice) scheme.onPrimaryContainer else scheme.onSecondaryContainer

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(container)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (onDevice) Icons.Outlined.PhoneAndroid else Icons.Outlined.Cloud,
            contentDescription = null,
            tint = foreground,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = if (onDevice) "On device" else "Colab",
            style = MaterialTheme.typography.labelSmall,
            color = foreground,
        )
    }
}
