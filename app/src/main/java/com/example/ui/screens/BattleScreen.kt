package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.example.data.model.MatchRecord
import com.example.data.repository.BattleRepository
import com.example.engine3d.Camera3D
import com.example.engine3d.MeshBuilder
import com.example.engine3d.Polygon3D
import com.example.engine3d.Renderer3D
import com.example.engine3d.Vector3
import com.example.game.GameEngine
import com.example.ui.components.TacticalHUD
import com.example.ui.components.VirtualJoystick
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BattleScreen(
    game: GameEngine,
    repository: BattleRepository,
    playerSkinColor: Color,
    onExitMatch: () -> Unit
) {
    BackHandler {
        onExitMatch()
    }

    val coroutineScope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer()
    val renderer = remember { Renderer3D() }
    val camera = remember { Camera3D() }

    var hasRecordedResult by remember { mutableStateOf(false) }

    // Movement input values from joystick
    var joyX by remember { mutableFloatStateOf(0f) }
    var joyY by remember { mutableFloatStateOf(0f) }
    var joySprint by remember { mutableStateOf(false) }

    // 60 FPS Game Loop
    LaunchedEffect(Unit) {
        var lastNano = System.nanoTime()
        while (true) {
            withFrameNanos { nowNano ->
                val deltaSec = ((nowNano - lastNano) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastNano = nowNano

                // Update Player Movement from Joystick
                if (joyX != 0f || joyY != 0f) {
                    // joyY < 0 is forward on screen, joyX > 0 is right
                    game.movePlayer(moveForward = -joyY, moveRight = joyX, isSprinting = joySprint)
                } else {
                    game.isPlayerMoving = false
                }

                // Update Game World
                game.update(deltaSec)

                // Record match result once when finished
                if ((!game.isPlayerAlive || game.isVictory) && !hasRecordedResult) {
                    hasRecordedResult = true
                    coroutineScope.launch {
                        val record = MatchRecord(
                            placement = if (game.isVictory) 1 else game.aliveCount + 1,
                            totalPlayers = 10,
                            kills = game.playerKills,
                            damageDealt = game.playerDamageDealt,
                            survivalTimeSeconds = game.matchTimeSec.toInt(),
                            weaponUsed = game.currentWeaponSlot.type.displayName,
                            isVictory = game.isVictory
                        )
                        repository.recordMatch(record)
                    }
                }
            }
        }
    }

    // Update 3D Camera to follow Player in 3rd-Person
    val camDistance = if (game.isPlayerAiming) 3.5f else 7.5f
    val camHeight = if (game.isPlayerCrouched) 2.2f else 3.2f

    // Camera offset behind player:
    val backX = -sin(game.playerYaw) * camDistance * cos(game.playerPitch)
    val backZ = cos(game.playerYaw) * camDistance * cos(game.playerPitch)
    val backY = sin(game.playerPitch) * camDistance

    camera.position = game.playerPos + Vector3(backX, camHeight + backY, backZ)
    camera.yaw = game.playerYaw
    camera.pitch = game.playerPitch
    camera.fovRad = if (game.isPlayerAiming) Math.toRadians(45.0).toFloat() else Math.toRadians(65.0).toFloat()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .testTag("battle_screen_root")
    ) {
        // Fullscreen 3D World Canvas & Camera Look Drag Handler
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Sensitivity: Dragging right turns camera right
                        val sens = 0.0055f
                        game.lookPlayer(
                            deltaYaw = dragAmount.x * sens,
                            deltaPitch = -dragAmount.y * sens
                        )
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val screenW = size.width
                val screenH = size.height

                // Draw Sky Gradient
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E3A8A), // Deep tactical sky
                            Color(0xFF38BDF8), // Horizon blue
                            Color(0xFF67E8F9)
                        ),
                        startY = 0f,
                        endY = screenH * 0.55f
                    ),
                    size = size
                )

                // Build 3D Scene Primitives
                val scenePolygons = mutableListOf<Polygon3D>()

                // 1. Ground tiles
                scenePolygons.addAll(game.groundPolys)

                // 2. Obstacles (Trees & Bunkers)
                for (obs in game.obstacles) {
                    if (obs.isTree) {
                        scenePolygons.addAll(MeshBuilder.createTree(obs.center))
                    } else {
                        scenePolygons.addAll(
                            MeshBuilder.createBox(
                                center = obs.center,
                                sizeX = obs.sizeX,
                                sizeY = obs.sizeY,
                                sizeZ = obs.sizeZ,
                                baseColor = obs.color,
                                topColor = Color(0xFF64748B)
                            )
                        )
                    }
                }

                // 3. Loot items on ground
                for (loot in game.lootItems) {
                    if (!loot.isCollected) {
                        loot.rotationAngle += 0.04f
                        scenePolygons.addAll(
                            MeshBuilder.createBox(
                                center = loot.position + Vector3(0f, 0.4f, 0f),
                                sizeX = 0.9f,
                                sizeY = 0.8f,
                                sizeZ = 0.9f,
                                baseColor = loot.type.color,
                                yawRad = loot.rotationAngle
                            )
                        )
                    }
                }

                // 4. Airdrop crate
                game.airdrop?.let { drop ->
                    if (!drop.isLooted) {
                        scenePolygons.addAll(MeshBuilder.createAirdropCrate(drop.position, drop.altitude))
                    }
                }

                // 5. Living Bots
                for (bot in game.bots) {
                    if (bot.isAlive) {
                        scenePolygons.addAll(
                            MeshBuilder.createCharacterModel(
                                position = bot.position,
                                yawRad = bot.yaw,
                                isMoving = bot.isMoving,
                                animTime = bot.animTime,
                                primaryColor = bot.primaryColor,
                                gunColor = bot.weapon.color
                            )
                        )
                    }
                }

                // 6. Player Character Model (Rendered unless ADS zoom hides body)
                if (game.isPlayerAlive && !game.isPlayerAiming) {
                    scenePolygons.addAll(
                        MeshBuilder.createCharacterModel(
                            position = game.playerPos,
                            yawRad = game.playerYaw,
                            isMoving = game.isPlayerMoving,
                            animTime = game.animTick,
                            primaryColor = playerSkinColor,
                            isCrouched = game.isPlayerCrouched,
                            isAiming = game.isPlayerAiming,
                            gunColor = game.currentWeaponSlot.type.color
                        )
                    )
                }

                // 7. 3D Storm Ring
                scenePolygons.addAll(
                    MeshBuilder.createStormRing(
                        center = game.stormZone.center,
                        radius = game.stormZone.currentRadius,
                        wallHeight = 24f,
                        timeSec = game.matchTimeSec
                    )
                )

                // Render all 3D geometry with depth sorting
                renderer.renderScene(
                    drawScope = this,
                    camera = camera,
                    polygons = scenePolygons,
                    bulletTracers = game.bulletTracers,
                    floatingTexts = game.floatingTexts,
                    textMeasurer = textMeasurer
                )

                // Danger Storm vignette if player caught outside safe zone!
                if (!game.stormZone.isInside(game.playerPos)) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.Transparent, Color(0x6600E5FF), Color(0xAA0284C7)),
                            center = Offset(screenW / 2f, screenH / 2f),
                            radius = screenW * 0.7f
                        )
                    )
                }
            }
        }

        // Virtual Joystick on Bottom-Left
        VirtualJoystick(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, bottom = 28.dp),
            size = 135.dp,
            onMove = { dx, dy, sprint ->
                joyX = dx
                joyY = dy
                joySprint = sprint
            }
        )

        // Tactical HUD Overlay (Status, Weapons, Health, Minimap, Action buttons)
        TacticalHUD(
            game = game,
            onFire = { game.firePlayerWeapon() },
            onReload = { game.reloadWeapon() },
            onJump = { game.jumpPlayer() },
            onToggleCrouch = { game.isPlayerCrouched = !game.isPlayerCrouched },
            onToggleAim = { game.isPlayerAiming = !game.isPlayerAiming },
            onSwitchWeapon = { idx -> game.switchWeapon(idx) },
            onUseMedkit = { game.useMedkit() },
            onUseShield = { game.useShieldBattery() },
            onPauseExit = onExitMatch
        )

        // Game Over / Victory Modal
        if (!game.isPlayerAlive || game.isVictory) {
            VictoryDefeatDialog(
                game = game,
                onPlayAgain = {
                    // Reset match with fresh state
                    game.playerPos = Vector3(0f, 0f, 20f)
                    game.playerHp = 100f
                    game.playerShield = 100f
                    game.playerKills = 0
                    game.playerDamageDealt = 0
                    game.isPlayerAlive = true
                    game.isVictory = false
                    game.matchTimeSec = 0f
                    game.stormZone.currentRadius = 100f
                    game.stormZone.currentPhaseIndex = 0
                    game.stormZone.isShrinking = false
                    game.stormZone.phaseTimer = game.stormZone.phases[0].waitDurationSec
                    game.bots.clear()
                    game.bullets.clear()
                    game.floatingTexts.clear()
                    game.killFeed.clear()
                    game.weapons.forEach {
                        it.currentAmmo = it.type.magSize
                        it.reserveAmmo = it.type.magSize * 4
                        it.isReloading = false
                    }
                    // Re-spawn bots
                    val botNames = listOf(
                        "Apex_Viper", "Ghost_Reaper", "Nova_Blade", "Cyber_Sam",
                        "Titan_07", "HawkEye_99", "Blaze_Storm", "Shadow_Wolf", "Drift_King"
                    )
                    val botColors = listOf(
                        Color(0xFFEF4444), Color(0xFF3B82F6), Color(0xFFA855F7), Color(0xFFF59E0B),
                        Color(0xFFEC4899), Color(0xFF14B8A6), Color(0xFFE11D48), Color(0xFF6366F1), Color(0xFF0284C7)
                    )
                    for (i in botNames.indices) {
                        val angle = (i.toFloat() / botNames.size) * 2 * Math.PI.toFloat()
                        val dist = 35f + (Math.random().toFloat() * 30f)
                        game.bots.add(
                            com.example.game.BotEntity(
                                id = i,
                                name = botNames[i],
                                position = Vector3(cos(angle) * dist, 0f, sin(angle) * dist),
                                primaryColor = botColors[i % botColors.size]
                            )
                        )
                    }
                    hasRecordedResult = false
                },
                onReturnLobby = onExitMatch
            )
        }
    }
}
