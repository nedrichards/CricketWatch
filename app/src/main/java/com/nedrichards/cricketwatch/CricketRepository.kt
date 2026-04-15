package com.nedrichards.cricketwatch

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class CricketRepository(private val apiKey: String) {

    private val httpClient = OkHttpClient.Builder()
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

    private val api = Retrofit.Builder()
        .baseUrl("https://api.cricapi.com/")
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(CricketApi::class.java)

    suspend fun getRelevantMatches(): List<MatchSummary> = withContext(Dispatchers.IO) {
        coroutineScope {
            val currentMatchesDeferred = async {
                try {
                    val response = api.getCurrentMatches(apiKey)
                    if (response.status == "success") {
                        response.data.map { match ->
                            match.copy(
                                name = cleanName(match.name),
                                teams = match.teams.map { cleanName(it) }
                            )
                        }
                    } else emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            }

            val cricScoreDeferred = async {
                try {
                    val response = api.getCricScore(apiKey)
                    if (response.status == "success") {
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
                    } else emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            }

            val currentMatches = currentMatchesDeferred.await()
            val cricScoreMatches = cricScoreDeferred.await()

            val allMatches = currentMatches.toMutableList()
            val existingIds = currentMatches.map { it.id }.toSet()
            allMatches.addAll(cricScoreMatches.filter { it.id !in existingIds })

            allMatches
                .filter(::isRelevantMatch)
                .sortedWith(
                    compareByDescending<MatchSummary> { it.matchStarted && !it.matchEnded }
                        .thenByDescending { !it.matchEnded }
                        .thenBy { it.name }
                )
        }
    }

    private fun parseCricScore(
        t1: String,
        t1s: String?,
        t2: String,
        t2s: String?
    ): List<ScoreSummary>? {
        val scores = mutableListOf<ScoreSummary>()
        
        fun parse(team: String, s: String?) {
            if (s.isNullOrBlank()) return
            // Example: "181/4 (20)" or "31/1 (9.3)"
            try {
                val parts = s.trim().split(" ")
                val scorePart = parts[0] // "181/4"
                val overPart = parts.getOrNull(1)?.removeSurrounding("(", ")") // "20"
                
                val rw = scorePart.split("/")
                val r = rw[0].toInt()
                val w = rw.getOrNull(1)?.toInt() ?: 0
                val o = overPart?.toDoubleOrNull() ?: 0.0
                
                scores.add(ScoreSummary(r, w, o, "${cleanName(team)} Inning 1"))
            } catch (_: Exception) {
                // Ignore parsing errors
            }
        }

        parse(t1, t1s)
        parse(t2, t2s)
        
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

    companion object {
        private val WATCH_TERMS = listOf("england", "surrey")
        private val EXCLUDE_TERMS = listOf("u19", "under-19", "under 19")
    }
}
