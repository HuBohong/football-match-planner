package au.edu.jcu.assessment.utilityapp.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

interface FootballApi {
    @GET("competitions/{code}/teams")
    suspend fun getTeams(@Path("code") code: String): TeamsResponse

    @GET("competitions/{code}/matches")
    suspend fun getMatches(@Path("code") code: String): MatchesResponse
}

object FootballApiFactory {
    fun create(token: String, baseUrl: String = "https://api.football-data.org/v4/"): FootballApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder().header("X-Auth-Token", token).build())
            }
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FootballApi::class.java)
    }
}

data class TeamsResponse(val teams: List<TeamDto>)
data class TeamDto(val id: Int?, val name: String?, val shortName: String?, val crest: String? = null) {
    val displayName: String get() = shortName?.takeIf { it.isNotBlank() }
        ?: name?.takeIf { it.isNotBlank() } ?: "Team to be confirmed"
}
data class MatchesResponse(val matches: List<MatchDto>)
data class MatchDto(
    val id: Int?,
    val utcDate: String?,
    val status: String?,
    val competition: CompetitionDto?,
    val homeTeam: TeamDto?,
    val awayTeam: TeamDto?,
    val score: ScoreDto?
)
data class CompetitionDto(val name: String?, val emblem: String? = null)
data class ScoreDto(val fullTime: GoalsDto?)
data class GoalsDto(val home: Int?, val away: Int?)
