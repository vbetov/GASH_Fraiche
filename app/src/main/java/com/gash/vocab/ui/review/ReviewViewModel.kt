package com.gash.vocab.ui.review

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gash.vocab.GashApp
import com.gash.vocab.data.db.ProgressEntity
import com.gash.vocab.data.db.WordEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class ReviewMode { START_PAGE, FRONT, CHECK, CLOZE, CHOICE, EXPLORE, RESULT }

/** Snapshot for undo: the UI state before the action plus the DB progress state. */
private data class UndoSnapshot(
    val uiState: ReviewUiState,
    val progressSnapshot: ProgressEntity?
)

data class ReviewUiState(
    val currentWord: WordEntity? = null,
    val mode: ReviewMode = ReviewMode.START_PAGE,
    val canUndo: Boolean = false,
    val queue: List<Int> = emptyList(),
    val queueIndex: Int = 0,
    val choiceOptions: List<String> = emptyList(),
    val selectedClozeIndex: Int = 0,
    val clozeRevealed: Boolean = false,
    val choiceAnswer: String? = null,
    val isCorrect: Boolean? = null,
    val sessionComplete: Boolean = false,
    val noCardsDue: Boolean = false,
    val allWeeks: List<String> = emptyList(),
    val allPos: List<String> = emptyList(),
    val selectedWeek: String? = null,
    val selectedPos: String? = null
)

class ReviewViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as GashApp).repository
    private val settings = (application as GashApp).settings

    private val _state = MutableStateFlow(ReviewUiState())
    val state: StateFlow<ReviewUiState> = _state.asStateFlow()

    /** Single-level undo snapshot. */
    private var undoSnapshot: UndoSnapshot? = null

    /** Save current state + DB progress before recording a review. */
    private suspend fun saveUndoSnapshot() {
        val s = _state.value
        val wordId = s.currentWord?.id ?: return
        val progress = repo.getProgress(wordId)
        undoSnapshot = UndoSnapshot(uiState = s, progressSnapshot = progress)
    }

    /** Undo the last review action — restore UI state and DB progress. */
    fun undo() {
        val snapshot = undoSnapshot ?: return
        viewModelScope.launch {
            repo.restoreProgress(
                snapshot.uiState.currentWord!!.id,
                snapshot.progressSnapshot
            )
            _state.value = snapshot.uiState.copy(canUndo = false)
            undoSnapshot = null
        }
    }

    /** True when a review session is actively in progress (not on start page or complete). */
    val isReviewActive: Boolean
        get() {
            val s = _state.value
            return s.mode != ReviewMode.START_PAGE && !s.sessionComplete
        }

    init {
        loadStartPage()
    }

    private fun loadStartPage() {
        viewModelScope.launch {
            val weeks = repo.getAllWeeks().first()
            val pos = repo.getAllPosValues().first()
            _state.value = _state.value.copy(
                mode = ReviewMode.START_PAGE,
                allWeeks = weeks,
                allPos = pos,
                sessionComplete = false,
                noCardsDue = false,
                selectedWeek = null,
                selectedPos = null
            )
        }
    }

    fun startSession() {
        viewModelScope.launch {
            val s = _state.value
            // Read live settings values each time a session starts
            val newPerDay = settings.effectiveNewPerDay
            val reviewsPerDay = settings.reviewsPerDay
            val queue = repo.getReviewQueue(newPerDay, reviewsPerDay)
            if (queue.isEmpty()) {
                _state.value = s.copy(
                    mode = ReviewMode.START_PAGE,
                    sessionComplete = false,
                    noCardsDue = true,
                    queue = emptyList()
                )
                return@launch
            }
            _state.value = s.copy(
                queue = queue, queueIndex = 0,
                sessionComplete = false, noCardsDue = false, selectedWeek = null
            )
            loadWord(queue[0])
        }
    }

    fun startWeekSession(week: String) {
        viewModelScope.launch {
            val weekIds = repo.getWordIdsByWeek(week)
            if (weekIds.isEmpty()) {
                _state.value = _state.value.copy(sessionComplete = true, queue = emptyList())
                return@launch
            }
            val shuffled = weekIds.shuffled()
            _state.value = _state.value.copy(
                queue = shuffled, queueIndex = 0,
                sessionComplete = false, selectedWeek = week
            )
            loadWord(shuffled[0])
        }
    }

    fun startPosSession(pos: String) {
        viewModelScope.launch {
            val posIds = repo.getWordIdsByPos(pos)
            if (posIds.isEmpty()) {
                _state.value = _state.value.copy(sessionComplete = true, queue = emptyList())
                return@launch
            }
            val shuffled = posIds.shuffled()
            _state.value = _state.value.copy(
                queue = shuffled, queueIndex = 0,
                sessionComplete = false, selectedPos = pos
            )
            loadWord(shuffled[0])
        }
    }

    fun backToStart() {
        loadStartPage()
    }

    private suspend fun loadWord(wordId: Int) {
        val word = repo.getWordById(wordId) ?: return
        _state.value = _state.value.copy(
            currentWord = word,
            mode = ReviewMode.FRONT,
            clozeRevealed = false,
            choiceAnswer = null,
            isCorrect = null,
            selectedClozeIndex = (0 until word.cloze.size).random()
        )
    }

    // ── User actions ──────────────────────────────────────────────

    fun doCheck() {
        _state.value = _state.value.copy(mode = ReviewMode.CHECK)
    }

    fun doCloze() {
        _state.value = _state.value.copy(mode = ReviewMode.CLOZE, clozeRevealed = false)
    }

    fun doChoice() {
        val word = _state.value.currentWord ?: return
        // Build 4 options: correct answer + 3 relatedEN distractors, shuffled
        val distractors = word.relatedEN.take(3)
        val options = (distractors + word.english).shuffled()
        _state.value = _state.value.copy(
            mode = ReviewMode.CHOICE,
            choiceOptions = options,
            choiceAnswer = null,
            isCorrect = null
        )
    }

    fun doExplore() {
        _state.value = _state.value.copy(mode = ReviewMode.EXPLORE)
    }

    fun doDontKnow() {
        val word = _state.value.currentWord ?: return
        viewModelScope.launch {
            saveUndoSnapshot()
            repo.recordReview(word.id, "dk", false)
            advance()
        }
    }

    /** Check mode — user says "I knew it" */
    fun confirmCheck() {
        val word = _state.value.currentWord ?: return
        viewModelScope.launch {
            saveUndoSnapshot()
            repo.recordReview(word.id, "check", true)
            advance()
        }
    }

    /** Check mode — user says "I didn't know" */
    fun failCheck() {
        val word = _state.value.currentWord ?: return
        viewModelScope.launch {
            saveUndoSnapshot()
            repo.recordReview(word.id, "dk", false)
            advance()
        }
    }

    /** Cloze — reveal answer */
    fun revealCloze() {
        _state.value = _state.value.copy(clozeRevealed = true)
    }

    /** Cloze — user confirms they knew it */
    fun confirmCloze() {
        val word = _state.value.currentWord ?: return
        viewModelScope.launch {
            saveUndoSnapshot()
            repo.recordReview(word.id, "cloze", true)
            advance()
        }
    }

    /** Cloze — user says they didn't know */
    fun failCloze() {
        val word = _state.value.currentWord ?: return
        viewModelScope.launch {
            saveUndoSnapshot()
            repo.recordReview(word.id, "dk", false)
            advance()
        }
    }

    /** Choice — user selects an answer */
    fun selectChoice(answer: String) {
        val word = _state.value.currentWord ?: return
        val correct = answer == word.english
        _state.value = _state.value.copy(
            choiceAnswer = answer,
            isCorrect = correct
        )
        viewModelScope.launch {
            saveUndoSnapshot()
            repo.recordReview(word.id, "choice", correct)
        }
    }

    /** After choice result shown, move to next */
    fun advanceFromChoice() {
        viewModelScope.launch { advance() }
    }

    /** Explore — just move on (no score impact) */
    fun advanceFromExplore() {
        viewModelScope.launch { advance() }
    }

    /** Record "didn't know" and go to Explore (Next on Explore won't record again) */
    fun failAndExplore() {
        val word = _state.value.currentWord ?: return
        viewModelScope.launch {
            saveUndoSnapshot()
            repo.recordReview(word.id, "dk", false)
        }
        _state.value = _state.value.copy(mode = ReviewMode.EXPLORE)
    }

    private suspend fun advance() {
        val s = _state.value
        val nextIndex = s.queueIndex + 1
        if (nextIndex >= s.queue.size) {
            _state.value = s.copy(sessionComplete = true, canUndo = true)
        } else {
            _state.value = s.copy(queueIndex = nextIndex, canUndo = true)
            loadWord(s.queue[nextIndex])
        }
    }
}
