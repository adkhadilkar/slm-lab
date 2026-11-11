@file:OptIn(ExperimentalMaterial3Api::class)

package com.slmlab.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.slmlab.app.data.AppContainer
import com.slmlab.app.data.ThemeMode
import com.slmlab.app.ui.components.PopButton
import com.slmlab.app.ui.components.PopTag
import com.slmlab.app.ui.components.PopTopBar
import com.slmlab.app.ui.theme.LocalPop
import com.slmlab.app.ui.theme.MonoFamily
import kotlin.math.roundToInt

@Composable
fun SettingsScreen() {
    val vm: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SettingsViewModel(AppContainer.progress) }
        }
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val themeOptions = listOf(
        ThemeMode.SYSTEM to "System",
        ThemeMode.LIGHT to "Light",
        ThemeMode.DARK to "Dark",
    )
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        vm.messageShown()
    }

    Scaffold(
        topBar = { PopTopBar(title = "Settings") },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            SectionLabel("Appearance")
            Text(
                text = "Theme",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                themeOptions.forEachIndexed { index, (mode, label) ->
                    SegmentedButton(
                        selected = state.themeMode == mode,
                        onClick = { vm.setThemeMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = themeOptions.size,
                        ),
                        label = { Text(label) },
                    )
                }
            }

            SectionLabel("Editor")
            Text(
                text = "Font size ${state.editorFontSp} sp",
                style = MaterialTheme.typography.bodyLarge,
            )
            Slider(
                value = state.editorFontSp.toFloat(),
                onValueChange = { vm.setEditorFontSp(it.roundToInt()) },
                valueRange = 10f..18f,
                steps = 7,
            )
            Text(
                text = "for c in corpus: print(c)",
                fontFamily = MonoFamily,
                fontSize = state.editorFontSp.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = muted,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionLabel("Progress")
            PopButton(
                text = "Reset progress",
                onClick = { vm.requestReset() },
                fill = LocalPop.current.red,
                contentColor = LocalPop.current.onBlock,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Clears completed lessons, checklist ticks, saved lab code and trained weights.",
                style = MaterialTheme.typography.bodyMedium,
                color = muted,
            )

            SectionLabel("About")
            Text(
                text = "SLM Lab",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Version 1.0",
                style = MaterialTheme.typography.bodyMedium,
                color = muted,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Python runs on this device. Nothing is uploaded.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (state.showResetDialog) {
        AlertDialog(
            onDismissRequest = { vm.dismissReset() },
            title = { Text("Reset progress?") },
            text = {
                Text("This clears completed lessons, checklist ticks, saved lab code and trained weights. This cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = { vm.confirmReset() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.dismissReset() }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Row(modifier = Modifier.padding(top = 20.dp, bottom = 10.dp)) {
        PopTag(text = text.uppercase(), fill = LocalPop.current.yellow)
    }
}
