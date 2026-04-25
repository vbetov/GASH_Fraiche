package com.gash.vocab.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate

/**
 * Persists user settings to SharedPreferences.
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("gash_settings", Context.MODE_PRIVATE)
    private val gson = Gson()

    // ── Study limits ──────────────────────────────────────────────

    var newPerDay: Int
        get() = prefs.getInt("new_per_day", 10)
        set(value) = prefs.edit().putInt("new_per_day", value).apply()

    var reviewsPerDay: Int
        get() = prefs.getInt("reviews_per_day", 50)
        set(value) = prefs.edit().putInt("reviews_per_day", value).apply()

    /** Date (ISO format) when uncap was activated. Empty/null means not active. */
    private var uncapDate: String?
        get() = prefs.getString("uncap_date", null)
        set(value) = prefs.edit().putString("uncap_date", value).apply()

    /** Returns true if new cards are uncapped for today. */
    val isUncappedToday: Boolean
        get() = uncapDate == LocalDate.now().toString()

    /** Effective new-per-day limit: uncapped (9999) if today is uncap day, else normal setting. */
    val effectiveNewPerDay: Int
        get() = if (isUncappedToday) 9999 else newPerDay

    fun uncapForToday() {
        uncapDate = LocalDate.now().toString()
    }

    fun removeUncap() {
        uncapDate = null
    }

    // ── Review settings ───────────────────────────────────────────

    /** Learning steps in minutes, comma-separated (e.g. "1,10") */
    var learningSteps: String
        get() = prefs.getString("learning_steps", "1,10") ?: "1,10"
        set(value) = prefs.edit().putString("learning_steps", value).apply()

    /** Graduating interval in days */
    var graduatingInterval: Int
        get() = prefs.getInt("graduating_interval", 1)
        set(value) = prefs.edit().putInt("graduating_interval", value).apply()

    /** Easy interval in days */
    var easyInterval: Int
        get() = prefs.getInt("easy_interval", 4)
        set(value) = prefs.edit().putInt("easy_interval", value).apply()

    // ── POS category mapping ──────────────────────────────────────

    /**
     * Map from POS value → display category name.
     * Unmapped POS values fall into "Other".
     */
    var posCategoryMap: Map<String, String>
        get() {
            val json = prefs.getString("pos_category_map", null) ?: return defaultPosCategoryMap()
            val type = object : TypeToken<Map<String, String>>() {}.type
            return gson.fromJson(json, type)
        }
        set(value) {
            prefs.edit().putString("pos_category_map", gson.toJson(value)).apply()
        }

    fun getCategoryForPos(pos: String): String {
        return posCategoryMap[pos] ?: "Other"
    }

    companion object {
        fun defaultPosCategoryMap(): Map<String, String> = mapOf(
            // Nouns
            "noun (f.)" to "Nouns",
            "noun (m.)" to "Nouns",
            "noun (m. pl.)" to "Nouns",
            "noun phrase (f.)" to "Nouns",
            "noun phrase (m.)" to "Nouns",
            // Verbs
            "verb" to "Verbs",
            "verb phrase" to "Verbs",
            // Adjectives
            "adjective" to "Adjectives",
            // Adverbs
            "adverb" to "Adverbs",
            "adverbial phrase" to "Adverbs",
            // Pronouns
            "pronoun" to "Pronouns",
            // Phrases
            "phrase" to "Phrases",
            // Other
            "preposition" to "Other",
            "prepositional phrase" to "Other",
            "conjunction" to "Other"
        )
    }
}
