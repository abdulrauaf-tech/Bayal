package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.MatchRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {
    @Query("SELECT * FROM match_records ORDER BY timestamp DESC")
    fun getAllMatches(): Flow<List<MatchRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchRecord): Long

    @Query("SELECT COUNT(*) FROM match_records")
    fun getMatchCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM match_records WHERE isVictory = 1")
    fun getVictoryCount(): Flow<Int>

    @Query("SELECT SUM(kills) FROM match_records")
    fun getTotalKills(): Flow<Int?>

    @Query("DELETE FROM match_records")
    suspend fun clearHistory()
}
