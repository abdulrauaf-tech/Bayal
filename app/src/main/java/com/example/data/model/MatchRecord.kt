package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "match_records")
data class MatchRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val placement: Int, // e.g. 1 for BOOYAH
    val totalPlayers: Int = 10,
    val kills: Int,
    val damageDealt: Int,
    val survivalTimeSeconds: Int,
    val weaponUsed: String,
    val isVictory: Boolean
)
