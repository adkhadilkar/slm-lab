@file:OptIn(ExperimentalMaterial3Api::class)

package com.slmlab.app.ui.lesson

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.slmlab.app.ui.components.KindChip
import com.slmlab.app.ui.components.MarkdownView

private val CardShape = RoundedCornerShape(14.dp)

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
            TopAppBar(
                title = {
                    Text(
                        text = lesson?.title ?: "Lesson",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
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
            Column(
                modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = state.error ?: "Could not load this lesson.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = { vm.retry() }) { Text("Retry") }
            }
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
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KindChip(kind = lesson.kind)
            Spacer(Modifier.width(12.dp))
            Text(
                text = "${lesson.minutes} min",
                style = MaterialTheme.typography.labelLarge,
                color = muted,
            )
        }

        if (lesson.kind == LessonKind.GUIDED) {
            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = CardShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            ) {
                Text(
                    text = "This lesson runs on Colab or a laptop. Copy the code blocks from here.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        MarkdownView(markdown = markdown, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(24.dp))
        Text(
            text = "Checklist",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(4.dp))
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
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (next != null) {
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { onOpenLesson(next.id) }) {
                Text(
                    text = "Next: ${next.title}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun LessonActions(
    hasLab: Boolean,
    completed: Boolean,
    onOpenLab: () -> Unit,
    onToggleComplete: () -> Unit,
) {
    Column {
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (hasLab) {
                Button(onClick = onOpenLab, modifier = Modifier.weight(1f)) {
                    Text("Open lab")
                }
            }
            OutlinedButton(onClick = onToggleComplete, modifier = Modifier.weight(1f)) {
                if (completed) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Completed")
                } else {
                    Text("Mark complete")
                }
            }
        }
    }
}
