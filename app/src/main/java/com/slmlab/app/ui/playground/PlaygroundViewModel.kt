package com.slmlab.app.ui.playground

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slmlab.app.data.CurriculumRepository
import com.slmlab.app.data.PyRunner
import com.slmlab.app.data.RunEvent
import java.util.Locale
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlaygroundUiState(
    val loading: Boolean = true,
    val loadError: String? = null,
    val prompt: String = "ROMEO:",
    val temperature: Float = 0.8f,
    /** 0 means top-k is off. */
    val topK: Int = 20,
    val length: Int = 200,
    val output: String = "",
    val generating: Boolean = false,
    val noModel: Boolean = false,
    val runError: String? = null,
    /** True while the shared runner is busy, whoever started the run. */
    val runnerBusy: Boolean = false,
) {
    /** True while a lab (not this playground) holds the runner. */
    val labRunning: Boolean
        get() = runnerBusy && !generating
}

class PlaygroundViewModel(
    private val repo: CurriculumRepository,
    private val runner: PyRunner,
) : ViewModel() {

    companion object {
        const val OUTPUT_CAP = 200_000
        const val FLUSH_MS = 100L
        const val LAB_PATH = "labs/playground.py"
        const val NO_MODEL = "NO_MODEL"
    }

    private val _state = MutableStateFlow(PlaygroundUiState())
    val state: StateFlow<PlaygroundUiState> = _state.asStateFlow()

    private val outputBuf = StringBuilder()
    private var source: String? = null
    private var flushJob: Job? = null

    init {
        viewModelScope.launch {
            runner.isRunning.collect { busy ->
                _state.update { it.copy(runnerBusy = busy) }
            }
        }
        load()
    }

    fun retry() {
        load()
    }

    fun setPrompt(text: String) {
        _state.update { it.copy(prompt = text) }
    }

    fun setTemperature(value: Float) {
        _state.update { it.copy(temperature = value) }
    }

    fun setTopK(value: Int) {
        _state.update { it.copy(topK = value) }
    }

    fun setLength(value: Int) {
        _state.update { it.copy(length = value) }
    }

    fun generate() {
        val current = _state.value
        val lab = source ?: return
        if (current.generating || current.labRunning || current.loading) return

        val seed = Random.nextInt(0, 1_000_000)
        val code = buildPrefix(
            prompt = current.prompt,
            temperature = current.temperature,
            topK = current.topK,
            length = current.length,
            seed = seed,
        ) + "\n" + lab

        outputBuf.setLength(0)
        flushJob?.cancel()
        _state.update {
            it.copy(generating = true, output = "", noModel = false, runError = null)
        }

        viewModelScope.launch {
            try {
                runner.run(code).collect { event ->
                    when (event) {
                        is RunEvent.Output -> {
                            appendOutput(event.text)
                            scheduleFlush()
                        }

                        is RunEvent.Metric -> Unit

                        is RunEvent.Finished -> {
                            flushJob?.cancel()
                            publish()
                            _state.update {
                                it.copy(
                                    generating = false,
                                    runError = if (event.ok) null else (event.error ?: "Generation failed."),
                                )
                            }
                        }
                    }
                }
                _state.update { it.copy(generating = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(generating = false, runError = e.message ?: "Generation failed.")
                }
            }
        }
    }

    fun stop() {
        if (_state.value.generating) runner.stop()
    }

    override fun onCleared() {
        // Only stop our own run; a lab run started later must not be interrupted.
        if (_state.value.generating) runner.stop()
        super.onCleared()
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, loadError = null) }
            try {
                source = repo.labSource(LAB_PATH)
                _state.update { it.copy(loading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(loading = false, loadError = "Could not load the playground script.")
                }
            }
        }
    }

    private fun appendOutput(text: String) {
        outputBuf.append(text)
        if (outputBuf.length > OUTPUT_CAP) {
            outputBuf.delete(0, outputBuf.length - OUTPUT_CAP)
        }
    }

    /** Publishes the output at most once per [FLUSH_MS] so text streams without flooding state. */
    private fun scheduleFlush() {
        if (flushJob?.isActive == true) return
        flushJob = viewModelScope.launch {
            delay(FLUSH_MS)
            publish()
        }
    }

    private fun publish() {
        val text = outputBuf.toString()
        val noModel = text.trim() == NO_MODEL
        _state.update {
            it.copy(output = if (noModel) "" else text, noModel = noModel)
        }
    }

    /** Python lines that define the knobs the playground script reads. */
    private fun buildPrefix(
        prompt: String,
        temperature: Float,
        topK: Int,
        length: Int,
        seed: Int,
    ): String {
        val topKLiteral = if (topK <= 0) "None" else topK.toString()
        return buildString {
            append("PROMPT = ").append(pyString(prompt)).append('\n')
            append("TEMPERATURE = ").append(String.format(Locale.US, "%.2f", temperature)).append('\n')
            append("TOP_K = ").append(topKLiteral).append('\n')
            append("MAX_TOKENS = ").append(length).append('\n')
            append("SEED = ").append(seed).append('\n')
        }
    }
}

/** A double-quoted Python string literal for [s]. Escapes backslash, quote and control characters. */
private fun pyString(s: String): String {
    val sb = StringBuilder("\"")
    for (c in s) {
        when {
            c == '\\' -> sb.append("\\\\")
            c == '"' -> sb.append("\\\"")
            c == '\n' -> sb.append("\\n")
            c == '\r' -> sb.append("\\r")
            c == '\t' -> sb.append("\\t")
            c.code < 0x20 -> sb.append("\\x").append(String.format(Locale.US, "%02x", c.code))
            else -> sb.append(c)
        }
    }
    sb.append('"')
    return sb.toString()
}
