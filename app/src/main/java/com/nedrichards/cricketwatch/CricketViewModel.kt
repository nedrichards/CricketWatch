package com.nedrichards.cricketwatch

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

sealed class CricketUiState {
    object Loading : CricketUiState()
    data class Success(
        val matches: List<MatchSummary>,
        val lastUpdated: Long = System.currentTimeMillis()
    ) : CricketUiState()
    data class Error(val message: String) : CricketUiState()
}

class CricketViewModel(private val repository: CricketRepository) : ViewModel() {

    private val _uiState = mutableStateOf<CricketUiState>(CricketUiState.Loading)
    val uiState: State<CricketUiState> = _uiState

    fun loadMatches() {
        viewModelScope.launch {
            _uiState.value = CricketUiState.Loading
            try {
                val matches = repository.getRelevantMatches()
                _uiState.value = CricketUiState.Success(
                    matches = matches,
                    lastUpdated = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                _uiState.value = CricketUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
