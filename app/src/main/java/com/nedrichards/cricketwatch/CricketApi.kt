package com.nedrichards.cricketwatch

import retrofit2.http.GET
import retrofit2.http.Query

interface CricketApi {
    @GET("v1/currentMatches")
    suspend fun getCurrentMatches(
        @Query("apikey") apiKey: String
    ): MatchListResponse

    @GET("v1/match_info")
    suspend fun getMatchInfo(
        @Query("apikey") apiKey: String,
        @Query("id") matchId: String
    ): MatchDetailResponse

    @GET("v1/cricScore")
    suspend fun getCricScore(
        @Query("apikey") apiKey: String
    ): CricScoreResponse
}
