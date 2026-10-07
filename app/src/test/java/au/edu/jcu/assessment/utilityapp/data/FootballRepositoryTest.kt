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
    fun combinesCompetitionsAndListsSharedClubOnlyOnce() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setBody("""{"teams":[{"shortName":"Arsenal"}]}"""))
            server.enqueue(MockResponse().setBody("""{"matches":[
                {"utcDate":"2026-10-17T19:30:00Z","status":"TIMED",
                 "homeTeam":{"shortName":"Arsenal"},"awayTeam":{"shortName":"Chelsea"}}]}"""))
            server.enqueue(MockResponse().setBody("""{"teams":[
                {"shortName":"Arsenal"},{"shortName":"Real Madrid"}]}"""))
            server.enqueue(MockResponse().setBody("""{"matches":[
                {"utcDate":"2026-10-20T19:00:00Z","status":"TIMED",
                 "homeTeam":{"shortName":"Real Madrid"},"awayTeam":{"shortName":"Arsenal"}}]}"""))
            val result = FootballRepository(FootballApiFactory.create("test", server.url("/v4/").toString())).loadSchedule()
            assertEquals(listOf("Premier League", "UEFA Champions League"), result.competitions)
            assertEquals(listOf("Arsenal"), result.teamsByLeague["Premier League"])
            assertEquals(listOf("Real Madrid"), result.teamsByLeague["UEFA Champions League"])
            val arsenalMatches = result.fixtures.filter { it.homeTeam == "Arsenal" || it.awayTeam == "Arsenal" }
            assertEquals(2, arsenalMatches.size)
            assertEquals(result.competitions, arsenalMatches.map { it.competition })
            assertEquals(listOf("/v4/competitions/PL/teams", "/v4/competitions/PL/matches",
                "/v4/competitions/CL/teams", "/v4/competitions/CL/matches"),
                List(4) { server.takeRequest().path })
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
