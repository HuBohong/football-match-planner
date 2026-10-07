package au.edu.jcu.assessment.utilityapp.data

import au.edu.jcu.assessment.utilityapp.data.remote.FootballApi
import au.edu.jcu.assessment.utilityapp.data.remote.MatchDto
import java.time.Instant
import java.time.format.DateTimeParseException

data class FootballSchedule(
    val fixtures: List<MatchFixture>,
    val teamsByLeague: Map<String, List<String>>,
    val fetchedAt: Instant,
    val competitions: List<String> = teamsByLeague.keys.toList()
)

data class FootballCompetition(val code: String, val name: String)

class FootballRepository(
    private val api: FootballApi,
    private val competitions: List<FootballCompetition> = listOf(
        FootballCompetition("PL", "Premier League"),
        FootballCompetition("SA", "Serie A"),
        FootballCompetition("FL1", "Ligue 1"),
        FootballCompetition("CL", "UEFA Champions League")
    )
) {
    suspend fun loadSchedule(): FootballSchedule {
        val teamsByLeague = linkedMapOf<String, List<String>>()
        val fixtures = mutableListOf<MatchFixture>()
        // Fetch sequentially: two requests per competition per load, without automatic polling.
        competitions.forEach { competition ->
            val teams = api.getTeams(competition.code).teams.map { it.displayName }.distinct().sorted()
            val matches = api.getMatches(competition.code).matches.mapNotNull {
                it.toFixture(competition.name)
            }
            fixtures += matches
            // Keep each competition complete; selection is shared across its team rows.
            teamsByLeague[competition.name] = teams
        }
        return FootballSchedule(fixtures, teamsByLeague, Instant.now(), competitions.map { it.name })
    }
}

fun MatchDto.toFixture(competitionName: String = "Premier League"): MatchFixture? {
    // An unscheduled fixture without a usable timestamp cannot be assigned to a date.
    val kickoff = try {
        Instant.parse(utcDate ?: return null)
    } catch (error: DateTimeParseException) {
        return null
    }
    val matchStatus = MatchStatus.entries.firstOrNull { it.name == status } ?: MatchStatus.UNKNOWN
    return MatchFixture(
        competition = competitionName,
        homeTeam = homeTeam?.displayName ?: "Team to be confirmed",
        awayTeam = awayTeam?.displayName ?: "Team to be confirmed",
        kickoff = kickoff,
        status = matchStatus,
        homeScore = score?.fullTime?.home,
        awayScore = score?.fullTime?.away
    )
}
