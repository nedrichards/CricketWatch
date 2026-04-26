package com.nedrichards.cricketwatch

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

sealed class CricketUiState {
    object Loading : CricketUiState()
    data class Success(
        val matches: List<MatchCardModel>,
        val lastUpdated: Long = System.currentTimeMillis(),
        val refreshError: String? = null
    ) : CricketUiState()
    data class Error(val message: String) : CricketUiState()
}

class CricketViewModel(private val repository: CricketRepository) : ViewModel() {

    private val _uiState = mutableStateOf<CricketUiState>(CricketUiState.Loading)
    val uiState: State<CricketUiState> = _uiState
    private var loadJob: Job? = null

    fun loadMatches() {
        if (loadJob?.isActive == true) return

        val previousState = _uiState.value
        loadJob = viewModelScope.launch {
            if (previousState !is CricketUiState.Success) {
                _uiState.value = CricketUiState.Loading
            }

            try {
                val matches = repository.getRelevantMatches()
                    .map(MatchSummary::toMatchCardModel)
                _uiState.value = CricketUiState.Success(
                    matches = matches,
                    lastUpdated = System.currentTimeMillis(),
                    refreshError = null
                )
            } catch (e: Exception) {
                _uiState.value = if (previousState is CricketUiState.Success) {
                    previousState.copy(refreshError = e.message ?: "Refresh failed")
                } else {
                    CricketUiState.Error(e.message ?: "Unknown error")
                }
            }
        }
    }
}
