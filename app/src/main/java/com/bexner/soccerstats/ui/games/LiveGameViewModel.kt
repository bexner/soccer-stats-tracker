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

    /** Events that carry a pitch position, for the overlay. */
    val positionedEvents: List<GameEvent> get() = events.filter { it.hasPitchPosition }
}

/** How events are being entered. */
enum class EntryMode(val label: String) {
    QUICK("Quick buttons"),
    PITCH("Tap the pitch")
}

/** The step the event wizard is on, or null when nothing is being logged. */
enum class DraftStage { PICK_ACTION, PICK_PLAYER, PICK_PLACEMENT }

/** An event being assembled across one or more prompts. */
data class EventDraft(
    val side: EventSide,
    val pitchX: Float? = null,
    val pitchY: Float? = null,
    val type: EventType? = null,
    val playerId: Long? = null
)

/**
 * Confirms back to the coach which third they tapped, using the same thresholds
 * as [GameEvent.pitchThird] so the prompt and the stored value never disagree.
 */
fun EventDraft.pitchThirdLabel(): String? = pitchY?.let {
    val third = when {
        it >= 0.667f -> "defensive third"
        it >= 0.333f -> "middle third"
        else -> "attacking third"
    }
    "Tapped in the $third"
}

class LiveGameViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: SoccerRepository
) : ViewModel() {

    val gameId: Long = savedStateHandle[Routes.GAME_ID_ARG] ?: 0L

    var side by mutableStateOf(EventSide.US)
        private set

    var entryMode by mutableStateOf(EntryMode.QUICK)
        private set

    var draft by mutableStateOf<EventDraft?>(null)
        private set

    var stage by mutableStateOf<DraftStage?>(null)
        private set

    /** Player coming off, waiting for a replacement to be chosen. */
    var substitutingOut by mutableStateOf<Long?>(null)
        private set

    private val _elapsedMs = MutableStateFlow(0L)
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

    fun onEntryModeChange(value: EntryMode) {
        entryMode = value
        cancelDraft()
    }

    // ----- Clock -----

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

    // ----- Event wizard -----

    /** Quick mode: action first, position never asked for. */
    fun onEventTapped(type: EventType) {
        advance(EventDraft(side = side, type = type))
    }

    /** Pitch mode: position first, then action. */
    fun onPitchTapped(x: Float, y: Float) {
        draft = EventDraft(side = side, pitchX = x, pitchY = y)
        stage = DraftStage.PICK_ACTION
    }

    fun onActionChosen(type: EventType) {
        val current = draft ?: EventDraft(side = side)
        advance(current.copy(type = type))
    }

    fun onPlayerChosen(playerId: Long?) {
        val current = draft ?: return
        advance(current.copy(playerId = playerId), playerResolved = true)
    }

    fun onPlacementChosen(goalX: Float, goalY: Float) {
        val current = draft ?: return
        commit(current, goalX, goalY)
    }

    fun skipPlacement() {
        val current = draft ?: return
        commit(current, null, null)
    }

    fun cancelDraft() {
        draft = null
        stage = null
    }

    /**
     * Moves the draft to whatever it still needs, or writes it if it's complete.
     * [playerResolved] distinguishes "no player chosen yet" from "deliberately
     * logged without one", so skipping doesn't loop back to the same prompt.
     */
    private fun advance(next: EventDraft, playerResolved: Boolean = false) {
        val type = next.type
        if (type == null) {
            draft = next
            stage = DraftStage.PICK_ACTION
            return
        }

        val needsPlayer = type.attributable &&
            next.side == EventSide.US &&
            !playerResolved &&
            next.playerId == null

        when {
            needsPlayer -> {
                draft = next
                stage = DraftStage.PICK_PLAYER
            }
            type in EventType.placementRelevant -> {
                draft = next
                stage = DraftStage.PICK_PLACEMENT
            }
            else -> commit(next, null, null)
        }
    }

    private fun commit(source: EventDraft, goalX: Float?, goalY: Float?) {
        val type = source.type ?: return
        draft = null
        stage = null
        viewModelScope.launch {
            repository.logEvent(
                gameId = gameId,
                type = type,
                side = source.side,
                playerId = source.playerId,
                pitchX = source.pitchX,
                pitchY = source.pitchY,
                goalX = goalX,
                goalY = goalY
            )
        }
    }

    // ----- Substitutions -----

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
