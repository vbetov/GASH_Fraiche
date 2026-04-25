package com.gash.vocab.data.importer

import android.content.Context
import com.gash.vocab.data.db.AppDatabase

/**
 * Loads seed vocabulary from assets/seed_vocab.json on first launch.
 * Uses SharedPreferences to track whether seeding has already run.
 */
object SeedLoader {

    private const val PREFS_NAME = "gash_prefs"
    private const val KEY_SEEDED = "db_seeded"

    suspend fun seedIfNeeded(context: Context, db: AppDatabase) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_SEEDED, false)) return

        // Prefer full vocab list; fall back to GitHub shortlist
        val jsonText = try {
            context.assets.open("seed_vocab.json").bufferedReader().readText()
        } catch (_: Exception) {
            try {
                context.assets.open("seed_vocab_github.json").bufferedReader().readText()
            } catch (_: Exception) {
                return // No seed file — skip silently
            }
        }

        val importer = VocabImporter(db.wordDao())
        val result = importer.importFromJson(jsonText)

        if (result.errors.isEmpty()) {
            prefs.edit().putBoolean(KEY_SEEDED, true).apply()
        }
    }
}
