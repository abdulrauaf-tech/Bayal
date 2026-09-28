package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.engine3d.Vector3
import com.example.game.BotEntity
import com.example.game.StormZone
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Minimap(
    playerPos: Vector3,
    playerYaw: Float,
    stormZone: StormZone,
    bots: List<BotEntity>,
    airdropPos: Vector3?,
    size: Dp = 96.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xCC0F172A))
            .border(2.dp, Color(0xFF38BDF8), CircleShape)
            .testTag("minimap_radar")
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radarScale = (this.size.width * 0.45f) / 75f // 75 meters radar range

            // Subtle radar grid rings
            drawCircle(
                color = Color(0x2238BDF8),
                radius = this.size.width * 0.25f,
                center = center,
                style = Stroke(width = 1f)
            )
            drawCircle(
                color = Color(0x2238BDF8),
                radius = this.size.width * 0.45f,
                center = center,
                style = Stroke(width = 1f)
            )

            // Draw Storm Zone Ring
            val stormOffsetWorldX = stormZone.center.x - playerPos.x
            val stormOffsetWorldZ = stormZone.center.z - playerPos.z
            val stormScreenCenter = center + Offset(stormOffsetWorldX * radarScale, stormOffsetWorldZ * radarScale)
            val stormScreenRadius = stormZone.currentRadius * radarScale

            drawCircle(
                color = Color(0x8800E5FF),
                radius = stormScreenRadius,
                center = stormScreenCenter,
                style = Stroke(width = 2.dp.toPx())
            )

            // Draw Living Bots
            for (bot in bots) {
                if (!bot.isAlive) continue
                val dx = (bot.position.x - playerPos.x) * radarScale
                val dz = (bot.position.z - playerPos.z) * radarScale
                val botPos = center + Offset(dx, dz)

                if ((botPos - center).getDistance() <= this.size.width / 2f - 4f) {
                    drawCircle(
                        color = Color(0xFFEF4444), // Enemy red ping
                        radius = 3.dp.toPx(),
                        center = botPos
                    )
                }
            }

            // Draw Airdrop if nearby
            airdropPos?.let { ad ->
                val dx = (ad.x - playerPos.x) * radarScale
                val dz = (ad.z - playerPos.z) * radarScale
                val adScreen = center + Offset(dx, dz)
                if ((adScreen - center).getDistance() <= this.size.width / 2f - 4f) {
                    drawCircle(
                        color = Color(0xFFF59E0B),
                        radius = 4.dp.toPx(),
                        center = adScreen
                    )
                }
            }

            // Draw Player Arrow at Center (Oriented by Player Yaw)
            val arrowLength = 7.dp.toPx()
            val arrowAngle = -playerYaw - (Math.PI.toFloat() / 2f)

            val pTip = center + Offset(cos(arrowAngle) * arrowLength, sin(arrowAngle) * arrowLength)
            val pLeft = center + Offset(cos(arrowAngle + 2.5f) * (arrowLength * 0.7f), sin(arrowAngle + 2.5f) * (arrowLength * 0.7f))
            val pRight = center + Offset(cos(arrowAngle - 2.5f) * (arrowLength * 0.7f), sin(arrowAngle - 2.5f) * (arrowLength * 0.7f))

            val arrowPath = Path().apply {
                moveTo(pTip.x, pTip.y)
                lineTo(pLeft.x, pLeft.y)
                lineTo(center.x, center.y)
                lineTo(pRight.x, pRight.y)
                close()
            }
            drawPath(arrowPath, Color(0xFF38BDF8))
        }
    }
}
