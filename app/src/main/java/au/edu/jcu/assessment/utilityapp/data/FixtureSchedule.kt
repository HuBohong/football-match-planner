package au.edu.jcu.assessment.utilityapp.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// Store an absolute kickoff instant; display time and date are derived from it.
data class MatchFixture(
    val competition: String,
    val homeTeam: String,
    val awayTeam: String,
    val kickoff: Instant,
    val status: MatchStatus = MatchStatus.SCHEDULED,
    val homeScore: Int? = null,
    val awayScore: Int? = null
)

enum class MatchStatus(val label: String) {
    SCHEDULED("Time unconfirmed"), TIMED("Scheduled"), FINISHED("Finished"),
    IN_PLAY("In progress"), PAUSED("Paused"), POSTPONED("Postponed"),
    CANCELLED("Cancelled"), SUSPENDED("Suspended"), AWARDED("Awarded"), UNKNOWN("Status unavailable")
}

enum class SchedulePeriod(val label: String) {
    YESTERDAY("Yesterday"), TODAY("Today"), UPCOMING("Upcoming")
}

fun MatchFixture.matchesPeriod(period: SchedulePeriod, now: Instant, zone: ZoneId): Boolean {
    val today = now.atZone(zone).toLocalDate()
    val matchDate = kickoff.atZone(zone).toLocalDate()
    return when (period) {
        SchedulePeriod.YESTERDAY -> matchDate == today.minusDays(1)
        SchedulePeriod.TODAY -> matchDate == today
        SchedulePeriod.UPCOMING ->
            (status == MatchStatus.SCHEDULED || status == MatchStatus.TIMED) && kickoff.isAfter(now)
    }
}

fun groupFixturesByLocalDate(
    fixtures: List<MatchFixture>,
    zone: ZoneId
): Map<LocalDate, List<MatchFixture>> = fixtures
    .sortedBy { it.kickoff }
    .groupBy { it.kickoff.atZone(zone).toLocalDate() }
