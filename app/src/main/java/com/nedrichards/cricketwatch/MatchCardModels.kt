package com.nedrichards.cricketwatch

import androidx.compose.runtime.Immutable
import java.util.Locale

@Immutable
data class MatchCardModel(
    val id: String,
    val title: String,
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
        title = name.compactMatchTitle(),
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

private fun String.compactMatchTitle(): String {
    val parts = split(",")
        .map(String::trim)
        .filter(String::isNotBlank)

    if (parts.isEmpty()) return this
    if (parts.size == 1) return parts.first().removeTrailingYear().normalizeCompetitionName()

    val teams = parts.first()
    val competitionParts = parts.drop(1)
        .filterNot(String::isMatchNumberLabel)
        .map(String::removeTrailingYear)
        .map(String::normalizeCompetitionName)
        .filter(String::isNotBlank)

    return when {
        competitionParts.isEmpty() -> teams
        competitionParts.first().equals(teams, ignoreCase = true) -> teams
        else -> "$teams, ${competitionParts.joinToString(", ")}"
    }
}

private fun String.isMatchNumberLabel(): Boolean =
    MATCH_NUMBER_REGEX.matches(this)

private fun String.removeTrailingYear(): String =
    replace(TRAILING_YEAR_REGEX, "").trim().trimEnd(',', '-').trim()

private fun String.normalizeCompetitionName(): String =
    replace(COMPETITION_SUFFIX_REGEX, "").trim().trimEnd(',', '-').trim()

private fun Double.formatOvers(): String =
    if (rem(1.0) == 0.0) toInt().toString() else toString()

private val BRACKETED_TEAM_REGEX = Regex("\\[(.+?)]")
private val NON_ALPHANUMERIC_TEAM_REGEX = Regex("[^A-Za-z0-9 ]")
private val WHITESPACE_REGEX = Regex("\\s+")
private val MATCH_NUMBER_REGEX =
    Regex("(?i)^((\\d+)(st|nd|rd|th)\\s+match|match\\s+\\d+|\\d+(st|nd|rd|th)\\s+test|\\d+(st|nd|rd|th)\\s+odi|\\d+(st|nd|rd|th)\\s+t20i?)$")
private val TRAILING_YEAR_REGEX = Regex("""(?:,?\s+)?\b(19|20)\d{2}\b$""")
private val COMPETITION_SUFFIX_REGEX =
    Regex("""(?i)(?:^|\s+)(division\s+(one|two|three)|north\s+group|south\s+group|group\s+[a-z0-9]+)$""")
private val TEAM_STOP_WORDS = setOf("cricket", "club", "of", "the")
