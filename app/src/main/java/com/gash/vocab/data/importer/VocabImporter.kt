package com.gash.vocab.data.importer

import android.content.Context
import android.net.Uri
import com.gash.vocab.data.db.WordDao
import com.gash.vocab.data.db.WordEntity
import com.gash.vocab.util.normaliseFrench
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Imports vocabulary entries from a JSON file selected via the system file picker.
 *
 * Upsert logic:
 * - If a word with the same normalised key already exists, update its content fields
 *   (keeping the existing ID so progress is preserved).
 * - If no match, assign the next sequential ID and insert.
 */
class VocabImporter(
    private val wordDao: WordDao,
    private val gson: Gson = Gson()
) {

    data class ImportResult(
        val inserted: Int,
        val updated: Int,
        val errors: List<String>
    )

    /**
     * Import from a content URI (from ACTION_OPEN_DOCUMENT).
     */
    suspend fun importFromUri(context: Context, uri: Uri): ImportResult {
        val jsonText = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader().readText()
        } ?: return ImportResult(0, 0, listOf("Could not read file"))

        return importFromJson(jsonText)
    }

    /**
     * Import from a raw JSON string (used for seed data from assets).
     */
    suspend fun importFromJson(jsonText: String): ImportResult {
        val type = object : TypeToken<List<VocabImportEntry>>() {}.type
        val entries: List<VocabImportEntry> = try {
            gson.fromJson(jsonText, type)
        } catch (e: Exception) {
            return ImportResult(0, 0, listOf("Invalid JSON: ${e.message}"))
        }

        var inserted = 0
        var updated = 0
        val errors = mutableListOf<String>()
        var nextId = (wordDao.getMaxId() ?: 0) + 1

        for (entry in entries) {
            try {
                val normKey = normaliseFrench(entry.french)
                val existing = wordDao.getWordByNormKey(normKey)

                if (existing != null) {
                    // Update content, preserve ID (and thus progress link)
                    wordDao.updateWord(
                        existing.copy(
                            french = entry.french,
                            english = entry.english,
                            example = entry.example,
                            exampleA2 = entry.exampleA2,
                            pos = entry.pos,
                            source = entry.source,
                            weeks = entry.weeks,
                            notes = entry.notes,
                            cloze = entry.cloze,
                            clozeA2 = entry.clozeA2,
                            related = entry.related,
                            relatedEN = entry.relatedEN,
                            etymology = entry.etymology,
                            normKey = normKey
                        )
                    )
                    updated++
                } else {
                    wordDao.insertWord(
                        WordEntity(
                            id = nextId++,
                            french = entry.french,
                            english = entry.english,
                            example = entry.example,
                            exampleA2 = entry.exampleA2,
                            pos = entry.pos,
                            source = entry.source,
                            weeks = entry.weeks,
                            notes = entry.notes,
                            cloze = entry.cloze,
                            clozeA2 = entry.clozeA2,
                            related = entry.related,
                            relatedEN = entry.relatedEN,
                            etymology = entry.etymology,
                            normKey = normKey
                        )
                    )
                    inserted++
                }
            } catch (e: Exception) {
                errors.add("Error importing '${entry.french}': ${e.message}")
            }
        }

        return ImportResult(inserted, updated, errors)
    }
}
