package com.gash.vocab.data.importer

/**
 * Data class matching the JSON import format from vocab_import_YYYY-MM-DD.json.
 * Gson deserialises directly into this structure.
 */
data class VocabImportEntry(
    val id: Int,
    val french: String,
    val english: String,
    val example: String,
    val pos: String,
    val source: String,
    val weeks: String = "",
    val notes: String = "",
    val cloze: List<String> = emptyList(),
    val related: List<String> = emptyList(),
    val relatedEN: List<String> = emptyList(),
    val etymology: List<String> = emptyList()
)
