package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.engine3d.BulletTracer
import com.example.engine3d.FloatingText
import com.example.engine3d.Polygon3D
import com.example.engine3d.Vector3
import java.util.Random
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class Bullet(
    var position: Vector3,
    val velocity: Vector3,
    val shooterId: Int, // -1 for Player, >= 0 for Bots
    val damage: Int,
    var lifetimeSec: Float = 2.0f,
    val color: Color = Color(0xFFFBBF24)
)

data class KillFeedMessage(
    val id: Long = System.currentTimeMillis() + (Math.random() * 1000).toLong(),
    val killer: String,
    val victim: String,
    val weapon: String,
    val isPlayerInvolved: Boolean
)

data class WorldObstacle(
    val center: Vector3,
    val sizeX: Float,
    val sizeY: Float,
    val sizeZ: Float,
    val color: Color,
    val isTree: Boolean = false
)

class GameEngine(
    val audioSystem: AudioSystem,
    val botDifficulty: String = "Normal"
) {
    private val random = Random()

    // Player State
    var playerPos = Vector3(0f, 0f, 20f)
    var playerVelocityY = 0f
    var playerYaw = 0f // In radians
    var playerPitch = 0.15f
    var isPlayerGrounded = true
    var isPlayerCrouched = false
    var isPlayerAiming = false
    var playerHp = 100f
    var playerShield = 100f
    var playerMedkits = 2
    var playerShieldBatteries = 2
    var playerKills = 0
    var playerDamageDealt = 0
    var isPlayerAlive = true
    var isVictory = false
    var matchTimeSec = 0f

    // Weapon Inventory
    val weapons = mutableListOf(
        WeaponSlot(WeaponType.ASSAULT_RIFLE),
        WeaponSlot(WeaponType.SHOTGUN),
        WeaponSlot(WeaponType.SNIPER_RIFLE)
    )
    var currentWeaponIndex = 0
    val currentWeaponSlot: WeaponSlot
        get() = weapons[currentWeaponIndex]

    var lastFireTimeMs = 0L

    // Storm Zone
    val stormZone = StormZone(center = Vector3(0f, 0f, 0f), currentRadius = 100f)

    // Bots (9 bots + 1 player = 10 total players, just like prompt!)
    val bots = mutableListOf<BotEntity>()

    // World Elements
    val obstacles = mutableListOf<WorldObstacle>()
    val groundPolys = mutableListOf<Polygon3D>()
    val lootItems = mutableListOf<LootItem>()
    var airdrop: Airdrop? = null

    // Projectiles & FX
    val bullets = mutableListOf<Bullet>()
    val bulletTracers = mutableListOf<BulletTracer>()
    val floatingTexts = mutableListOf<FloatingText>()
    val killFeed = mutableListOf<KillFeedMessage>()

    var animTick = 0f
    var isPlayerMoving = false

    // Hit marker flash
    var showHitMarkerUntil = 0L

    init {
        setupWorld()
        spawnBots()
        spawnLoot()
    }

    private fun setupWorld() {
        // Ground tiles (grid of 10x10 quads spanning 240x240 meters)
        val tileSize = 24f
        for (gx in -5 until 5) {
            for (gz in -5 until 5) {
                val x1 = gx * tileSize
                val z1 = gz * tileSize
                val x2 = x1 + tileSize
                val z2 = z1 + tileSize

                // Checkerboard subtle color variation for terrain depth
                val isAlt = (gx + gz) % 2 == 0
                val grassColor = if (isAlt) Color(0xFF22C55E) else Color(0xFF16A34A) // Vibrant arena grass

                groundPolys.add(
                    Polygon3D(
                        vertices = listOf(
                            Vector3(x1, 0f, z1),
                            Vector3(x2, 0f, z1),
                            Vector3(x2, 0f, z2),
                            Vector3(x1, 0f, z2)
                        ),
                        color = grassColor
                    )
                )
            }
        }

        // Add 3D Trees scattered around
        for (i in 0 until 24) {
            val tx = (random.nextFloat() * 160f - 80f)
            val tz = (random.nextFloat() * 160f - 80f)
            if (Vector3(tx, 0f, tz).length() > 8f) { // not right on top of player spawn
                obstacles.add(
                    WorldObstacle(
                        center = Vector3(tx, 0f, tz),
                        sizeX = 2.5f,
                        sizeY = 7f,
                        sizeZ = 2.5f,
                        color = Color(0xFF1E824C),
                        isTree = true
                    )
                )
            }
        }

        // Add 3D Military Bunkers & Crates for cover
        val bunkerPositions = listOf(
            Vector3(15f, 0f, 10f),
            Vector3(-20f, 0f, -15f),
            Vector3(25f, 0f, -30f),
            Vector3(-30f, 0f, 25f),
            Vector3(0f, 0f, -40f),
            Vector3(-15f, 0f, -50f)
        )
        for (bp in bunkerPositions) {
            obstacles.add(
                WorldObstacle(
                    center = bp + Vector3(0f, 1.5f, 0f),
                    sizeX = 5f,
                    sizeY = 3f,
                    sizeZ = 5f,
                    color = Color(0xFF475569) // Slate bunker
                )
            )
        }

        // Add scattered ammo/sandbag barricades
        for (i in 0 until 10) {
            val bx = (random.nextFloat() * 120f - 60f)
            val bz = (random.nextFloat() * 120f - 60f)
            obstacles.add(
                WorldObstacle(
                    center = Vector3(bx, 0.75f, bz),
                    sizeX = 3f,
                    sizeY = 1.5f,
                    sizeZ = 1f,
                    color = Color(0xFFB45309) // Sandbag brown
                )
            )
        }
    }

    private fun spawnBots() {
        val botNames = listOf(
            "Apex_Viper", "Ghost_Reaper", "Nova_Blade", "Cyber_Sam",
            "Titan_07", "HawkEye_99", "Blaze_Storm", "Shadow_Wolf", "Drift_King"
        )
        val botColors = listOf(
            Color(0xFFEF4444), Color(0xFF3B82F6), Color(0xFFA855F7), Color(0xFFF59E0B),
            Color(0xFFEC4899), Color(0xFF14B8A6), Color(0xFFE11D48), Color(0xFF6366F1), Color(0xFF0284C7)
        )

        for (i in botNames.indices) {
            val angle = (i.toFloat() / botNames.size) * 2 * PI.toFloat()
            val dist = 35f + (random.nextFloat() * 30f)
            val pos = Vector3(
                x = cos(angle) * dist,
                y = 0f,
                z = sin(angle) * dist
            )
            val botWeapon = when (i % 4) {
                0 -> WeaponType.ASSAULT_RIFLE
                1 -> WeaponType.SHOTGUN
                2 -> WeaponType.SNIPER_RIFLE
                else -> WeaponType.ASSAULT_RIFLE
            }

            bots.add(
                BotEntity(
                    id = i,
                    name = botNames[i],
                    position = pos,
                    weapon = botWeapon,
                    primaryColor = botColors[i % botColors.size],
                    health = 100f,
                    shield = 50f
                )
            )
        }
    }

    private fun spawnLoot() {
        var idCounter = 1L
        // Scatter Medkits, Shields, and weapons
        for (i in 0 until 14) {
            val lx = (random.nextFloat() * 120f - 60f)
            val lz = (random.nextFloat() * 120f - 60f)
            val type = when (i % 6) {
                0, 1 -> LootType.MEDKIT
                2, 3 -> LootType.SHIELD_BATTERY
                4 -> LootType.AMMO_BOX
                else -> LootType.WEAPON_SNIPER
            }
            lootItems.add(LootItem(id = idCounter++, type = type, position = Vector3(lx, 0.4f, lz)))
        }

        // Spawn Initial Airdrop descending
        airdrop = Airdrop(
            position = Vector3(random.nextFloat() * 40f - 20f, 0f, random.nextFloat() * 40f - 20f),
            altitude = 35f
        )
    }

    val aliveCount: Int
        get() = (if (isPlayerAlive) 1 else 0) + bots.count { it.isAlive }

    fun movePlayer(moveForward: Float, moveRight: Float, isSprinting: Boolean) {
        if (!isPlayerAlive) return

        val speed = when {
            isSprinting -> 0.32f
            isPlayerCrouched -> 0.12f
            isPlayerAiming -> 0.14f
            else -> 0.22f
        }

        val forwardDir = Vector3(sin(playerYaw), 0f, -cos(playerYaw)).normalized()
        val rightDir = Vector3(cos(playerYaw), 0f, sin(playerYaw)).normalized()

        val moveDelta = (forwardDir * moveForward + rightDir * moveRight) * speed

        isPlayerMoving = moveForward != 0f || moveRight != 0f

        val newPos = playerPos + moveDelta

        // World Bounds Check
        val clampedX = newPos.x.coerceIn(-115f, 115f)
        val clampedZ = newPos.z.coerceIn(-115f, 115f)

        // Simple Obstacle Collision Check
        var canMove = true
        for (obs in obstacles) {
            val dist = Vector3(clampedX, 0f, clampedZ).distance2D(obs.center)
            val minDist = (obs.sizeX + obs.sizeZ) * 0.35f
            if (dist < minDist) {
                canMove = false
                break
            }
        }

        if (canMove) {
            playerPos = Vector3(clampedX, playerPos.y, clampedZ)
        }

        // Check picking up loot
        for (loot in lootItems) {
            if (!loot.isCollected && playerPos.distance2D(loot.position) < 2.5f) {
                collectLoot(loot)
            }
        }
    }

    private fun collectLoot(loot: LootItem) {
        loot.isCollected = true
        audioSystem.playHeal()
        when (loot.type) {
            LootType.MEDKIT -> {
                playerMedkits++
                floatingTexts.add(FloatingText(playerPos + Vector3(0f, 3f, 0f), "+1 Medkit", Color(0xFF10B981), 1f))
            }
            LootType.SHIELD_BATTERY -> {
                playerShieldBatteries++
                floatingTexts.add(FloatingText(playerPos + Vector3(0f, 3f, 0f), "+1 Shield Battery", Color(0xFF00E5FF), 1f))
            }
            LootType.AMMO_BOX -> {
                weapons.forEach { it.reserveAmmo += it.type.magSize * 2 }
                floatingTexts.add(FloatingText(playerPos + Vector3(0f, 3f, 0f), "+Ammo Restocked", Color(0xFFFBBF24), 1f))
            }
            LootType.WEAPON_SNIPER -> {
                weapons[2].reserveAmmo += 15
                floatingTexts.add(FloatingText(playerPos + Vector3(0f, 3f, 0f), "Sniper Ammo +15", Color(0xFFF59E0B), 1f))
            }
            LootType.WEAPON_SHOTGUN -> {
                weapons[1].reserveAmmo += 20
                floatingTexts.add(FloatingText(playerPos + Vector3(0f, 3f, 0f), "Shotgun Shells +20", Color(0xFFEF4444), 1f))
            }
            LootType.WEAPON_ROCKET -> {
                floatingTexts.add(FloatingText(playerPos + Vector3(0f, 3f, 0f), "Heavy Ammo", Color(0xFFA855F7), 1f))
            }
        }
    }

    fun lookPlayer(deltaYaw: Float, deltaPitch: Float) {
        playerYaw += deltaYaw
        playerPitch = (playerPitch + deltaPitch).coerceIn(-0.6f, 0.7f)
    }

    fun jumpPlayer() {
        if (isPlayerGrounded && isPlayerAlive) {
            playerVelocityY = 0.28f
            isPlayerGrounded = false
        }
    }

    fun firePlayerWeapon() {
        if (!isPlayerAlive) return
        val slot = currentWeaponSlot
        val now = System.currentTimeMillis()

        if (slot.isReloading) return

        if (slot.currentAmmo <= 0) {
            reloadWeapon()
            return
        }

        if (now - lastFireTimeMs < slot.type.fireRateMs) return

        lastFireTimeMs = now
        slot.currentAmmo--

        // Play Gunshot audio & tactile
        audioSystem.playGunshot(slot.type)

        // Spawn bullet(s)
        val bulletOrigin = playerPos + Vector3(0f, if (isPlayerCrouched) 1.8f else 2.4f, 0f)
        val forwardDir = Vector3(
            x = sin(playerYaw) * cos(playerPitch),
            y = -sin(playerPitch),
            z = -cos(playerYaw) * cos(playerPitch)
        ).normalized()

        for (p in 0 until slot.type.pellets) {
            val spreadX = (random.nextFloat() * 2f - 1f) * slot.type.spreadAngleRad * (if (isPlayerAiming) 0.3f else 1f)
            val spreadY = (random.nextFloat() * 2f - 1f) * slot.type.spreadAngleRad * (if (isPlayerAiming) 0.3f else 1f)

            val bulletDir = (forwardDir + Vector3(spreadX, spreadY, 0f)).normalized()
            bullets.add(
                Bullet(
                    position = bulletOrigin,
                    velocity = bulletDir * slot.type.bulletSpeed,
                    shooterId = -1, // Player
                    damage = slot.type.damage,
                    color = slot.type.color
                )
            )
        }
    }

    fun reloadWeapon() {
        val slot = currentWeaponSlot
        if (slot.isReloading || slot.currentAmmo == slot.type.magSize || slot.reserveAmmo <= 0) return
        slot.isReloading = true
        slot.reloadProgress = 0f
    }

    fun switchWeapon(index: Int) {
        if (index in 0 until weapons.size) {
            currentWeaponIndex = index
        }
    }

    fun useMedkit() {
        if (!isPlayerAlive || playerMedkits <= 0 || playerHp >= 100f) return
        playerMedkits--
        playerHp = (playerHp + 50f).coerceAtMost(100f)
        audioSystem.playHeal()
        floatingTexts.add(FloatingText(playerPos + Vector3(0f, 3.2f, 0f), "+50 HP", Color(0xFF10B981), 1f))
    }

    fun useShieldBattery() {
        if (!isPlayerAlive || playerShieldBatteries <= 0 || playerShield >= 100f) return
        playerShieldBatteries--
        playerShield = (playerShield + 50f).coerceAtMost(100f)
        audioSystem.playHeal()
        floatingTexts.add(FloatingText(playerPos + Vector3(0f, 3.2f, 0f), "+50 Shield", Color(0xFF00E5FF), 1f))
    }

    fun update(deltaSec: Float) {
        if (!isPlayerAlive && !isVictory) return

        matchTimeSec += deltaSec
        animTick += deltaSec

        // Update Jump Physics
        if (!isPlayerGrounded) {
            playerPos = playerPos + Vector3(0f, playerVelocityY, 0f)
            playerVelocityY -= 0.015f // gravity
            if (playerPos.y <= 0f) {
                playerPos = Vector3(playerPos.x, 0f, playerPos.z)
                playerVelocityY = 0f
                isPlayerGrounded = true
            }
        }

        // Update Reload
        val slot = currentWeaponSlot
        if (slot.isReloading) {
            slot.reloadProgress += deltaSec / (slot.type.reloadTimeMs / 1000f)
            if (slot.reloadProgress >= 1f) {
                slot.isReloading = false
                val needed = slot.type.magSize - slot.currentAmmo
                val reloadAmount = needed.coerceAtMost(slot.reserveAmmo)
                slot.currentAmmo += reloadAmount
                slot.reserveAmmo -= reloadAmount
            }
        }

        // Update Storm Zone
        stormZone.update(deltaSec)

        // Storm damage to player if outside
        if (isPlayerAlive && !stormZone.isInside(playerPos)) {
            val dmg = stormZone.currentPhase.damagePerSecond * deltaSec
            damagePlayer(dmg, "Storm Zone")
        }

        // Update Airdrop
        airdrop?.let { drop ->
            if (!drop.isLanded) {
                drop.altitude -= 4f * deltaSec
                if (drop.altitude <= 0f) {
                    drop.altitude = 0f
                    drop.isLanded = true
                }
            } else if (!drop.isLooted && playerPos.distance2D(drop.position) < 3f) {
                drop.isLooted = true
                playerShield = 100f
                playerMedkits += 2
                weapons.forEach { it.reserveAmmo += it.type.magSize * 2 }
                audioSystem.playHeal()
                floatingTexts.add(FloatingText(playerPos + Vector3(0f, 3.5f, 0f), "AIRDROP SECURED! 🔥", Color(0xFFF59E0B), 1f, 1.3f))
            }
        }

        // Update Bullets
        bulletTracers.clear()
        val bulletIter = bullets.iterator()
        while (bulletIter.hasNext()) {
            val b = bulletIter.next()
            val oldPos = b.position
            b.position = b.position + b.velocity
            b.lifetimeSec -= deltaSec

            bulletTracers.add(BulletTracer(start = oldPos, end = b.position, color = b.color))

            // Collision with Ground
            if (b.position.y <= 0f) {
                bulletIter.remove()
                continue
            }

            // Collision with Player (if shot by bot)
            if (b.shooterId >= 0 && isPlayerAlive) {
                if (b.position.distanceTo(playerPos + Vector3(0f, 1.8f, 0f)) < 1.4f) {
                    val botShooter = bots.firstOrNull { it.id == b.shooterId }
                    damagePlayer(b.damage.toFloat(), botShooter?.name ?: "Enemy")
                    bulletIter.remove()
                    continue
                }
            }

            // Collision with Bots (if shot by player or other bots)
            var hitBot = false
            for (bot in bots) {
                if (bot.isAlive && bot.id != b.shooterId) {
                    if (b.position.distanceTo(bot.position + Vector3(0f, 1.8f, 0f)) < 1.4f) {
                        val fatal = bot.takeDamage(b.damage.toFloat())
                        hitBot = true

                        if (b.shooterId == -1) {
                            // Player hit this bot
                            playerDamageDealt += b.damage
                            showHitMarkerUntil = System.currentTimeMillis() + 180L
                            audioSystem.playHitMarker()
                            floatingTexts.add(
                                FloatingText(
                                    position = bot.position + Vector3(0f, 3.8f, 0f),
                                    text = "-${b.damage}",
                                    color = if (bot.shield > 0f) Color(0xFF00E5FF) else Color(0xFFEF4444),
                                    alpha = 1f
                                )
                            )

                            if (fatal) {
                                playerKills++
                                killFeed.add(0, KillFeedMessage(killer = "YOU", victim = bot.name, weapon = currentWeaponSlot.type.displayName, isPlayerInvolved = true))
                                floatingTexts.add(FloatingText(bot.position + Vector3(0f, 4.5f, 0f), "ELIMINATED +100XP", Color(0xFFFBBF24), 1f, 1.2f))
                                // Drop loot
                                lootItems.add(LootItem(System.currentTimeMillis(), LootType.MEDKIT, bot.position))
                            }
                        } else {
                            // Bot killed bot
                            if (fatal) {
                                val killerBot = bots.firstOrNull { it.id == b.shooterId }
                                killerBot?.kills = (killerBot?.kills ?: 0) + 1
                                killFeed.add(0, KillFeedMessage(killer = killerBot?.name ?: "Bot", victim = bot.name, weapon = killerBot?.weapon?.displayName ?: "Rifle", isPlayerInvolved = false))
                            }
                        }
                        break
                    }
                }
            }

            if (hitBot) {
                bulletIter.remove()
                continue
            }

            if (b.lifetimeSec <= 0f) {
                bulletIter.remove()
            }
        }

        // Update Bots AI
        updateBots(deltaSec)

        // Update Floating Texts
        val textIter = floatingTexts.iterator()
        while (textIter.hasNext()) {
            val ft = textIter.next()
            // Drift upward
            val updated = ft.copy(
                position = ft.position + Vector3(0f, 0.8f * deltaSec, 0f),
                alpha = (ft.alpha - deltaSec * 0.9f).coerceAtLeast(0f)
            )
            if (updated.alpha <= 0f) {
                textIter.remove()
            }
        }

        // Check Victory
        if (isPlayerAlive && bots.none { it.isAlive } && !isVictory) {
            isVictory = true
            audioSystem.playVictoryFanfare()
        }
    }

    private fun damagePlayer(amount: Float, attacker: String) {
        var remaining = amount
        if (playerShield > 0f) {
            val shieldDmg = remaining.coerceAtMost(playerShield)
            playerShield -= shieldDmg
            remaining -= shieldDmg
        }
        if (remaining > 0f) {
            playerHp -= remaining
        }

        if (playerHp <= 0f) {
            playerHp = 0f
            isPlayerAlive = false
            killFeed.add(0, KillFeedMessage(killer = attacker, victim = "YOU", weapon = "Combat", isPlayerInvolved = true))
            audioSystem.playDefeatSound()
        }
    }

    private fun updateBots(deltaSec: Float) {
        val speed = when (botDifficulty) {
            "Easy" -> 0.08f
            "Hard" -> 0.18f
            "Chaos" -> 0.22f
            else -> 0.13f
        }

        for (bot in bots) {
            if (!bot.isAlive) continue

            bot.animTime += deltaSec
            bot.shootCooldown -= deltaSec

            // Storm damage to bots
            if (!stormZone.isInside(bot.position)) {
                val dmg = stormZone.currentPhase.damagePerSecond * deltaSec
                if (bot.takeDamage(dmg)) {
                    killFeed.add(0, KillFeedMessage(killer = "Storm Zone", victim = bot.name, weapon = "Zone", isPlayerInvolved = false))
                    continue
                }
            }

            // Decide Target: Player or nearest living Bot
            val distToPlayer = bot.position.distanceTo(playerPos)
            val isOutsideStorm = !stormZone.isInside(bot.position)

            if (isOutsideStorm) {
                // Must retreat to storm center!
                bot.state = BotState.RETREAT_STORM
                val toCenter = (stormZone.center - bot.position).normalized()
                bot.position = bot.position + toCenter * speed
                bot.yaw = atan2(toCenter.x, -toCenter.z)
                bot.isMoving = true
            } else if (distToPlayer < 45f && isPlayerAlive) {
                // Combat Player
                bot.state = BotState.COMBAT
                val toPlayer = (playerPos - bot.position).normalized()
                bot.yaw = atan2(toPlayer.x, -toPlayer.z)

                // Move closer if far, or strafe
                if (distToPlayer > 12f) {
                    bot.position = bot.position + toPlayer * speed
                    bot.isMoving = true
                } else {
                    // Strafe sideways around player
                    val strafe = Vector3(toPlayer.z, 0f, -toPlayer.x).normalized()
                    bot.position = bot.position + strafe * (speed * 0.5f)
                    bot.isMoving = true
                }

                // Shoot at player
                if (bot.shootCooldown <= 0f && distToPlayer < 35f) {
                    val fireInterval = when (botDifficulty) {
                        "Easy" -> 1.8f
                        "Hard" -> 0.7f
                        "Chaos" -> 0.45f
                        else -> 1.1f
                    }
                    bot.shootCooldown = fireInterval + random.nextFloat() * 0.4f
                    val aimNoise = when (botDifficulty) {
                        "Easy" -> 0.15f
                        "Hard" -> 0.04f
                        else -> 0.08f
                    }
                    val shotDir = (toPlayer + Vector3((random.nextFloat() - 0.5f) * aimNoise, (random.nextFloat() - 0.5f) * aimNoise, (random.nextFloat() - 0.5f) * aimNoise)).normalized()
                    bullets.add(
                        Bullet(
                            position = bot.position + Vector3(0f, 2.2f, 0f),
                            velocity = shotDir * bot.weapon.bulletSpeed * 0.9f,
                            shooterId = bot.id,
                            damage = (bot.weapon.damage * 0.65f).toInt(), // Fair balanced bot damage
                            color = Color(0xFFEF4444)
                        )
                    )
                }
            } else {
                // Patrol toward safe zone
                bot.state = BotState.PATROL
                val toCenter = (stormZone.center - bot.position).normalized()
                bot.position = bot.position + toCenter * (speed * 0.6f)
                bot.yaw = atan2(toCenter.x, -toCenter.z)
                bot.isMoving = true
            }
        }
    }
}
