package com.nedrichards.cricketwatch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
