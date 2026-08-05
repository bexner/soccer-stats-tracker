package com.bexner.soccerstats.ui.games

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bexner.soccerstats.data.SoccerRepository
import com.bexner.soccerstats.data.entity.Attendance
import com.bexner.soccerstats.data.entity.Game
import com.bexner.soccerstats.data.entity.GameAttendance
import com.bexner.soccerstats.data.entity.LineupSlot
import com.bexner.soccerstats.data.entity.Player
import com.bexner.soccerstats.ui.navigation.Routes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GameDetailUiState(
    val game: Game? = null,
    val players: List<Player> = emptyList(),
    val attendance: List<GameAttendance> = emptyList(),
    val lineup: List<LineupSlot> = emptyList(),
    val isLoading: Boolean = true
) {
    private val statusByPlayer: Map<Long, Attendance>
        get() = attendance.associate { it.playerId to it.status }

    fun statusFor(playerId: Long): Attendance =
        statusByPlayer[playerId] ?: Attendance.UNKNOWN

    /** Yes and Maybe players, in roster order — the pool a lineup draws from. */
    val availablePlayers: List<Player>
        get() = players.filter { statusFor(it.id).isAvailable }

    val yesCount: Int get() = players.count { statusFor(it.id) == Attendance.YES }
    val maybeCount: Int get() = players.count { statusFor(it.id) == Attendance.MAYBE }
    val noCount: Int get() = players.count { statusFor(it.id) == Attendance.NO }

    val lineupPlayerIds: Set<Long> get() = lineup.map { it.playerId }.toSet()
}

class GameDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: SoccerRepository
) : ViewModel() {

    val gameId: Long = savedStateHandle[Routes.GAME_ID_ARG] ?: 0L

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<GameDetailUiState> =
        repository.observeGame(gameId)
            .flatMapLatest { game ->
                if (game == null) {
                    flowOf(GameDetailUiState(isLoading = false))
                } else {
                    combine(
                        repository.observeRoster(game.teamId),
                        repository.observeAttendance(gameId),
                        repository.observeLineup(gameId)
                    ) { players, attendance, lineup ->
                        GameDetailUiState(
                            game = game,
                            players = players.filter { it.isActive },
                            attendance = attendance,
                            lineup = lineup,
                            isLoading = false
                        )
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = GameDetailUiState()
            )

    fun setAttendance(playerId: Long, status: Attendance) {
        viewModelScope.launch { repository.setAttendance(gameId, playerId, status) }
    }

    /** Bulk helper — most of the squad usually says yes. */
    fun markAllRemaining(status: Attendance) {
        val state = uiState.value
        viewModelScope.launch {
            state.players
                .filter { state.statusFor(it.id) == Attendance.UNKNOWN }
                .forEach { repository.setAttendance(gameId, it.id, status) }
        }
    }
}
