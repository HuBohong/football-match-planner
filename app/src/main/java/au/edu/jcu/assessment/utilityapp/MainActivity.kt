package au.edu.jcu.assessment.utilityapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.edu.jcu.assessment.utilityapp.ui.theme.MobileTech_UtilityAppTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MobileTech_UtilityAppTheme {
                UtilityApp()
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
fun UtilityApp() {
    var selectedTab by remember { mutableStateOf("Utility") }

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
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                "Utility" -> UtilityScreen()
                "Settings" -> SettingsScreen()
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

@Composable
fun UtilityScreen() {
    val today = LocalDate.now()
    val demoFixtures = remember(today) { createDemoFixtures(today) }
    // -1 selects the upcoming list; 0, 1, and 2 select an individual day.
    var selectedDayOffset by rememberSaveable { mutableIntStateOf(-1) }
    val selectedDate = today.plusDays(selectedDayOffset.toLong())
    val visibleFixtures = demoFixtures.filter {
        if (selectedDayOffset == -1) !it.date.isBefore(today) else it.date == selectedDate
    }
    val fixturesByDate = visibleFixtures
        .sortedWith(compareBy<MatchFixture> { it.date }.thenBy { it.kickoffTime })
        .groupBy { it.date }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Football Schedule", style = MaterialTheme.typography.headlineMedium)
                Text("Demo fixtures · Sample times", style = MaterialTheme.typography.bodyMedium)
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
            item {
                DateHeader(selectedDate, today)
            }
            item {
                Text("No demo matches on this date.", style = MaterialTheme.typography.bodyLarge)
            }
        }
        fixturesByDate.forEach { (date, matches) ->
            item(key = "date-$date") {
                DateHeader(date, today)
            }
            items(matches) { match ->
                MatchCard(match)
            }
        }
    }
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
fun MatchCard(match: MatchFixture) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${match.kickoffTime} · ${match.competition}",
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
fun SettingsScreen() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp), Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings Screen", style = MaterialTheme.typography.headlineMedium)
        Text("This is where you can add toggles or preferences.")
    }
}
