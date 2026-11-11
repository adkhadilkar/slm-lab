package com.slmlab.app.ui.lab

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slmlab.app.data.CurriculumRepository
import com.slmlab.app.data.MetricPoint
import com.slmlab.app.data.ProgressStore
import com.slmlab.app.data.PyRunner
import com.slmlab.app.data.RunEvent
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LabTab { CODE, OUTPUT, CHART }

data class LabUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val title: String = "",
    val code: TextFieldValue = TextFieldValue(""),
    val fontSp: Int = 13,
    val tab: LabTab = LabTab.CODE,
    val output: String = "",
    val outputUnread: Boolean = false,
    val series: Map<String, List<MetricPoint>> = emptyMap(),
    val running: Boolean = false,
    val status: String = "Idle",
    val statusIsError: Boolean = false,
    /** One-shot snackbar text; cleared by [LabViewModel.messageShown]. */
    val message: String? = null,
)

class LabViewModel(
    private val lessonId: String,
    private val repo: CurriculumRepository,
    private val progress: ProgressStore,
    private val runner: PyRunner,
) : ViewModel() {

    companion object {
        const val OUTPUT_CAP = 200_000
        const val FLUSH_MS = 100L
        const val SAVE_DEBOUNCE_MS = 500L
    }

    private val _state = MutableStateFlow(LabUiState())
    val state: StateFlow<LabUiState> = _state.asStateFlow()

    /** Touched only on the main thread (all event handling runs in viewModelScope). */
    private val outputBuf = StringBuilder()
    private val seriesBuf = mutableMapOf<String, MutableList<MetricPoint>>()

    private var labAsset: String? = null
    private var flushJob: Job? = null
    private var timerJob: Job? = null
    private var saveJob: Job? = null
    private var pendingSave: String? = null
    private val saveScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var runStartNanos = 0L
    private var stopRequested = false
    private var userChangedTab = false
    private var chartSwitched = false

    init {
        viewModelScope.launch {
            progress.settings.collect { settings ->
                _state.update { it.copy(fontSp = settings.editorFontSp) }
            }
        }
        load()
    }

    fun retry() {
        load()
    }

    fun messageShown() {
        _state.update { it.copy(message = null) }
    }

    fun selectTab(tab: LabTab) {
        if (tab != _state.value.tab) userChangedTab = true
        _state.update {
            it.copy(
                tab = tab,
                outputUnread = if (tab == LabTab.OUTPUT) false else it.outputUnread,
            )
        }
    }

    fun onCodeChange(value: TextFieldValue) {
        val current = _state.value
        if (current.running) return
        _state.update { it.copy(code = value) }
        if (value.text != current.code.text) scheduleSave(value.text)
    }

    fun clearOutput() {
        outputBuf.setLength(0)
        _state.update { it.copy(output = "", outputUnread = false) }
    }

    fun run() {
        val current = _state.value
        if (current.running || current.loading || current.error != null) return
        val code = current.code.text

        outputBuf.setLength(0)
        seriesBuf.clear()
        stopRequested = false
        userChangedTab = false
        chartSwitched = false
        runStartNanos = System.nanoTime()
        flushJob?.cancel()

        _state.update {
            it.copy(
                running = true,
                tab = LabTab.OUTPUT,
                output = "",
                outputUnread = false,
                series = emptyMap(),
                status = "Running 0:00",
                statusIsError = false,
            )
        }
        startTimer()

        viewModelScope.launch {
            try {
                runner.run(code).collect { event -> handleEvent(event) }
                if (_state.value.running) {
                    finishRun(ok = false, error = "The run ended unexpectedly.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (_state.value.running) finishRun(ok = false, error = e.message)
            }
        }
    }

    fun stop() {
        if (!_state.value.running) return
        stopRequested = true
        runner.stop()
    }

    fun resetCode() {
        val asset = labAsset ?: return
        if (_state.value.running) return
        saveJob?.cancel()
        pendingSave = null
        viewModelScope.launch {
            try {
                val source = repo.labSource(asset)
                progress.clearCode(lessonId)
                _state.update { it.copy(code = TextFieldValue(source)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(message = "Could not reset the code.") }
            }
        }
    }

    override fun onCleared() {
        if (_state.value.running) runner.stop()
        // Keep an edit that has not been written yet (the 500 ms debounce is still pending).
        pendingSave?.let { text ->
            saveScope.launch { progress.saveCode(lessonId, text) }
        }
        super.onCleared()
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val lesson = repo.curriculum().lesson(lessonId)
                val asset = lesson?.labAsset
                if (lesson == null || asset == null) {
                    _state.update { it.copy(loading = false, error = "This lesson has no lab.") }
                    return@launch
                }
                labAsset = asset
                val source = progress.savedCode(lessonId) ?: repo.labSource(asset)
                _state.update {
                    it.copy(loading = false, title = lesson.title, code = TextFieldValue(source))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = "Could not load this lab.") }
            }
        }
    }

    private fun scheduleSave(text: String) {
        pendingSave = text
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(SAVE_DEBOUNCE_MS)
            pendingSave = null
            progress.saveCode(lessonId, text)
        }
    }

    private fun handleEvent(event: RunEvent) {
        when (event) {
            is RunEvent.Output -> {
                appendOutput(event.text)
                if (_state.value.tab != LabTab.OUTPUT) {
                    _state.update { it.copy(outputUnread = true) }
                }
                scheduleFlush()
            }

            is RunEvent.Metric -> {
                seriesBuf.getOrPut(event.name) { mutableListOf() }
                    .add(MetricPoint(event.step, event.value))
                scheduleFlush()
                if (!userChangedTab && !chartSwitched) {
                    chartSwitched = true
                    _state.update { it.copy(tab = LabTab.CHART) }
                }
            }

            is RunEvent.Finished -> finishRun(event.ok, event.error)
        }
    }

    private fun appendOutput(text: String) {
        outputBuf.append(text)
        if (outputBuf.length > OUTPUT_CAP) {
            outputBuf.delete(0, outputBuf.length - OUTPUT_CAP)
        }
    }

    /** Publishes the buffers to state at most once per [FLUSH_MS]. */
    private fun scheduleFlush() {
        if (flushJob?.isActive == true) return
        flushJob = viewModelScope.launch {
            delay(FLUSH_MS)
            publish()
        }
    }

    private fun publish() {
        val text = outputBuf.toString()
        val series = seriesBuf.mapValues { it.value.toList() }
        _state.update { it.copy(output = text, series = series) }
    }

    private fun finishRun(ok: Boolean, error: String?) {
        flushJob?.cancel()
        publish()
        timerJob?.cancel()
        val status = when {
            !ok -> "Error: ${error?.takeIf { it.isNotBlank() } ?: "unknown error"}"
            stopRequested -> "Stopped"
            else -> String.format(Locale.US, "Finished in %.1f s", elapsedMs() / 1000.0)
        }
        _state.update { it.copy(running = false, status = status, statusIsError = !ok) }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_state.value.running) {
                _state.update { it.copy(status = "Running ${formatClock(elapsedMs())}") }
                delay(1000L)
            }
        }
    }

    private fun elapsedMs(): Long = (System.nanoTime() - runStartNanos) / 1_000_000L

    private fun formatClock(ms: Long): String {
        val totalSeconds = ms / 1000
        return String.format(Locale.US, "%d:%02d", totalSeconds / 60, totalSeconds % 60)
    }
}
