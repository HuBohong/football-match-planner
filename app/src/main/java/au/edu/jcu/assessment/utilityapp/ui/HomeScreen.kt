package au.edu.jcu.assessment.utilityapp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.edu.jcu.assessment.utilityapp.data.MatchFixture
import au.edu.jcu.assessment.utilityapp.data.SchedulePeriod
import au.edu.jcu.assessment.utilityapp.data.groupFixturesByLocalDate
import au.edu.jcu.assessment.utilityapp.data.matchesPeriod
import java.time.Instant
import java.time.ZoneId

@Composable
fun HomeScreen(
    followedTeams: List<String>,
    onTeamsSelected: (List<String>) -> Unit,
    use24HourFormat: Boolean,
    displayZone: ZoneId,
    fixtures: List<MatchFixture>,
    teamsByLeague: Map<String, List<String>>
) {
    var showTeamsDialog by rememberSaveable { mutableStateOf(false) }
    var showTeamsMenu by rememberSaveable { mutableStateOf(false) }
    val pageLayer = rememberGraphicsLayer()
    var pageOrigin by remember { mutableStateOf(Offset.Zero) }
    BackHandler(enabled = showTeamsMenu) { showTeamsMenu = false }
    val now = Instant.now()
    val today = now.atZone(displayZone).toLocalDate()
    var selectedPeriod by rememberSaveable { mutableStateOf(SchedulePeriod.UPCOMING) }
    val selectedDate = if (selectedPeriod == SchedulePeriod.YESTERDAY) today.minusDays(1) else today
    val visibleFixtures = fixtures.filter { match ->
        val matchesDate = match.matchesPeriod(selectedPeriod, now, displayZone)
        val matchesTeam = match.homeTeam in followedTeams || match.awayTeam in followedTeams
        matchesDate && matchesTeam
    }
    val fixturesByDate = groupFixturesByLocalDate(visibleFixtures, displayZone)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val availableHeight = maxHeight
        Box(Modifier.fillMaxSize().onGloballyPositioned { pageOrigin = it.positionInRoot() }
            .drawWithContent {
                pageLayer.record { this@drawWithContent.drawContent() }
                drawLayer(pageLayer)
            }) {
            if (followedTeams.isEmpty()) {
                ChooseTeamsPrompt(onChooseTeams = { showTeamsDialog = true })
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Football Schedule",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleLarge,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                        // Reserve the collapsed button's space; the expanding card is an overlay.
                        Box(Modifier.size(48.dp))
                    }
                    // One continuous panel: only the fixtures scroll beneath the period controls.
                    GlassPanel(
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(bottom = 16.dp)
                    ) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                                SchedulePeriod.entries.forEach { period ->
                                    val active = selectedPeriod == period
                                    TextButton(
                                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                            .semantics { selected = active },
                                        onClick = { selectedPeriod = period },
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                        colors = ButtonDefaults.textButtonColors(
                                            containerColor = Color.Transparent,
                                            contentColor = if (active) Color.White else Color.White.copy(alpha = 0.55f)
                                        )
                                    ) {
                                        Text(
                                            period.label,
                                            fontSize = 16.sp,
                                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp),
                                color = Color.White.copy(alpha = 0.2f))
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                            ) {
                                if (visibleFixtures.isEmpty()) {
                                    if (selectedPeriod != SchedulePeriod.UPCOMING) {
                                        item { DateHeader(selectedDate, today) }
                                    }
                                    item {
                                        val upcoming = selectedPeriod == SchedulePeriod.UPCOMING
                                        ScheduleStatusContent(
                                            title = when (selectedPeriod) {
                                                SchedulePeriod.YESTERDAY -> "No matches yesterday"
                                                SchedulePeriod.TODAY -> "No matches today"
                                                SchedulePeriod.UPCOMING -> "No upcoming matches"
                                            },
                                            message = if (upcoming) {
                                                "There are no upcoming fixtures for your teams in the current schedule. You can choose more teams."
                                            } else {
                                                "There are no matches for your teams on this date. Check Upcoming for their next fixtures."
                                            },
                                            actionLabel = if (upcoming) "Edit teams" else "View upcoming",
                                            onAction = {
                                                if (upcoming) showTeamsDialog = true
                                                else selectedPeriod = SchedulePeriod.UPCOMING
                                            }
                                        )
                                    }
                                }
                                fixturesByDate.forEach { (date, matches) ->
                                    item(key = "date-$date") { DateHeader(date, today) }
                                    matches.groupBy { it.competition }.forEach { (competition, competitionMatches) ->
                                        item(key = "competition-$date-$competition") {
                                            CompetitionHeader(competition, competitionMatches.first().competitionEmblem)
                                        }
                                        items(competitionMatches) { match ->
                                            MatchRow(match, use24HourFormat, displayZone)
                                            HorizontalDivider(color = Color.White.copy(alpha = 0.16f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (showTeamsMenu) {
            Box(Modifier.matchParentSize().pointerInput(Unit) {
                detectTapGestures { showTeamsMenu = false }
            })
        }
        if (followedTeams.isNotEmpty()) {
            TeamsMenuPanel(
                expanded = showTeamsMenu,
                onToggle = { showTeamsMenu = !showTeamsMenu },
                onEditTeams = {
                    showTeamsMenu = false
                    showTeamsDialog = true
                },
                teamsByLeague = teamsByLeague,
                followedTeams = followedTeams,
                fixtures = fixtures,
                backdrop = pageLayer,
                backdropOrigin = pageOrigin,
                availableHeight = availableHeight,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 24.dp, end = 24.dp)
            )
        }
    }
    if (showTeamsDialog) {
        MyTeamsDialog(
            followedTeams = followedTeams,
            teamsByLeague = teamsByLeague,
            onSave = {
                onTeamsSelected(it)
                showTeamsDialog = false
            },
            onDismiss = { showTeamsDialog = false }
        )
    }
}

@Composable
private fun ChooseTeamsPrompt(onChooseTeams: () -> Unit) {
    ScheduleStatusScreen(
        title = "Follow your favourite teams",
        message = "Choose teams to bring their fixtures together, with kickoff times in your chosen timezone.",
        actionLabel = "Choose my teams",
        onAction = onChooseTeams
    )
}
