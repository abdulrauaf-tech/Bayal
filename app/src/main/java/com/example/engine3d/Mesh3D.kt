package com.example.engine3d

import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class Polygon3D(
    val vertices: List<Vector3>,
    val color: Color,
    val isWireframe: Boolean = false,
    val strokeWidth: Float = 1f,
    val isDoubleSided: Boolean = true,
    val customAlpha: Float = 1f
)

object MeshBuilder {

    fun createBox(
        center: Vector3,
        sizeX: Float,
        sizeY: Float,
        sizeZ: Float,
        baseColor: Color,
        topColor: Color? = null,
        yawRad: Float = 0f
    ): List<Polygon3D> {
        val hx = sizeX / 2f
        val hy = sizeY / 2f
        val hz = sizeZ / 2f

        // Local 8 corners
        val corners = arrayOf(
            Vector3(-hx, -hy, -hz), // 0: bottom back left
            Vector3(hx, -hy, -hz),  // 1: bottom back right
            Vector3(hx, -hy, hz),   // 2: bottom front right
            Vector3(-hx, -hy, hz),  // 3: bottom front left
            Vector3(-hx, hy, -hz),  // 4: top back left
            Vector3(hx, hy, -hz),   // 5: top back right
            Vector3(hx, hy, hz),    // 6: top front right
            Vector3(-hx, hy, hz)    // 7: top front left
        )

        // Rotate by yaw if specified and add center
        val rotated = corners.map {
            val r = if (yawRad != 0f) it.rotateY(yawRad) else it
            r + center
        }

        val topC = topColor ?: baseColor
        val sideC1 = baseColor
        val sideC2 = Color(
            (baseColor.red * 0.85f).coerceIn(0f, 1f),
            (baseColor.green * 0.85f).coerceIn(0f, 1f),
            (baseColor.blue * 0.85f).coerceIn(0f, 1f),
            baseColor.alpha
        )
        val sideC3 = Color(
            (baseColor.red * 0.7f).coerceIn(0f, 1f),
            (baseColor.green * 0.7f).coerceIn(0f, 1f),
            (baseColor.blue * 0.7f).coerceIn(0f, 1f),
            baseColor.alpha
        )

        val polys = mutableListOf<Polygon3D>()

        // Top face
        polys.add(Polygon3D(listOf(rotated[4], rotated[5], rotated[6], rotated[7]), topC))
        // Bottom face
        polys.add(Polygon3D(listOf(rotated[3], rotated[2], rotated[1], rotated[0]), sideC3))
        // Front face (towards +Z)
        polys.add(Polygon3D(listOf(rotated[7], rotated[6], rotated[2], rotated[3]), sideC1))
        // Back face (towards -Z)
        polys.add(Polygon3D(listOf(rotated[5], rotated[4], rotated[0], rotated[1]), sideC3))
        // Left face (towards -X)
        polys.add(Polygon3D(listOf(rotated[4], rotated[7], rotated[3], rotated[0]), sideC2))
        // Right face (towards +X)
        polys.add(Polygon3D(listOf(rotated[6], rotated[5], rotated[1], rotated[2]), sideC2))

        return polys
    }

    fun createTree(center: Vector3, scale: Float = 1f): List<Polygon3D> {
        val polys = mutableListOf<Polygon3D>()
        // Trunk
        polys.addAll(
            createBox(
                center = center + Vector3(0f, 1.5f * scale, 0f),
                sizeX = 0.8f * scale,
                sizeY = 3.0f * scale,
                sizeZ = 0.8f * scale,
                baseColor = Color(0xFF6B4226) // wood brown
            )
        )
        // Foliage Level 1 (lower)
        polys.addAll(
            createBox(
                center = center + Vector3(0f, 3.8f * scale, 0f),
                sizeX = 3.6f * scale,
                sizeY = 1.8f * scale,
                sizeZ = 3.6f * scale,
                baseColor = Color(0xFF1E824C),
                topColor = Color(0xFF2ECC71)
            )
        )
        // Foliage Level 2 (middle)
        polys.addAll(
            createBox(
                center = center + Vector3(0f, 5.2f * scale, 0f),
                sizeX = 2.6f * scale,
                sizeY = 1.6f * scale,
                sizeZ = 2.6f * scale,
                baseColor = Color(0xFF27AE60),
                topColor = Color(0xFF38D37E)
            )
        )
        // Foliage Level 3 (top peak)
        polys.addAll(
            createBox(
                center = center + Vector3(0f, 6.4f * scale, 0f),
                sizeX = 1.4f * scale,
                sizeY = 1.2f * scale,
                sizeZ = 1.4f * scale,
                baseColor = Color(0xFF2ECC71),
                topColor = Color(0xFF58D68D)
            )
        )
        return polys
    }

    fun createStormRing(
        center: Vector3,
        radius: Float,
        segments: Int = 24,
        wallHeight: Float = 18f,
        timeSec: Float = 0f
    ): List<Polygon3D> {
        val polys = mutableListOf<Polygon3D>()
        val angleStep = (2 * PI / segments).toFloat()

        for (i in 0 until segments) {
            val a1 = i * angleStep
            val a2 = (i + 1) * angleStep

            val x1 = center.x + radius * cos(a1)
            val z1 = center.z + radius * sin(a1)
            val x2 = center.x + radius * cos(a2)
            val z2 = center.z + radius * sin(a2)

            // Dynamic electrical shimmer alpha
            val shimmer = 0.35f + 0.15f * sin(timeSec * 3f + i * 0.4f)
            val stormColor = Color(0x6600E5FF) // Electric cyan blue

            // Storm wall quad
            val p0 = Vector3(x1, 0f, z1)
            val p1 = Vector3(x2, 0f, z2)
            val p2 = Vector3(x2, wallHeight, z2)
            val p3 = Vector3(x1, wallHeight, z1)

            polys.add(
                Polygon3D(
                    vertices = listOf(p0, p1, p2, p3),
                    color = stormColor,
                    customAlpha = shimmer,
                    isDoubleSided = true
                )
            )
        }
        return polys
    }

    fun createCharacterModel(
        position: Vector3,
        yawRad: Float,
        isMoving: Boolean,
        animTime: Float,
        primaryColor: Color,
        secondaryColor: Color = Color(0xFF1E293B),
        isCrouched: Boolean = false,
        isAiming: Boolean = false,
        gunColor: Color = Color(0xFF111827)
    ): List<Polygon3D> {
        val polys = mutableListOf<Polygon3D>()
        val crouchOffset = if (isCrouched) -0.5f else 0f
        val legSwing = if (isMoving) sin(animTime * 10f) * 0.4f else 0f
        val armSwing = if (isMoving && !isAiming) -sin(animTime * 10f) * 0.4f else 0f

        val basePos = position + Vector3(0f, crouchOffset, 0f)

        // Shadow circle / quad on ground
        val shadowSize = if (isCrouched) 1.4f else 1.2f
        polys.add(
            Polygon3D(
                vertices = listOf(
                    position + Vector3(-shadowSize, 0.05f, -shadowSize),
                    position + Vector3(shadowSize, 0.05f, -shadowSize),
                    position + Vector3(shadowSize, 0.05f, shadowSize),
                    position + Vector3(-shadowSize, 0.05f, shadowSize)
                ),
                color = Color(0x40000000),
                isDoubleSided = true
            )
        )

        // Left Leg
        val leftLegPos = basePos + Vector3(-0.35f, 0.8f, legSwing).rotateY(yawRad)
        polys.addAll(
            createBox(
                center = leftLegPos,
                sizeX = 0.4f,
                sizeY = if (isCrouched) 1.0f else 1.5f,
                sizeZ = 0.4f,
                baseColor = secondaryColor,
                yawRad = yawRad
            )
        )

        // Right Leg
        val rightLegPos = basePos + Vector3(0.35f, 0.8f, -legSwing).rotateY(yawRad)
        polys.addAll(
            createBox(
                center = rightLegPos,
                sizeX = 0.4f,
                sizeY = if (isCrouched) 1.0f else 1.5f,
                sizeZ = 0.4f,
                baseColor = secondaryColor,
                yawRad = yawRad
            )
        )

        // Torso / Chest Armor
        val torsoPos = basePos + Vector3(0f, if (isCrouched) 1.7f else 2.3f, 0f)
        polys.addAll(
            createBox(
                center = torsoPos,
                sizeX = 1.1f,
                sizeY = 1.4f,
                sizeZ = 0.65f,
                baseColor = primaryColor,
                topColor = Color.White,
                yawRad = yawRad
            )
        )

        // Head / Helmet
        val headPos = basePos + Vector3(0f, if (isCrouched) 2.65f else 3.25f, 0f)
        polys.addAll(
            createBox(
                center = headPos,
                sizeX = 0.65f,
                sizeY = 0.65f,
                sizeZ = 0.65f,
                baseColor = secondaryColor,
                topColor = primaryColor,
                yawRad = yawRad
            )
        )

        // Visor on helmet
        val visorPos = headPos + Vector3(0f, 0f, 0.35f).rotateY(yawRad)
        polys.addAll(
            createBox(
                center = visorPos,
                sizeX = 0.5f,
                sizeY = 0.2f,
                sizeZ = 0.1f,
                baseColor = Color(0xFF00E5FF),
                yawRad = yawRad
            )
        )

        // Left Arm
        val leftArmPos = basePos + Vector3(-0.75f, if (isCrouched) 1.8f else 2.4f, armSwing).rotateY(yawRad)
        polys.addAll(
            createBox(
                center = leftArmPos,
                sizeX = 0.35f,
                sizeY = 1.2f,
                sizeZ = 0.35f,
                baseColor = secondaryColor,
                yawRad = yawRad
            )
        )

        // Right Arm & Weapon
        val rightArmForward = if (isAiming) 0.5f else 0.2f
        val rightArmPos = basePos + Vector3(0.75f, if (isCrouched) 1.9f else 2.5f, rightArmForward - armSwing).rotateY(yawRad)
        polys.addAll(
            createBox(
                center = rightArmPos,
                sizeX = 0.35f,
                sizeY = 1.2f,
                sizeZ = 0.35f,
                baseColor = secondaryColor,
                yawRad = yawRad
            )
        )

        // Weapon (Held pointing forward in front of character)
        val gunForward = if (isAiming) 0.8f else 0.6f
        val gunPos = basePos + Vector3(0.5f, if (isCrouched) 1.8f else 2.3f, gunForward).rotateY(yawRad)
        polys.addAll(
            createBox(
                center = gunPos,
                sizeX = 0.2f,
                sizeY = 0.3f,
                sizeZ = 1.2f,
                baseColor = gunColor,
                yawRad = yawRad
            )
        )

        return polys
    }

    fun createAirdropCrate(position: Vector3, altitude: Float): List<Polygon3D> {
        val polys = mutableListOf<Polygon3D>()
        val cratePos = position + Vector3(0f, altitude + 0.9f, 0f)

        // Main red military crate with yellow stripes
        polys.addAll(
            createBox(
                center = cratePos,
                sizeX = 1.8f,
                sizeY = 1.8f,
                sizeZ = 1.8f,
                baseColor = Color(0xFFDC2626), // Military red
                topColor = Color(0xFFFBBF24)   // Gold top
            )
        )

        // Parachute canopy above if airborne
        if (altitude > 0.5f) {
            val chutePos = cratePos + Vector3(0f, 3.5f, 0f)
            polys.addAll(
                createBox(
                    center = chutePos,
                    sizeX = 4.0f,
                    sizeY = 1.2f,
                    sizeZ = 4.0f,
                    baseColor = Color(0xFFF59E0B),
                    topColor = Color(0xFFFEF08A)
                )
            )
            // Parachute lines
            polys.add(
                Polygon3D(
                    vertices = listOf(
                        cratePos + Vector3(-0.8f, 0.9f, -0.8f),
                        chutePos + Vector3(-1.8f, -0.6f, -1.8f)
                    ),
                    color = Color.White,
                    isWireframe = true,
                    strokeWidth = 2f
                )
            )
            polys.add(
                Polygon3D(
                    vertices = listOf(
                        cratePos + Vector3(0.8f, 0.9f, 0.8f),
                        chutePos + Vector3(1.8f, -0.6f, 1.8f)
                    ),
                    color = Color.White,
                    isWireframe = true,
                    strokeWidth = 2f
                )
            )
        }

        return polys
    }
}
