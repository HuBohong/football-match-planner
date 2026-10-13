package au.edu.jcu.assessment.utilityapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import au.edu.jcu.assessment.utilityapp.data.MatchFixture
import au.edu.jcu.assessment.utilityapp.data.MatchStatus
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DateHeader(date: LocalDate, today: LocalDate) {
    val relativeLabel = when (date) {
        today.minusDays(1) -> " · Yesterday"
        today -> " · Today"
        today.plusDays(1) -> " · Tomorrow"
        else -> ""
    }
    Text(
        text = date.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)) + relativeLabel,
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp).semantics { heading() },
        style = MaterialTheme.typography.titleMedium,
        color = Color.White,
        textAlign = TextAlign.Center
    )
}

@Composable
fun CompetitionHeader(name: String, emblem: String?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp).semantics { heading() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FootballBadge(emblem, name, Modifier.size(26.dp))
        Text(name, style = MaterialTheme.typography.labelLarge, color = Color.White)
    }
}

@Composable
fun MatchRow(match: MatchFixture, use24HourFormat: Boolean, displayZone: ZoneId) {
    val displayedTime = if (match.status == MatchStatus.SCHEDULED) "Time TBC" else match.kickoff.atZone(displayZone).format(
        DateTimeFormatter.ofPattern(if (use24HourFormat) "HH:mm" else "h:mm a", Locale.ENGLISH)
    )
    val showScore = match.status in setOf(MatchStatus.FINISHED, MatchStatus.IN_PLAY, MatchStatus.PAUSED, MatchStatus.AWARDED)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FixtureTeam(match.homeTeam, match.homeCrest, Modifier.weight(1f))
        Column(modifier = Modifier.weight(1.2f), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (showScore) "${match.homeScore ?: "–"} – ${match.awayScore ?: "–"}" else displayedTime,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            if (match.status != MatchStatus.TIMED) {
                Text(match.status.label, style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f), textAlign = TextAlign.Center)
            }
        }
        FixtureTeam(match.awayTeam, match.awayCrest, Modifier.weight(1f))
    }
}

@Composable
private fun FixtureTeam(name: String, crest: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FootballBadge(crest, name, Modifier.size(40.dp))
        Text(name, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.65f),
            textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
