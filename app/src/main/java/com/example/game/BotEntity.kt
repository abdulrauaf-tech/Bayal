package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.engine3d.Vector3

enum class BotState {
    PATROL,
    COMBAT,
    RETREAT_STORM,
    LOOT
}

data class BotEntity(
    val id: Int,
    val name: String,
    var position: Vector3,
    var yaw: Float = 0f,
    var health: Float = 100f,
    var shield: Float = 50f,
    var weapon: WeaponType = WeaponType.ASSAULT_RIFLE,
    var isAlive: Boolean = true,
    var state: BotState = BotState.PATROL,
    var shootCooldown: Float = 0f,
    var animTime: Float = 0f,
    var isMoving: Boolean = false,
    val primaryColor: Color = Color(0xFF1E293B),
    var kills: Int = 0
) {
    val totalEffectiveHp: Float
        get() = health + shield

    fun takeDamage(amount: Float): Boolean {
        var remaining = amount
        if (shield > 0f) {
            val shieldDmg = remaining.coerceAtMost(shield)
            shield -= shieldDmg
            remaining -= shieldDmg
        }
        if (remaining > 0f) {
            health -= remaining
        }
        if (health <= 0f) {
            health = 0f
            isAlive = false
            return true // Fatal
        }
        return false
    }
}
