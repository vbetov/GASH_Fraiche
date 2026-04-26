package com.gash.vocab.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {

    @Query("SELECT * FROM words ORDER BY id ASC")
    fun getAllWords(): Flow<List<WordEntity>>

    @Query("SELECT * FROM words ORDER BY id ASC")
    suspend fun getAllWordsList(): List<WordEntity>

    @Query("SELECT * FROM words WHERE id = :id")
    suspend fun getWordById(id: Int): WordEntity?

    @Query("SELECT * FROM words WHERE norm_key = :normKey LIMIT 1")
    suspend fun getWordByNormKey(normKey: String): WordEntity?

    @Query("SELECT MAX(id) FROM words")
    suspend fun getMaxId(): Int?

    @Query("SELECT COUNT(*) FROM words")
    suspend fun getWordCount(): Int

    /** All word IDs currently in the vocabulary table. */
    @Query("SELECT id FROM words")
    suspend fun getAllWordIds(): List<Int>

    @Query("SELECT DISTINCT pos FROM words ORDER BY pos ASC")
    fun getAllPosValues(): Flow<List<String>>

    @Query("SELECT DISTINCT source FROM words ORDER BY source ASC")
    fun getAllSources(): Flow<List<String>>

    @Query("SELECT DISTINCT weeks FROM words WHERE weeks != '' ORDER BY weeks ASC")
    fun getAllWeeks(): Flow<List<String>>

    @Query("SELECT id FROM words WHERE weeks = :weeks ORDER BY id ASC")
    suspend fun getWordIdsByWeek(weeks: String): List<Int>

    @Query("SELECT id FROM words WHERE pos = :pos ORDER BY id ASC")
    suspend fun getWordIdsByPos(pos: String): List<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<WordEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: WordEntity)

    @Update
    suspend fun updateWord(word: WordEntity)
}
