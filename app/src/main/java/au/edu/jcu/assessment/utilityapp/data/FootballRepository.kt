package au.edu.jcu.assessment.utilityapp.data

import au.edu.jcu.assessment.utilityapp.data.remote.FootballApi
import au.edu.jcu.assessment.utilityapp.data.remote.MatchDto
import java.time.Instant
import java.time.format.DateTimeParseException

data class FootballSchedule(
    val fixtures: List<MatchFixture>,
    val teamsByLeague: Map<String, List<String>>,
    val fetchedAt: Instant
)

class FootballRepository(private val api: FootballApi) {
    suspend fun loadPremierLeague(): FootballSchedule {
        val teams = api.getTeams("PL").teams.map { it.displayName }.distinct().sorted()
        val matches = api.getMatches("PL").matches.mapNotNull { it.toFixture() }
        return FootballSchedule(matches, mapOf("Premier League" to teams), Instant.now())
    }
}

fun MatchDto.toFixture(): MatchFixture? {
    // An unscheduled fixture without a usable timestamp cannot be assigned to a date.
    val kickoff = try {
        Instant.parse(utcDate ?: return null)
    } catch (error: DateTimeParseException) {
        return null
    }
    val matchStatus = MatchStatus.entries.firstOrNull { it.name == status } ?: MatchStatus.UNKNOWN
    return MatchFixture(
        competition = competition?.name ?: "Premier League",
        homeTeam = homeTeam?.displayName ?: "Team to be confirmed",
        awayTeam = awayTeam?.displayName ?: "Team to be confirmed",
        kickoff = kickoff,
        status = matchStatus,
        homeScore = score?.fullTime?.home,
        awayScore = score?.fullTime?.away
    )
}
