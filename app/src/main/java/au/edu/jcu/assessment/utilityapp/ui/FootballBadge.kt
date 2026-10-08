package au.edu.jcu.assessment.utilityapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage

// Image requests use Coil's own public-image client/cache, never the authenticated API client.
@Composable
fun FootballBadge(url: String?, name: String, modifier: Modifier = Modifier) {
    SubcomposeAsyncImage(
        model = url,
        contentDescription = null, // The adjacent name already identifies the team/competition.
        modifier = modifier,
        contentScale = ContentScale.Fit,
        loading = { BadgeFallback(name) },
        error = { BadgeFallback(name) }
    )
}

@Composable
private fun BadgeFallback(name: String) {
    Box(modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape),
        contentAlignment = Alignment.Center) {
        Text(name.split(" ").filter { it.isNotEmpty() }.take(2).joinToString("") { it.take(1) },
            fontSize = 10.sp, color = Color.White)
    }
}
