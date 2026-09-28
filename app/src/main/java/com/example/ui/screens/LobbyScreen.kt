package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlayerProfile
import com.example.engine3d.Camera3D
import com.example.engine3d.MeshBuilder
import com.example.engine3d.Polygon3D
import com.example.engine3d.Renderer3D
import com.example.engine3d.Vector3
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LobbyScreen(
    profile: PlayerProfile,
    onStartMatch: () -> Unit,
    onOpenLocker: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "lobby_rotation")
    val heroYaw by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hero_yaw"
    )

    val currentSkin = AVAILABLE_SKINS.firstOrNull { it.id == profile.equippedSkinId } ?: AVAILABLE_SKINS[0]
    val renderer = remember { Renderer3D() }
    val camera = remember { Camera3D(position = Vector3(0f, 2.2f, 6.2f), yaw = 0f, pitch = 0.05f) }
    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F172A),
                        Color(0xFF020617)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("lobby_screen_root")
    ) {
        // Top Header: Profile Info, Level, XP
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(currentSkin.color)
                            .border(1.5.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎖️", fontSize = 16.sp)
                    }

                    Column {
                        Text(
                            text = profile.playerName,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "LEVEL ${profile.level}",
                            color = Color(0xFFFBBF24),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // XP Progress Bar
                val currentLevelXp = profile.xp % 1000
                val progress = currentLevelXp / 1000f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth(0.42f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF38BDF8),
                    trackColor = Color(0xFF1E293B)
                )
            }

            // Quick Action Buttons (History & Settings)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onOpenHistory,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                        .testTag("open_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Match History",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                        .testTag("open_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White
                    )
                }
            }
        }

        // Center 3D Interactive Hero Preview
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 70.dp, bottom = 220.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val polys = mutableListOf<Polygon3D>()

                // Rotating Pedestal Base
                val pedestalSegments = 16
                val pedestalRadius = 2.2f
                val step = (2 * Math.PI / pedestalSegments).toFloat()
                for (i in 0 until pedestalSegments) {
                    val a1 = i * step
                    val a2 = (i + 1) * step
                    val p1 = Vector3(cos(a1) * pedestalRadius, -0.2f, sin(a1) * pedestalRadius)
                    val p2 = Vector3(cos(a2) * pedestalRadius, -0.2f, sin(a2) * pedestalRadius)
                    val p3 = Vector3(cos(a2) * pedestalRadius, 0f, sin(a2) * pedestalRadius)
                    val p4 = Vector3(cos(a1) * pedestalRadius, 0f, sin(a1) * pedestalRadius)

                    polys.add(
                        Polygon3D(
                            vertices = listOf(p1, p2, p3, p4),
                            color = Color(0xFF1E293B)
                        )
                    )
                    // Top cap triangle
                    polys.add(
                        Polygon3D(
                            vertices = listOf(Vector3(0f, 0f, 0f), p4, p3),
                            color = Color(0xFF334155)
                        )
                    )
                }

                // 3D Player Character on Pedestal
                polys.addAll(
                    MeshBuilder.createCharacterModel(
                        position = Vector3(0f, 0f, 0f),
                        yawRad = heroYaw,
                        isMoving = false,
                        animTime = 0f,
                        primaryColor = currentSkin.color,
                        gunColor = Color(0xFF38BDF8)
                    )
                )

                renderer.renderScene(
                    drawScope = this,
                    camera = camera,
                    polygons = polys,
                    bulletTracers = emptyList(),
                    floatingTexts = emptyList(),
                    textMeasurer = textMeasurer
                )
            }
        }

        // Bottom Controls Section: Match Details, Locker button, and DEPLOY button
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Mode & Locker Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Battle Royale Mode Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🎯", fontSize = 16.sp)
                        Column {
                            Text(
                                text = "BATTLE ROYALE 3D",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "10 Survivors • Solo Arena",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Locker & Skins Button
                Button(
                    onClick = onOpenLocker,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("open_locker_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Checkroom,
                        contentDescription = "Locker",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = "LOCKER",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            // Giant "DEPLOY BATTLE ROYALE" Button
            Button(
                onClick = onStartMatch,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .testTag("deploy_match_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF4444)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "START BATTLE 🔫",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
