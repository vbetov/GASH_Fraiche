package com.gash.vocab.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a vocabulary word.
 * Maps to the 16-column spreadsheet schema from DFAT French Vocabulary Consolidated.xlsx.
 */
@Entity(tableName = "words")
data class WordEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int,

    @ColumnInfo(name = "french")
    val french: String,

    @ColumnInfo(name = "english")
    val english: String,

    @ColumnInfo(name = "example")
    val example: String,

    @ColumnInfo(name = "pos")
    val pos: String,

    @ColumnInfo(name = "source")
    val source: String,

    @ColumnInfo(name = "weeks")
    val weeks: String = "",

    @ColumnInfo(name = "notes")
    val notes: String = "",

    /** Three cloze sentences stored as JSON array via TypeConverter */
    @ColumnInfo(name = "cloze")
    val cloze: List<String>,

    /** Three related words in "French (English)" format */
    @ColumnInfo(name = "related")
    val related: List<String>,

    /** Three related word English translations only (used as MC distractors) */
    @ColumnInfo(name = "related_en")
    val relatedEN: List<String>,

    /** Three etymologically related French words */
    @ColumnInfo(name = "etymology")
    val etymology: List<String>,

    /** Normalised key for deduplication: NFC + straight apostrophes + lowercase */
    @ColumnInfo(name = "norm_key")
    val normKey: String
)
