package au.edu.jcu.assessment.utilityapp.data

import au.edu.jcu.assessment.utilityapp.data.remote.FootballApi
import au.edu.jcu.assessment.utilityapp.data.remote.MatchDto
import au.edu.jcu.assessment.utilityapp.data.remote.TeamsResponse
import au.edu.jcu.assessment.utilityapp.data.remote.MatchesResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
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
        FootballCompetition("PD", "La Liga"),
        FootballCompetition("BL1", "Bundesliga"),
        FootballCompetition("CL", "UEFA Champions League")
    ),
    private val cache: FootballResponseCache = FootballResponseCache(),
    private val waitForQuota: suspend (Long) -> Unit = { delay(it) }
) {
    suspend fun loadSchedule(onQuotaWait: (Long) -> Unit = {}): FootballSchedule = withContext(Dispatchers.IO) {
        val teamsByLeague = linkedMapOf<String, List<String>>()
        val fixtures = mutableListOf<MatchFixture>()
        val matchTimestamps = mutableListOf<Instant>()
        // Each successful response is cached immediately, so retries resume unfinished work.
        competitions.forEach { competition ->
            val teamsResponse = cachedRequest("${competition.code}-teams", TeamsResponse::class.java,
                Duration.ofHours(24), onQuotaWait) { api.getTeams(competition.code) }
            // Version 2 retains crest/emblem URLs omitted by older cached DTOs.
            val matchesResponse = cachedRequest("${competition.code}-matches-v2", MatchesResponse::class.java,
                Duration.ofHours(3), onQuotaWait) { api.getMatches(competition.code) }
            val teams = teamsResponse.value.teams.map { it.displayName }.distinct().sorted()
            val matches = matchesResponse.value.matches.mapNotNull {
                it.toFixture(competition.name)
            }
            fixtures += matches
            matchTimestamps += matchesResponse.fetchedAt
            // Keep each competition complete; selection is shared across its team rows.
            teamsByLeague[competition.name] = teams
        }
        FootballSchedule(fixtures, teamsByLeague, matchTimestamps.minOrNull() ?: Instant.now(), competitions.map { it.name })
    }

    private suspend fun <T> cachedRequest(
        key: String,
        type: Class<T>,
        maxAge: Duration,
        onQuotaWait: (Long) -> Unit,
        request: suspend () -> T
    ): CachedResponse<T> {
        cache.read(key, type, maxAge)?.let { return it }
        val value = try {
            request()
        } catch (error: HttpException) {
            if (error.code() != 429) throw error
            val seconds = retryAfterSeconds(error)
            onQuotaWait(seconds)
            waitForQuota(seconds * 1000)
            // Retry this endpoint once; previously loaded endpoints remain cached.
            request()
        }
        return cache.write(key, value)
    }

    private fun retryAfterSeconds(error: HttpException): Long {
        val header = error.response()?.headers()?.get("Retry-After")
        val seconds = header?.toLongOrNull() ?: try {
            header?.let {
                Duration.between(Instant.now(), ZonedDateTime.parse(it,
                    DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()).seconds
            }
        } catch (error: DateTimeParseException) {
            null
        }
        return seconds?.coerceIn(1, 86400) ?: 65L
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
        awayScore = score?.fullTime?.away,
        homeCrest = homeTeam?.crest,
        awayCrest = awayTeam?.crest,
        competitionEmblem = competition?.emblem
    )
}
