package com.gash.vocab.data.repository

import android.content.Context
import android.net.Uri
import com.gash.vocab.data.db.AppDatabase
import com.gash.vocab.data.db.ProgressEntity
import com.gash.vocab.data.db.WordEntity
import com.gash.vocab.domain.SM2
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class VocabRepository(private val db: AppDatabase) {

    private val wordDao = db.wordDao()
    private val progressDao = db.progressDao()
    private val isoFormatter = DateTimeFormatter.ISO_INSTANT
    private val gson = Gson()

    // ── Word queries ──────────────────────────────────────────────

    fun getAllWords(): Flow<List<WordEntity>> = wordDao.getAllWords()

    suspend fun getWordById(id: Int): WordEntity? = wordDao.getWordById(id)

    fun getAllPosValues(): Flow<List<String>> = wordDao.getAllPosValues()

    fun getAllSources(): Flow<List<String>> = wordDao.getAllSources()

    suspend fun getWordCount(): Int = wordDao.getWordCount()

    fun getAllWeeks(): Flow<List<String>> = wordDao.getAllWeeks()

    suspend fun getWordIdsByWeek(weeks: String): List<Int> = wordDao.getWordIdsByWeek(weeks)

    suspend fun getWordIdsByPos(pos: String): List<Int> = wordDao.getWordIdsByPos(pos)

    // ── Progress queries ──────────────────────────────────────────

    fun getAllProgress(): Flow<List<ProgressEntity>> = progressDao.getAllProgress()

    suspend fun getProgress(wordId: Int): ProgressEntity? = progressDao.getProgress(wordId)

    // ── Review session ────────────────────────────────────────────

    /**
     * Build the review queue: due cards + new cards up to daily limits.
     * Returns word IDs in review order.
     */
    suspend fun getReviewQueue(newPerDay: Int, reviewsPerDay: Int): List<Int> {
        val now = Instant.now().toEpochMilli()
        val todayPrefix = LocalDate.now().toString() // "2026-04-24"

        // Due reviews
        val dueCards = progressDao.getDueCards(now).take(reviewsPerDay)

        // New cards (not yet in progress table)
        val studiedToday = progressDao.getStudiedTodayCount(todayPrefix)
        val newSlots = maxOf(0, newPerDay - studiedToday)
        val newWordIds = progressDao.getNewWordIds(newSlots)

        return dueCards.map { it.wordId } + newWordIds
    }

    /**
     * Record a review result.
     *
     * @param wordId  the word reviewed
     * @param mode    interaction mode: "check", "cloze", "choice", "dk"
     * @param correct whether the user got it right
     */
    suspend fun recordReview(wordId: Int, mode: String, correct: Boolean) {
        val quality = SM2.qualityForMode(mode, correct)
        val current = progressDao.getProgress(wordId)
        val sm2Result = SM2.calculate(quality, current)
        val nowIso = Instant.now().toString()

        val newHistory = (current?.qualityHistory ?: emptyList()) + quality

        val updated = ProgressEntity(
            wordId = wordId,
            easeFactor = sm2Result.easeFactor,
            intervalDays = sm2Result.intervalDays,
            repetitions = sm2Result.repetitions,
            nextReview = sm2Result.nextReview,
            firstEncountered = current?.firstEncountered ?: nowIso,
            lastEncountered = nowIso,
            knewCheck = (current?.knewCheck ?: 0) + if (mode == "check" && correct) 1 else 0,
            knewCloze = (current?.knewCloze ?: 0) + if (mode == "cloze" && correct) 1 else 0,
            knewChoice = (current?.knewChoice ?: 0) + if (mode == "choice" && correct) 1 else 0,
            didntKnow = (current?.didntKnow ?: 0) + if (!correct) 1 else 0,
            qualityHistory = newHistory
        )

        if (current == null) {
            progressDao.insertProgress(updated)
        } else {
            progressDao.updateProgress(updated)
        }
    }

    /** Restore a previous progress state, or delete the entry if it was null (word was new). */
    suspend fun restoreProgress(wordId: Int, previous: ProgressEntity?) {
        if (previous == null) {
            progressDao.deleteProgress(wordId)
        } else {
            progressDao.insertProgress(previous) // REPLACE strategy overwrites
        }
    }

    // ── Progress export/import ────────────────────────────────────

    suspend fun exportProgress(context: Context, uri: Uri): Result<Int> {
        return try {
            val allProgress = progressDao.getAllProgressList()
            val json = gson.toJson(allProgress)
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                val writer = stream.bufferedWriter()
                writer.write(json)
                writer.flush()
            }
            Result.success(allProgress.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importProgress(context: Context, uri: Uri): Result<Int> {
        return try {
            val json = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader().readText()
            } ?: return Result.failure(Exception("Could not read file"))

            val type = object : TypeToken<List<ProgressEntity>>() {}.type
            val entries: List<ProgressEntity> = gson.fromJson(json, type)

            entries.forEach { entry ->
                val existing = progressDao.getProgress(entry.wordId)
                if (existing != null) {
                    progressDao.updateProgress(entry)
                } else {
                    progressDao.insertProgress(entry)
                }
            }
            Result.success(entries.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
