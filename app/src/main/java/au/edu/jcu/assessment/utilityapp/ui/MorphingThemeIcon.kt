package au.edu.jcu.assessment.utilityapp.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Native sun-to-crescent morph; no React runtime or JavaScript dependency. */
@Composable
fun MorphingThemeIcon(dark: Boolean, modifier: Modifier = Modifier) {
    // Morphicons snappy: stiffness 420, damping 30, unit mass.
    // https://github.com/guillermolg00/morphicons/blob/main/src/core/spring.ts
    val progress by animateFloatAsState(
        targetValue = if (dark) 1f else 0f,
        animationSpec = spring(stiffness = 420f, dampingRatio = 30f / (2f * sqrt(420f))),
        label = "Sun moon morph"
    )
    Canvas(modifier) {
        val unit = size.minDimension / 24f
        val origin = Offset((size.width - 24f * unit) / 2f, (size.height - 24f * unit) / 2f)
        fun blend(sun: Float, moon: Float) = sun + (moon - sun) * progress
        fun point(x: Float, y: Float) = origin + Offset(x, y) * unit

        // Four corresponding Bezier segments turn a circle into a crescent.
        // Both endpoints are complete outlines, so the icon never flashes blank.
        val path = Path().apply {
            val top = point(blend(12f, 15f), blend(7f, 3f))
            moveTo(top.x, top.y)
            fun segment(sun: FloatArray, moon: FloatArray) {
                val a = point(blend(sun[0], moon[0]), blend(sun[1], moon[1]))
                val b = point(blend(sun[2], moon[2]), blend(sun[3], moon[3]))
                val end = point(blend(sun[4], moon[4]), blend(sun[5], moon[5]))
                cubicTo(a.x, a.y, b.x, b.y, end.x, end.y)
            }
            segment(floatArrayOf(9.24f, 7f, 7f, 9.24f, 7f, 12f),
                floatArrayOf(8.37f, 3f, 3f, 7.03f, 3f, 12f))
            segment(floatArrayOf(7f, 14.76f, 9.24f, 17f, 12f, 17f),
                floatArrayOf(3f, 16.97f, 8.37f, 21f, 15f, 21f))
            segment(floatArrayOf(14.76f, 17f, 17f, 14.76f, 17f, 12f),
                floatArrayOf(11.13f, 21f, 8f, 16.97f, 8f, 12f))
            segment(floatArrayOf(17f, 9.24f, 14.76f, 7f, 12f, 7f),
                floatArrayOf(8f, 7.03f, 11.13f, 3f, 15f, 3f))
            close()
        }
        drawPath(path, Color.White, style = Stroke(width = 2f * unit, cap = StrokeCap.Round))
        val rays = (1f - progress).coerceIn(0f, 1f)
        repeat(8) { index ->
            val angle = (index * Math.PI / 4 + progress * Math.PI / 6).toFloat()
            val direction = Offset(cos(angle), sin(angle))
            val centre = point(12f, 12f)
            drawLine(
                color = Color.White.copy(alpha = rays),
                start = centre + direction * (8f * unit),
                end = centre + direction * ((8f + 2f * rays) * unit),
                strokeWidth = 2f * unit,
                cap = StrokeCap.Round
            )
        }
    }
}
