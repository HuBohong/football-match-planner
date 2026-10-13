package au.edu.jcu.assessment.utilityapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import au.edu.jcu.assessment.utilityapp.data.FootballRepository
import au.edu.jcu.assessment.utilityapp.data.FootballResponseCache
import au.edu.jcu.assessment.utilityapp.data.TeamPreferencesRepository
import au.edu.jcu.assessment.utilityapp.data.remote.FootballApiFactory
import au.edu.jcu.assessment.utilityapp.ui.FootballUiState
import au.edu.jcu.assessment.utilityapp.ui.FootballViewModel
import au.edu.jcu.assessment.utilityapp.ui.GlassPanel
import au.edu.jcu.assessment.utilityapp.ui.GradientBackground
import au.edu.jcu.assessment.utilityapp.ui.HomeScreen
import au.edu.jcu.assessment.utilityapp.ui.LocalGradientBackground
import au.edu.jcu.assessment.utilityapp.ui.ScheduleStatusScreen
import au.edu.jcu.assessment.utilityapp.ui.SettingsScreen
import au.edu.jcu.assessment.utilityapp.ui.TeamPreferencesViewModel
import au.edu.jcu.assessment.utilityapp.ui.theme.MobileTech_UtilityAppTheme
import au.edu.jcu.assessment.utilityapp.ui.theme.ThemeMode
import java.io.File
import java.time.ZoneId

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val teamFactory = TeamPreferencesViewModel.Factory(TeamPreferencesRepository.from(this))
        val footballFactory = FootballViewModel.Factory(
            repository = FootballRepository(
                api = FootballApiFactory.create(BuildConfig.FOOTBALL_API_TOKEN),
                cache = FootballResponseCache(File(filesDir, "football-cache"))
            ),
            hasToken = BuildConfig.FOOTBALL_API_TOKEN.isNotBlank()
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
                enableEdgeToEdge(
                    statusBarStyle = barStyle,
                    navigationBarStyle = barStyle
                )
            }
            MobileTech_UtilityAppTheme(darkTheme = darkTheme) {
                val teamViewModel: TeamPreferencesViewModel = viewModel(factory = teamFactory)
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
                        } else {
                            HomeScreen(
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
}

@Composable
private fun ScheduleLoadScreen(state: FootballUiState, onRetry: () -> Unit) {
    ScheduleStatusScreen(
        title = if (state.isLoading) "Getting your schedule" else "Schedule unavailable",
        message = if (state.isLoading) state.loadingMessage else
            state.errorMessage ?: "Your schedule hasn't loaded yet. Try again to fetch matches.",
        isLoading = state.isLoading,
        actionLabel = "Try again",
        onAction = onRetry
    )
}
