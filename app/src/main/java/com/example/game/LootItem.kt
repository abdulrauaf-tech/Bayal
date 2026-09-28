package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.engine3d.Vector3

enum class LootType(val displayName: String, val color: Color) {
    MEDKIT("Medkit (+50 HP)", Color(0xFF10B981)),
    SHIELD_BATTERY("Shield Battery (+50 Shield)", Color(0xFF00E5FF)),
    AMMO_BOX("Ammo Reserve (+60)", Color(0xFFFBBF24)),
    WEAPON_SNIPER("AWM Sniper Rifle", Color(0xFFF59E0B)),
    WEAPON_SHOTGUN("SPAS-12 Shotgun", Color(0xFFEF4444)),
    WEAPON_ROCKET("RPG-7 Rocket", Color(0xFFA855F7))
}

data class LootItem(
    val id: Long,
    val type: LootType,
    val position: Vector3,
    var isCollected: Boolean = false,
    var rotationAngle: Float = 0f
)

data class Airdrop(
    val position: Vector3,
    var altitude: Float = 40f,
    var isLanded: Boolean = false,
    var isLooted: Boolean = false
)
