package com.gash.vocab.ui.browse

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gash.vocab.GashApp
import com.gash.vocab.data.db.ProgressEntity
import com.gash.vocab.data.db.WordEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class CompletionLevel(val label: String) {
    ALL("All"),
    NEW("New"),
    LEARNING("Learning"),
    MATURE("Mature")
}

enum class SortField(val label: String) {
    ID("Default"),
    FIRST_SEEN("First Seen"),
    LAST_SEEN("Last Seen"),
    CHECK("Check ✓"),
    CLOZE("Cloze ✍"),
    CHOICE("Choice"),
    DONT_KNOW("Don't Know ✗"),
    EASE_FACTOR("Ease Factor")
}

data class BrowseUiState(
    val words: List<WordEntity> = emptyList(),
    val progressMap: Map<Int, ProgressEntity> = emptyMap(),
    val allPos: List<String> = emptyList(),
    val allSources: List<String> = emptyList(),
    val allWeeks: List<String> = emptyList(),
    val filterPos: String? = null,
    val filterSource: String? = null,
    val filterWeeks: String? = null,
    val filterCompletion: CompletionLevel = CompletionLevel.ALL,
    val searchQuery: String = "",
    val sortField: SortField = SortField.ID,
    val sortAscending: Boolean = true,
    val expandedWordId: Int? = null
)

class BrowseViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as GashApp).repository

    private val _filterPos = MutableStateFlow<String?>(null)
    private val _filterSource = MutableStateFlow<String?>(null)
    private val _filterWeeks = MutableStateFlow<String?>(null)
    private val _filterCompletion = MutableStateFlow(CompletionLevel.ALL)
    private val _searchQuery = MutableStateFlow("")
    private val _sortField = MutableStateFlow(SortField.ID)
    private val _sortAscending = MutableStateFlow(true)
    private val _expandedWordId = MutableStateFlow<Int?>(null)

    val state: StateFlow<BrowseUiState> = combine(
        repo.getAllWords(),
        repo.getAllProgress(),
        combine(repo.getAllPosValues(), repo.getAllSources(), repo.getAllWeeks()) { pos, src, wks ->
            Triple(pos, src, wks)
        },
        combine(
            _filterPos, _filterSource, _filterWeeks,
            _filterCompletion, _searchQuery
        ) { pos, src, wks, comp, q ->
            FilterState(pos, src, wks, comp, q)
        },
        combine(_sortField, _sortAscending, _expandedWordId) { sort, asc, exp ->
            Triple(sort, asc, exp)
        }
    ) { args ->
        val words = args[0] as List<WordEntity>
        val progress = args[1] as List<ProgressEntity>
        val (posValues, sources, weeksValues) = args[2] as Triple<List<String>, List<String>, List<String>>
        val filters = args[3] as FilterState
        val (sortField, sortAsc, expanded) = args[4] as Triple<SortField, Boolean, Int?>

        val progressMap = progress.associateBy { it.wordId }

        val filtered = words.filter { w ->
            val matchPos = filters.pos == null || w.pos == filters.pos
            val matchSource = filters.source == null || w.source == filters.source
            val matchWeeks = filters.weeks == null || w.weeks == filters.weeks
            val matchQuery = filters.query.isBlank() ||
                    w.french.contains(filters.query, ignoreCase = true) ||
                    w.english.contains(filters.query, ignoreCase = true)
            val matchCompletion = when (filters.completion) {
                CompletionLevel.ALL -> true
                CompletionLevel.NEW -> progressMap[w.id] == null
                CompletionLevel.LEARNING -> {
                    val p = progressMap[w.id]
                    p != null && p.intervalDays < 21
                }
                CompletionLevel.MATURE -> {
                    val p = progressMap[w.id]
                    p != null && p.intervalDays >= 21
                }
            }
            matchPos && matchSource && matchWeeks && matchQuery && matchCompletion
        }

        val sorted = sortWords(filtered, sortField, sortAsc, progressMap)

        BrowseUiState(
            words = sorted,
            progressMap = progressMap,
            allPos = posValues,
            allSources = sources,
            allWeeks = weeksValues,
            filterPos = filters.pos,
            filterSource = filters.source,
            filterWeeks = filters.weeks,
            filterCompletion = filters.completion,
            searchQuery = filters.query,
            sortField = sortField,
            sortAscending = sortAsc,
            expandedWordId = expanded
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BrowseUiState())

    private fun sortWords(
        words: List<WordEntity>,
        field: SortField,
        ascending: Boolean,
        progressMap: Map<Int, ProgressEntity>
    ): List<WordEntity> {
        val comparator: Comparator<WordEntity> = when (field) {
            SortField.ID -> compareBy { it.id }
            SortField.FIRST_SEEN -> compareBy(nullsLast()) { progressMap[it.id]?.firstEncountered }
            SortField.LAST_SEEN -> compareBy(nullsLast()) { progressMap[it.id]?.lastEncountered }
            SortField.CHECK -> compareBy { progressMap[it.id]?.knewCheck ?: 0 }
            SortField.CLOZE -> compareBy { progressMap[it.id]?.knewCloze ?: 0 }
            SortField.CHOICE -> compareBy { progressMap[it.id]?.knewChoice ?: 0 }
            SortField.DONT_KNOW -> compareBy { progressMap[it.id]?.didntKnow ?: 0 }
            SortField.EASE_FACTOR -> compareBy { progressMap[it.id]?.easeFactor ?: 2.5 }
        }
        return if (ascending) words.sortedWith(comparator) else words.sortedWith(comparator.reversed())
    }

    fun setFilterPos(pos: String?) { _filterPos.value = pos }
    fun setFilterSource(source: String?) { _filterSource.value = source }
    fun setFilterWeeks(weeks: String?) { _filterWeeks.value = weeks }
    fun setFilterCompletion(level: CompletionLevel) { _filterCompletion.value = level }
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setSortField(field: SortField) { _sortField.value = field }
    fun toggleSortDirection() { _sortAscending.value = !_sortAscending.value }
    fun toggleExpanded(wordId: Int) {
        _expandedWordId.value = if (_expandedWordId.value == wordId) null else wordId
    }

    private data class FilterState(
        val pos: String?,
        val source: String?,
        val weeks: String?,
        val completion: CompletionLevel,
        val query: String
    )
}
