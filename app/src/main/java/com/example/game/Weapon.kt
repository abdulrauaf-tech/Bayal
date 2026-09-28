package com.example.game

import androidx.compose.ui.graphics.Color

enum class WeaponType(
    val displayName: String,
    val damage: Int,
    val fireRateMs: Long,
    val magSize: Int,
    val reloadTimeMs: Long,
    val bulletSpeed: Float,
    val spreadAngleRad: Float,
    val pellets: Int,
    val color: Color,
    val isAutomatic: Boolean
) {
    ASSAULT_RIFLE(
        displayName = "M4A1 Assault",
        damage = 28,
        fireRateMs = 120L,
        magSize = 30,
        reloadTimeMs = 1800L,
        bulletSpeed = 1.4f,
        spreadAngleRad = 0.03f,
        pellets = 1,
        color = Color(0xFF38BDF8), // Tactical Cyan
        isAutomatic = true
    ),
    SNIPER_RIFLE(
        displayName = "AWM Bolt-Action",
        damage = 95,
        fireRateMs = 1200L,
        magSize = 5,
        reloadTimeMs = 2600L,
        bulletSpeed = 2.4f,
        spreadAngleRad = 0.005f,
        pellets = 1,
        color = Color(0xFFF59E0B), // Legendary Amber
        isAutomatic = false
    ),
    SHOTGUN(
        displayName = "SPAS-12 Shotgun",
        damage = 18, // 18 * 6 pellets = up to 108 close-up!
        fireRateMs = 650L,
        magSize = 8,
        reloadTimeMs = 2200L,
        bulletSpeed = 1.2f,
        spreadAngleRad = 0.09f,
        pellets = 6,
        color = Color(0xFFEF4444), // Crimson
        isAutomatic = false
    ),
    ROCKET_LAUNCHER(
        displayName = "RPG-7 Rocket",
        damage = 110,
        fireRateMs = 2000L,
        magSize = 1,
        reloadTimeMs = 3000L,
        bulletSpeed = 0.9f,
        spreadAngleRad = 0.01f,
        pellets = 1,
        color = Color(0xFFA855F7), // Epic Purple
        isAutomatic = false
    )
}

data class WeaponSlot(
    val type: WeaponType,
    var currentAmmo: Int = type.magSize,
    var reserveAmmo: Int = type.magSize * 4,
    var isReloading: Boolean = false,
    var reloadProgress: Float = 0f
)
