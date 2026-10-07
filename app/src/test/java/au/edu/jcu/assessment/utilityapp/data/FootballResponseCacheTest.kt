package au.edu.jcu.assessment.utilityapp.data

import au.edu.jcu.assessment.utilityapp.data.remote.MatchesResponse
import au.edu.jcu.assessment.utilityapp.data.remote.TeamsResponse
import au.edu.jcu.assessment.utilityapp.data.remote.TeamDto
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FootballResponseCacheTest {
    @get:Rule val temporary = TemporaryFolder()
    private val start = Instant.parse("2026-10-07T10:00:00Z")
    private fun cache(time: Instant) = FootballResponseCache(temporary.root, Clock.fixed(time, ZoneOffset.UTC))

    @Test
    fun restoresSavedResponseAndOriginalTimestampAfterRecreation() {
        cache(start).write("PL-teams", TeamsResponse(listOf(TeamDto(57, "Arsenal FC", "Arsenal"))))
        val restored = cache(start.plusSeconds(60)).read("PL-teams", TeamsResponse::class.java, Duration.ofHours(24))!!
        assertEquals("Arsenal", restored.value.teams.single().displayName)
        assertEquals(start, restored.fetchedAt)
    }

    @Test
    fun expiredAndFutureDatedEntriesAreNotReused() {
        cache(start).write("PL-matches", MatchesResponse(emptyList()))
        val stillFresh = cache(start.plusSeconds(10799)).read("PL-matches", MatchesResponse::class.java, Duration.ofHours(3))!!
        assertEquals(start, stillFresh.fetchedAt)
        assertNull(cache(start.plusSeconds(10800)).read("PL-matches", MatchesResponse::class.java, Duration.ofHours(3)))
        assertNull(cache(start.minusSeconds(1)).read("PL-matches", MatchesResponse::class.java, Duration.ofHours(3)))
    }

    @Test
    fun corruptedFileIsIgnoredAndCanBeReplaced() {
        File(temporary.root, "PL-teams.json").writeText("invalid json")
        val responses = cache(start)
        assertNull(responses.read("PL-teams", TeamsResponse::class.java, Duration.ofHours(24)))
        responses.write("PL-teams", TeamsResponse(emptyList()))
        assertEquals(0, cache(start).read("PL-teams", TeamsResponse::class.java, Duration.ofHours(24))!!.value.teams.size)
    }
}
