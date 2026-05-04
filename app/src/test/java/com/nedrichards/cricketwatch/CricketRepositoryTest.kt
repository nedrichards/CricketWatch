package com.nedrichards.cricketwatch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlinx.coroutines.runBlocking

class CricketRepositoryTest {

    @Test
    fun testFormatScoreEnglish() {
        val repo = CricketRepository("fake_key")
        val formatted = repo.formatScoreEnglish(245, 4, 45.2)
        assertEquals("245/4 (45.2)", formatted)
    }

    @Test
    fun testFilteringLogic() {
        val repo = CricketRepository("fake_key")
        val matches = listOf(
            MatchSummary("1", "England vs Australia", null, null, null, null, listOf("England", "Australia"), null, null, true, false),
            MatchSummary("2", "Surrey vs Kent", null, null, null, null, listOf("Surrey", "Kent"), null, null, true, false),
            MatchSummary("3", "India vs Pakistan", null, null, null, null, listOf("India", "Pakistan"), null, null, true, false)
        )

        val filtered = matches.filter(repo::isRelevantMatch)

        assertEquals(2, filtered.size)
        assertTrue(filtered.any { it.name.contains("England") })
        assertTrue(filtered.any { it.name.contains("Surrey") })
        assertTrue(filtered.none { it.name.contains("India") })
    }

    @Test
    fun testFilteringUsesTeamsAsWellAsName() {
        val repo = CricketRepository("fake_key")
        val match = MatchSummary(
            id = "4",
            name = "County Championship Division One",
            matchType = null,
            status = "Day 4: Lunch",
            venue = null,
            date = null,
            teams = listOf("Warwickshire", "Surrey"),
            score = null,
            series_id = null,
            matchStarted = true,
            matchEnded = false
        )

        assertTrue(repo.isRelevantMatch(match))
    }

    @Test
    fun testLiveMatchesSortAheadOfCompletedMatches() {
        val matches = listOf(
            MatchSummary("1", "Surrey vs Essex", null, null, null, null, listOf("Surrey", "Essex"), null, null, true, false),
            MatchSummary("2", "Surrey vs Kent", null, null, null, null, listOf("Surrey", "Kent"), null, null, false, true),
            MatchSummary("3", "England vs India", null, null, null, null, listOf("England", "India"), null, null, false, false)
        )

        val sorted = matches.sortedWith(
            compareByDescending<MatchSummary> { it.matchStarted && !it.matchEnded }
                .thenByDescending { !it.matchEnded }
                .thenBy { it.name }
        )

        assertEquals("1", sorted.first().id)
    }

    @Test
    fun testMatchCardModelPrecomputesScoreRows() {
        val model = MatchSummary(
            id = "1",
            name = "Surrey vs Kent",
            matchType = null,
            status = "Surrey need 42",
            venue = null,
            date = null,
            teams = listOf("Surrey", "Kent"),
            score = listOf(
                ScoreSummary(r = 245, w = 8, o = 50.0, inning = "Surrey Inning 1"),
                ScoreSummary(r = 203, w = 4, o = 44.3, inning = "Kent Inning 1")
            ),
            series_id = null,
            matchStarted = true,
            matchEnded = false
        ).toMatchCardModel()

        assertEquals(
            listOf(
                DisplayScoreModel("SUR", "245/8", "50", false),
                DisplayScoreModel("KEN", "203/4", "44.3", true)
            ),
            model.scoreRows
        )
    }

    @Test
    fun testMatchCardModelCombinesBothInningsByTeam() {
        val model = MatchSummary(
            id = "two-innings",
            name = "Surrey vs Sussex, County Championship",
            matchType = "test",
            status = "Day 3: Surrey lead by 80",
            venue = null,
            date = null,
            teams = listOf("Surrey", "Sussex"),
            score = listOf(
                ScoreSummary(r = 210, w = 10, o = 70.0, inning = "Surrey Inning 1"),
                ScoreSummary(r = 180, w = 10, o = 61.4, inning = "Sussex Inning 1"),
                ScoreSummary(r = 50, w = 2, o = 18.0, inning = "Surrey Inning 2")
            ),
            series_id = null,
            matchStarted = true,
            matchEnded = false
        ).toMatchCardModel()

        assertEquals(
            listOf(
                DisplayScoreModel("SUR", "210 & 50/2", "", true),
                DisplayScoreModel("SUS", "180", "", false)
            ),
            model.scoreRows
        )
    }

    @Test
    fun testMatchCardModelMarksDeclaredInningsWithoutShowingAllOutWickets() {
        val model = MatchSummary(
            id = "declared",
            name = "Surrey vs Sussex, County Championship",
            matchType = "test",
            status = null,
            venue = null,
            date = null,
            teams = listOf("Surrey", "Sussex"),
            score = listOf(
                ScoreSummary(r = 405, w = 7, o = 110.0, inning = "Surrey Inning 1", declared = true),
                ScoreSummary(r = 120, w = 10, o = 42.2, inning = "Sussex Inning 1")
            ),
            series_id = null,
            matchStarted = true,
            matchEnded = false
        ).toMatchCardModel()

        assertEquals("405/7d", model.scoreRows.first().score)
        assertEquals("120", model.scoreRows.last().score)
    }

    @Test
    fun testShortTeamNameUsesBracketedAbbreviation() {
        val model = MatchSummary(
            id = "2",
            name = "England Women vs India Women",
            matchType = null,
            status = null,
            venue = null,
            date = null,
            teams = listOf("England Women [ENG-W]", "India Women"),
            score = listOf(
                ScoreSummary(r = 101, w = 2, o = 13.0, inning = "England Women Inning 1")
            ),
            series_id = null,
            matchStarted = true,
            matchEnded = false
        ).toMatchCardModel()

        assertEquals("ENG-W", model.scoreRows.single().shortTeam)
    }

    @Test
    fun testMatchCardModelDropsOrdinalMatchLabelButKeepsCompetition() {
        val model = MatchSummary(
            id = "3",
            name = "Surrey vs Essex, 15th Match, County Championship Division One 2026",
            matchType = null,
            status = null,
            venue = null,
            date = null,
            teams = listOf("Surrey", "Essex"),
            score = null,
            series_id = null,
            matchStarted = true,
            matchEnded = false
        ).toMatchCardModel()

        assertEquals(
            "Surrey vs Essex, County Championship",
            model.title
        )
    }

    @Test
    fun testMatchCardModelRemovesTrailingYearFromCompetitionOnlyTitle() {
        val model = MatchSummary(
            id = "4",
            name = "County Championship Division One 2026",
            matchType = null,
            status = null,
            venue = null,
            date = null,
            teams = listOf("Surrey", "Essex"),
            score = null,
            series_id = null,
            matchStarted = false,
            matchEnded = false
        ).toMatchCardModel()

        assertEquals("County Championship", model.title)
    }

    @Test
    fun testMatchCardModelKeepsCompetitionFamilyForBlastMatches() {
        val model = MatchSummary(
            id = "5",
            name = "Surrey vs Kent, South Group, Vitality Blast 2026",
            matchType = null,
            status = null,
            venue = null,
            date = null,
            teams = listOf("Surrey", "Kent"),
            score = null,
            series_id = null,
            matchStarted = false,
            matchEnded = false
        ).toMatchCardModel()

        assertEquals("Surrey vs Kent, Vitality Blast", model.title)
    }

    @Test
    fun testGetRelevantMatchesThrowsWhenBothFeedsFail() = runBlocking {
        val repo = CricketRepository(
            apiKey = "fake_key",
            api = FakeCricketApi(
                currentMatches = MatchListResponse(
                    data = emptyList(),
                    status = "failure",
                    reason = "hits today exceeded hits limit"
                ),
                cricScore = CricScoreResponse(
                    data = emptyList(),
                    status = "failure",
                    reason = "hits today exceeded hits limit"
                )
            )
        )

        try {
            repo.getRelevantMatches()
            fail("Expected getRelevantMatches to throw when both feeds fail")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("Current matches unavailable"))
            assertTrue(e.message!!.contains("Score feed unavailable"))
        }
    }

    @Test
    fun testGetRelevantMatchesUsesRemainingFeedWhenOneFails() = runBlocking {
        val api = FakeCricketApi(
            currentMatches = MatchListResponse(
                data = emptyList(),
                status = "failure",
                reason = "quota exceeded"
            ),
            cricScore = CricScoreResponse(
                data = listOf(
                    CricScoreSummary(
                        id = "surrey-1",
                        name = "Surrey vs Essex, County Championship",
                        t1 = "Surrey",
                        t2 = "Essex",
                        t1s = "192/3 (72)",
                        t2s = "409/10 (117.1)",
                        status = "Day 2: Stumps",
                        ms = "live",
                        matchType = "test",
                        series = "County Championship"
                    )
                ),
                status = "success"
            )
        )
        val repo = CricketRepository(
            apiKey = "fake_key",
            api = api
        )

        val matches = repo.getRelevantMatches()

        assertEquals(1, matches.size)
        assertEquals("surrey-1", matches.single().id)
        assertEquals(1, api.currentMatchesCalls)
        assertEquals(1, api.cricScoreCalls)
    }

    @Test
    fun testCricScoreParsesMultipleAndDeclaredInnings() = runBlocking {
        val api = FakeCricketApi(
            currentMatches = MatchListResponse(
                data = emptyList(),
                status = "failure",
                reason = "quota exceeded"
            ),
            cricScore = CricScoreResponse(
                data = listOf(
                    CricScoreSummary(
                        id = "surrey-sussex",
                        name = "Surrey vs Sussex, County Championship",
                        t1 = "Surrey",
                        t2 = "Sussex",
                        t1s = "210/10 (70) & 50/2 (18)",
                        t2s = "180/7d (61.4)",
                        status = "Day 3: Surrey lead by 80",
                        ms = "live",
                        matchType = "test",
                        series = "County Championship"
                    )
                ),
                status = "success"
            )
        )
        val repo = CricketRepository(
            apiKey = "fake_key",
            api = api
        )

        val model = repo.getRelevantMatches().single().toMatchCardModel()

        assertEquals(
            listOf(
                DisplayScoreModel("SUR", "210 & 50/2", "", true),
                DisplayScoreModel("SUS", "180/7d", "", false)
            ),
            model.scoreRows
        )
    }

    @Test
    fun testGetRelevantMatchesEnrichesIncompleteCurrentScoreFromMatchInfo() = runBlocking {
        val api = FakeCricketApi(
            currentMatches = MatchListResponse(
                data = listOf(
                    MatchSummary(
                        id = "surrey-sussex",
                        name = "Surrey vs Sussex, County Championship",
                        matchType = "test",
                        status = "Day 4: 2nd Session - Sussex trail by 23 runs",
                        venue = "The Oval",
                        date = "2026-05-04",
                        teams = listOf("Surrey", "Sussex"),
                        score = listOf(
                            ScoreSummary(r = 622, w = 10, o = 158.2, inning = "Surrey Inning 1"),
                            ScoreSummary(r = 92, w = 5, o = 39.0, inning = "Sussex Inning 2")
                        ),
                        series_id = "series-1",
                        matchStarted = true,
                        matchEnded = false
                    )
                ),
                status = "success"
            ),
            cricScore = CricScoreResponse(
                data = emptyList(),
                status = "success"
            ),
            matchInfo = mapOf(
                "surrey-sussex" to MatchDetailResponse(
                    data = MatchDetail(
                        id = "surrey-sussex",
                        name = "Surrey vs Sussex, County Championship",
                        status = "Day 4: 2nd Session - Sussex trail by 23 runs",
                        venue = "The Oval",
                        teams = listOf("Surrey", "Sussex"),
                        score = listOf(
                            ScoreSummary(r = 622, w = 10, o = 158.2, inning = "Surrey Inning 1"),
                            ScoreSummary(r = 158, w = 10, o = 59.2, inning = "Sussex Inning 1"),
                            ScoreSummary(r = 92, w = 5, o = 39.0, inning = "Sussex Inning 2")
                        ),
                        scorecard = null
                    ),
                    status = "success"
                )
            )
        )
        val repo = CricketRepository(
            apiKey = "fake_key",
            api = api
        )

        val model = repo.getRelevantMatches().single().toMatchCardModel()

        assertEquals(
            listOf(
                DisplayScoreModel("SUR", "622", "", false),
                DisplayScoreModel("SUS", "158 & 92/5", "", true)
            ),
            model.scoreRows
        )
        assertEquals(1, api.matchInfoCalls)
        assertEquals(0, api.cricScoreCalls)
    }

    @Test
    fun testGetRelevantMatchesSkipsScoreFeedWhenPrimaryFeedHasRelevantMatch() = runBlocking {
        val api = FakeCricketApi(
            currentMatches = MatchListResponse(
                data = listOf(
                    MatchSummary(
                        id = "surrey-2",
                        name = "Surrey vs Essex, County Championship",
                        matchType = "test",
                        status = "Day 2: Stumps",
                        venue = "The Oval",
                        date = "2026-04-26",
                        teams = listOf("Surrey", "Essex"),
                        score = listOf(
                            ScoreSummary(r = 409, w = 10, o = 117.1, inning = "Essex Inning 1"),
                            ScoreSummary(r = 192, w = 3, o = 72.0, inning = "Surrey Inning 1")
                        ),
                        series_id = "series-1",
                        matchStarted = true,
                        matchEnded = false
                    )
                ),
                status = "success"
            ),
            cricScore = CricScoreResponse(
                data = emptyList(),
                status = "success"
            )
        )
        val repo = CricketRepository(
            apiKey = "fake_key",
            api = api
        )

        val matches = repo.getRelevantMatches()

        assertEquals(1, matches.size)
        assertEquals("surrey-2", matches.single().id)
        assertEquals(1, api.currentMatchesCalls)
        assertEquals(0, api.cricScoreCalls)
    }
}

private class FakeCricketApi(
    private val currentMatches: MatchListResponse,
    private val cricScore: CricScoreResponse,
    private val matchInfo: Map<String, MatchDetailResponse> = emptyMap()
) : CricketApi {
    var currentMatchesCalls: Int = 0
        private set
    var cricScoreCalls: Int = 0
        private set
    var matchInfoCalls: Int = 0
        private set

    override suspend fun getCurrentMatches(apiKey: String): MatchListResponse = currentMatches
        .also { currentMatchesCalls++ }

    override suspend fun getMatchInfo(apiKey: String, matchId: String): MatchDetailResponse {
        matchInfoCalls++
        return matchInfo[matchId] ?: throw UnsupportedOperationException("Not used in tests")
    }

    override suspend fun getCricScore(apiKey: String): CricScoreResponse = cricScore
        .also { cricScoreCalls++ }
}
