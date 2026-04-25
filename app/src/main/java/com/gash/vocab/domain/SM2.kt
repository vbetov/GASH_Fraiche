package com.gash.vocab.domain

import com.gash.vocab.data.db.ProgressEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * SM-2 spaced repetition algorithm.
 *
 * Quality scores by interaction mode:
 *   check  = 4  (highest — user saw French, self-assessed "I knew it")
 *   cloze  = 3  (fill-in-the-blank correct)
 *   choice = 2  (multiple choice correct)
 *   don't know = 0
 */
object SM2 {

    data class Result(
        val easeFactor: Double,
        val intervalDays: Int,
        val repetitions: Int,
        val nextReview: Long
    )

    /**
     * Calculate next review parameters.
     *
     * @param quality  0–5 quality score
     * @param current  current progress state (null for first encounter)
     */
    fun calculate(quality: Int, current: ProgressEntity?): Result {
        val ef = current?.easeFactor ?: 2.5
        val reps = current?.repetitions ?: 0
        val interval = current?.intervalDays ?: 0

        // SM-2 ease factor adjustment
        val newEf = if (quality >= 2) {
            maxOf(1.3, ef + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02)))
        } else {
            // Failed — reset but don't reduce EF below 1.3
            maxOf(1.3, ef - 0.2)
        }

        val newReps: Int
        val newInterval: Int

        if (quality < 2) {
            // Failed — reset to beginning
            newReps = 0
            newInterval = 1
        } else {
            newReps = reps + 1
            newInterval = when {
                reps == 0 -> 1
                reps == 1 -> 6
                else -> maxOf(1, (interval * newEf).toInt())
            }
        }

        val nextReview = LocalDate.now()
            .plusDays(newInterval.toLong())
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        return Result(
            easeFactor = newEf,
            intervalDays = newInterval,
            repetitions = newReps,
            nextReview = nextReview
        )
    }

    /**
     * Return the quality score for a given interaction mode and correctness.
     */
    fun qualityForMode(mode: String, correct: Boolean): Int {
        if (!correct) return 0
        return when (mode) {
            "check" -> 5
            "cloze" -> 2
            "choice" -> 1
            else -> 0
        }
    }
}
