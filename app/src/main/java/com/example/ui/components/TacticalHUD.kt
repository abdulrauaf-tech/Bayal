package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.GameEngine
import com.example.game.WeaponSlot
import kotlin.math.ceil

@Composable
fun TacticalHUD(
    game: GameEngine,
    onFire: () -> Unit,
    onReload: () -> Unit,
    onJump: () -> Unit,
    onToggleCrouch: () -> Unit,
    onToggleAim: () -> Unit,
    onSwitchWeapon: (Int) -> Unit,
    onUseMedkit: () -> Unit,
    onUseShield: () -> Unit,
    onPauseExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val aliveCount = game.aliveCount
    val hp = game.playerHp
    val shield = game.playerShield
    val kills = game.playerKills
    val currentSlot = game.currentWeaponSlot
    val isAiming = game.isPlayerAiming
    val isCrouched = game.isPlayerCrouched

    Box(modifier = modifier.fillMaxSize()) {

        // Top Status Bar: Alive Players, HP, Shield, Kills, Minimap
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 36.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left Column: Player Stats & Storm Status
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Survivors & Kills Badge (Matches prompt: "Players: 10 | HP: 100")
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xCC0F172A),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "PLAYERS: $aliveCount",
                            color = if (aliveCount <= 3) Color(0xFFF59E0B) else Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(12.dp)
                                .background(Color(0x44FFFFFF))
                        )
                        Text(
                            text = "KILLS: $kills 💀",
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                // Health Bar (HP: 100)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Health",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                    Box(
                        modifier = Modifier
                            .width(130.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0x66000000))
                    ) {
                        val hpPercent = (hp / 100f).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(hpPercent)
                                .height(10.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF10B981), Color(0xFF22C55E))
                                    )
                                )
                        )
                    }
                    Text(
                        text = "${hp.toInt()}",
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // Shield Bar (Shield: 100)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Shield",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(16.dp)
                    )
                    Box(
                        modifier = Modifier
                            .width(130.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x66000000))
                    ) {
                        val shieldPercent = (shield / 100f).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(shieldPercent)
                                .height(8.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF0284C7), Color(0xFF38BDF8))
                                    )
                                )
                        )
                    }
                    Text(
                        text = "${shield.toInt()}",
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                // Storm Zone Timer Badge
                val stormTimer = ceil(game.stormZone.phaseTimer).toInt()
                val stormText = if (game.stormZone.isShrinking) {
                    "⚠️ STORM SHRINKING: ${stormTimer}s"
                } else {
                    "SAFE ZONE CLOSES: ${stormTimer}s"
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (game.stormZone.isShrinking) Color(0xAAEF4444) else Color(0xAA0284C7)
                ) {
                    Text(
                        text = stormText,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Right Column: Minimap Radar & Pause Button
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Minimap(
                    playerPos = game.playerPos,
                    playerYaw = game.playerYaw,
                    stormZone = game.stormZone,
                    bots = game.bots,
                    airdropPos = game.airdrop?.position
                )

                IconButton(
                    onClick = onPauseExit,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x88000000))
                        .testTag("pause_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Exit Match",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Kill Feed Messages (Upper Left under stats)
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 135.dp, start = 16.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            game.killFeed.take(3).forEach { kf ->
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (kf.isPlayerInvolved) Color(0xDDE11D48) else Color(0x990F172A)
                ) {
                    Text(
                        text = "${kf.killer} ⚔️ ${kf.victim} [${kf.weapon}]",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = if (kf.isPlayerInvolved) FontWeight.Black else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Center Crosshair & Hitmarker
        Box(
            modifier = Modifier.align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            val crosshairSpread = if (isAiming) 8.dp else 16.dp
            Canvas(modifier = Modifier.size(48.dp)) {
                val c = Offset(this.size.width / 2f, this.size.height / 2f)
                val spreadPx = crosshairSpread.toPx()
                val lineLen = 8.dp.toPx()
                val color = if (isAiming) Color(0xFFEF4444) else Color(0xCCFFFFFF)

                // Top, bottom, left, right ticks
                drawLine(color, Offset(c.x, c.y - spreadPx), Offset(c.x, c.y - spreadPx - lineLen), 2.dp.toPx())
                drawLine(color, Offset(c.x, c.y + spreadPx), Offset(c.x, c.y + spreadPx + lineLen), 2.dp.toPx())
                drawLine(color, Offset(c.x - spreadPx, c.y), Offset(c.x - spreadPx - lineLen, c.y), 2.dp.toPx())
                drawLine(color, Offset(c.x + spreadPx, c.y), Offset(c.x + spreadPx + lineLen, c.y), 2.dp.toPx())
                // Center dot
                drawCircle(color, 2.dp.toPx(), c)
            }

            // Hitmarker Flash
            val isHitVisible = System.currentTimeMillis() < game.showHitMarkerUntil
            AnimatedVisibility(
                visible = isHitVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Canvas(modifier = Modifier.size(36.dp)) {
                    val c = Offset(this.size.width / 2f, this.size.height / 2f)
                    val s = 12.dp.toPx()
                    drawLine(Color(0xFFEF4444), Offset(c.x - s, c.y - s), Offset(c.x + s, c.y + s), 3.dp.toPx())
                    drawLine(Color(0xFFEF4444), Offset(c.x + s, c.y - s), Offset(c.x - s, c.y + s), 3.dp.toPx())
                }
            }
        }

        // Bottom Bar: Weapon inventory, Fire button, Jump, Crouch, ADS, Heals
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 20.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Weapon Selection & Ammo bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Weapons Selector
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    game.weapons.forEachIndexed { index, slot ->
                        val isSelected = index == game.currentWeaponIndex
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) slot.type.color.copy(alpha = 0.35f) else Color(0x880F172A),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (isSelected) slot.type.color else Color(0x44FFFFFF)
                            ),
                            modifier = Modifier
                                .clickable { onSwitchWeapon(index) }
                                .testTag("weapon_slot_$index")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = slot.type.displayName.split(" ")[0],
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = "${slot.currentAmmo}/${slot.reserveAmmo}",
                                    color = slot.type.color,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Quick Meds / Shield button
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Medkit Button
                    Surface(
                        shape = CircleShape,
                        color = Color(0xCC10B981),
                        modifier = Modifier
                            .size(44.dp)
                            .clickable { onUseMedkit() }
                            .testTag("use_medkit_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "💊 ${game.playerMedkits}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Shield Battery Button
                    Surface(
                        shape = CircleShape,
                        color = Color(0xCC0284C7),
                        modifier = Modifier
                            .size(44.dp)
                            .clickable { onUseShield() }
                            .testTag("use_shield_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "🛡️ ${game.playerShieldBatteries}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Action Buttons Row (Reload, Crouch, Jump, ADS, and giant FIRE button!)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Left side: Space for Virtual Joystick + Sprint
                Spacer(modifier = Modifier.width(140.dp))

                // Right side: Combat Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tactical Mini Action Column (Reload, Crouch, Jump, ADS)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Reload Button
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xAA1E293B))
                                    .border(1.dp, Color(0xFF64748B), CircleShape)
                                    .clickable { onReload() }
                                    .testTag("reload_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reload",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // ADS / Scope Aim Button
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(if (isAiming) Color(0xDD0284C7) else Color(0xAA1E293B))
                                    .border(1.dp, if (isAiming) Color(0xFF38BDF8) else Color(0xFF64748B), CircleShape)
                                    .clickable { onToggleAim() }
                                    .testTag("aim_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🎯",
                                    fontSize = 18.sp
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Crouch Button
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(if (isCrouched) Color(0xDD3B82F6) else Color(0xAA1E293B))
                                    .border(1.dp, Color(0xFF64748B), CircleShape)
                                    .clickable { onToggleCrouch() }
                                    .testTag("crouch_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🛡️",
                                    fontSize = 16.sp
                                )
                            }

                            // Jump Button
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xAA1E293B))
                                    .border(1.dp, Color(0xFF64748B), CircleShape)
                                    .clickable { onJump() }
                                    .testTag("jump_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🦘",
                                    fontSize = 18.sp
                                )
                            }
                        }
                    }

                    // Giant Combat FIRE Button (Matches prompt: button id="fire" FIRE 🔫)
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFFEF4444), Color(0xFFB91C1C))
                                )
                            )
                            .border(3.dp, Color(0xFFFF8A80), CircleShape)
                            .clickable { onFire() }
                            .testTag("fire_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "FIRE",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "🔫",
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
