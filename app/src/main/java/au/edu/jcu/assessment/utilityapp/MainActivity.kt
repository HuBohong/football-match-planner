package au.edu.jcu.assessment.utilityapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import au.edu.jcu.assessment.utilityapp.data.FootballRepository
import au.edu.jcu.assessment.utilityapp.data.FootballResponseCache
import au.edu.jcu.assessment.utilityapp.data.FootballSchedule
import au.edu.jcu.assessment.utilityapp.data.TeamPreferencesRepository
import au.edu.jcu.assessment.utilityapp.data.remote.FootballApiFactory
import au.edu.jcu.assessment.utilityapp.ui.FootballUiState
import au.edu.jcu.assessment.utilityapp.ui.FootballViewModel
import au.edu.jcu.assessment.utilityapp.ui.GlassPanel
import au.edu.jcu.assessment.utilityapp.ui.GradientBackground
import au.edu.jcu.assessment.utilityapp.ui.HomeScreen
import au.edu.jcu.assessment.utilityapp.ui.LocalGradientBackground
import au.edu.jcu.assessment.utilityapp.ui.MorphingThemeIcon
import au.edu.jcu.assessment.utilityapp.ui.ScheduleStatusScreen
import au.edu.jcu.assessment.utilityapp.ui.TeamPreferencesViewModel
import au.edu.jcu.assessment.utilityapp.ui.theme.MobileTech_UtilityAppTheme
import au.edu.jcu.assessment.utilityapp.ui.theme.ThemeMode
import java.io.File
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
                        } else HomeScreen(
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
