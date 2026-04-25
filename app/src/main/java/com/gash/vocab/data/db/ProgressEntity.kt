package com.gash.vocab.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Room entity tracking SM-2 spaced repetition progress for each word.
 * Linked to WordEntity via foreign key on wordId.
 */
@Entity(
    tableName = "progress",
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["word_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ProgressEntity(
    @PrimaryKey
    @ColumnInfo(name = "word_id")
    val wordId: Int,

    /** SM-2 ease factor, starts at 2.5 */
    @ColumnInfo(name = "ease_factor")
    val easeFactor: Double = 2.5,

    /** Current interval in days */
    @ColumnInfo(name = "interval_days")
    val intervalDays: Int = 0,

    /** Number of consecutive correct repetitions */
    @ColumnInfo(name = "repetitions")
    val repetitions: Int = 0,

    /** Unix timestamp (ms) of next scheduled review */
    @ColumnInfo(name = "next_review")
    val nextReview: Long = 0L,

    /** ISO timestamp of first encounter */
    @ColumnInfo(name = "first_encountered")
    val firstEncountered: String? = null,

    /** ISO timestamp of last encounter */
    @ColumnInfo(name = "last_encountered")
    val lastEncountered: String? = null,

    /** Count of correct "check" (quality=4) responses */
    @ColumnInfo(name = "knew_check")
    val knewCheck: Int = 0,

    /** Count of correct "cloze" (quality=3) responses */
    @ColumnInfo(name = "knew_cloze")
    val knewCloze: Int = 0,

    /** Count of correct "choice" (quality=2) responses */
    @ColumnInfo(name = "knew_choice")
    val knewChoice: Int = 0,

    /** Count of "don't know" (quality=0) responses */
    @ColumnInfo(name = "didnt_know")
    val didntKnow: Int = 0,

    /** History of quality scores as JSON array, e.g. [4, 3, 0, 4] */
    @ColumnInfo(name = "quality_history")
    val qualityHistory: List<Int> = emptyList()
)
