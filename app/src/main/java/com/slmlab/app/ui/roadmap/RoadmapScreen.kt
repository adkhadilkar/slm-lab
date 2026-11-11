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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.slmlab.app.data.AppContainer
import com.slmlab.app.data.LessonKind
import com.slmlab.app.data.LessonMeta
import com.slmlab.app.data.Phase
import com.slmlab.app.ui.components.EmptyState
import com.slmlab.app.ui.components.PopCard
import com.slmlab.app.ui.components.PopTag
import com.slmlab.app.ui.components.SegmentedProgress
import com.slmlab.app.ui.components.popBox
import com.slmlab.app.ui.theme.LocalPop
import kotlin.math.roundToInt

@Composable
fun RoadmapScreen(onOpenLesson: (String) -> Unit) {
    val vm: RoadmapViewModel = viewModel(
        factory = viewModelFactory {
            initializer { RoadmapViewModel(AppContainer.repo, AppContainer.progress) }
        }
    )
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
        val curriculum = state.curriculum
        when {
            state.loading -> Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            state.error != null || curriculum == null -> EmptyState(
                message = "Could not load the lessons.",
                modifier = Modifier.padding(padding).fillMaxSize(),
                actionLabel = "Retry",
                onAction = { vm.retry() },
            )

            else -> {
                val all = curriculum.allLessons
                val next = state.nextLesson
                LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item(key = "title") {
                        TitleBlock(done = state.completedCount, total = state.totalLessons)
                    }
                    item(key = "hero") {
                        HeroCard(
                            total = state.totalLessons,
                            done = state.completedCount,
                            next = next,
                            nextNumber = if (next == null) 0 else all.indexOf(next) + 1,
                            onContinue = { lesson -> onOpenLesson(lesson.id) },
                        )
                    }
                    curriculum.phases.forEachIndexed { phaseIndex, phase ->
                        item(key = "phase-${phase.id}") {
                            PhaseHeader(
                                phase = phase,
                                phaseIndex = phaseIndex,
                                done = phase.lessons.count { it.id in state.completed },
                            )
                        }
                        phase.lessons.forEach { lesson ->
                            item(key = lesson.id) {
                                LessonRow(
                                    lesson = lesson,
                                    number = all.indexOf(lesson) + 1,
                                    done = lesson.id in state.completed,
                                    current = lesson.id == next?.id,
                                    onClick = { onOpenLesson(lesson.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TitleBlock(done: Int, total: Int) {
    val pop = LocalPop.current
    val scheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PopTag(text = "SLM LAB", fill = pop.ink, contentColor = scheme.background)
            PopTag(text = "$done OF $total DONE", fill = scheme.surface, contentColor = scheme.onSurface)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Build a tiny GPT.\nThen make it useful.",
            style = MaterialTheme.typography.headlineLarge,
            color = scheme.onBackground,
        )
    }
}

@Composable
private fun HeroCard(
    total: Int,
    done: Int,
    next: LessonMeta?,
    nextNumber: Int,
    onContinue: (LessonMeta) -> Unit,
) {
    val pop = LocalPop.current
    val percent = if (total == 0) 0 else (done * 100f / total).roundToInt()

    PopCard(
        modifier = Modifier.fillMaxWidth(),
        fill = pop.yellow,
        radius = 22.dp,
        border = 2.5.dp,
        shadow = 6.dp,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (next == null) "ALL $total LESSONS DONE" else "UP NEXT · LESSON ${twoDigits(nextNumber)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = pop.onBlock,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = next?.title ?: "Path complete",
                    style = MaterialTheme.typography.headlineMedium,
                    color = pop.onBlock,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (next != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${next.minutes} min · ${whereItRuns(next.kind)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = pop.onBlock,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.headlineLarge,
                color = pop.onBlock,
            )
        }
        Spacer(Modifier.height(14.dp))
        SegmentedProgress(done = done, total = total)
        if (next != null) {
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .popBox(fill = pop.onBlock, ink = pop.onBlock, radius = 14.dp)
                    .clickable(role = Role.Button) { onContinue(next) }
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Continue lesson",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun PhaseHeader(phase: Phase, phaseIndex: Int, done: Int) {
    val pop = LocalPop.current
    val scheme = MaterialTheme.colorScheme
    val tagFill = when (phaseIndex % 3) {
        0 -> pop.pink
        1 -> pop.orange
        else -> pop.green
    }
    Column(modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 2.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PopTag(text = phase.weeks.uppercase(), fill = tagFill)
            Spacer(Modifier.width(8.dp))
            Text(
                text = phase.title,
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "$done / ${phase.lessons.size}",
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = phase.subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LessonRow(
    lesson: LessonMeta,
    number: Int,
    done: Boolean,
    current: Boolean,
    onClick: () -> Unit,
) {
    val pop = LocalPop.current
    val scheme = MaterialTheme.colorScheme
    val titleColor = when {
        current -> pop.onBlue
        done -> scheme.onSurfaceVariant
        else -> scheme.onSurface
    }
    val metaColor = if (current) pop.onBlue else scheme.onSurfaceVariant
    val meta = buildString {
        append(if (lesson.kind == LessonKind.ON_DEVICE) "On device" else "Colab")
        append(" · ${lesson.minutes} min")
        if (lesson.labAsset != null) append(" · lab")
    }

    PopCard(
        modifier = Modifier.fillMaxWidth(),
        fill = if (current) pop.blue else scheme.surface,
        radius = 16.dp,
        border = if (current) 2.5.dp else 2.dp,
        shadow = if (current) 5.dp else 0.dp,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = twoDigits(number),
                style = MaterialTheme.typography.labelMedium,
                color = metaColor,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lesson.title,
                    style = if (current) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
                    color = titleColor,
                    textDecoration = if (done) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!done) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = metaColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            when {
                done -> Box(
                    modifier = Modifier
                        .size(26.dp)
                        .popBox(fill = pop.green, ink = pop.ink, radius = 8.dp)
                        .semantics { contentDescription = "Completed" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = pop.onBlock,
                        modifier = Modifier.size(16.dp),
                    )
                }

                current -> Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Up next",
                    tint = pop.onBlue,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

private fun twoDigits(n: Int): String = n.toString().padStart(2, '0')

private fun whereItRuns(kind: LessonKind): String =
    if (kind == LessonKind.ON_DEVICE) "runs on this phone" else "runs on Colab or a laptop"
