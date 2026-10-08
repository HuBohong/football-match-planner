package au.edu.jcu.assessment.utilityapp.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

// Native rendition of the Morphicons menu -> X rotation/merge, not a JS wrapper.
// Reference: https://github.com/guillermolg00/morphicons (smooth: stiffness 170, damping 26).
@Composable
fun MorphingTeamsIcon(expanded: Boolean, modifier: Modifier = Modifier) {
    val progress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = spring(dampingRatio = 26f / (2f * sqrt(170f)), stiffness = 170f),
        label = "Teams menu morph"
    )
    Canvas(modifier) {
        val unit = size.minDimension / 24f
        val origin = Offset((size.width - 24f * unit) / 2f, (size.height - 24f * unit) / 2f)
        // Polar interpolation keeps strokes straight. Top/bottom merge onto one
        // diagonal instead of fading a line out of existence.
        val halfLength = 8f * (sqrt(2f) * 12f / 16f).pow(progress)
        for (line in 0..2) {
            val startY = 6f + 6f * line
            val centre = Offset(12f, startY + (12f - startY) * progress)
            val angle = (if (line == 1) 1f else -1f) * (Math.PI.toFloat() / 4f) * progress
            val delta = Offset(cos(angle) * halfLength, sin(angle) * halfLength)
            drawLine(
                color = Color.White,
                start = origin + (centre - delta) * unit,
                end = origin + (centre + delta) * unit,
                strokeWidth = 2f * unit,
                cap = StrokeCap.Round
            )
        }
    }
}
