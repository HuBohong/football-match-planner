package au.edu.jcu.assessment.utilityapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import au.edu.jcu.assessment.utilityapp.ui.FootballBadge
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import au.edu.jcu.assessment.utilityapp.data.FootballResponseCache
import au.edu.jcu.assessment.utilityapp.data.FootballRepository
import au.edu.jcu.assessment.utilityapp.data.FootballSchedule
import au.edu.jcu.assessment.utilityapp.data.remote.FootballApiFactory
import au.edu.jcu.assessment.utilityapp.ui.GlassPanel
import au.edu.jcu.assessment.utilityapp.ui.MorphingThemeIcon
import au.edu.jcu.assessment.utilityapp.ui.ScheduleStatusContent
import au.edu.jcu.assessment.utilityapp.ui.ScheduleStatusScreen
import au.edu.jcu.assessment.utilityapp.ui.TeamsMenuPanel
import au.edu.jcu.assessment.utilityapp.ui.GradientBackground
import au.edu.jcu.assessment.utilityapp.ui.LocalGradientBackground
import au.edu.jcu.assessment.utilityapp.ui.FootballUiState
import au.edu.jcu.assessment.utilityapp.ui.FootballViewModel
import au.edu.jcu.assessment.utilityapp.data.TeamPreferencesRepository
import au.edu.jcu.assessment.utilityapp.data.MatchFixture
import au.edu.jcu.assessment.utilityapp.data.MatchStatus
import au.edu.jcu.assessment.utilityapp.data.SchedulePeriod
import au.edu.jcu.assessment.utilityapp.data.matchesPeriod
import au.edu.jcu.assessment.utilityapp.data.groupFixturesByLocalDate
import au.edu.jcu.assessment.utilityapp.ui.TeamPreferencesViewModel
import au.edu.jcu.assessment.utilityapp.ui.theme.ThemeMode
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
            var themeMode by rememberSaveable { mutableStateOf(ThemeMode.SYSTEM) }
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // Status/navigation icons must remain readable when overriding the system theme.
            SideEffect {
                val barStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = barStyle,
                    navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
            }
            MobileTech_UtilityAppTheme(darkTheme = darkTheme) {
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
                    onRetrySchedule = footballViewModel::loadSchedule,
                    themeMode = themeMode,
                    onThemeModeChanged = { themeMode = it }
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
    onRetrySchedule: () -> Unit = {},
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeChanged: (ThemeMode) -> Unit = {}
) {
    var selectedTab by rememberSaveable { mutableStateOf("Utility") }
    var use24HourFormat by rememberSaveable { mutableStateOf(true) }
    var selectedTimezone by rememberSaveable { mutableStateOf("Device") }
    val displayZone = if (selectedTimezone == "Device") ZoneId.systemDefault() else ZoneId.of(selectedTimezone)

    // Sampled from the supplied Apple Sports screenshot; dark green fills the lower two thirds.
    val gradientStops = arrayOf(
        0f to Color(0xFF396832),
        0.34f to Color(0xFF182A15),
        1f to Color(0xFF182A15)
    )
    var backgroundHeight by remember { mutableStateOf(1f) }
    val navigationColors = NavigationBarItemDefaults.colors(
        selectedIconColor = Color.White,
        selectedTextColor = Color.White,
        indicatorColor = Color(0xFF4B8148),
        unselectedIconColor = Color.White.copy(alpha = 0.9f),
        unselectedTextColor = Color.White.copy(alpha = 0.9f)
    )
    CompositionLocalProvider(LocalGradientBackground provides GradientBackground(gradientStops, backgroundHeight)) {
        Box(modifier = Modifier.fillMaxSize().onSizeChanged { backgroundHeight = it.height.toFloat() }
            .background(Brush.verticalGradient(*gradientStops))) {
            Scaffold(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,
                bottomBar = {
                    NavigationBar(containerColor = Color.Transparent, tonalElevation = 0.dp) {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Home, contentDescription = null) },
                            label = { Text("Home") },
                            colors = navigationColors,
                            selected = selectedTab == "Utility",
                            onClick = { selectedTab = "Utility" }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            label = { Text("Settings") },
                            colors = navigationColors,
                            selected = selectedTab == "Settings",
                            onClick = { selectedTab = "Settings" }
                        )
                    }
                }
            ) { innerPadding ->
                Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    // Settings stays available even if Home cannot load saved teams or fixtures.
                    if (selectedTab == "Settings") {
                        SettingsScreen(
                            use24HourFormat = use24HourFormat,
                            onTimeFormatChanged = { use24HourFormat = it },
                            selectedTimezone = selectedTimezone,
                            onTimezoneChanged = { selectedTimezone = it },
                            displayZone = displayZone,
                            followedTeams = followedTeams,
                            schedule = footballState.schedule,
                            themeMode = themeMode,
                            onThemeModeChanged = onThemeModeChanged
                        )
                    } else if (followedTeams == null || isSaving) {
                        ScheduleStatusScreen(
                            title = when {
                                isSaving -> "Saving your teams"
                                errorMessage != null -> "Couldn't load your teams"
                                else -> "Loading your teams"
                            },
                            message = errorMessage ?: if (isSaving) {
                                "Your schedule will follow your new selection."
                            } else {
                                "Restoring the teams you follow."
                            },
                            isLoading = isSaving || errorMessage == null,
                            actionLabel = "Try again",
                            onAction = onRetry
                        )
                    } else {
                        if (errorMessage != null) {
                            GlassPanel(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(errorMessage, color = Color.White,
                                        style = MaterialTheme.typography.bodyMedium)
                                    TextButton(onClick = onRetry,
                                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                                        Text("Reload saved teams")
                                    }
                                }
                            }
                        }
                        if (footballState.schedule == null) {
                            ScheduleLoadScreen(footballState, onRetrySchedule)
                        } else UtilityScreen(
                            followedTeams = followedTeams,
                            onTeamsSelected = onTeamsSelected,
                            use24HourFormat = use24HourFormat,
                            displayZone = displayZone,
                            fixtures = footballState.schedule.fixtures,
                            teamsByLeague = footballState.schedule.teamsByLeague
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleLoadScreen(state: FootballUiState, onRetry: () -> Unit) {
    ScheduleStatusScreen(
        title = if (state.isLoading) "Getting your schedule" else "Schedule unavailable",
        message = if (state.isLoading) state.loadingMessage else
            state.errorMessage ?: "Your schedule hasn't loaded yet. Try again to fetch matches.",
        isLoading = state.isLoading,
        actionLabel = "Try again",
        onAction = onRetry
    )
}

@Composable
fun UtilityScreen(
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
              onEditTeams = { showTeamsMenu = false; showTeamsDialog = true },
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
fun ChooseTeamsPrompt(onChooseTeams: () -> Unit) {
    ScheduleStatusScreen(
        title = "Follow your favourite teams",
        message = "Choose teams to bring their fixtures together, with kickoff times in your chosen timezone.",
        actionLabel = "Choose my teams",
        onAction = onChooseTeams
    )
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
fun FixtureTeam(name: String, crest: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FootballBadge(crest, name, Modifier.size(40.dp))
        Text(name, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.65f),
            textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun SettingsScreen(
    use24HourFormat: Boolean,
    onTimeFormatChanged: (Boolean) -> Unit,
    selectedTimezone: String,
    onTimezoneChanged: (String) -> Unit,
    displayZone: ZoneId,
    followedTeams: List<String>?,
    schedule: FootballSchedule?,
    themeMode: ThemeMode,
    onThemeModeChanged: (ThemeMode) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.titleLarge, color = Color.White)
        ThemeSettingsCard(themeMode, onThemeModeChanged)
        SettingsGlassCard {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .toggleable(value = use24HourFormat, role = Role.Switch,
                        onValueChange = onTimeFormatChanged).padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Use 24-hour time", style = MaterialTheme.typography.titleMedium)
                    Text(if (use24HourFormat) "Example: 20:30" else "Example: 8:30 PM",
                        style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.75f))
                }
                Switch(checked = use24HourFormat, onCheckedChange = null,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF4B8148),
                        uncheckedThumbColor = Color.White.copy(alpha = 0.75f),
                        uncheckedTrackColor = Color.Transparent,
                        uncheckedBorderColor = Color.White.copy(alpha = 0.5f)
                    ))
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.16f))
            Text("Timezone", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp).semantics { heading() })
            Text("Kickoff times and dates follow this timezone.",
                style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.75f))
            Column(modifier = Modifier.selectableGroup()) {
                listOf(
                    "Device" to "Phone timezone (${ZoneId.systemDefault().id})",
                    "Asia/Shanghai" to "Beijing · Asia/Shanghai",
                    "Australia/Brisbane" to "Brisbane · Australia/Brisbane",
                    "Europe/London" to "London · Europe/London",
                    "UTC" to "UTC"
                ).forEach { (zoneId, label) ->
                    SettingsRadioRow(label, selectedTimezone == zoneId) { onTimezoneChanged(zoneId) }
                }
            }
        }
        SettingsGlassCard {
            Text("Schedule information", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() })
            Text("Data provided by football-data.org", style = MaterialTheme.typography.bodyMedium)
            Text("Current coverage: " + (schedule?.competitions?.joinToString() ?: "Not loaded"),
                style = MaterialTheme.typography.bodyMedium)
            Text("Free-plan scores may be delayed. Kickoff times can change.",
                style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.75f))
            Text("Schedules are cached for up to 3 hours; updates are checked when you reopen the app.",
                style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.75f))
            schedule?.let {
                Text("Updated: " + it.fetchedAt.atZone(displayZone).format(
                    DateTimeFormatter.ofPattern("d MMM yyyy HH:mm", Locale.ENGLISH)),
                    style = MaterialTheme.typography.bodyMedium)
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.16f))
            Text("All times: ${displayZone.id}", style = MaterialTheme.typography.bodyMedium)
            Text("Showing: All competitions", style = MaterialTheme.typography.bodyMedium)
            Text("Following: ${followedTeams?.joinToString()?.ifEmpty { "No teams selected" } ?: "Not loaded"}",
                style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun SettingsGlassCard(content: @Composable () -> Unit) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

@Composable
fun SettingsRadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RadioButton(selected = selected, onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = Color.White,
                unselectedColor = Color.White.copy(alpha = 0.6f)))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.White)
    }
}

@Composable
fun ThemeSettingsCard(themeMode: ThemeMode, onThemeModeChanged: (ThemeMode) -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    SettingsGlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Appearance", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() })
                Text(if (dark) "Dark theme" else "Light theme",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f))
            }
            IconToggleButton(
                checked = dark,
                onCheckedChange = { onThemeModeChanged(if (it) ThemeMode.DARK else ThemeMode.LIGHT) },
                modifier = Modifier.size(48.dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                    .semantics {
                        contentDescription = "Dark theme"
                        stateDescription = if (dark) "On" else "Off"
                    }
            ) {
                MorphingThemeIcon(dark = dark, modifier = Modifier.size(24.dp))
            }
        }
        if (themeMode == ThemeMode.SYSTEM) {
            Text("Following device setting", style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f))
        } else {
            TextButton(onClick = { onThemeModeChanged(ThemeMode.SYSTEM) },
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                Text("Use device setting")
            }
        }
    }
}
