package com.gash.vocab.data.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Room TypeConverters for serialising List<String> and List<Int> fields to/from JSON.
 * Used by WordEntity (cloze, related, relatedEN, etymology) and ProgressEntity (qualityHistory).
 */
class Converters {

    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromIntList(value: List<Int>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toIntList(value: String): List<Int> {
        val type = object : TypeToken<List<Int>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }
}
