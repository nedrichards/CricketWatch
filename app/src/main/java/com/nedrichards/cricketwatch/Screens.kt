package com.nedrichards.cricketwatch

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MatchListScreen(
    state: CricketUiState,
    onRefresh: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

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
                            coroutineScope.launch {
                                listState.scrollBy(it.verticalScrollPixels)
                            }
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
                        items(state.matches) { match ->
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
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            match.score?.forEach { score ->
                val isBatting = match.status?.contains(score.inning.substringBefore(" Inning"), ignoreCase = true) == true
                Text(
                    text = "${score.inning}: ${score.r}/${score.w} (${score.o})${if (isBatting) " *" else ""}",
                    style = MaterialTheme.typography.caption2,
                    color = if (isBatting) Color.Cyan else Color.White,
                    fontWeight = if (isBatting) FontWeight.Bold else FontWeight.Normal
                )
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
