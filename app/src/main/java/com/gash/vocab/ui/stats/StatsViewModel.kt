package com.gash.vocab.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gash.vocab.GashApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

data class StatsUiState(
    val totalWords: Int = 0,
    val seenWords: Int = 0,
    val matureWords: Int = 0,
    val dueToday: Int = 0,
    val studiedToday: Int = 0,
    val totalReviews: Int = 0,
    val accuracy: String = "—",
    val avgEaseFactor: String = "—",
    val checkCount: Int = 0,
    val clozeCount: Int = 0,
    val choiceCount: Int = 0,
    val dontKnowCount: Int = 0,
    val isLoaded: Boolean = false
)

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as GashApp).repository
    private val db = (application as GashApp).database

    private val _state = MutableStateFlow(StatsUiState())
    val state: StateFlow<StatsUiState> = _state.asStateFlow()

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            val totalWords = repo.getWordCount()
            val allProgress = db.progressDao().getAllProgressList()

            val seen = allProgress.count { it.repetitions > 0 }
            val mature = allProgress.count { it.intervalDays >= 21 }

            val now = Instant.now().toEpochMilli()
            val dueToday = allProgress.count { it.nextReview in 1..now }

            val todayPrefix = LocalDate.now().toString()
            val studiedToday = allProgress.count {
                it.lastEncountered?.startsWith(todayPrefix) == true
            }

            var totalReviews = 0
            var correctReviews = 0
            var totalCheck = 0
            var totalCloze = 0
            var totalChoice = 0
            var totalDk = 0

            allProgress.forEach { p ->
                totalCheck += p.knewCheck
                totalCloze += p.knewCloze
                totalChoice += p.knewChoice
                totalDk += p.didntKnow

                p.qualityHistory.forEach { q ->
                    totalReviews++
                    if (q >= 2) correctReviews++
                }
            }

            val accuracy = if (totalReviews > 0) {
                "${(correctReviews * 100 / totalReviews)}%"
            } else "—"

            val avgEf = if (allProgress.isNotEmpty()) {
                val avg = allProgress.map { it.easeFactor }.average()
                String.format("%.2f", avg)
            } else "—"

            _state.value = StatsUiState(
                totalWords = totalWords,
                seenWords = seen,
                matureWords = mature,
                dueToday = dueToday,
                studiedToday = studiedToday,
                totalReviews = totalReviews,
                accuracy = accuracy,
                avgEaseFactor = avgEf,
                checkCount = totalCheck,
                clozeCount = totalCloze,
                choiceCount = totalChoice,
                dontKnowCount = totalDk,
                isLoaded = true
            )
        }
    }
}
