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
            val repository = FootballRepository(FootballApiFactory.create("test-token", server.url("/v4/").toString()))
            val result = repository.loadPremierLeague()
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
    fun deniedAccessPropagatesForUiErrorHandling() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(401).setBody("{}"))
            val repository = FootballRepository(FootballApiFactory.create("invalid", server.url("/v4/").toString()))
            try {
                repository.loadPremierLeague()
                throw AssertionError("Expected denied access")
            } catch (error: HttpException) {
                assertEquals(401, error.code())
            }
        } finally {
            server.shutdown()
        }
    }
}
