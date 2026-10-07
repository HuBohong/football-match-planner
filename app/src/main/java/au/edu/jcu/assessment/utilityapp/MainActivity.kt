package au.edu.jcu.assessment.utilityapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import au.edu.jcu.assessment.utilityapp.data.FootballResponseCache
import au.edu.jcu.assessment.utilityapp.data.FootballRepository
import au.edu.jcu.assessment.utilityapp.data.FootballSchedule
import au.edu.jcu.assessment.utilityapp.data.remote.FootballApiFactory
import au.edu.jcu.assessment.utilityapp.ui.FootballUiState
import au.edu.jcu.assessment.utilityapp.ui.FootballViewModel
import au.edu.jcu.assessment.utilityapp.data.TeamPreferencesRepository
import au.edu.jcu.assessment.utilityapp.data.MatchFixture
import au.edu.jcu.assessment.utilityapp.data.MatchStatus
import au.edu.jcu.assessment.utilityapp.data.SchedulePeriod
import au.edu.jcu.assessment.utilityapp.data.matchesPeriod
import au.edu.jcu.assessment.utilityapp.data.groupFixturesByLocalDate
import au.edu.jcu.assessment.utilityapp.ui.TeamPreferencesViewModel
import au.edu.jcu.assessment.utilityapp.ui.theme.MobileTech_UtilityAppTheme
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val factory = TeamPreferencesViewModel.Factory(TeamPreferencesRepository.from(this))
        val footballFactory = FootballViewModel.Factory(
            FootballRepository(FootballApiFactory.create(BuildConfig.FOOTBALL_API_TOKEN),
                cache = FootballResponseCache(File(filesDir, "football-cache"))),
            BuildConfig.FOOTBALL_API_TOKEN.isNotBlank()
        )
        setContent {
            MobileTech_UtilityAppTheme {
                val teamViewModel: TeamPreferencesViewModel = viewModel(factory = factory)
                val preferences by teamViewModel.uiState.collectAsStateWithLifecycle()
                val footballViewModel: FootballViewModel = viewModel(factory = footballFactory)
                val footballState by footballViewModel.uiState.collectAsStateWithLifecycle()
                UtilityApp(
                    followedTeams = preferences.followedTeams,
                    onTeamsSelected = teamViewModel::saveTeams,
                    isSaving = preferences.isSaving,
                    errorMessage = preferences.errorMessage,
                    onRetry = teamViewModel::loadTeams,
                    footballState = footballState,
                    onRetrySchedule = footballViewModel::loadSchedule
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun UtilityAppPreview() {
    MobileTech_UtilityAppTheme {
        UtilityApp()
    }
}

@Composable
fun UtilityApp(
    followedTeams: List<String>? = emptyList(),
    onTeamsSelected: (List<String>) -> Unit = {},
    isSaving: Boolean = false,
    errorMessage: String? = null,
    onRetry: () -> Unit = {},
    footballState: FootballUiState = FootballUiState(isLoading = false),
    onRetrySchedule: () -> Unit = {}
) {
    var selectedTab by rememberSaveable { mutableStateOf("Utility") }
    // Kept above the screen so changing tabs does not discard the filter.
    var selectedCompetition by rememberSaveable { mutableStateOf("All competitions") }
    var use24HourFormat by rememberSaveable { mutableStateOf(true) }
    var selectedTimezone by rememberSaveable { mutableStateOf("Device") }
    val displayZone = if (selectedTimezone == "Device") ZoneId.systemDefault() else ZoneId.of(selectedTimezone)

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Utility") },
                    label = { Text("Utility") },
                    selected = selectedTab == "Utility",
                    onClick = { selectedTab = "Utility" }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    selected = selectedTab == "Settings",
                    onClick = { selectedTab = "Settings" }
                )
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (errorMessage != null) {
                Text(
                    errorMessage,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (followedTeams == null || isSaving) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (errorMessage == null) {
                            CircularProgressIndicator()
                            Text(if (isSaving) "Saving teams…" else "Loading saved teams…")
                        } else {
                            Button(onClick = onRetry) { Text("Retry") }
                        }
                    }
                }
            } else {
                when (selectedTab) {
                    "Utility" -> if (footballState.schedule == null) {
                        ScheduleLoadScreen(footballState, onRetrySchedule)
                    } else UtilityScreen(
                        selectedCompetition = selectedCompetition,
                        onCompetitionSelected = { selectedCompetition = it },
                        followedTeams = followedTeams,
                        onTeamsSelected = onTeamsSelected,
                        use24HourFormat = use24HourFormat,
                        displayZone = displayZone,
                        fixtures = footballState.schedule.fixtures,
                        teamsByLeague = footballState.schedule.teamsByLeague,
                        competitions = footballState.schedule.competitions
                    )
                    "Settings" -> SettingsScreen(
                        use24HourFormat = use24HourFormat,
                        onTimeFormatChanged = { use24HourFormat = it },
                        selectedTimezone = selectedTimezone,
                        onTimezoneChanged = { selectedTimezone = it },
                        displayZone = displayZone,
                        selectedCompetition = selectedCompetition,
                        followedTeams = followedTeams,
                        schedule = footballState.schedule
                    )
                }
            }
        }
    }
}

@Composable
fun ScheduleLoadScreen(state: FootballUiState, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (state.isLoading) {
                CircularProgressIndicator()
                Text(state.loadingMessage, textAlign = TextAlign.Center)
            } else {
                Text(state.errorMessage ?: "No schedule loaded.", textAlign = TextAlign.Center)
                Button(onClick = onRetry) { Text("Retry") }
            }
        }
    }
}

@Composable
fun UtilityScreen(
    selectedCompetition: String,
    onCompetitionSelected: (String) -> Unit,
    followedTeams: List<String>,
    onTeamsSelected: (List<String>) -> Unit,
    use24HourFormat: Boolean,
    displayZone: ZoneId,
    fixtures: List<MatchFixture>,
    teamsByLeague: Map<String, List<String>>,
    competitions: List<String>
) {
    var showFilterDialog by rememberSaveable { mutableStateOf(false) }
    var showTeamsDialog by rememberSaveable { mutableStateOf(false) }
    val now = Instant.now()
    val today = now.atZone(displayZone).toLocalDate()
    var selectedPeriod by rememberSaveable { mutableStateOf(SchedulePeriod.UPCOMING) }
    val selectedDate = if (selectedPeriod == SchedulePeriod.YESTERDAY) today.minusDays(1) else today
    val visibleFixtures = fixtures.filter { match ->
        val matchesDate = match.matchesPeriod(selectedPeriod, now, displayZone)
        val matchesCompetition = selectedCompetition == "All competitions" ||
            match.competition == selectedCompetition
        val matchesTeam = match.homeTeam in followedTeams || match.awayTeam in followedTeams
        matchesDate && matchesCompetition && matchesTeam
    }
    val fixturesByDate = groupFixturesByLocalDate(visibleFixtures, displayZone)

    if (followedTeams.isEmpty()) {
        ChooseTeamsPrompt(onChooseTeams = { showTeamsDialog = true })
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Football Schedule", style = MaterialTheme.typography.headlineMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(onClick = { showTeamsDialog = true }) {
                            Text("My teams (${followedTeams.size})")
                        }
                        OutlinedButton(onClick = { showFilterDialog = true }) {
                            Text("Filter")
                        }
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SchedulePeriod.entries.forEach { period ->
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = selectedPeriod == period,
                            onClick = { selectedPeriod = period },
                            label = {
                                Text(
                                    period.label,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        )
                    }
                }
            }
            if (visibleFixtures.isEmpty()) {
                if (selectedPeriod != SchedulePeriod.UPCOMING) {
                    item {
                        DateHeader(selectedDate, today)
                    }
                }
                item {
                    Text("No matches for these filters.", style = MaterialTheme.typography.bodyLarge)
                }
            }
            fixturesByDate.forEach { (date, matches) ->
                item(key = "date-$date") {
                    DateHeader(date, today)
                }
                items(matches) { match ->
                    MatchCard(match, use24HourFormat, displayZone)
                }
            }
        }
    }
    if (showFilterDialog) {
        CompetitionFilterDialog(
            selectedCompetition = selectedCompetition,
            competitions = competitions,
            onCompetitionSelected = {
                onCompetitionSelected(it)
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false }
        )
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
fun ChooseTeamsPrompt(onChooseTeams: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Football Schedule", style = MaterialTheme.typography.headlineMedium)
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Follow your favourite teams",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Choose teams to see their matches together in your schedule.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Button(onClick = onChooseTeams) { Text("Choose my teams") }
            }
        }
    }
}

@Composable
fun MyTeamsDialog(
    followedTeams: List<String>,
    teamsByLeague: Map<String, List<String>>,
    onSave: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    // Edit a draft so Cancel/Back cannot change the active schedule filter.
    var draftTeams by rememberSaveable { mutableStateOf(ArrayList(followedTeams)) }
    var expandedCompetition by rememberSaveable { mutableStateOf<String?>(null) }
    val unavailableTeams = followedTeams - teamsByLeague.values.flatten().toSet()
    val selectableTeams = teamsByLeague + if (unavailableTeams.isEmpty()) emptyMap()
        else mapOf("Previously followed · not in current coverage" to unavailableTeams)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("My teams") },
        confirmButton = {
            TextButton(onClick = { onSave(draftTeams.distinct()) }) { Text("Done") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Choose a competition, then select teams. A team followed in one list is followed in every list.")
                selectableTeams.forEach { (league, teams) ->
                    OutlinedButton(
                        onClick = {
                            expandedCompetition = if (expandedCompetition == league) null else league
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(league, modifier = Modifier.semantics { heading() },
                                style = MaterialTheme.typography.titleSmall)
                            Text("${teams.size} teams · ${teams.count { it in draftTeams }} followed",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        Text(if (expandedCompetition == league) "Hide" else "Choose")
                    }
                    if (expandedCompetition == league) {
                        teams.forEach { team ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .toggleable(
                                        value = team in draftTeams,
                                        role = Role.Checkbox,
                                        onValueChange = { checked ->
                                            draftTeams = ArrayList(
                                                if (checked) (draftTeams + team).distinct() else draftTeams - team
                                            )
                                        }
                                    )
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Checkbox(checked = team in draftTeams, onCheckedChange = null)
                                Text(team, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        }
    )
}

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
        modifier = Modifier.fillMaxWidth().semantics { heading() },
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center
    )
}

@Composable
fun MatchCard(match: MatchFixture, use24HourFormat: Boolean, displayZone: ZoneId) {
    val displayedTime = if (match.status == MatchStatus.SCHEDULED) "Time TBC" else match.kickoff.atZone(displayZone).format(
        DateTimeFormatter.ofPattern(if (use24HourFormat) "HH:mm" else "h:mm a", Locale.ENGLISH)
    )
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$displayedTime · ${match.competition}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = match.homeTeam,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (match.status in setOf(MatchStatus.FINISHED, MatchStatus.IN_PLAY, MatchStatus.PAUSED, MatchStatus.AWARDED)) {
                        "${match.homeScore ?: "–"} - ${match.awayScore ?: "–"}"
                    } else "VS",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = match.awayTeam,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            }
            Text(
                text = match.status.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CompetitionFilterDialog(
    selectedCompetition: String,
    competitions: List<String>,
    onCompetitionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter by competition") },
            confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        text = {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).selectableGroup()
        ) {
            (listOf("All competitions") + competitions)
                .forEach { competition ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .selectable(
                                selected = selectedCompetition == competition,
                                role = Role.RadioButton,
                                onClick = { onCompetitionSelected(competition) }
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = selectedCompetition == competition,
                            onClick = null
                        )
                        Text(competition, style = MaterialTheme.typography.bodyLarge)
                    }
                }
        }
        }
    )
}

@Composable
fun SettingsScreen(
    use24HourFormat: Boolean,
    onTimeFormatChanged: (Boolean) -> Unit,
    selectedTimezone: String,
    onTimezoneChanged: (String) -> Unit,
    displayZone: ZoneId,
    selectedCompetition: String,
    followedTeams: List<String>,
    schedule: FootballSchedule?
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Schedule information", style = MaterialTheme.typography.titleMedium)
                Text("Data provided by football-data.org", style = MaterialTheme.typography.bodyMedium)
                Text("Current coverage: " + (schedule?.competitions?.joinToString() ?: "Not loaded"),
                    style = MaterialTheme.typography.bodyMedium)
                Text("Free-plan scores may be delayed. Kickoff times can change.", style = MaterialTheme.typography.bodyMedium)
                Text("Schedules are cached for up to 3 hours; updates are checked when you reopen the app.",
                    style = MaterialTheme.typography.bodyMedium)
                schedule?.let {
                    Text("Updated: " + it.fetchedAt.atZone(displayZone).format(
                        DateTimeFormatter.ofPattern("d MMM yyyy HH:mm", Locale.ENGLISH)))
                }
                Text("All times: ${displayZone.id}", style = MaterialTheme.typography.bodyMedium)
                Text("Showing: $selectedCompetition", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "Following: ${followedTeams.joinToString().ifEmpty { "No teams selected" }}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .toggleable(
                    value = use24HourFormat,
                    role = Role.Switch,
                    onValueChange = onTimeFormatChanged
                )
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Use 24-hour time", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (use24HourFormat) "Example: 20:30" else "Example: 8:30 PM",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Switch(checked = use24HourFormat, onCheckedChange = null)
        }
        Text("Timezone", style = MaterialTheme.typography.titleMedium)
        Text("Kickoff times and date headings use the selected timezone.")
        Column(modifier = Modifier.selectableGroup()) {
            listOf(
                "Device" to "Phone timezone (${ZoneId.systemDefault().id})",
                "Asia/Shanghai" to "Beijing · Asia/Shanghai",
                "Australia/Brisbane" to "Brisbane · Australia/Brisbane",
                "Europe/London" to "London · Europe/London",
                "UTC" to "UTC"
            ).forEach { (zoneId, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .selectable(
                            selected = selectedTimezone == zoneId,
                            role = Role.RadioButton,
                            onClick = { onTimezoneChanged(zoneId) }
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RadioButton(selected = selectedTimezone == zoneId, onClick = null)
                    Text(label, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
