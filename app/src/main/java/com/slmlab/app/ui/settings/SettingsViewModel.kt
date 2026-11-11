package com.slmlab.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slmlab.app.data.ProgressStore
import com.slmlab.app.data.ThemeMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val editorFontSp: Int = 13,
    val showResetDialog: Boolean = false,
    /** One-shot snackbar text; cleared by [SettingsViewModel.messageShown]. */
    val message: String? = null,
)

class SettingsViewModel(
    private val progress: ProgressStore,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            progress.settings.collect { settings ->
                _state.update {
                    it.copy(themeMode = settings.themeMode, editorFontSp = settings.editorFontSp)
                }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _state.update { it.copy(themeMode = mode) }
        viewModelScope.launch {
            progress.setThemeMode(mode)
        }
    }

    fun setEditorFontSp(sp: Int) {
        _state.update { it.copy(editorFontSp = sp) }
        viewModelScope.launch {
            progress.setEditorFontSp(sp)
        }
    }

    fun requestReset() {
        _state.update { it.copy(showResetDialog = true) }
    }

    fun dismissReset() {
        _state.update { it.copy(showResetDialog = false) }
    }

    fun confirmReset() {
        _state.update { it.copy(showResetDialog = false) }
        viewModelScope.launch {
            try {
                progress.resetAll()
                _state.update { it.copy(message = "Progress reset") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(message = "Could not reset progress.") }
            }
        }
    }

    fun messageShown() {
        _state.update { it.copy(message = null) }
    }
}
