package com.bexner.soccerstats.ui.teams

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bexner.soccerstats.data.SoccerRepository
import com.bexner.soccerstats.data.entity.TeamWithPlayerCount
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class TeamListUiState(
    val teams: List<TeamWithPlayerCount> = emptyList(),
    val isLoading: Boolean = true
)

class TeamListViewModel(
    private val repository: SoccerRepository
) : ViewModel() {

    val uiState: StateFlow<TeamListUiState> =
        repository.observeTeamsWithPlayerCount()
            .map { TeamListUiState(teams = it, isLoading = false) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = TeamListUiState()
            )
}
