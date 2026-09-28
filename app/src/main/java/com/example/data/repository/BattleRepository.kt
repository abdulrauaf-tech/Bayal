package com.example.data.repository

import com.example.data.db.MatchDao
import com.example.data.db.ProfileDao
import com.example.data.model.MatchRecord
import com.example.data.model.PlayerProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class BattleRepository(
    private val matchDao: MatchDao,
    private val profileDao: ProfileDao
) {
    val allMatches: Flow<List<MatchRecord>> = matchDao.getAllMatches()
    val playerProfile: Flow<PlayerProfile?> = profileDao.getProfile()

    suspend fun recordMatch(record: MatchRecord) {
        matchDao.insertMatch(record)

        val currentProfile = profileDao.getProfile().firstOrNull() ?: PlayerProfile()
        val newMatches = currentProfile.totalMatches + 1
        val newWins = if (record.isVictory) currentProfile.totalWins + 1 else currentProfile.totalWins
        val newKills = currentProfile.totalKills + record.kills
        val gainedXp = record.kills * 100 + (if (record.isVictory) 500 else 100) + (record.survivalTimeSeconds * 2)
        val totalXp = currentProfile.xp + gainedXp
        val newLevel = 1 + (totalXp / 1000)

        profileDao.saveProfile(
            currentProfile.copy(
                totalMatches = newMatches,
                totalWins = newWins,
                totalKills = newKills,
                xp = totalXp,
                level = newLevel
            )
        )
    }

    suspend fun ensureProfileExists() {
        val existing = profileDao.getProfile().firstOrNull()
        if (existing == null) {
            profileDao.saveProfile(PlayerProfile())
        }
    }

    suspend fun updateSkin(skinId: String) = profileDao.updateSkin(skinId)
    suspend fun updateCamo(camoId: String) = profileDao.updateCamo(camoId)
    suspend fun updateDifficulty(difficulty: String) = profileDao.updateDifficulty(difficulty)
    suspend fun updateSettings(sens: Float, sound: Boolean, haptics: Boolean) =
        profileDao.updateSettings(sens, sound, haptics)
    suspend fun clearHistory() = matchDao.clearHistory()
}
