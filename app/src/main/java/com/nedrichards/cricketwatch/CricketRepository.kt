package com.nedrichards.cricketwatch

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class CricketRepository(
    private val apiKey: String,
    private val api: CricketApi = createApi()
) {

    suspend fun getRelevantMatches(): List<MatchSummary> = withContext(Dispatchers.IO) {
        val currentMatchesResult = fetchCurrentMatches()
        val currentMatches = (currentMatchesResult as? FeedResult.Success)?.matches.orEmpty()
        val relevantCurrentMatches = currentMatches.filter(::isRelevantMatch)

        if (relevantCurrentMatches.isNotEmpty()) {
            return@withContext sortMatches(enrichCurrentMatches(relevantCurrentMatches))
        }

        val cricScoreResult = fetchCricScoreMatches()

        if (currentMatchesResult is FeedResult.Failure && cricScoreResult is FeedResult.Failure) {
            throw IllegalStateException(
                listOf(currentMatchesResult.message, cricScoreResult.message).joinToString(" | ")
            )
        }

        val cricScoreMatches = (cricScoreResult as? FeedResult.Success)?.matches.orEmpty()
        val relevantCricScoreMatches = cricScoreMatches.filter(::isRelevantMatch)

        sortMatches(relevantCurrentMatches + relevantCricScoreMatches)
    }

    private suspend fun fetchCurrentMatches(): FeedResult {
        return try {
            val response = api.getCurrentMatches(apiKey)
            if (response.status == "success") {
                FeedResult.Success(
                    response.data.map { match ->
                        match.copy(
                            name = cleanName(match.name),
                            teams = match.teams.map { cleanName(it) }
                        )
                    }
                )
            } else {
                FeedResult.Failure("Current matches unavailable: ${response.reason ?: "unknown error"}")
            }
        } catch (e: Exception) {
            FeedResult.Failure("Current matches unavailable: ${e.message ?: "request failed"}")
        }
    }

    private suspend fun fetchCricScoreMatches(): FeedResult {
        return try {
            val response = api.getCricScore(apiKey)
            if (response.status == "success") {
                FeedResult.Success(
                    response.data.map { cs ->
                        MatchSummary(
                            id = cs.id,
                            name = cleanName(cs.name ?: "${cs.t1} vs ${cs.t2}"),
                            matchType = cs.matchType,
                            status = cs.status,
                            venue = null,
                            date = null,
                            teams = listOf(cleanName(cs.t1), cleanName(cs.t2)),
                            score = parseCricScore(cs.t1, cs.t1s, cs.t2, cs.t2s),
                            series_id = null,
                            matchStarted = cs.ms != "fixture",
                            matchEnded = cs.ms == "result"
                        )
                    }
                )
            } else {
                FeedResult.Failure("Score feed unavailable: ${response.reason ?: "unknown error"}")
            }
        } catch (e: Exception) {
            FeedResult.Failure("Score feed unavailable: ${e.message ?: "request failed"}")
        }
    }

    private suspend fun enrichCurrentMatches(matches: List<MatchSummary>): List<MatchSummary> =
        coroutineScope {
            matches.map { match ->
                async { enrichCurrentMatch(match) }
            }.awaitAll()
        }

    private suspend fun enrichCurrentMatch(match: MatchSummary): MatchSummary {
        return try {
            val response = api.getMatchInfo(apiKey, match.id)
            val detail = response.data
            val detailScore = detail.score.orEmpty()
            val summaryScore = match.score.orEmpty()

            match.copy(
                status = detail.status ?: match.status,
                teams = if (detail.teams.isNotEmpty()) detail.teams.map { cleanName(it) } else match.teams,
                score = if (detailScore.size > summaryScore.size) detailScore else match.score
            )
        } catch (_: Exception) {
            match
        }
    }

    private fun parseCricScore(
        t1: String,
        t1s: String?,
        t2: String,
        t2s: String?
    ): List<ScoreSummary>? {
        fun parse(team: String, s: String?): List<ScoreSummary> {
            if (s.isNullOrBlank()) return emptyList()
            // Examples: "181/4 (20)", "253/7d (75)", or "181/10 (55) & 31/1 (9.3)".
            return CRIC_SCORE_REGEX.findAll(s).mapIndexedNotNull { index, match ->
                val r = match.groupValues[1].toIntOrNull() ?: return@mapIndexedNotNull null
                val w = match.groupValues[2].toIntOrNull() ?: 0
                val declared = match.groupValues[3].isNotBlank()
                val o = match.groupValues[4].toDoubleOrNull() ?: 0.0

                ScoreSummary(
                    r = r,
                    w = w,
                    o = o,
                    inning = "${cleanName(team)} Inning ${index + 1}",
                    declared = declared
                )
            }.toList()
        }

        val team1Scores = parse(t1, t1s)
        val team2Scores = parse(t2, t2s)
        val scores = mutableListOf<ScoreSummary>()
        val maxInnings = maxOf(team1Scores.size, team2Scores.size)

        repeat(maxInnings) { index ->
            team1Scores.getOrNull(index)?.let(scores::add)
            team2Scores.getOrNull(index)?.let(scores::add)
        }

        return if (scores.isEmpty()) null else scores
    }

    fun formatScoreEnglish(r: Int, w: Int, o: Double): String {
        return "$r/$w ($o)"
    }

    private fun cleanName(name: String): String {
        return name.replace(Regex("\\[.*?\\]"), "").trim()
    }

    internal fun isRelevantMatch(match: MatchSummary): Boolean {
        val searchableText = buildString {
            append(match.name)
            append(' ')
            append(match.teams.joinToString(" "))
            append(' ')
            append(match.status ?: "")
        }.lowercase()

        val isTargetTeam = WATCH_TERMS.any { searchableText.contains(it) }
        val isExcluded = EXCLUDE_TERMS.any { searchableText.contains(it) }

        return isTargetTeam && !isExcluded
    }

    private fun sortMatches(matches: List<MatchSummary>): List<MatchSummary> =
        matches.sortedWith(
            compareByDescending<MatchSummary> { it.matchStarted && !it.matchEnded }
                .thenByDescending { !it.matchEnded }
                .thenBy { it.name }
        )

    private sealed interface FeedResult {
        data class Success(val matches: List<MatchSummary>) : FeedResult
        data class Failure(val message: String) : FeedResult
    }

    companion object {
        private val WATCH_TERMS = listOf("england", "surrey")
        private val EXCLUDE_TERMS = listOf("u19", "under-19", "under 19")
        private val CRIC_SCORE_REGEX = Regex("""(\d+)\s*/\s*(\d+)\s*(d|dec|declared)?\s*(?:\(([\d.]+)\))?""", RegexOption.IGNORE_CASE)

        private fun createApi(): CricketApi {
            val httpClient = OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .protocols(listOf(Protocol.HTTP_1_1))
                .addInterceptor { chain ->
                    val request: Request = chain.request().newBuilder()
                        .header("Accept", "application/json")
                        .header("User-Agent", "CricketWatch/1.0 (Wear OS)")
                        .build()
                    chain.proceed(request)
                }
                .build()

            return Retrofit.Builder()
                .baseUrl("https://api.cricapi.com/")
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(CricketApi::class.java)
        }
    }
}
