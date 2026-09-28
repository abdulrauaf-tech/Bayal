package com.example.engine3d

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.math.max

data class RenderItem(
    val depth: Float,
    val drawAction: (DrawScope) -> Unit
)

data class FloatingText(
    val position: Vector3,
    val text: String,
    val color: Color,
    val alpha: Float,
    val scale: Float = 1f
)

data class BulletTracer(
    val start: Vector3,
    val end: Vector3,
    val color: Color = Color(0xFFFBBF24),
    val width: Float = 3f
)

class Renderer3D {

    private val sunDirection = Vector3(0.4f, 1.0f, -0.5f).normalized()

    fun renderScene(
        drawScope: DrawScope,
        camera: Camera3D,
        polygons: List<Polygon3D>,
        bulletTracers: List<BulletTracer>,
        floatingTexts: List<FloatingText>,
        textMeasurer: TextMeasurer
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height
        val renderList = mutableListOf<RenderItem>()

        // Process 3D Polygons
        for (poly in polygons) {
            val count = poly.vertices.size
            if (count < 2) continue

            // Transform each vertex to Camera Space
            val camPoints = poly.vertices.map { camera.worldToCamera(it) }

            // Frustum near-plane check: if all points behind near plane, discard
            if (camPoints.all { it.z < camera.nearPlane }) {
                continue
            }

            // Calculate depth for sorting (average Z)
            var sumZ = 0f
            for (p in camPoints) {
                sumZ += max(camera.nearPlane, p.z)
            }
            val avgDepth = sumZ / count

            if (poly.isWireframe && count == 2) {
                val p1Proj = camera.cameraToScreen(camPoints[0], width, height)
                val p2Proj = camera.cameraToScreen(camPoints[1], width, height)

                if (p1Proj.isVisible || p2Proj.isVisible) {
                    renderList.add(
                        RenderItem(avgDepth) { scope ->
                            scope.drawLine(
                                color = poly.color.copy(alpha = poly.customAlpha),
                                start = p1Proj.offset,
                                end = p2Proj.offset,
                                strokeWidth = poly.strokeWidth
                            )
                        }
                    )
                }
                continue
            }

            // Normal and Lighting calculation (in world space)
            val litColor = if (count >= 3 && !poly.isWireframe) {
                val v0 = poly.vertices[0]
                val v1 = poly.vertices[1]
                val v2 = poly.vertices[2]
                val normal = (v1 - v0).cross(v2 - v0).normalized()
                val dot = normal.dot(sunDirection)
                val lightIntensity = (0.5f + 0.5f * max(0f, dot)).coerceIn(0.3f, 1.15f)

                Color(
                    red = (poly.color.red * lightIntensity).coerceIn(0f, 1f),
                    green = (poly.color.green * lightIntensity).coerceIn(0f, 1f),
                    blue = (poly.color.blue * lightIntensity).coerceIn(0f, 1f),
                    alpha = (poly.color.alpha * poly.customAlpha).coerceIn(0f, 1f)
                )
            } else {
                poly.color.copy(alpha = (poly.color.alpha * poly.customAlpha).coerceIn(0f, 1f))
            }

            // Project all vertices to screen
            val screenOffsets = camPoints.map { p ->
                val safeZ = max(0.15f, p.z)
                val fovFactor = (width * 0.5f) / kotlin.math.tan(camera.fovRad * 0.5f)
                val sx = width * 0.5f + (p.x / safeZ) * fovFactor
                val sy = height * 0.5f - (p.y / safeZ) * fovFactor
                Offset(sx, sy)
            }

            // Check if bounds overlap screen roughly
            val minX = screenOffsets.minOf { it.x }
            val maxX = screenOffsets.maxOf { it.x }
            val minY = screenOffsets.minOf { it.y }
            val maxY = screenOffsets.maxOf { it.y }

            if (maxX < -150f || minX > width + 150f || maxY < -150f || minY > height + 150f) {
                continue
            }

            renderList.add(
                RenderItem(avgDepth) { scope ->
                    val path = Path()
                    path.moveTo(screenOffsets[0].x, screenOffsets[0].y)
                    for (i in 1 until screenOffsets.size) {
                        path.lineTo(screenOffsets[i].x, screenOffsets[i].y)
                    }
                    path.close()

                    scope.drawPath(
                        path = path,
                        color = litColor,
                        style = if (poly.isWireframe) Stroke(width = poly.strokeWidth) else Fill
                    )
                }
            )
        }

        // Process Bullet Tracers
        for (tracer in bulletTracers) {
            val p1Proj = camera.projectWorld(tracer.start, width, height)
            val p2Proj = camera.projectWorld(tracer.end, width, height)

            if (p1Proj.isVisible || p2Proj.isVisible) {
                val depth = (p1Proj.depth + p2Proj.depth) * 0.5f
                renderList.add(
                    RenderItem(depth) { scope ->
                        scope.drawLine(
                            color = tracer.color,
                            start = p1Proj.offset,
                            end = p2Proj.offset,
                            strokeWidth = tracer.width
                        )
                        // Glowing bullet head dot
                        scope.drawCircle(
                            color = Color.White,
                            radius = tracer.width * 1.5f,
                            center = p2Proj.offset
                        )
                    }
                )
            }
        }

        // Process Floating Text (e.g. Damage Numbers, Headshots)
        for (fText in floatingTexts) {
            val proj = camera.projectWorld(fText.position, width, height)
            if (proj.isVisible && proj.depth > camera.nearPlane) {
                renderList.add(
                    RenderItem(proj.depth) { scope ->
                        val measured = textMeasurer.measure(
                            text = fText.text,
                            style = TextStyle(
                                color = fText.color.copy(alpha = fText.alpha),
                                fontSize = (16f * fText.scale).sp,
                                fontWeight = FontWeight.Black
                            )
                        )
                        scope.drawText(
                            textLayoutResult = measured,
                            topLeft = Offset(
                                proj.offset.x - measured.size.width / 2f,
                                proj.offset.y - measured.size.height / 2f
                            )
                        )
                    }
                )
            }
        }

        // Sort descending by depth (Painter's Algorithm: farthest items drawn first)
        renderList.sortByDescending { it.depth }

        // Execute all draws
        for (item in renderList) {
            item.drawAction(drawScope)
        }
    }
}
