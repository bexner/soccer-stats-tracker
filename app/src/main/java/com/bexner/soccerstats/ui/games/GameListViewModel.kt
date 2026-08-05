package com.bexner.soccerstats.ui.games

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bexner.soccerstats.data.SoccerRepository
import com.bexner.soccerstats.data.entity.Game
import com.bexner.soccerstats.data.entity.GameWithCounts
import com.bexner.soccerstats.data.entity.Team
import com.bexner.soccerstats.ui.navigation.Routes
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GameListUiState(
    val team: Team? = null,
    val games: List<GameWithCounts> = emptyList(),
    val isLoading: Boolean = true
) {
    val upcoming: List<GameWithCounts>
        get() = games.filter { it.game.kickoffAt >= todayStart() }

    val past: List<GameWithCounts>
        get() = games.filter { it.game.kickoffAt < todayStart() }.reversed()

    private fun todayStart(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}

class GameListViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: SoccerRepository
) : ViewModel() {

    val teamId: Long = savedStateHandle[Routes.TEAM_ID_ARG] ?: 0L

    val uiState: StateFlow<GameListUiState> =
        combine(
            repository.observeTeam(teamId),
            repository.observeGames(teamId)
        ) { team, games ->
            GameListUiState(team = team, games = games, isLoading = false)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = GameListUiState()
        )

    fun delete(game: Game) {
        viewModelScope.launch { repository.deleteGame(game) }
    }
}
