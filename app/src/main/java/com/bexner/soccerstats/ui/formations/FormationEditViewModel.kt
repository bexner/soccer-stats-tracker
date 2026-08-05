package com.bexner.soccerstats.ui.formations

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bexner.soccerstats.data.SoccerRepository
import com.bexner.soccerstats.data.entity.Formation
import com.bexner.soccerstats.data.entity.FormationSlot
import com.bexner.soccerstats.data.entity.MatchFormat
import com.bexner.soccerstats.data.entity.Position
import com.bexner.soccerstats.data.entity.ShapePhase
import com.bexner.soccerstats.ui.navigation.Routes
import kotlinx.coroutines.launch

/**
 * Holds both shapes in memory while the coach drags markers around, and writes
 * them in one transaction on save, so a half-finished shape never reaches the
 * database.
 */
data class FormationEditState(
    val name: String = "",
    val format: MatchFormat = MatchFormat.SEVEN_V_SEVEN,
    val hasKeeper: Boolean = true,
    val notes: String = "",
    /** Every slot across both phases. */
    val slots: List<FormationSlot> = emptyList(),
    val phase: ShapePhase = ShapePhase.DEFENDING,
    val selectedSlotIndex: Int? = null,
    val nameError: String? = null
) {
    fun slotsFor(target: ShapePhase): List<FormationSlot> =
        slots.filter { it.phase == target }.sortedBy { it.slotIndex }

    val currentSlots: List<FormationSlot> get() = slotsFor(phase)

    val required: Int get() = format.playersOnField

    val placed: Int get() = currentSlots.size

    fun isPhaseComplete(target: ShapePhase): Boolean = slotsFor(target).size == required

    val isCurrentPhaseComplete: Boolean get() = isPhaseComplete(phase)

    val hasAttackingShape: Boolean get() = slotsFor(ShapePhase.ATTACKING).isNotEmpty()

    /** Defending is the shape a formation must have; attacking is optional. */
    val isValid: Boolean
        get() = name.isNotBlank() && isPhaseComplete(ShapePhase.DEFENDING)
}

class FormationEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: SoccerRepository
) : ViewModel() {

    private val formationId: Long = savedStateHandle[Routes.FORMATION_ID_ARG] ?: 0L
    private val initialFormat: String =
        savedStateHandle[Routes.FORMAT_ARG] ?: MatchFormat.SEVEN_V_SEVEN.name

    val isNew: Boolean = formationId == 0L

    var state by mutableStateOf(FormationEditState())
        private set

    private var loaded: Formation? = null

    init {
        if (isNew) {
            val format = MatchFormat.fromName(initialFormat)
            val keeper = format.keeperByDefault
            state = FormationEditState(
                format = format,
                hasKeeper = keeper,
                slots = bothPhaseDefaults(format, keeper)
            )
        } else {
            viewModelScope.launch {
                repository.getFormation(formationId)?.let { entry ->
                    loaded = entry.formation
                    state = FormationEditState(
                        name = entry.formation.name,
                        format = entry.formation.format,
                        hasKeeper = entry.formation.hasKeeper,
                        notes = entry.formation.notes,
                        slots = entry.orderedSlots
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) {
        state = state.copy(name = value, nameError = null)
    }

    fun onNotesChange(value: String) {
        state = state.copy(notes = value)
    }

    fun onPhaseChange(phase: ShapePhase) {
        state = state.copy(phase = phase, selectedSlotIndex = null)
    }

    fun onFormatChange(format: MatchFormat) {
        // Changing format changes how many markers are needed, so rebuild both shapes.
        val keeper = format.keeperByDefault
        state = state.copy(
            format = format,
            hasKeeper = keeper,
            slots = bothPhaseDefaults(format, keeper),
            selectedSlotIndex = null
        )
    }

    fun onKeeperChange(hasKeeper: Boolean) {
        state = state.copy(
            hasKeeper = hasKeeper,
            slots = bothPhaseDefaults(state.format, hasKeeper),
            selectedSlotIndex = null
        )
    }

    fun onSlotMoved(slotIndex: Int, x: Float, y: Float) {
        val phase = state.phase
        state = state.copy(
            slots = state.slots.map {
                if (it.phase == phase && it.slotIndex == slotIndex) it.copy(x = x, y = y) else it
            }
        )
    }

    fun onSlotTapped(slotIndex: Int) {
        state = state.copy(
            selectedSlotIndex = if (state.selectedSlotIndex == slotIndex) null else slotIndex
        )
    }

    fun onSelectedRoleChange(role: Position) {
        val index = state.selectedSlotIndex ?: return
        val phase = state.phase
        state = state.copy(
            slots = state.slots.map {
                if (it.phase == phase && it.slotIndex == index) it.copy(role = role) else it
            }
        )
    }

    fun onSelectedLabelChange(label: String) {
        val index = state.selectedSlotIndex ?: return
        val phase = state.phase
        state = state.copy(
            slots = state.slots.map {
                if (it.phase == phase && it.slotIndex == index) it.copy(label = label.take(4)) else it
            }
        )
    }

    /**
     * Starts the current shape from the other one. Coaches usually move a handful
     * of players between phases rather than redrawing from scratch.
     */
    fun copyFromOtherPhase() {
        val target = state.phase
        val source =
            if (target == ShapePhase.DEFENDING) ShapePhase.ATTACKING else ShapePhase.DEFENDING
        val sourceSlots = state.slotsFor(source)
        if (sourceSlots.isEmpty()) return

        state = state.copy(
            slots = state.slots.filter { it.phase != target } +
                sourceSlots.map { it.copy(id = 0, phase = target) },
            selectedSlotIndex = null
        )
    }

    private fun bothPhaseDefaults(format: MatchFormat, hasKeeper: Boolean): List<FormationSlot> =
        defaultSlots(format, hasKeeper, ShapePhase.DEFENDING) +
            defaultSlots(format, hasKeeper, ShapePhase.ATTACKING)

    /** Spreads the required markers into evenly spaced lines as a starting point. */
    private fun defaultSlots(
        format: MatchFormat,
        hasKeeper: Boolean,
        phase: ShapePhase
    ): List<FormationSlot> {
        val slots = mutableListOf<FormationSlot>()
        var index = 0

        if (hasKeeper) {
            slots += FormationSlot(
                formationId = formationId,
                phase = phase,
                slotIndex = index++,
                role = Position.GOALKEEPER,
                x = 0.5f,
                y = 0.93f
            )
        }

        val outfield = format.outfieldCount(hasKeeper)
        val back = (outfield + 2) / 3
        val front = outfield / 3
        val middle = outfield - back - front

        // The attacking default starts a little higher up the pitch.
        val shift = if (phase == ShapePhase.ATTACKING) 0.06f else 0f

        fun line(count: Int, y: Float, role: Position) {
            repeat(count) { i ->
                slots += FormationSlot(
                    formationId = formationId,
                    phase = phase,
                    slotIndex = index++,
                    role = role,
                    x = (i + 1f) / (count + 1f),
                    y = (y - shift).coerceIn(0.03f, 0.97f)
                )
            }
        }

        line(back, 0.76f, Position.DEFENDER)
        line(middle, 0.50f, Position.MIDFIELDER)
        line(front, 0.22f, Position.FORWARD)
        return slots
    }

    fun save(onSaved: () -> Unit) {
        if (state.name.isBlank()) {
            state = state.copy(nameError = "Give the formation a name")
            return
        }
        viewModelScope.launch {
            val existing = loaded
            if (existing == null) {
                repository.addFormation(
                    Formation(
                        name = state.name.trim(),
                        format = state.format,
                        hasKeeper = state.hasKeeper,
                        notes = state.notes.trim(),
                        isPreset = false
                    ),
                    state.slots
                )
            } else {
                repository.updateFormation(
                    existing.copy(
                        name = state.name.trim(),
                        format = state.format,
                        hasKeeper = state.hasKeeper,
                        notes = state.notes.trim(),
                        // An edited preset becomes the coach's own formation.
                        isPreset = false
                    ),
                    state.slots
                )
            }
            onSaved()
        }
    }
}
