package au.edu.jcu.assessment.utilityapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import au.edu.jcu.assessment.utilityapp.UtilityApp
import au.edu.jcu.assessment.utilityapp.data.FootballSchedule
import au.edu.jcu.assessment.utilityapp.ui.theme.MobileTech_UtilityAppTheme
import java.time.Instant

// Debug-only previews: inspect every state without credentials or API requests.
private val emptyPreviewSchedule = FootballSchedule(
    fixtures = emptyList(),
    teamsByLeague = mapOf("Premier League" to listOf("Arsenal", "Chelsea")),
    fetchedAt = Instant.parse("2026-10-11T00:00:00Z")
)

@Preview(name = "Loading and quota wait", showBackground = true)
@Composable
fun LoadingSchedulePreview() {
    MobileTech_UtilityAppTheme {
        UtilityApp(footballState = FootballUiState(
            loadingMessage = "The football service is busy. Waiting about 65 seconds, then continuing automatically…"
        ))
    }
}

@Preview(name = "Connection error and retry", showBackground = true)
@Composable
fun UnavailableSchedulePreview() {
    var retrying by remember { mutableStateOf(false) }
    MobileTech_UtilityAppTheme {
        UtilityApp(
            footballState = if (retrying) FootballUiState() else FootballUiState(
                isLoading = false,
                errorMessage = "Could not load matches. Check your connection and retry."
            ),
            onRetrySchedule = { retrying = true }
        )
    }
}

@Preview(name = "First-time team selection", showBackground = true)
@Composable
fun ChooseTeamsPreview() {
    var followedTeams by remember { mutableStateOf(emptyList<String>()) }
    MobileTech_UtilityAppTheme {
        UtilityApp(
            followedTeams = followedTeams,
            onTeamsSelected = { followedTeams = it },
            footballState = FootballUiState(schedule = emptyPreviewSchedule, isLoading = false)
        )
    }
}

@Preview(name = "Empty period and recovery", showBackground = true)
@Composable
fun EmptySchedulePreview() {
    var followedTeams by remember { mutableStateOf(listOf("Arsenal")) }
    MobileTech_UtilityAppTheme {
        UtilityApp(
            followedTeams = followedTeams,
            onTeamsSelected = { followedTeams = it },
            footballState = FootballUiState(schedule = emptyPreviewSchedule, isLoading = false)
        )
    }
}

@Preview(name = "Saved-team error with Settings available", showBackground = true)
@Composable
fun SavedTeamsUnavailablePreview() {
    MobileTech_UtilityAppTheme {
        UtilityApp(followedTeams = null, errorMessage = "Could not read saved teams. Please retry.")
    }
}
