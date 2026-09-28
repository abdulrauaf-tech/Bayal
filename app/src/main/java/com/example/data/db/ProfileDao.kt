package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.PlayerProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM player_profile WHERE id = 1 LIMIT 1")
    fun getProfile(): Flow<PlayerProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: PlayerProfile)

    @Query("UPDATE player_profile SET equippedSkinId = :skinId WHERE id = 1")
    suspend fun updateSkin(skinId: String)

    @Query("UPDATE player_profile SET equippedCamoId = :camoId WHERE id = 1")
    suspend fun updateCamo(camoId: String)

    @Query("UPDATE player_profile SET botDifficulty = :difficulty WHERE id = 1")
    suspend fun updateDifficulty(difficulty: String)

    @Query("UPDATE player_profile SET sensitivity = :sens, soundEnabled = :sound, hapticsEnabled = :haptics WHERE id = 1")
    suspend fun updateSettings(sens: Float, sound: Boolean, haptics: Boolean)
}
