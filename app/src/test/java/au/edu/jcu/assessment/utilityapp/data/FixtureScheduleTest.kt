package au.edu.jcu.assessment.utilityapp.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class FixtureScheduleTest {
    private fun fixture(time: String) = MatchFixture("Demo", "Home", "Away", Instant.parse(time))

    @Test
    fun changingZoneMovesFixturesAcrossDateHeadingsWithoutChangingKickoffs() {
        val early = fixture("2026-10-07T15:30:00Z")
        val late = fixture("2026-10-07T18:30:00Z")
        val reversedFixtures = listOf(late, early)
        val london = groupFixturesByLocalDate(reversedFixtures, ZoneId.of("Europe/London"))
        val beijing = groupFixturesByLocalDate(reversedFixtures, ZoneId.of("Asia/Shanghai"))

        assertEquals(listOf(LocalDate.of(2026, 10, 7)), london.keys.toList())
        assertEquals(listOf(early, late), london.values.single())
        assertEquals(listOf(LocalDate.of(2026, 10, 7), LocalDate.of(2026, 10, 8)), beijing.keys.toList())
        assertEquals(listOf(late), beijing[LocalDate.of(2026, 10, 8)])
        assertEquals(LocalTime.of(2, 30), late.kickoff.atZone(ZoneId.of("Asia/Shanghai")).toLocalTime())
        assertEquals(Instant.parse("2026-10-07T18:30:00Z"), late.kickoff)
    }

    @Test
    fun repeatedHourAtDaylightSavingChangeStillSortsByActualKickoff() {
        val first = fixture("2026-10-25T00:45:00Z")
        val second = fixture("2026-10-25T01:15:00Z")
        val london = ZoneId.of("Europe/London")
        val grouped = groupFixturesByLocalDate(listOf(second, first), london)

        assertEquals(listOf(first, second), grouped[LocalDate.of(2026, 10, 25)])
        assertEquals(LocalTime.of(1, 45), first.kickoff.atZone(london).toLocalTime())
        assertEquals(LocalTime.of(1, 15), second.kickoff.atZone(london).toLocalTime())
    }
}
