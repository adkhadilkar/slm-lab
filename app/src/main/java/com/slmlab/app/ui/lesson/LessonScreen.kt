package com.slmlab.app.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.slmlab.app.data.AppContainer
import com.slmlab.app.data.LessonKind
import com.slmlab.app.data.LessonMeta
import com.slmlab.app.ui.components.EmptyState
import com.slmlab.app.ui.components.KindChip
import com.slmlab.app.ui.components.MarkdownView
import com.slmlab.app.ui.components.PopButton
import com.slmlab.app.ui.components.PopCard
import com.slmlab.app.ui.components.PopTag
import com.slmlab.app.ui.components.PopTopBar
import com.slmlab.app.ui.theme.LocalPop

@Composable
fun LessonScreen(
    lessonId: String,
    onBack: () -> Unit,
    onOpenLab: (String) -> Unit,
    onOpenLesson: (String) -> Unit,
) {
    val vm: LessonViewModel = viewModel(
        key = "lesson-$lessonId",
        factory = viewModelFactory {
            initializer { LessonViewModel(lessonId, AppContainer.repo, AppContainer.progress) }
        },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        vm.messageShown()
    }

    val lesson = state.lesson

    Scaffold(
        topBar = {
            PopTopBar(
                title = lesson?.title ?: "Lesson",
                overline = if (lesson == null) null else "Lesson · ${lesson.minutes} min",
                onBack = onBack,
            )
        },
        bottomBar = {
            if (lesson != null) {
                LessonActions(
                    hasLab = lesson.labAsset != null,
                    completed = state.completed,
                    onOpenLab = { onOpenLab(lessonId) },
                    onToggleComplete = { vm.toggleCompleted() },
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { padding ->
        if (state.loading) {
            Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (lesson == null) {
            EmptyState(
                message = state.error ?: "Could not load this lesson.",
                modifier = Modifier.padding(padding).fillMaxSize(),
                actionLabel = "Retry",
                onAction = { vm.retry() },
            )
        } else {
            LessonBody(
                modifier = Modifier.padding(padding),
                lesson = lesson,
                markdown = state.markdown,
                checked = state.checked,
                next = state.next,
                onToggle = { index -> vm.toggleChecked(index) },
                onOpenLesson = onOpenLesson,
            )
        }
    }
}

@Composable
private fun LessonBody(
    modifier: Modifier,
    lesson: LessonMeta,
    markdown: String,
    checked: Set<Int>,
    next: LessonMeta?,
    onToggle: (Int) -> Unit,
    onOpenLesson: (String) -> Unit,
) {
    val pop = LocalPop.current
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KindChip(kind = lesson.kind)
            if (lesson.labAsset != null) {
                Spacer(Modifier.width(8.dp))
                PopTag(text = "HAS A LAB", fill = pop.yellow)
            }
        }

        if (lesson.kind == LessonKind.GUIDED) {
            Spacer(Modifier.height(12.dp))
            PopCard(modifier = Modifier.fillMaxWidth(), fill = pop.orange, radius = 16.dp) {
                Text(
                    text = "This lesson runs on Colab or a laptop. Copy the code blocks from here.",
                    style = MaterialTheme.typography.labelLarge,
                    color = pop.onBlock,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        PopCard(modifier = Modifier.fillMaxWidth(), radius = 20.dp, contentPadding = PaddingValues(16.dp)) {
            MarkdownView(markdown = markdown, modifier = Modifier.fillMaxWidth())
        }

        Spacer(Modifier.height(20.dp))
        PopCard(
            modifier = Modifier.fillMaxWidth(),
            fill = scheme.surface,
            radius = 20.dp,
            shadow = 5.dp,
            contentPadding = PaddingValues(start = 6.dp, end = 14.dp, top = 12.dp, bottom = 8.dp),
        ) {
            Row(
                modifier = Modifier.padding(start = 10.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Checklist",
                    style = MaterialTheme.typography.titleLarge,
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                PopTag(
                    text = "${checked.size} / ${lesson.checklist.size}",
                    fill = if (checked.size == lesson.checklist.size) pop.green else pop.pink,
                )
            }
            lesson.checklist.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable { onToggle(index) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = index in checked,
                        onCheckedChange = { onToggle(index) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = pop.green,
                            checkmarkColor = pop.onBlock,
                            uncheckedColor = pop.ink,
                        ),
                    )
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurface,
                        modifier = Modifier.weight(1f).padding(vertical = 6.dp),
                    )
                }
            }
        }

        if (next != null) {
            Spacer(Modifier.height(16.dp))
            PopCard(
                modifier = Modifier.fillMaxWidth(),
                radius = 16.dp,
                onClick = { onOpenLesson(next.id) },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "NEXT LESSON",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = next.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = scheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = scheme.onSurface,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun LessonActions(
    hasLab: Boolean,
    completed: Boolean,
    onOpenLab: () -> Unit,
    onToggleComplete: () -> Unit,
) {
    val pop = LocalPop.current
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.background)
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (hasLab) {
            PopButton(
                text = "Open lab",
                onClick = onOpenLab,
                icon = Icons.Filled.PlayArrow,
                modifier = Modifier.weight(1f),
            )
        }
        PopButton(
            text = if (completed) "Completed" else "Mark complete",
            onClick = onToggleComplete,
            icon = if (completed) Icons.Filled.Check else null,
            fill = if (completed) pop.green else scheme.surface,
            contentColor = if (completed) pop.onBlock else scheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}
