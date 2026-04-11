package com.nedrichards.cricketwatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.MaterialTheme
import kotlinx.coroutines.delay

private const val LIVE_REFRESH_INTERVAL_MS = 60_000L

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val apiKey = BuildConfig.CRICKET_API_KEY
        val repository = CricketRepository(apiKey)

        setContent {
            MaterialTheme {
                val viewModel: CricketViewModel = viewModel(
                    factory = CricketViewModelFactory(repository)
                )

                LaunchedEffect(Unit) {
                    viewModel.loadMatches()
                    while (true) {
                        delay(LIVE_REFRESH_INTERVAL_MS)
                        viewModel.loadMatches()
                    }
                }

                MatchListScreen(
                    state = viewModel.uiState.value,
                    onRefresh = { viewModel.loadMatches() }
                )
            }
        }
    }
}

class CricketViewModelFactory(private val repository: CricketRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CricketViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CricketViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
