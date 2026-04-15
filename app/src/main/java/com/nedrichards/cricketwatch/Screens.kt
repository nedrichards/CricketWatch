package com.nedrichards.cricketwatch

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MatchListScreen(
    state: CricketUiState,
    onRefresh: () -> Unit
) {
    val listState = rememberScalingLazyListState(initialCenterItemIndex = 0)
    val focusRequester = remember { FocusRequester() }

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = {
            PositionIndicator(scalingLazyListState = listState)
        }
    ) {
        when (state) {
            is CricketUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is CricketUiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Error: ${state.message}", color = Color.Red, textAlign = TextAlign.Center)
                    Button(onClick = onRefresh) {
                        Text("Retry")
                    }
                }
            }
            is CricketUiState.Success -> {
                ScalingLazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .onRotaryScrollEvent {
                            listState.dispatchRawDelta(it.verticalScrollPixels)
                            true
                        }
                        .focusRequester(focusRequester)
                        .focusable(),
                    autoCentering = AutoCenteringParams(itemIndex = 0),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (state.matches.isEmpty()) {
                        item {
                            Text(
                                "No England/Surrey\nmatches live",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 20.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.body1
                            )
                        }
                    } else {
                        items(state.matches, key = { it.id }) { match ->
                            MatchCard(match)
                        }
                    }
                    item {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Chip(
                                onClick = onRefresh,
                                label = { Text("Refresh", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                                colors = ChipDefaults.primaryChipColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                                    .padding(horizontal = 10.dp)
                            )
                            val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
                            Text(
                                text = "Last updated: ${timeFormat.format(Date(state.lastUpdated))}",
                                style = MaterialTheme.typography.caption3,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
                
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            }
        }
    }
}

@Composable
fun MatchCard(match: MatchSummary) {
    Card(
        onClick = { /* No-op for glancing experience */ },
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                match.name,
                style = MaterialTheme.typography.caption1,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            match.displayScoreRows().forEach { score ->
                ScoreRow(score)
            }
            if (match.status != null) {
                Text(
                    text = match.status,
                    style = MaterialTheme.typography.caption2,
                    color = Color.Yellow,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ScoreRow(score: DisplayScore) {
    val rowColor = if (score.isBatting) Color.Cyan else Color.White

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = score.shortTeam,
            style = MaterialTheme.typography.caption2,
            color = rowColor,
            fontWeight = if (score.isBatting) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.28f)
        )
        Text(
            text = score.score,
            style = MaterialTheme.typography.caption1,
            color = rowColor,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.38f)
        )
        Text(
            text = "${score.overs} ov${if (score.isBatting) " *" else ""}",
            style = MaterialTheme.typography.caption2,
            color = rowColor,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.34f)
        )
    }
}

private data class DisplayScore(
    val shortTeam: String,
    val score: String,
    val overs: String,
    val isBatting: Boolean
)

private fun MatchSummary.displayScoreRows(): List<DisplayScore> {
    val scores = score.orEmpty()
    return scores.mapIndexed { index, score ->
        val team = score.teamName(teams, index)
        DisplayScore(
            shortTeam = team.shortTeamName(),
            score = "${score.r}/${score.w}",
            overs = score.o.formatOvers(),
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
    val bracketed = Regex("\\[(.+?)]").find(this)?.groupValues?.getOrNull(1)
    if (!bracketed.isNullOrBlank()) return bracketed.take(5).uppercase(Locale.getDefault())

    val words = replace(Regex("[^A-Za-z0-9 ]"), " ")
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() && !TEAM_STOP_WORDS.contains(it.lowercase(Locale.getDefault())) }

    if (words.isEmpty()) return take(5).uppercase(Locale.getDefault())
    if (words.size == 1) return words.first().take(3).uppercase(Locale.getDefault())
    if (words.last().equals("Women", ignoreCase = true)) {
        return "${words.first().take(3)}W".uppercase(Locale.getDefault())
    }

    return words.joinToString("") { it.first().uppercaseChar().toString() }
        .take(5)
}

private fun Double.formatOvers(): String =
    if (rem(1.0) == 0.0) toInt().toString() else toString()

private val TEAM_STOP_WORDS = setOf("cricket", "club", "of", "the")
