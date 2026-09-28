package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.GameEngine

@Composable
fun VictoryDefeatDialog(
    game: GameEngine,
    onPlayAgain: () -> Unit,
    onReturnLobby: () -> Unit
) {
    val isVictory = game.isVictory
    val placement = if (isVictory) 1 else game.aliveCount + 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .testTag("match_result_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A),
            border = androidx.compose.foundation.BorderStroke(
                2.dp,
                if (isVictory) Color(0xFFF59E0B) else Color(0xFFEF4444)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Banner
                if (isVictory) {
                    Text(
                        text = "🏆 BOOYAH! 🏆",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFBBF24),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "#1 VICTORY ROYALE",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                } else {
                    Text(
                        text = "💀 GAME OVER 💀",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFEF4444),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "RANK #$placement OF 10 SURVIVORS",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                }

                // Match Statistics Grid
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatRow("Kills", "${game.playerKills} 💀", Color(0xFFEF4444))
                        StatRow("Damage Dealt", "${game.playerDamageDealt} 💥", Color(0xFFF59E0B))
                        val minutes = (game.matchTimeSec / 60).toInt()
                        val seconds = (game.matchTimeSec % 60).toInt()
                        StatRow("Survival Time", String.format("%02d:%02d ⏱️", minutes, seconds), Color(0xFF38BDF8))
                        val xpEarned = game.playerKills * 100 + (if (isVictory) 500 else 100) + (game.matchTimeSec.toInt() * 2)
                        StatRow("XP Gained", "+$xpEarned XP ⭐", Color(0xFF10B981))
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Actions
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onPlayAgain,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("play_again_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isVictory) Color(0xFFF59E0B) else Color(0xFFEF4444)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "PLAY AGAIN 🔄",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    OutlinedButton(
                        onClick = onReturnLobby,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("return_lobby_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text(
                            text = "RETURN TO LOBBY 🏠",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(0xFF94A3B8),
            fontSize = 13.sp
        )
        Text(
            text = value,
            color = valueColor,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}
