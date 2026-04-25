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
     * Words due for review: next_review <= now, earliest-due first.
     * Cards sharing the same next_review timestamp are randomised relative to
     * each other so they have equal probability of selection when capped.
     */
    @Query("SELECT * FROM progress WHERE next_review <= :now AND next_review > 0 ORDER BY next_review ASC, RANDOM()")
    suspend fun getDueCards(now: Long): List<ProgressEntity>

    /**
     * Same as [getDueCards] but restricted to words whose `pos` matches.
     * Earliest-due first, RANDOM() tiebreaker within equal next_review values.
     */
    @Query(
        """
        SELECT p.* FROM progress p
        INNER JOIN words w ON w.id = p.word_id
        WHERE p.next_review <= :now AND p.next_review > 0 AND w.pos = :pos
        ORDER BY p.next_review ASC, RANDOM()
        """
    )
    suspend fun getDueCardsByPos(now: Long, pos: String): List<ProgressEntity>

    /**
     * New words: words that have no progress entry yet.
     * Returns word IDs that are NOT in the progress table, randomly selected
     * from the eligible pool so each new card has equal probability of
     * appearing in any given session.
     */
    @Query("SELECT id FROM words WHERE id NOT IN (SELECT word_id FROM progress) ORDER BY RANDOM() LIMIT :limit")
    suspend fun getNewWordIds(limit: Int): List<Int>

    /**
     * Same as [getNewWordIds] but restricted to a particular `pos`.
     */
    @Query(
        """
        SELECT id FROM words
        WHERE id NOT IN (SELECT word_id FROM progress) AND pos = :pos
        ORDER BY RANDOM() LIMIT :limit
        """
    )
    suspend fun getNewWordIdsByPos(pos: String, limit: Int): List<Int>

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
