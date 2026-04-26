package com.gash.vocab.data.repository

import android.content.Context
import android.net.Uri
import com.gash.vocab.data.db.AppDatabase
import com.gash.vocab.data.db.ProgressEntity
import com.gash.vocab.data.db.WordEntity
import com.gash.vocab.domain.SM2
import com.gash.vocab.util.normaliseFrench
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
     * Build the review queue for a single Part of Speech.
     *
     * Same shape as [getReviewQueue] (due cards in next_review order with
     * RANDOM() tiebreaker, then new cards in random order), restricted to
     * words whose `pos` matches [pos]. Daily study limits (`newPerDay`,
     * `reviewsPerDay`) are intentionally NOT applied here — POS sessions
     * pull every currently-due card and every unseen card for that POS.
     */
    suspend fun getReviewQueueByPos(pos: String): List<Int> {
        val now = Instant.now().toEpochMilli()

        val dueCards = progressDao.getDueCardsByPos(now, pos)
        val newWordIds = progressDao.getNewWordIdsByPos(pos, Int.MAX_VALUE)

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

    // ── AI prompt template ─────────────────────────────────────────

    /**
     * Reads the bundled prompt template (`assets/vocab_prompt_template.md`),
     * injects the current vocabulary as a markdown list into the
     * `{{EXISTING_VOCAB_LIST}}` placeholder so the AI assistant can deduplicate
     * against it, and writes the rendered file to [uri].
     *
     * Returns the number of words injected on success.
     */
    suspend fun savePromptTemplate(context: Context, uri: Uri): Result<Int> {
        return try {
            val template = context.assets.open("vocab_prompt_template.md")
                .bufferedReader().use { it.readText() }

            val words = wordDao.getAllWordsList()
            val vocabBlock = if (words.isEmpty()) {
                "_(no existing vocabulary in the database yet — start IDs from 1)_"
            } else {
                words.joinToString("\n") { word ->
                    "- ${word.french}  *(normalised: `${word.normKey}`)*"
                }
            }

            val rendered = template.replace("{{EXISTING_VOCAB_LIST}}", vocabBlock)

            val outputStream = context.contentResolver.openOutputStream(uri)
                ?: return Result.failure(Exception("Could not open file for writing"))
            outputStream.use { stream ->
                stream.bufferedWriter().use { it.write(rendered) }
            }

            Result.success(words.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Progress export/import ────────────────────────────────────

    /**
     * Wire format for an exported progress entry.
     *
     * Mirrors every field on [ProgressEntity], plus two extra identity fields
     * (`french`, `normKey`) so that on re-import we can match progress to the
     * current vocabulary by *content*, not by raw `wordId`. That means a seed
     * revision that reorders or renumbers words won't break re-imports.
     *
     * All fields have defaults so a legacy export (only `wordId` + progress
     * fields, no `french`/`normKey`) still parses cleanly into this class —
     * the importer falls back to wordId matching in that case.
     */
    data class ExportableProgress(
        // ── Identity ──
        /** French headword. Human-readable; may be re-normalised on import. */
        val french: String? = null,
        /** Normalised French key — preferred matcher on import. */
        val normKey: String? = null,
        // ── Progress fields (mirror ProgressEntity) ──
        val wordId: Int = 0,
        val easeFactor: Double = 2.5,
        val intervalDays: Int = 0,
        val repetitions: Int = 0,
        val nextReview: Long = 0L,
        val firstEncountered: String? = null,
        val lastEncountered: String? = null,
        val knewCheck: Int = 0,
        val knewCloze: Int = 0,
        val knewChoice: Int = 0,
        val didntKnow: Int = 0,
        val qualityHistory: List<Int> = emptyList()
    ) {
        fun toProgressEntity(targetWordId: Int): ProgressEntity = ProgressEntity(
            wordId = targetWordId,
            easeFactor = easeFactor,
            intervalDays = intervalDays,
            repetitions = repetitions,
            nextReview = nextReview,
            firstEncountered = firstEncountered,
            lastEncountered = lastEncountered,
            knewCheck = knewCheck,
            knewCloze = knewCloze,
            knewChoice = knewChoice,
            didntKnow = didntKnow,
            qualityHistory = qualityHistory
        )

        companion object {
            fun fromEntity(entity: ProgressEntity, word: WordEntity): ExportableProgress =
                ExportableProgress(
                    french = word.french,
                    normKey = word.normKey,
                    wordId = entity.wordId,
                    easeFactor = entity.easeFactor,
                    intervalDays = entity.intervalDays,
                    repetitions = entity.repetitions,
                    nextReview = entity.nextReview,
                    firstEncountered = entity.firstEncountered,
                    lastEncountered = entity.lastEncountered,
                    knewCheck = entity.knewCheck,
                    knewCloze = entity.knewCloze,
                    knewChoice = entity.knewChoice,
                    didntKnow = entity.didntKnow,
                    qualityHistory = entity.qualityHistory
                )
        }
    }

    suspend fun exportProgress(context: Context, uri: Uri): Result<Int> {
        return try {
            val allProgress = progressDao.getAllProgressList()
            // Index current vocabulary by id so each progress row can carry its
            // french + normKey on the wire.
            val wordsById = wordDao.getAllWordsList().associateBy { it.id }

            val exportable = allProgress.mapNotNull { p ->
                val word = wordsById[p.wordId] ?: return@mapNotNull null
                ExportableProgress.fromEntity(p, word)
            }

            val json = gson.toJson(exportable)
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                val writer = stream.bufferedWriter()
                writer.write(json)
                writer.flush()
            }
            Result.success(exportable.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Result of a progress-import operation.
     *
     * @property imported   entries successfully written to the progress table
     * @property remapped   subset of [imported] where the matched word's id differs
     *                      from the entry's stored `wordId` (i.e. the word was
     *                      identified by content fingerprint after a renumber)
     * @property skipped    human-readable identifiers of entries that didn't match
     *                      any word in the current vocabulary
     */
    data class ProgressImportSummary(
        val imported: Int,
        val remapped: Int,
        val skipped: List<String>
    )

    suspend fun importProgress(context: Context, uri: Uri): Result<ProgressImportSummary> {
        return try {
            val json = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader().readText()
            } ?: return Result.failure(Exception("Could not read file"))

            val type = object : TypeToken<List<ExportableProgress>>() {}.type
            val entries: List<ExportableProgress> = gson.fromJson(json, type)
                ?: return Result.failure(Exception("File is not a progress JSON list"))

            // Build a content-fingerprint lookup against the *current* vocabulary.
            val currentWords = wordDao.getAllWordsList()
            val byNormKey: Map<String, Int> = currentWords.associate { it.normKey to it.id }
            val validIds: Set<Int> = currentWords.map { it.id }.toHashSet()

            val skipped = mutableListOf<String>()
            var imported = 0
            var remapped = 0

            entries.forEach { entry ->
                // Resolve the target word by content fingerprint first. Re-normalise
                // `french` from the file in case the export pre-dated a normaliser
                // tweak. Fall back to raw `wordId` only when no identity fields were
                // provided (legacy exports).
                val candidateKey = when {
                    !entry.normKey.isNullOrBlank() -> entry.normKey
                    !entry.french.isNullOrBlank() -> normaliseFrench(entry.french)
                    else -> null
                }

                val targetId: Int? = when {
                    candidateKey != null -> byNormKey[candidateKey]
                    entry.wordId in validIds -> entry.wordId
                    else -> null
                }

                if (targetId == null) {
                    skipped.add(entry.french ?: "id=${entry.wordId}")
                    return@forEach
                }

                if (targetId != entry.wordId) remapped++

                val progressRow = entry.toProgressEntity(targetWordId = targetId)
                val existing = progressDao.getProgress(targetId)
                if (existing != null) {
                    progressDao.updateProgress(progressRow)
                } else {
                    progressDao.insertProgress(progressRow)
                }
                imported++
            }

            Result.success(
                ProgressImportSummary(
                    imported = imported,
                    remapped = remapped,
                    skipped = skipped
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
