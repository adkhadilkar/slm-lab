@file:OptIn(ExperimentalMaterial3Api::class)

package com.slmlab.app.ui.roadmap

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.slmlab.app.data.AppContainer
import com.slmlab.app.data.LessonMeta
import com.slmlab.app.data.Phase
import com.slmlab.app.ui.components.KindChip
import com.slmlab.app.ui.components.ProgressRing
import kotlin.math.roundToInt

private val CardShape = RoundedCornerShape(14.dp)

@Composable
fun RoadmapScreen(onOpenLesson: (String) -> Unit) {
    val vm: RoadmapViewModel = viewModel(
        factory = viewModelFactory {
            initializer { RoadmapViewModel(AppContainer.repo, AppContainer.progress) }
        }
    )
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("SLM Lab") }) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        val curriculum = state.curriculum
        when {
            state.loading -> Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            state.error != null || curriculum == null -> Column(
                modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Could not load the lessons.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = { vm.retry() }) { Text("Retry") }
            }

            else -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                item(key = "header") {
                    ProgressHeader(
                        total = state.totalLessons,
                        done = state.completedCount,
                        next = state.nextLesson,
                        onContinue = { lesson -> onOpenLesson(lesson.id) },
                    )
                }
                curriculum.phases.forEach { phase ->
                    phaseSection(phase, state.completed, onOpenLesson)
                }
            }
        }
    }
}

private fun LazyListScope.phaseSection(
    phase: Phase,
    completed: Set<String>,
    onOpenLesson: (String) -> Unit,
) {
    val doneCount = phase.lessons.count { it.id in completed }
    item(key = "phase-${phase.id}") {
        PhaseHeader(phase = phase, done = doneCount)
    }
    items(phase.lessons, key = { it.id }) { lesson ->
        LessonRow(
            lesson = lesson,
            done = lesson.id in completed,
            onClick = { onOpenLesson(lesson.id) },
        )
    }
    item(key = "gap-${phase.id}") {
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ProgressHeader(
    total: Int,
    done: Int,
    next: LessonMeta?,
    onContinue: (LessonMeta) -> Unit,
) {
    val fraction = if (total == 0) 0f else done.toFloat() / total
    val percent = (fraction * 100).roundToInt()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val complete = total > 0 && done == total

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center) {
                    ProgressRing(progress = fraction, size = 64.dp)
                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "$done of $total lessons",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (complete) {
                        Text(
                            text = "Path complete",
                            style = MaterialTheme.typography.bodyMedium,
                            color = muted,
                        )
                    }
                }
            }

            if (next != null) {
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable { onContinue(next) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "CONTINUE",
                            style = MaterialTheme.typography.labelSmall,
                            color = muted,
                        )
                        Text(
                            text = next.title,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open ${next.title}",
                    )
                }
            }
        }
    }
}

@Composable
private fun PhaseHeader(phase: Phase, done: Int) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp)) {
        Text(
            text = phase.weeks.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = muted,
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = phase.title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = phase.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = muted,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = "$done / ${phase.lessons.size}",
                style = MaterialTheme.typography.labelLarge,
                color = muted,
            )
        }
    }
}

@Composable
private fun LessonRow(lesson: LessonMeta, done: Boolean, onClick: () -> Unit) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (done) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Completed",
                tint = MaterialTheme.colorScheme.primary,
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.RadioButtonUnchecked,
                contentDescription = "Not completed",
                tint = muted,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = lesson.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = lesson.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            KindChip(kind = lesson.kind)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${lesson.minutes} min",
                style = MaterialTheme.typography.labelSmall,
                color = muted,
            )
        }
    }
}
