package com.slmlab.app.ui.roadmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slmlab.app.data.Curriculum
import com.slmlab.app.data.CurriculumRepository
import com.slmlab.app.data.LessonMeta
import com.slmlab.app.data.ProgressStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RoadmapUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val curriculum: Curriculum? = null,
    val completed: Set<String> = emptySet(),
) {
    val totalLessons: Int
        get() = curriculum?.allLessons?.size ?: 0

    val completedCount: Int
        get() = curriculum?.allLessons?.count { it.id in completed } ?: 0

    /** The first lesson in path order that is not complete, or null when all are complete. */
    val nextLesson: LessonMeta?
        get() = curriculum?.allLessons?.firstOrNull { it.id !in completed }
}

class RoadmapViewModel(
    private val repo: CurriculumRepository,
    progress: ProgressStore,
) : ViewModel() {

    private val _state = MutableStateFlow(RoadmapUiState())
    val state: StateFlow<RoadmapUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            progress.completedLessons.collect { ids ->
                _state.update { it.copy(completed = ids) }
            }
        }
        load()
    }

    fun retry() {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val curriculum = repo.curriculum()
                _state.update { it.copy(loading = false, curriculum = curriculum) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = "Could not load the lessons.") }
            }
        }
    }
}
