package com.bexner.soccerstats.ui.games

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bexner.soccerstats.data.SoccerRepository
import com.bexner.soccerstats.data.entity.Attendance
import com.bexner.soccerstats.data.entity.FormationWithSlots
import com.bexner.soccerstats.data.entity.Game
import com.bexner.soccerstats.data.entity.Player
import com.bexner.soccerstats.data.entity.ShapePhase
import com.bexner.soccerstats.ui.navigation.Routes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LineupUiState(
    val game: Game? = null,
    val available: List<Player> = emptyList(),
    val formations: List<FormationWithSlots> = emptyList(),
    val isLoading: Boolean = true
)

class LineupViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: SoccerRepository
) : ViewModel() {

    val gameId: Long = savedStateHandle[Routes.GAME_ID_ARG] ?: 0L

    /** slotIndex -> playerId, edited locally and written on save. */
    var assignments by mutableStateOf<Map<Int, Long>>(emptyMap())
        private set

    var selectedFormationId by mutableStateOf<Long?>(null)
        private set

    var phase by mutableStateOf(ShapePhase.DEFENDING)
        private set

    /** Slot awaiting a player choice; null when the picker is closed. */
    var pickingForSlot by mutableStateOf<Int?>(null)
        private set


    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<LineupUiState> =
        repository.observeGame(gameId)
            .flatMapLatest { game ->
                if (game == null) {
                    flowOf(LineupUiState(isLoading = false))
                } else {
                    combine(
                        repository.observeRoster(game.teamId),
                        repository.observeAttendance(gameId),
                        repository.observeFormations()
                    ) { players, attendance, formations ->
                        val statuses = attendance.associate { it.playerId to it.status }
                        LineupUiState(
                            game = game,
                            available = players.filter {
                                it.isActive &&
                                    (statuses[it.id] ?: Attendance.UNKNOWN).isAvailable
                            },
                            formations = formations,
                            isLoading = false
                        )
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = LineupUiState()
            )

    init {
        // Seed once from whatever is already saved, then let the coach edit
        // freely — re-syncing on every DB emission would fight their taps.
        viewModelScope.launch {
            selectedFormationId = repository.getGame(gameId)?.formationId
            assignments = repository.observeLineup(gameId)
                .first()
                .associate { it.slotIndex to it.playerId }
        }
    }

    fun onFormationSelected(formationId: Long) {
        // Changing shape invalidates slot bindings, since slot counts differ.
        if (selectedFormationId != formationId) {
            assignments = emptyMap()
        }
        selectedFormationId = formationId
    }

    fun onPhaseChange(value: ShapePhase) {
        phase = value
    }

    fun onSlotTapped(slotIndex: Int) {
        pickingForSlot = slotIndex
    }

    fun dismissPicker() {
        pickingForSlot = null
    }

    fun assignPlayer(slotIndex: Int, playerId: Long) {
        // A player can only hold one slot, so clear any previous assignment.
        assignments = assignments
            .filterValues { it != playerId }
            .plus(slotIndex to playerId)
        pickingForSlot = null
    }

    fun clearSlot(slotIndex: Int) {
        assignments = assignments - slotIndex
        pickingForSlot = null
    }

    fun autoFill(formation: FormationWithSlots) {
        val slots = formation.slotsFor(ShapePhase.DEFENDING)
        val taken = assignments.values.toMutableSet()
        val pool = uiState.value.available.filter { it.id !in taken }.toMutableList()
        val next = assignments.toMutableMap()

        slots.forEach { slot ->
            if (next[slot.slotIndex] != null) return@forEach
            // Prefer a player whose usual position matches the slot's role.
            val match = pool.firstOrNull { it.position == slot.role } ?: pool.firstOrNull()
            if (match != null) {
                next[slot.slotIndex] = match.id
                pool.remove(match)
                taken.add(match.id)
            }
        }
        assignments = next
    }

    fun clearAll() {
        assignments = emptyMap()
    }

    fun save(onSaved: () -> Unit) {
        val formationId = selectedFormationId ?: return
        viewModelScope.launch {
            repository.saveLineup(gameId, formationId, assignments)
            onSaved()
        }
    }
}
