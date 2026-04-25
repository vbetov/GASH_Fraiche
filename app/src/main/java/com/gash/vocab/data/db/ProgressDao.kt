package com.gash.vocab.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {

    @Query("SELECT * FROM progress WHERE word_id = :wordId")
    suspend fun getProgress(wordId: Int): ProgressEntity?

    @Query("SELECT * FROM progress")
    fun getAllProgress(): Flow<List<ProgressEntity>>

    @Query("SELECT * FROM progress")
    suspend fun getAllProgressList(): List<ProgressEntity>

    /**
     * Words due for review: next_review <= now, ordered by earliest due first.
     */
    @Query("SELECT * FROM progress WHERE next_review <= :now AND next_review > 0 ORDER BY next_review ASC")
    suspend fun getDueCards(now: Long): List<ProgressEntity>

    /**
     * New words: words that have no progress entry yet.
     * Returns word IDs that are NOT in the progress table.
     */
    @Query("SELECT id FROM words WHERE id NOT IN (SELECT word_id FROM progress) ORDER BY id ASC LIMIT :limit")
    suspend fun getNewWordIds(limit: Int): List<Int>

    /**
     * Count of words studied today (first_encountered or last_encountered matches today's date prefix).
     */
    @Query("SELECT COUNT(*) FROM progress WHERE last_encountered LIKE :todayPrefix || '%'")
    suspend fun getStudiedTodayCount(todayPrefix: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: ProgressEntity)

    @Update
    suspend fun updateProgress(progress: ProgressEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProgressIfNotExists(progress: ProgressEntity)

    @Query("DELETE FROM progress WHERE word_id = :wordId")
    suspend fun deleteProgress(wordId: Int)
}
