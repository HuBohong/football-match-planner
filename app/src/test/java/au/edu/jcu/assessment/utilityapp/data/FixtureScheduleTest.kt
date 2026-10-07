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
    fun periodFiltersUseLocalDatesAndUpcomingExcludesPastAndFinishedFixtures() {
        val now = Instant.parse("2026-10-07T10:00:00Z")
        val beijing = ZoneId.of("Asia/Shanghai")
        val yesterday = fixture("2026-10-06T12:00:00Z").copy(status = MatchStatus.FINISHED)
        val pastToday = fixture("2026-10-07T08:00:00Z")
        val futureToday = fixture("2026-10-07T12:00:00Z")
        val futureTomorrow = fixture("2026-10-07T18:30:00Z")
        val finished = futureToday.copy(status = MatchStatus.FINISHED)
        val fixtures = listOf(yesterday, pastToday, futureToday, futureTomorrow, finished)

        assertEquals(listOf(yesterday), fixtures.filter { it.matchesPeriod(SchedulePeriod.YESTERDAY, now, beijing) })
        assertEquals(listOf(pastToday, futureToday, finished), fixtures.filter { it.matchesPeriod(SchedulePeriod.TODAY, now, beijing) })
        assertEquals(listOf(futureToday, futureTomorrow), fixtures.filter { it.matchesPeriod(SchedulePeriod.UPCOMING, now, beijing) })
    }

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
