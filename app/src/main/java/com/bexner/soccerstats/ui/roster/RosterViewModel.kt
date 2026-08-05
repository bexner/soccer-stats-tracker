package com.bexner.soccerstats.ui.roster

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bexner.soccerstats.data.SoccerRepository
import com.bexner.soccerstats.data.entity.Player
import com.bexner.soccerstats.data.entity.Team
import com.bexner.soccerstats.ui.navigation.Routes
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RosterUiState(
    val team: Team? = null,
    val players: List<Player> = emptyList(),
    val isLoading: Boolean = true
) {
    val activeCount: Int get() = players.count { it.isActive }
}

class RosterViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: SoccerRepository
) : ViewModel() {

    val teamId: Long = savedStateHandle[Routes.TEAM_ID_ARG] ?: 0L

    val uiState: StateFlow<RosterUiState> =
        combine(
            repository.observeTeam(teamId),
            repository.observeRoster(teamId)
        ) { team, players ->
            RosterUiState(team = team, players = players, isLoading = false)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RosterUiState()
        )

    fun togglePlayerActive(player: Player) {
        viewModelScope.launch {
            repository.updatePlayer(player.copy(isActive = !player.isActive))
        }
    }

    fun deletePlayer(player: Player) {
        viewModelScope.launch {
            repository.deletePlayer(player)
        }
    }

    fun deleteTeam(onDeleted: () -> Unit) {
        viewModelScope.launch {
            uiState.value.team?.let { repository.deleteTeam(it) }
            onDeleted()
        }
    }
}
