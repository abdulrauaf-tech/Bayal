package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_profile")
data class PlayerProfile(
    @PrimaryKey
    val id: Int = 1,
    val playerName: String = "Survivor-101",
    val level: Int = 1,
    val xp: Int = 0,
    val totalMatches: Int = 0,
    val totalWins: Int = 0,
    val totalKills: Int = 0,
    val equippedSkinId: String = "skin_commando",
    val equippedCamoId: String = "camo_tactical",
    val botDifficulty: String = "Normal", // Easy, Normal, Hard, Chaos
    val sensitivity: Float = 1.0f,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true
)
