package com.example.engine3d

import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

data class ProjectedPoint(
    val offset: Offset,
    val depth: Float,
    val isVisible: Boolean
)

class Camera3D(
    var position: Vector3 = Vector3(0f, 6f, 12f),
    var yaw: Float = 0f, // radians around Y
    var pitch: Float = 0.2f, // radians around X
    var fovRad: Float = Math.toRadians(65.0).toFloat(),
    var nearPlane: Float = 0.3f
) {
    // Computes Camera Space coordinates for a World Point
    fun worldToCamera(worldPoint: Vector3): Vector3 {
        // Step 1: Translate relative to camera
        val dx = worldPoint.x - position.x
        val dy = worldPoint.y - position.y
        val dz = worldPoint.z - position.z

        // Step 2: Rotate around Y by -yaw
        val cosY = cos(-yaw)
        val sinY = sin(-yaw)
        val x1 = dx * cosY - dz * sinY
        val z1 = dx * sinY + dz * cosY
        val y1 = dy

        // Step 3: Rotate around X by -pitch
        val cosP = cos(-pitch)
        val sinP = sin(-pitch)
        val y2 = y1 * cosP - z1 * sinP
        val z2 = y1 * sinP + z1 * cosP
        val x2 = x1

        // In our camera coordinate system:
        // +X is right, +Y is up, +Z is forward
        return Vector3(x2, y2, z2)
    }

    // Projects Camera Space coordinates into Screen Pixels
    fun cameraToScreen(cameraPoint: Vector3, screenWidth: Float, screenHeight: Float): ProjectedPoint {
        val z = cameraPoint.z
        if (z < nearPlane) {
            return ProjectedPoint(Offset.Zero, z, false)
        }

        val fovFactor = (screenWidth * 0.5f) / tan(fovRad * 0.5f)
        val screenX = screenWidth * 0.5f + (cameraPoint.x / z) * fovFactor
        val screenY = screenHeight * 0.5f - (cameraPoint.y / z) * fovFactor

        val margin = 200f
        val isVisible = screenX >= -margin && screenX <= screenWidth + margin &&
                        screenY >= -margin && screenY <= screenHeight + margin

        return ProjectedPoint(Offset(screenX, screenY), z, isVisible)
    }

    fun projectWorld(worldPoint: Vector3, screenWidth: Float, screenHeight: Float): ProjectedPoint {
        val cam = worldToCamera(worldPoint)
        return cameraToScreen(cam, screenWidth, screenHeight)
    }

    // Direction vector in world coordinates where camera is looking
    fun getForwardVector(): Vector3 {
        val cp = cos(pitch)
        return Vector3(
            x = sin(yaw) * cp,
            y = -sin(pitch),
            z = -cos(yaw) * cp
        ).normalized()
    }

    fun getRightVector(): Vector3 {
        return Vector3(
            x = cos(yaw),
            y = 0f,
            z = sin(yaw)
        ).normalized()
    }
}
