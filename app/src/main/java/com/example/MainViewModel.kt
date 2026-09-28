package com.example

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.BattleDatabase
import com.example.data.model.MatchRecord
import com.example.data.model.PlayerProfile
import com.example.data.repository.BattleRepository
import com.example.game.AudioSystem
import com.example.game.GameEngine
import com.example.ui.screens.AVAILABLE_SKINS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    LOBBY,
    BATTLE,
    LOCKER,
    HISTORY,
    SETTINGS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = BattleDatabase.getDatabase(application)
    val repository = BattleRepository(database.matchDao(), database.profileDao())
    val audioSystem = AudioSystem(application)

    val profile: StateFlow<PlayerProfile> = repository.playerProfile
        .map { it ?: PlayerProfile() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PlayerProfile()
        )

    val matches: StateFlow<List<MatchRecord>> = repository.allMatches
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentScreen = MutableStateFlow(AppScreen.LOBBY)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _activeGame = MutableStateFlow<GameEngine?>(null)
    val activeGame: StateFlow<GameEngine?> = _activeGame.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureProfileExists()
        }
    }

    fun startMatch() {
        val currentProf = profile.value
        audioSystem.isSoundEnabled = currentProf.soundEnabled
        audioSystem.isHapticsEnabled = currentProf.hapticsEnabled

        val game = GameEngine(
            audioSystem = audioSystem,
            botDifficulty = currentProf.botDifficulty
        )
        _activeGame.value = game
        _currentScreen.value = AppScreen.BATTLE
    }

    fun exitMatch() {
        _activeGame.value = null
        _currentScreen.value = AppScreen.LOBBY
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun selectSkin(skinId: String) {
        viewModelScope.launch {
            repository.updateSkin(skinId)
        }
    }

    fun updateDifficulty(difficulty: String) {
        viewModelScope.launch {
            repository.updateDifficulty(difficulty)
        }
    }

    fun updateSettings(sensitivity: Float, sound: Boolean, haptics: Boolean) {
        audioSystem.isSoundEnabled = sound
        audioSystem.isHapticsEnabled = haptics
        viewModelScope.launch {
            repository.updateSettings(sensitivity, sound, haptics)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun getEquippedSkinColor(): Color {
        val skinId = profile.value.equippedSkinId
        return AVAILABLE_SKINS.firstOrNull { it.id == skinId }?.color ?: Color(0xFF10B981)
    }
}
