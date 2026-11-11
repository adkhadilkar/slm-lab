@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.slmlab.app.ui.lab

import android.content.ClipData
import android.content.ClipboardManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.slmlab.app.data.AppContainer
import com.slmlab.app.data.MetricPoint
import com.slmlab.app.ui.components.CodeEditor
import com.slmlab.app.ui.components.ConsolePane
import com.slmlab.app.ui.components.EmptyState
import com.slmlab.app.ui.components.LossChart
import com.slmlab.app.ui.components.PopButton
import com.slmlab.app.ui.components.PopIconButton
import com.slmlab.app.ui.components.PopTopBar
import com.slmlab.app.ui.components.SymbolBar
import com.slmlab.app.ui.components.insertAtCursor
import com.slmlab.app.ui.components.popBox
import com.slmlab.app.ui.theme.LocalPop
import kotlinx.coroutines.launch

@Composable
fun LabScreen(lessonId: String, onBack: () -> Unit) {
    val vm: LabViewModel = viewModel(
        key = "lab-$lessonId",
        factory = viewModelFactory {
            initializer {
                LabViewModel(lessonId, AppContainer.repo, AppContainer.progress, AppContainer.runner)
            }
        },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current
    val focusManager = LocalFocusManager.current
    val imeVisible = WindowInsets.isImeVisible
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val pop = LocalPop.current

    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    var showLeaveDialog by rememberSaveable { mutableStateOf(false) }

    // Keep the screen on while a run is in progress.
    DisposableEffect(state.running) {
        view.keepScreenOn = state.running
        onDispose { view.keepScreenOn = false }
    }

    BackHandler(enabled = state.running) {
        showLeaveDialog = true
    }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        vm.messageShown()
    }

    val copyCode: () -> Unit = {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard?.setPrimaryClip(ClipData.newPlainText("Lab code", state.code.text))
        scope.launch { snackbarHostState.showSnackbar("Code copied") }
    }

    Scaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
        topBar = {
            PopTopBar(
                title = if (state.title.isEmpty()) "Lab" else state.title,
                overline = "Lab · runs on this phone",
                onBack = {
                    if (state.running) {
                        showLeaveDialog = true
                    } else {
                        onBack()
                    }
                },
                actions = {
                    Box {
                        PopIconButton(
                            icon = Icons.Filled.MoreVert,
                            contentDescription = "More actions",
                            onClick = { menuOpen = true },
                        )
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Reset code") },
                                enabled = !state.running,
                                onClick = {
                                    menuOpen = false
                                    showResetDialog = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Copy code") },
                                onClick = {
                                    menuOpen = false
                                    copyCode()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Clear output") },
                                onClick = {
                                    menuOpen = false
                                    vm.clearOutput()
                                },
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                val showSymbols = imeVisible &&
                    state.tab == LabTab.CODE &&
                    !state.running &&
                    !state.loading &&
                    state.error == null
                if (showSymbols) {
                    SymbolBar(
                        onInsert = { text -> vm.onCodeChange(state.code.insertAtCursor(text)) },
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (state.running) {
                        PopButton(
                            text = "Stop",
                            onClick = { vm.stop() },
                            icon = Icons.Filled.Stop,
                            fill = pop.red,
                            contentColor = pop.onBlock,
                        )
                    } else {
                        PopButton(
                            text = "Run",
                            onClick = {
                                focusManager.clearFocus()
                                vm.run()
                            },
                            icon = Icons.Filled.PlayArrow,
                            enabled = !state.loading && state.error == null,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    if (state.running) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = state.status,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (state.statusIsError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { padding ->
        val error = state.error
        when {
            state.loading -> Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            error != null -> EmptyState(
                message = error,
                modifier = Modifier.padding(padding).fillMaxSize(),
                actionLabel = "Retry",
                onAction = { vm.retry() },
            )

            else -> Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                LabTabs(
                    selected = state.tab,
                    outputUnread = state.outputUnread,
                    onSelect = { vm.selectTab(it) },
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp)
                        .popBox(fill = MaterialTheme.colorScheme.surfaceVariant, ink = pop.ink, radius = 18.dp),
                ) {
                    when (state.tab) {
                        LabTab.CODE -> CodeEditor(
                            value = state.code,
                            onValueChange = { vm.onCodeChange(it) },
                            fontSizeSp = state.fontSp,
                            modifier = Modifier.fillMaxSize(),
                            readOnly = state.running,
                        )

                        LabTab.OUTPUT -> ConsolePane(
                            text = state.output,
                            fontSizeSp = state.fontSp,
                            modifier = Modifier.fillMaxSize(),
                        )

                        LabTab.CHART -> ChartPane(series = state.series)
                    }
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset code") },
            text = { Text("Reset to the original code? Your edits will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    vm.resetCode()
                }) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Stop the run and leave?") },
            confirmButton = {
                TextButton(onClick = {
                    showLeaveDialog = false
                    vm.stop()
                    onBack()
                }) {
                    Text("Stop and leave")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text("Stay")
                }
            },
        )
    }
}

@Composable
private fun LabTabs(selected: LabTab, outputUnread: Boolean, onSelect: (LabTab) -> Unit) {
    val pop = LocalPop.current
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .popBox(fill = scheme.surface, ink = pop.ink, radius = 16.dp)
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        LabTab.entries.forEach { tab ->
            val isSelected = tab == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .popBox(
                        fill = if (isSelected) pop.yellow else scheme.surface,
                        ink = if (isSelected) pop.ink else scheme.surface,
                        radius = 11.dp,
                    )
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(tab) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = tabLabel(tab, outputUnread),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isSelected) pop.onBlock else scheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun tabLabel(tab: LabTab, outputUnread: Boolean): String = when (tab) {
    LabTab.CODE -> "Code"
    LabTab.OUTPUT -> if (outputUnread) "Output •" else "Output"
    LabTab.CHART -> "Chart"
}

@Composable
private fun ChartPane(series: Map<String, List<MetricPoint>>) {
    if (series.isEmpty()) {
        EmptyState(
            message = "This lab does not log metrics yet. Labs that train a model draw their loss here.",
            modifier = Modifier.fillMaxSize(),
        )
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            LossChart(series = series, modifier = Modifier.fillMaxWidth())
        }
    }
}
