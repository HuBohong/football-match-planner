package au.edu.jcu.assessment.utilityapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import au.edu.jcu.assessment.utilityapp.data.TeamPreferencesRepository
import au.edu.jcu.assessment.utilityapp.ui.TeamPreferencesViewModel
import au.edu.jcu.assessment.utilityapp.ui.theme.MobileTech_UtilityAppTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val factory = TeamPreferencesViewModel.Factory(TeamPreferencesRepository.from(this))
        setContent {
            MobileTech_UtilityAppTheme {
                val teamViewModel: TeamPreferencesViewModel = viewModel(factory = factory)
                val preferences by teamViewModel.uiState.collectAsStateWithLifecycle()
                UtilityApp(
                    followedTeams = preferences.followedTeams,
                    onTeamsSelected = teamViewModel::saveTeams,
                    isSaving = preferences.isSaving,
                    errorMessage = preferences.errorMessage,
                    onRetry = teamViewModel::loadTeams
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
    onRetry: () -> Unit = {}
) {
    var selectedTab by rememberSaveable { mutableStateOf("Utility") }
    // Kept above the screen so changing tabs does not discard the filter.
    var selectedCompetition by rememberSaveable { mutableStateOf("All competitions") }
    var use24HourFormat by rememberSaveable { mutableStateOf(true) }

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
                    "Utility" -> UtilityScreen(
                        selectedCompetition = selectedCompetition,
                        onCompetitionSelected = { selectedCompetition = it },
                        followedTeams = followedTeams,
                        onTeamsSelected = onTeamsSelected,
                        use24HourFormat = use24HourFormat
                    )
                    "Settings" -> SettingsScreen(
                        use24HourFormat = use24HourFormat,
                        onTimeFormatChanged = { use24HourFormat = it }
                    )
                }
            }
        }
    }
}

// Display-only sample data; these are not real fixtures or converted kickoff times.
data class MatchFixture(
    val competition: String,
    val homeTeam: String,
    val awayTeam: String,
    val kickoffTime: String,
    val date: LocalDate
)

// Relative dates keep the fictional fixtures useful whenever the demo is opened.
private fun createDemoFixtures(today: LocalDate) = listOf(
    MatchFixture("Premier League", "Arsenal", "Chelsea", "20:30", today),
    MatchFixture("La Liga", "Real Madrid", "Atletico Madrid", "22:00", today),
    MatchFixture("Champions League", "Barcelona", "Bayern Munich", "03:00", today.plusDays(1))
)

private val demoTeamsByLeague = linkedMapOf(
    "Premier League" to listOf("Arsenal", "Chelsea"),
    "La Liga" to listOf("Atletico Madrid", "Barcelona", "Real Madrid"),
    "Bundesliga" to listOf("Bayern Munich")
)

@Composable
fun UtilityScreen(
    selectedCompetition: String,
    onCompetitionSelected: (String) -> Unit,
    followedTeams: List<String>,
    onTeamsSelected: (List<String>) -> Unit,
    use24HourFormat: Boolean
) {
    var showFilterDialog by rememberSaveable { mutableStateOf(false) }
    var showTeamsDialog by rememberSaveable { mutableStateOf(false) }
    val today = LocalDate.now()
    val demoFixtures = remember(today) { createDemoFixtures(today) }
    // -1 selects the upcoming list; 0, 1, and 2 select an individual day.
    var selectedDayOffset by rememberSaveable { mutableIntStateOf(-1) }
    val selectedDate = today.plusDays(selectedDayOffset.toLong())
    val visibleFixtures = demoFixtures.filter { match ->
        val matchesDate = if (selectedDayOffset == -1) {
            !match.date.isBefore(today)
        } else {
            match.date == selectedDate
        }
        val matchesCompetition = selectedCompetition == "All competitions" ||
            match.competition == selectedCompetition
        val matchesTeam = match.homeTeam in followedTeams || match.awayTeam in followedTeams
        matchesDate && matchesCompetition && matchesTeam
    }
    val fixturesByDate = visibleFixtures
        .sortedWith(compareBy<MatchFixture> { it.date }.thenBy { it.kickoffTime })
        .groupBy { it.date }

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
                    Text("Demo fixtures · Sample times", style = MaterialTheme.typography.bodyMedium)
                    Text("Showing: $selectedCompetition", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "Following: ${followedTeams.joinToString()}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(-1 to "Upcoming", 0 to "Today", 1 to "Tomorrow", 2 to "+2 days")
                        .forEach { (offset, label) ->
                        FilterChip(
                            selected = selectedDayOffset == offset,
                            onClick = { selectedDayOffset = offset },
                            label = { Text(label) }
                        )
                    }
                }
            }
            if (visibleFixtures.isEmpty()) {
                if (selectedDayOffset != -1) {
                    item {
                        DateHeader(selectedDate, today)
                    }
                }
                item {
                    Text("No demo matches match these filters.", style = MaterialTheme.typography.bodyLarge)
                }
            }
            fixturesByDate.forEach { (date, matches) ->
                item(key = "date-$date") {
                    DateHeader(date, today)
                }
                items(matches) { match ->
                    MatchCard(match, use24HourFormat)
                }
            }
        }
    }
    if (showFilterDialog) {
        CompetitionFilterDialog(
            selectedCompetition = selectedCompetition,
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
        Text("Demo fixtures · Sample times", style = MaterialTheme.typography.bodyMedium)
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
    onSave: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    // Edit a draft so Cancel/Back cannot change the active schedule filter.
    var draftTeams by rememberSaveable { mutableStateOf(ArrayList(followedTeams)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("My teams · Demo") },
        confirmButton = {
            TextButton(onClick = { onSave(draftTeams) }) { Text("Done") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Choose teams to follow. Selecting none returns to the team selection prompt.")
                demoTeamsByLeague.forEach { (league, teams) ->
                    Text(
                        text = league,
                        modifier = Modifier.padding(top = 16.dp).semantics { heading() },
                        style = MaterialTheme.typography.titleSmall
                    )
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
                                            if (checked) draftTeams + team else draftTeams - team
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
    )
}

@Composable
fun DateHeader(date: LocalDate, today: LocalDate) {
    val relativeLabel = when (date) {
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
fun MatchCard(match: MatchFixture, use24HourFormat: Boolean) {
    val displayedTime = LocalTime.parse(match.kickoffTime).format(
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
                    text = "VS",
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
        }
    }
}

@Composable
fun CompetitionFilterDialog(
    selectedCompetition: String,
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
            listOf("All competitions", "Premier League", "La Liga", "Champions League")
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
    onTimeFormatChanged: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
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
    }
}
