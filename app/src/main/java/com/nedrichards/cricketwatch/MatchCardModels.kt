package com.nedrichards.cricketwatch

import androidx.compose.runtime.Immutable
import java.util.Locale

@Immutable
data class MatchCardModel(
    val id: String,
    val name: String,
    val status: String?,
    val scoreRows: List<DisplayScoreModel>
)

@Immutable
data class DisplayScoreModel(
    val shortTeam: String,
    val score: String,
    val overs: String,
    val isBatting: Boolean
)

fun MatchSummary.toMatchCardModel(): MatchCardModel =
    MatchCardModel(
        id = id,
        name = name,
        status = status,
        scoreRows = buildDisplayScoreRows()
    )

private fun MatchSummary.buildDisplayScoreRows(): List<DisplayScoreModel> {
    val scores = score.orEmpty()
    return scores.mapIndexed { index, scoreSummary ->
        val team = scoreSummary.teamName(teams, index)
        DisplayScoreModel(
            shortTeam = team.shortTeamName(),
            score = "${scoreSummary.r}/${scoreSummary.w}",
            overs = scoreSummary.o.formatOvers(),
            isBatting = !matchEnded && matchStarted && index == scores.lastIndex
        )
    }
}

private fun ScoreSummary.teamName(teams: List<String>, index: Int): String {
    val inningTeam = inning.substringBefore(" Inning").trim()
    return teams.firstOrNull { team ->
        inningTeam.equals(team, ignoreCase = true) ||
            inningTeam.contains(team, ignoreCase = true) ||
            team.contains(inningTeam, ignoreCase = true)
    } ?: teams.getOrNull(index) ?: inningTeam
}

private fun String.shortTeamName(): String {
    val locale = Locale.getDefault()
    val bracketed = BRACKETED_TEAM_REGEX.find(this)?.groupValues?.getOrNull(1)
    if (!bracketed.isNullOrBlank()) return bracketed.take(5).uppercase(locale)

    val words = replace(NON_ALPHANUMERIC_TEAM_REGEX, " ")
        .split(WHITESPACE_REGEX)
        .filter { it.isNotBlank() && !TEAM_STOP_WORDS.contains(it.lowercase(locale)) }

    if (words.isEmpty()) return take(5).uppercase(locale)
    if (words.size == 1) return words.first().take(3).uppercase(locale)
    if (words.last().equals("Women", ignoreCase = true)) {
        return "${words.first().take(3)}W".uppercase(locale)
    }

    return words.joinToString("") { it.first().uppercaseChar().toString() }
        .take(5)
}

private fun Double.formatOvers(): String =
    if (rem(1.0) == 0.0) toInt().toString() else toString()

private val BRACKETED_TEAM_REGEX = Regex("\\[(.+?)]")
private val NON_ALPHANUMERIC_TEAM_REGEX = Regex("[^A-Za-z0-9 ]")
private val WHITESPACE_REGEX = Regex("\\s+")
private val TEAM_STOP_WORDS = setOf("cricket", "club", "of", "the")
