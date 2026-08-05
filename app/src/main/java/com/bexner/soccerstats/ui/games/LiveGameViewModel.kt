package com.bexner.soccerstats.ui.games

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bexner.soccerstats.data.SoccerRepository
import com.bexner.soccerstats.data.entity.EventSide
import com.bexner.soccerstats.data.entity.EventType
import com.bexner.soccerstats.data.entity.Game
import com.bexner.soccerstats.data.entity.GameEvent
import com.bexner.soccerstats.data.entity.Player
import com.bexner.soccerstats.data.entity.PlayerStint
import com.bexner.soccerstats.ui.navigation.Routes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LiveGameUiState(
    val game: Game? = null,
    val players: List<Player> = emptyList(),
    val stints: List<PlayerStint> = emptyList(),
    val events: List<GameEvent> = emptyList(),
    val isLoading: Boolean = true
) {
    private val playersById: Map<Long, Player> get() = players.associateBy { it.id }

    fun player(id: Long?): Player? = id?.let { playersById[it] }

    /** Players currently on the pitch, with the slot they occupy. */
    val onPitch: List<PlayerStint> get() = stints.filter { it.isOpen }

    val onPitchIds: Set<Long> get() = onPitch.map { it.playerId }.toSet()

    /** Anyone in the squad who isn't currently on. */
    val bench: List<Player> get() = players.filter { it.id !in onPitchIds }

    /** Total match time each player has accumulated, open stints included. */
    fun minutesFor(playerId: Long, elapsedMs: Long): Long =
        stints.filter { it.playerId == playerId }.sumOf { it.durationMsAt(elapsedMs) }
}

/** A pending event that still needs a player picked before it's written. */
data class PendingEvent(val type: EventType, val side: EventSide)

class LiveGameViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: SoccerRepository
) : ViewModel() {

    val gameId: Long = savedStateHandle[Routes.GAME_ID_ARG] ?: 0L

    /** Which side the event buttons currently apply to. */
    var side by mutableStateOf(EventSide.US)
        private set

    var pendingEvent by mutableStateOf<PendingEvent?>(null)
        private set

    /** Player coming off, waiting for a replacement to be chosen. */
    var substitutingOut by mutableStateOf<Long?>(null)
        private set

    private val _elapsedMs = MutableStateFlow(0L)

    /** Ticks once a second while running so the clock display stays live. */
    val elapsedMs: StateFlow<Long> = _elapsedMs.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<LiveGameUiState> =
        repository.observeGame(gameId)
            .flatMapLatest { game ->
                if (game == null) {
                    flowOf(LiveGameUiState(isLoading = false))
                } else {
                    combine(
                        repository.observeRoster(game.teamId),
                        repository.observeStints(gameId),
                        repository.observeEvents(gameId)
                    ) { players, stints, events ->
                        LiveGameUiState(
                            game = game,
                            players = players.filter { it.isActive },
                            stints = stints,
                            events = events,
                            isLoading = false
                        )
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = LiveGameUiState()
            )

    init {
        // Recomputed from persisted clock state rather than counted up locally,
        // so backgrounding the app or locking the phone can't drift the time.
        viewModelScope.launch {
            while (true) {
                val game = uiState.value.game
                _elapsedMs.value = game?.elapsedMsAt() ?: 0L
                delay(if (game?.isClockRunning == true) 250L else 1000L)
            }
        }
    }

    fun onSideChange(value: EventSide) {
        side = value
    }

    fun startClock() {
        viewModelScope.launch { repository.startClock(gameId) }
    }

    fun stopClock() {
        viewModelScope.launch { repository.stopClock(gameId) }
    }

    fun endPeriod() {
        viewModelScope.launch { repository.endPeriod(gameId) }
    }

    fun finishGame(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.finishGame(gameId)
            onDone()
        }
    }

    /**
     * Events that name a player open a picker first; the rest are written on the
     * single tap. Keeps the common case to one touch during a game.
     */
    fun onEventTapped(type: EventType) {
        if (type.attributable && side == EventSide.US) {
            pendingEvent = PendingEvent(type, side)
        } else {
            viewModelScope.launch { repository.logEvent(gameId, type, side) }
        }
    }

    fun confirmPendingEvent(playerId: Long?) {
        val pending = pendingEvent ?: return
        pendingEvent = null
        viewModelScope.launch {
            repository.logEvent(gameId, pending.type, pending.side, playerId)
        }
    }

    fun dismissPendingEvent() {
        pendingEvent = null
    }

    fun beginSubstitution(playerId: Long) {
        substitutingOut = playerId
    }

    fun completeSubstitution(inPlayerId: Long) {
        val outId = substitutingOut ?: return
        substitutingOut = null
        viewModelScope.launch { repository.substitute(gameId, outId, inPlayerId) }
    }

    fun cancelSubstitution() {
        substitutingOut = null
    }

    fun undo(event: GameEvent) {
        viewModelScope.launch { repository.deleteEvent(event) }
    }
}
