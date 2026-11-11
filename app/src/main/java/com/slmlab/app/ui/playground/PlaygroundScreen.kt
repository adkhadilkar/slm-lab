@file:OptIn(ExperimentalMaterial3Api::class)

package com.slmlab.app.ui.playground

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.slmlab.app.data.AppContainer
import com.slmlab.app.ui.components.EmptyState
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun PlaygroundScreen(onOpenLesson: (String) -> Unit) {
    val vm: PlaygroundViewModel = viewModel(
        factory = viewModelFactory {
            initializer { PlaygroundViewModel(AppContainer.repo, AppContainer.runner) }
        }
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Scaffold(
        topBar = { TopAppBar(title = { Text("Playground") }) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            if (state.loadError != null) {
                Text(
                    text = state.loadError ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = state.prompt,
                onValueChange = { vm.setPrompt(it) },
                label = { Text("Prompt") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                enabled = !state.generating,
            )

            Spacer(Modifier.height(16.dp))
            SliderRow(
                label = "Temperature",
                valueText = String.format(Locale.US, "%.1f", state.temperature),
            )
            Slider(
                value = state.temperature,
                onValueChange = { vm.setTemperature(((it * 10f).roundToInt()) / 10f) },
                valueRange = 0.1f..2.0f,
                steps = 18,
                enabled = !state.generating,
            )

            Spacer(Modifier.height(8.dp))
            SliderRow(
                label = "Top-k",
                valueText = if (state.topK == 0) "off" else state.topK.toString(),
            )
            Slider(
                value = state.topK.toFloat(),
                onValueChange = { vm.setTopK(it.roundToInt()) },
                valueRange = 0f..65f,
                steps = 64,
                enabled = !state.generating,
            )

            Spacer(Modifier.height(8.dp))
            SliderRow(
                label = "Length",
                valueText = state.length.toString(),
            )
            Slider(
                value = state.length.toFloat(),
                onValueChange = { vm.setLength(((it / 50f).roundToInt()) * 50) },
                valueRange = 50f..500f,
                steps = 8,
                enabled = !state.generating,
            )

            Spacer(Modifier.height(16.dp))
            if (state.generating) {
                Button(
                    onClick = { vm.stop() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Stop")
                }
            } else {
                Button(
                    onClick = { vm.generate() },
                    enabled = !state.loading && !state.labRunning,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Generate")
                }
                if (state.labRunning) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "A lab is running. Stop it first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = muted,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            if (state.generating) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
            }

            if (state.noModel) {
                EmptyState(
                    message = "No trained model yet. Train one in the lesson 'Training a tiny GPT'.",
                    modifier = Modifier.fillMaxWidth(),
                    actionLabel = "Open that lesson",
                    onAction = { onOpenLesson("p1-05") },
                )
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        if (state.output.isNotEmpty()) {
                            Text(
                                text = state.output,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                            )
                        } else if (!state.generating) {
                            Text(
                                text = "Generated text appears here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = muted,
                            )
                        }
                        val runError = state.runError
                        if (runError != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Error: $runError",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SliderRow(label: String, valueText: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = valueText,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
