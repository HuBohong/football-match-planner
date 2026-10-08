package au.edu.jcu.assessment.utilityapp.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.edu.jcu.assessment.utilityapp.data.MatchFixture

@Composable
fun TeamsMenuPanel(
    expanded: Boolean,
    onToggle: () -> Unit,
    onEditTeams: () -> Unit,
    teamsByLeague: Map<String, List<String>>,
    followedTeams: List<String>,
    fixtures: List<MatchFixture>,
    backdrop: GraphicsLayer,
    backdropOrigin: Offset,
    availableHeight: Dp,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(if (expanded) 1f else 0f,
        spring(dampingRatio = 0.9f, stiffness = 300f), label = "Teams card expansion")
    val targetHeight = minOf(408.dp, availableHeight * 0.75f).coerceAtLeast(48.dp)
    val width = 48.dp + 192.dp * progress.coerceIn(0f, 1f)
    val height = 48.dp + (targetHeight - 48.dp) * progress.coerceIn(0f, 1f)
    var origin by remember { mutableStateOf(Offset.Zero) }
    val shape = RoundedCornerShape(24.dp)
    GlassPanel(modifier = modifier.width(width).height(height)
        .onGloballyPositioned { origin = it.positionInRoot() }, shape = shape,
        tint = Color(0xFF4B8148).copy(alpha = 0.24f)) {
        // Replay only the underlying page layer, blur it, and clip to this card.
        // The page itself never receives a blur or a dark modal scrim.
        Box(Modifier.fillMaxSize().clip(shape).pointerInput(Unit) {
            // Consume card-body taps so they cannot reach the outside-dismiss layer.
            detectTapGestures { }
        }) {
            Canvas(Modifier.matchParentSize().graphicsLayer {
                renderEffect = BlurEffect(18.dp.toPx(), 18.dp.toPx(), TileMode.Clamp)
            }) {
                val relative = origin - backdropOrigin
                translate(-relative.x, -relative.y) { drawLayer(backdrop) }
            }
            Box(Modifier.matchParentSize().background(Color(0xFF4B8148).copy(alpha = 0.32f)))
            if (progress > 0.02f) {
                Column(Modifier.width(240.dp).align(Alignment.TopEnd)
                    .graphicsLayer { alpha = progress.coerceIn(0f, 1f) }) {
                    Row(Modifier.fillMaxWidth().height(48.dp).padding(start = 16.dp, end = 48.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("My teams", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    }
                    Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp)) {
                        teamsByLeague.forEach { (league, teams) ->
                            val emblem = fixtures.firstOrNull { it.competition == league }?.competitionEmblem
                            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                FootballBadge(emblem, league, Modifier.size(24.dp))
                                Text(league, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White)
                                Text(teams.count { it in followedTeams }.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.65f))
                            }
                        }
                    }
                    TextButton(onClick = onEditTeams, modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                        Text("Edit teams")
                    }
                }
            }
            IconButton(onClick = onToggle, modifier = Modifier.align(Alignment.TopEnd).size(48.dp)
                .semantics {
                    contentDescription = if (expanded) "Close My teams" else "My teams (${followedTeams.size} followed)"
                    stateDescription = if (expanded) "Expanded" else "Collapsed"
                }) {
                MorphingTeamsIcon(expanded, Modifier.size(24.dp))
            }
        }
    }
}
