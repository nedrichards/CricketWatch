package com.nedrichards.cricketwatch

import com.google.gson.annotations.SerializedName

data class MatchListResponse(
    val data: List<MatchSummary>,
    val status: String,
    val reason: String? = null
)

data class CricScoreResponse(
    val data: List<CricScoreSummary>,
    val status: String,
    val reason: String? = null
)

data class CricScoreSummary(
    val id: String,
    val name: String?,
    val t1: String,
    val t2: String,
    val t1s: String?,
    val t2s: String?,
    val status: String?,
    val ms: String?, // match status: live, result, fixture
    val matchType: String?,
    val series: String?
)

data class MatchSummary(
    val id: String,
    val name: String,
    val matchType: String?,
    val status: String?,
    val venue: String?,
    val date: String?,
    val teams: List<String>,
    val score: List<ScoreSummary>?,
    val series_id: String?,
    val matchStarted: Boolean,
    val matchEnded: Boolean
)

data class ScoreSummary(
    val r: Int,
    val w: Int,
    val o: Double,
    val inning: String
)

data class MatchDetailResponse(
    val data: MatchDetail,
    val status: String
)

data class MatchDetail(
    val id: String,
    val name: String,
    val status: String?,
    val venue: String?,
    val teams: List<String>,
    val scorecard: List<InningDetail>?
)

data class InningDetail(
    val inning: String,
    val batting: List<Batsman>?,
    val bowling: List<Bowler>?,
    val totals: InningTotals?
)

data class Batsman(
    val batsman: PlayerInfo,
    val r: Int,
    val b: Int,
    @SerializedName("4s") val fours: Int,
    @SerializedName("6s") val sixes: Int,
    val sr: Double?,
    val dismissal: String?
)

data class Bowler(
    val bowler: PlayerInfo,
    val o: Double,
    val m: Int,
    val r: Int,
    val w: Int,
    val eco: Double?
)

data class PlayerInfo(
    val id: String,
    val name: String
)

data class InningTotals(
    val R: Int,
    val W: Int,
    val O: Double
)
