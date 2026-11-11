package com.slmlab.app.ui.lesson

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slmlab.app.data.CurriculumRepository
import com.slmlab.app.data.LessonMeta
import com.slmlab.app.data.ProgressStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LessonUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val lesson: LessonMeta? = null,
    val next: LessonMeta? = null,
    val markdown: String = "",
    /** Indices of ticked checklist items. */
    val checked: Set<Int> = emptySet(),
    val completed: Boolean = false,
    /** One-shot snackbar text; cleared by [LessonViewModel.messageShown]. */
    val message: String? = null,
)

class LessonViewModel(
    private val lessonId: String,
    private val repo: CurriculumRepository,
    private val progress: ProgressStore,
) : ViewModel() {

    private val _state = MutableStateFlow(LessonUiState())
    val state: StateFlow<LessonUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(progress.completedLessons, progress.checkedItems) { completed, checked ->
                val prefix = "$lessonId#"
                val indices = checked
                    .filter { it.startsWith(prefix) }
                    .mapNotNull { it.removePrefix(prefix).toIntOrNull() }
                    .toSet()
                completed to indices
            }.collect { (completed, indices) ->
                _state.update { it.copy(completed = lessonId in completed, checked = indices) }
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

    fun toggleChecked(index: Int) {
        val current = _state.value
        val lesson = current.lesson ?: return
        val nowChecked = index !in current.checked
        val newChecked = if (nowChecked) current.checked + index else current.checked - index
        _state.update { it.copy(checked = newChecked) }
        val allTicked = lesson.checklist.indices.all { it in newChecked }
        viewModelScope.launch {
            progress.setChecked(lessonId, index, nowChecked)
            if (nowChecked && allTicked && !_state.value.completed) {
                progress.setLessonCompleted(lessonId, true)
                _state.update { it.copy(completed = true, message = "Lesson completed") }
            }
        }
    }

    fun toggleCompleted() {
        val now = !_state.value.completed
        _state.update { it.copy(completed = now) }
        viewModelScope.launch {
            progress.setLessonCompleted(lessonId, now)
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val curriculum = repo.curriculum()
                val lesson = curriculum.lesson(lessonId)
                if (lesson == null) {
                    _state.update { it.copy(loading = false, error = "Lesson not found.") }
                    return@launch
                }
                val markdown = repo.lessonMarkdown(lesson.contentAsset)
                _state.update {
                    it.copy(
                        loading = false,
                        lesson = lesson,
                        next = curriculum.nextAfter(lessonId),
                        markdown = markdown,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = "Could not load this lesson.") }
            }
        }
    }
}
