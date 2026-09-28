package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.BattleScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.LobbyScreen
import com.example.ui.screens.LockerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F172A)
                ) {
                    BattleRoyaleApp()
                }
            }
        }
    }
}

@Composable
fun BattleRoyaleApp(
    viewModel: MainViewModel = viewModel()
) {
    val screen by viewModel.currentScreen.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val matches by viewModel.matches.collectAsState()
    val activeGame by viewModel.activeGame.collectAsState()

    when (screen) {
        AppScreen.LOBBY -> {
            LobbyScreen(
                profile = profile,
                onStartMatch = { viewModel.startMatch() },
                onOpenLocker = { viewModel.navigateTo(AppScreen.LOCKER) },
                onOpenHistory = { viewModel.navigateTo(AppScreen.HISTORY) },
                onOpenSettings = { viewModel.navigateTo(AppScreen.SETTINGS) }
            )
        }
        AppScreen.BATTLE -> {
            activeGame?.let { game ->
                BattleScreen(
                    game = game,
                    repository = viewModel.repository,
                    playerSkinColor = viewModel.getEquippedSkinColor(),
                    onExitMatch = { viewModel.exitMatch() }
                )
            } ?: run {
                viewModel.navigateTo(AppScreen.LOBBY)
            }
        }
        AppScreen.LOCKER -> {
            LockerScreen(
                currentSkinId = profile.equippedSkinId,
                onSelectSkin = { skinId -> viewModel.selectSkin(skinId) },
                onBack = { viewModel.navigateTo(AppScreen.LOBBY) }
            )
        }
        AppScreen.HISTORY -> {
            HistoryScreen(
                matches = matches,
                onClearHistory = { viewModel.clearHistory() },
                onBack = { viewModel.navigateTo(AppScreen.LOBBY) }
            )
        }
        AppScreen.SETTINGS -> {
            SettingsScreen(
                profile = profile,
                onUpdateDifficulty = { diff -> viewModel.updateDifficulty(diff) },
                onUpdateSettings = { sens, sound, haptics ->
                    viewModel.updateSettings(sens, sound, haptics)
                },
                onBack = { viewModel.navigateTo(AppScreen.LOBBY) }
            )
        }
    }
}
