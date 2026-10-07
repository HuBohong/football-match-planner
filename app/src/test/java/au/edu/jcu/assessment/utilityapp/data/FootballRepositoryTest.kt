package au.edu.jcu.assessment.utilityapp.data

import au.edu.jcu.assessment.utilityapp.data.remote.FootballApiFactory
import java.time.Instant
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException

class FootballRepositoryTest {
    @Test
    fun loadsAuthenticatedTeamsAndMapsStatusesScoresAndUtcTimes() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setBody("""{"teams":[
                {"id":57,"name":"Arsenal FC","shortName":"Arsenal"},
                {"id":61,"name":"Chelsea FC","shortName":"Chelsea"}]}"""))
            server.enqueue(MockResponse().setBody("""{"matches":[
                {"utcDate":"2026-10-17T19:30:00Z","status":"TIMED",
                 "competition":{"name":"Premier League"},
                 "homeTeam":{"shortName":"Arsenal"},"awayTeam":{"shortName":"Chelsea"},
                 "score":{"fullTime":{"home":null,"away":null}}},
                {"utcDate":"2026-10-06T19:30:00Z","status":"FINISHED",
                 "homeTeam":{"shortName":"Chelsea"},"awayTeam":{"shortName":"Arsenal"},
                 "score":{"fullTime":{"home":2,"away":1}}},
                {"utcDate":"2026-10-18T19:30:00Z","status":"POSTPONED"},
                {"utcDate":null,"status":"SCHEDULED"}]}"""))
            val repository = FootballRepository(FootballApiFactory.create("test-token", server.url("/v4/").toString()),
                listOf(FootballCompetition("PL", "Premier League")))
            val result = repository.loadSchedule()
            assertEquals(listOf("Arsenal", "Chelsea"), result.teamsByLeague["Premier League"])
            assertEquals(3, result.fixtures.size)
            assertEquals(Instant.parse("2026-10-17T19:30:00Z"), result.fixtures[0].kickoff)
            assertEquals(MatchStatus.TIMED, result.fixtures[0].status)
            assertEquals(null, result.fixtures[0].homeScore)
            assertEquals(MatchStatus.FINISHED, result.fixtures[1].status)
            assertEquals(2, result.fixtures[1].homeScore)
            assertEquals(1, result.fixtures[1].awayScore)
            assertEquals(MatchStatus.POSTPONED, result.fixtures[2].status)
            assertEquals("/v4/competitions/PL/teams", server.takeRequest().let {
                assertEquals("test-token", it.getHeader("X-Auth-Token")); it.path
            })
            assertEquals("/v4/competitions/PL/matches", server.takeRequest().path)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun keepsCompleteCompetitionListsAndCombinesFollowedClubMatches() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setBody("""{"teams":[{"shortName":"Arsenal"}]}"""))
            server.enqueue(MockResponse().setBody("""{"matches":[
                {"utcDate":"2026-10-17T19:30:00Z","status":"TIMED",
                 "homeTeam":{"shortName":"Arsenal"},"awayTeam":{"shortName":"Chelsea"}}]}"""))
            server.enqueue(MockResponse().setBody("""{"teams":[
                {"shortName":"Inter"},{"shortName":"Juventus"}]}"""))
            server.enqueue(MockResponse().setBody("""{"matches":[
                {"utcDate":"2026-10-18T18:45:00Z","status":"TIMED",
                 "homeTeam":{"shortName":"Inter"},"awayTeam":{"shortName":"Juventus"}}]}"""))
            server.enqueue(MockResponse().setBody("""{"teams":[
                {"shortName":"PSG"},{"shortName":"Monaco"}]}"""))
            server.enqueue(MockResponse().setBody("""{"matches":[
                {"utcDate":"2026-10-18T19:00:00Z","status":"TIMED",
                 "homeTeam":{"shortName":"PSG"},"awayTeam":{"shortName":"Monaco"}}]}"""))
            server.enqueue(MockResponse().setBody("""{"teams":[
                {"shortName":"Real Madrid"},{"shortName":"Barcelona"}]}"""))
            server.enqueue(MockResponse().setBody("""{"matches":[
                {"utcDate":"2026-10-18T19:15:00Z","status":"TIMED",
                 "competition":{"name":"Primera Division"},
                 "homeTeam":{"shortName":"Real Madrid"},"awayTeam":{"shortName":"Barcelona"}}]}"""))
            server.enqueue(MockResponse().setBody("""{"teams":[
                {"shortName":"Bayern"},{"shortName":"Dortmund"}]}"""))
            server.enqueue(MockResponse().setBody("""{"matches":[
                {"utcDate":"2026-10-17T16:30:00Z","status":"TIMED",
                 "homeTeam":{"shortName":"Bayern"},"awayTeam":{"shortName":"Dortmund"}}]}"""))
            server.enqueue(MockResponse().setBody("""{"teams":[
                {"shortName":"Arsenal"},{"shortName":"Inter"},{"shortName":"PSG"},{"shortName":"Real Madrid"},{"shortName":"Bayern"}]}"""))
            server.enqueue(MockResponse().setBody("""{"matches":[
                {"utcDate":"2026-10-20T19:00:00Z","status":"TIMED",
                 "homeTeam":{"shortName":"Inter"},"awayTeam":{"shortName":"Arsenal"}},
                {"utcDate":"2026-10-21T19:00:00Z","status":"TIMED",
                 "homeTeam":{"shortName":"Real Madrid"},"awayTeam":{"shortName":"PSG"}},
                {"utcDate":"2026-10-21T19:00:00Z","status":"TIMED",
                 "homeTeam":{"shortName":"Bayern"},"awayTeam":{"shortName":"Real Madrid"}}]}"""))
            val result = FootballRepository(FootballApiFactory.create("test", server.url("/v4/").toString())).loadSchedule()
            assertEquals(listOf("Premier League", "Serie A", "Ligue 1", "La Liga", "Bundesliga", "UEFA Champions League"), result.competitions)
            assertEquals(listOf("Arsenal"), result.teamsByLeague["Premier League"])
            assertEquals(listOf("Inter", "Juventus"), result.teamsByLeague["Serie A"])
            assertEquals(listOf("Monaco", "PSG"), result.teamsByLeague["Ligue 1"])
            assertEquals(listOf("Arsenal", "Bayern", "Inter", "PSG", "Real Madrid"), result.teamsByLeague["UEFA Champions League"])
            assertEquals(listOf("Barcelona", "Real Madrid"), result.teamsByLeague["La Liga"])
            assertEquals(listOf("Bayern", "Dortmund"), result.teamsByLeague["Bundesliga"])
            val bayernMatches = result.fixtures.filter { it.homeTeam == "Bayern" || it.awayTeam == "Bayern" }
            assertEquals(listOf("Bundesliga", "UEFA Champions League"), bayernMatches.map { it.competition })
            val madridMatches = result.fixtures.filter { it.homeTeam == "Real Madrid" || it.awayTeam == "Real Madrid" }
            assertEquals(listOf("La Liga", "UEFA Champions League", "UEFA Champions League"), madridMatches.map { it.competition })
            val psgMatches = result.fixtures.filter { it.homeTeam == "PSG" || it.awayTeam == "PSG" }
            assertEquals(listOf("Ligue 1", "UEFA Champions League"), psgMatches.map { it.competition })
            val arsenalMatches = result.fixtures.filter { it.homeTeam == "Arsenal" || it.awayTeam == "Arsenal" }
            assertEquals(listOf("Premier League", "UEFA Champions League"), arsenalMatches.map { it.competition })
            val interMatches = result.fixtures.filter { it.homeTeam == "Inter" || it.awayTeam == "Inter" }
            assertEquals(listOf("Serie A", "UEFA Champions League"), interMatches.map { it.competition })
            // Following both opponents must still show their shared fixture just once.
            val followed = setOf("Arsenal", "Inter")
            assertEquals(3, result.fixtures.filter { it.homeTeam in followed || it.awayTeam in followed }.size)
            assertEquals(listOf("/v4/competitions/PL/teams", "/v4/competitions/PL/matches",
                "/v4/competitions/SA/teams", "/v4/competitions/SA/matches",
                "/v4/competitions/FL1/teams", "/v4/competitions/FL1/matches",
                "/v4/competitions/PD/teams", "/v4/competitions/PD/matches",
                "/v4/competitions/BL1/teams", "/v4/competitions/BL1/matches",
                "/v4/competitions/CL/teams", "/v4/competitions/CL/matches"),
                List(12) { server.takeRequest().path })
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun quotaRetryResumesFailedEndpointAndSuccessfulReloadUsesCache() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setBody("""{"teams":[{"shortName":"Arsenal"}]}"""))
            server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "2").setBody("{}"))
            server.enqueue(MockResponse().setBody("""{"matches":[]}"""))
            val waits = mutableListOf<Long>()
            val notifications = mutableListOf<Long>()
            val repository = FootballRepository(
                FootballApiFactory.create("test", server.url("/v4/").toString()),
                listOf(FootballCompetition("PL", "Premier League")),
                waitForQuota = { waits += it }
            )
            val first = repository.loadSchedule { notifications += it }
            val second = repository.loadSchedule()
            assertEquals(listOf(2000L), waits)
            assertEquals(listOf(2L), notifications)
            assertEquals(first.fetchedAt, second.fetchedAt)
            assertEquals(first.teamsByLeague, second.teamsByLeague)
            assertEquals(3, server.requestCount)
            assertEquals(listOf("/v4/competitions/PL/teams", "/v4/competitions/PL/matches",
                "/v4/competitions/PL/matches"), List(3) { server.takeRequest().path })
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun manualRetryRetainsPartialProgressAfterRepeatedRateLimit() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setBody("""{"teams":[]}"""))
            repeat(2) { server.enqueue(MockResponse().setResponseCode(429).setBody("{}")) }
            server.enqueue(MockResponse().setBody("""{"matches":[]}"""))
            val waits = mutableListOf<Long>()
            val repository = FootballRepository(
                FootballApiFactory.create("test", server.url("/v4/").toString()),
                listOf(FootballCompetition("PL", "Premier League")),
                waitForQuota = { waits += it }
            )
            try {
                repository.loadSchedule()
                throw AssertionError("Expected repeated rate limit")
            } catch (error: HttpException) {
                assertEquals(429, error.code())
            }
            repository.loadSchedule()
            assertEquals(listOf(65000L), waits)
            assertEquals(4, server.requestCount)
            assertEquals("/v4/competitions/PL/teams", server.takeRequest().path)
            repeat(3) { assertEquals("/v4/competitions/PL/matches", server.takeRequest().path) }
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun deniedAccessPropagatesForUiErrorHandling() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(401).setBody("{}"))
            val repository = FootballRepository(FootballApiFactory.create("invalid", server.url("/v4/").toString()),
                listOf(FootballCompetition("PL", "Premier League")))
            try {
                repository.loadSchedule()
                throw AssertionError("Expected denied access")
            } catch (error: HttpException) {
                assertEquals(401, error.code())
            }
        } finally {
            server.shutdown()
        }
    }
}
