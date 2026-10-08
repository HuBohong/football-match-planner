package au.edu.jcu.assessment.utilityapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp

// Both the page and glass sample the same gradient in screen coordinates.
data class GradientBackground(val stops: Array<Pair<Float, Color>>, val height: Float)
val LocalGradientBackground = staticCompositionLocalOf {
    GradientBackground(arrayOf(0f to Color(0xFF396832), 1f to Color(0xFF182A15)), 1f)
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    tint: Color = Color(0xFF1C2B19).copy(alpha = 0.45f),
    content: @Composable () -> Unit
) {
    val background = LocalGradientBackground.current
    var top by remember { mutableFloatStateOf(0f) }
    Box(modifier = modifier.onGloballyPositioned { top = it.positionInRoot().y }
        .clip(shape).border(1.dp, Color.White.copy(alpha = 0.22f), shape)) {
        // Blur only the backdrop; text and controls are drawn in a separate sharp layer.
        Box(modifier = Modifier.matchParentSize()
            .blur(28.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            .background(Brush.verticalGradient(*background.stops,
                startY = -top, endY = background.height.coerceAtLeast(1f) - top)))
        Box(modifier = Modifier.matchParentSize()
            .background(tint)
            .background(Brush.verticalGradient(listOf(
                Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.02f)))))
        CompositionLocalProvider(LocalContentColor provides Color.White) { content() }
    }
}
