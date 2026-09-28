package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun VirtualJoystick(
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    onMove: (deltaX: Float, deltaY: Float, isSprinting: Boolean) -> Unit
) {
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .size(size)
            .testTag("virtual_joystick")
            .pointerInput(Unit) {
                val radius = this.size.width / 2f
                detectDragGestures(
                    onDragStart = { offset ->
                        val center = Offset(radius, radius)
                        val diff = offset - center
                        val dist = diff.getDistance()
                        val clampedDist = dist.coerceAtMost(radius)
                        val angle = kotlin.math.atan2(diff.y, diff.x)
                        val clamped = Offset(cos(angle) * clampedDist, sin(angle) * clampedDist)
                        thumbOffset = clamped
                        val normX = (clamped.x / radius).coerceIn(-1f, 1f)
                        val normY = (clamped.y / radius).coerceIn(-1f, 1f)
                        val isSprinting = clampedDist >= radius * 0.85f
                        onMove(normX, normY, isSprinting)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val center = Offset(radius, radius)
                        val diff = change.position - center
                        val dist = diff.getDistance()
                        val clampedDist = dist.coerceAtMost(radius)
                        val angle = kotlin.math.atan2(diff.y, diff.x)
                        val clamped = Offset(cos(angle) * clampedDist, sin(angle) * clampedDist)
                        thumbOffset = clamped
                        val normX = (clamped.x / radius).coerceIn(-1f, 1f)
                        val normY = (clamped.y / radius).coerceIn(-1f, 1f)
                        val isSprinting = clampedDist >= radius * 0.85f
                        onMove(normX, normY, isSprinting)
                    },
                    onDragEnd = {
                        thumbOffset = Offset.Zero
                        onMove(0f, 0f, false)
                    },
                    onDragCancel = {
                        thumbOffset = Offset.Zero
                        onMove(0f, 0f, false)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val outerRadius = size.toPx() / 2f
            val thumbRadius = outerRadius * 0.38f

            // Outer Base Ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x33000000), Color(0x660F172A)),
                    center = center,
                    radius = outerRadius
                ),
                radius = outerRadius,
                center = center
            )
            drawCircle(
                color = Color(0x6638BDF8),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
            // Inner guide cross
            drawLine(
                color = Color(0x22FFFFFF),
                start = Offset(center.x - outerRadius * 0.5f, center.y),
                end = Offset(center.x + outerRadius * 0.5f, center.y),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = Color(0x22FFFFFF),
                start = Offset(center.x, center.y - outerRadius * 0.5f),
                end = Offset(center.x, center.y + outerRadius * 0.5f),
                strokeWidth = 1.dp.toPx()
            )

            // Movable Thumb Nub
            val currentThumb = center + thumbOffset
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF38BDF8), Color(0xFF0284C7)),
                    center = currentThumb,
                    radius = thumbRadius
                ),
                radius = thumbRadius,
                center = currentThumb
            )
            drawCircle(
                color = Color.White,
                radius = thumbRadius,
                center = currentThumb,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}
