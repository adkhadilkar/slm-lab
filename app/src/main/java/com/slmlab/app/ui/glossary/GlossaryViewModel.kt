package com.slmlab.app.ui.glossary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slmlab.app.data.CurriculumRepository
import com.slmlab.app.data.GlossaryTerm
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Terms that share a first letter. Non-letter initials are grouped under '#'. */
data class LetterGroup(val letter: Char, val terms: List<GlossaryTerm>)

data class GlossaryUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val query: String = "",
    /** Terms matching [query], grouped by initial, in A to Z order. */
    val groups: List<LetterGroup> = emptyList(),
)

class GlossaryViewModel(
    private val repo: CurriculumRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(GlossaryUiState())
    val state: StateFlow<GlossaryUiState> = _state.asStateFlow()

    /** All terms, sorted by the repository. */
    private var allTerms: List<GlossaryTerm> = emptyList()

    init {
        load()
    }

    fun retry() {
        load()
    }

    fun setQuery(text: String) {
        _state.update { it.copy(query = text, groups = groupFor(allTerms, text)) }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                allTerms = repo.glossary()
                _state.update { it.copy(loading = false, groups = groupFor(allTerms, it.query)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = "Could not load the glossary.") }
            }
        }
    }

    private fun groupFor(terms: List<GlossaryTerm>, query: String): List<LetterGroup> {
        val q = query.trim()
        val visible = if (q.isEmpty()) {
            terms
        } else {
            terms.filter {
                it.term.contains(q, ignoreCase = true) || it.definition.contains(q, ignoreCase = true)
            }
        }
        return visible
            .groupBy { letterOf(it.term) }
            .map { (letter, items) -> LetterGroup(letter, items) }
            .sortedBy { if (it.letter == '#') '￿' else it.letter }
    }

    private fun letterOf(term: String): Char {
        val c = term.trim().firstOrNull()?.uppercaseChar() ?: return '#'
        return if (c.isLetter()) c else '#'
    }
}
