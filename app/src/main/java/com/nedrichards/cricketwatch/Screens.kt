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
                            if (state.refreshError != null) {
                                Text(
                                    text = "Refresh failed: ${state.refreshError}",
                                    style = MaterialTheme.typography.caption3,
                                    color = Color(0xFFFFB300),
                                    textAlign = TextAlign.Center,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp, start = 10.dp, end = 10.dp)
                                )
                            }
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
fun MatchCard(match: MatchCardModel) {
    Card(
        onClick = { /* No-op for glancing experience */ },
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                match.title,
                style = MaterialTheme.typography.caption1,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            match.scoreRows.forEach { score ->
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
private fun ScoreRow(score: DisplayScoreModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = score.shortTeam,
            style = MaterialTheme.typography.caption2,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.24f)
        )
        Text(
            text = score.score,
            style = MaterialTheme.typography.caption2,
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.Start,
            modifier = Modifier.weight(if (score.overs.isBlank()) 0.76f else 0.46f)
        )
        if (score.overs.isNotBlank()) {
            Text(
                text = score.overs + if (score.isBatting) "*" else "",
                style = MaterialTheme.typography.caption2,
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(0.30f)
            )
        }
    }
}
