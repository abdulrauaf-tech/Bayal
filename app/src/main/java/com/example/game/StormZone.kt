package com.example.game

import com.example.engine3d.Vector3
import kotlin.math.sqrt

data class StormPhase(
    val phaseNumber: Int,
    val waitDurationSec: Float,
    val shrinkDurationSec: Float,
    val targetRadius: Float,
    val damagePerSecond: Float
)

class StormZone(
    var center: Vector3 = Vector3(0f, 0f, 0f),
    var currentRadius: Float = 110f
) {
    val phases = listOf(
        StormPhase(1, 25f, 25f, 75f, 2f),
        StormPhase(2, 20f, 20f, 45f, 4f),
        StormPhase(3, 15f, 15f, 22f, 7f),
        StormPhase(4, 10f, 12f, 8f, 12f),
        StormPhase(5, 5f, 10f, 1f, 20f)
    )

    var currentPhaseIndex = 0
    var isShrinking = false
    var phaseTimer = phases[0].waitDurationSec
    var targetCenter = center
    var initialRadiusForPhase = currentRadius
    var initialCenterForPhase = center

    val currentPhase: StormPhase
        get() = phases.getOrElse(currentPhaseIndex) { phases.last() }

    fun update(deltaSec: Float) {
        phaseTimer -= deltaSec

        if (!isShrinking) {
            // Waiting countdown
            if (phaseTimer <= 0f) {
                isShrinking = true
                initialRadiusForPhase = currentRadius
                initialCenterForPhase = center
                phaseTimer = currentPhase.shrinkDurationSec

                // Pick a new safe center slightly within current circle
                val offsetDist = (currentRadius - currentPhase.targetRadius) * 0.45f
                val angle = (Math.random() * 2 * Math.PI).toFloat()
                targetCenter = center + Vector3(
                    kotlin.math.cos(angle) * offsetDist,
                    0f,
                    kotlin.math.sin(angle) * offsetDist
                )
            }
        } else {
            // Actively shrinking
            val totalTime = currentPhase.shrinkDurationSec
            val progress = (1f - (phaseTimer / totalTime)).coerceIn(0f, 1f)

            currentRadius = initialRadiusForPhase + (currentPhase.targetRadius - initialRadiusForPhase) * progress
            center = initialCenterForPhase + (targetCenter - initialCenterForPhase) * progress

            if (phaseTimer <= 0f) {
                isShrinking = false
                if (currentPhaseIndex < phases.size - 1) {
                    currentPhaseIndex++
                    phaseTimer = currentPhase.waitDurationSec
                } else {
                    // Final closed circle
                    phaseTimer = 999f
                }
            }
        }
    }

    fun isInside(position: Vector3): Boolean {
        val dx = position.x - center.x
        val dz = position.z - center.z
        return sqrt(dx * dx + dz * dz) <= currentRadius
    }

    fun getDistanceOutside(position: Vector3): Float {
        val dx = position.x - center.x
        val dz = position.z - center.z
        val dist = sqrt(dx * dx + dz * dz)
        return (dist - currentRadius).coerceAtLeast(0f)
    }
}
