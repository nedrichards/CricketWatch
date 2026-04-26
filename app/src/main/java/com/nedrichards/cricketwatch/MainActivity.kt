package com.nedrichards.cricketwatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.wear.compose.material.MaterialTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val LIVE_REFRESH_INTERVAL_MS = 60_000L

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val apiKey = BuildConfig.CRICKET_API_KEY
        val repository = CricketRepository(apiKey)
        val viewModel = ViewModelProvider(
            this,
            CricketViewModelFactory(repository)
        )[CricketViewModel::class.java]

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loadMatches()
                while (true) {
                    delay(LIVE_REFRESH_INTERVAL_MS)
                    viewModel.loadMatches()
                }
            }
        }

        setContent {
            MaterialTheme {
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
