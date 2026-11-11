package com.slmlab.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.slmlab.app.data.LessonKind
import com.slmlab.app.ui.theme.LocalPop

/** Outlined label for where a lesson runs: "On device" (green) or "Colab" (orange). */
@Composable
fun KindChip(kind: LessonKind, modifier: Modifier = Modifier) {
    val pop = LocalPop.current
    val onDevice = kind == LessonKind.ON_DEVICE
    Row(
        modifier = modifier
            .popBox(fill = if (onDevice) pop.green else pop.orange, ink = pop.ink, radius = 8.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (onDevice) Icons.Outlined.PhoneAndroid else Icons.Outlined.Cloud,
            contentDescription = null,
            tint = pop.onBlock,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = if (onDevice) "ON DEVICE" else "COLAB",
            style = MaterialTheme.typography.labelSmall,
            color = pop.onBlock,
        )
    }
}
