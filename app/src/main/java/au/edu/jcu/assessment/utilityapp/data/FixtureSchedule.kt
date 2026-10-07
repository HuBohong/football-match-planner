package au.edu.jcu.assessment.utilityapp.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// Store an absolute kickoff instant; display time and date are derived from it.
data class MatchFixture(
    val competition: String,
    val homeTeam: String,
    val awayTeam: String,
    val kickoff: Instant
)

fun groupFixturesByLocalDate(
    fixtures: List<MatchFixture>,
    zone: ZoneId
): Map<LocalDate, List<MatchFixture>> = fixtures
    .sortedBy { it.kickoff }
    .groupBy { it.kickoff.atZone(zone).toLocalDate() }
