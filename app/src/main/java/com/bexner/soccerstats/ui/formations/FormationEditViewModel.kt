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
import com.bexner.soccerstats.ui.navigation.Routes
import kotlinx.coroutines.launch

/**
 * Slots are held in memory as the coach drags them and written in one go on save,
 * so a half-finished shape never lands in the database.
 */
data class FormationEditState(
    val name: String = "",
    val format: MatchFormat = MatchFormat.SEVEN_V_SEVEN,
    val hasKeeper: Boolean = true,
    val slots: List<FormationSlot> = emptyList(),
    val selectedSlotIndex: Int? = null,
    val nameError: String? = null
) {
    val placed: Int get() = slots.size
    val required: Int get() = format.playersOnField
    val isComplete: Boolean get() = placed == required
    val isValid: Boolean get() = name.isNotBlank() && isComplete
}

class FormationEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: SoccerRepository
) : ViewModel() {

    private val formationId: Long = savedStateHandle[Routes.FORMATION_ID_ARG] ?: 0L
    private val initialFormat: String = savedStateHandle[Routes.FORMAT_ARG] ?: MatchFormat.SEVEN_V_SEVEN.name

    val isNew: Boolean = formationId == 0L

    var state by mutableStateOf(FormationEditState())
        private set

    private var loaded: Formation? = null

    init {
        if (isNew) {
            val format = MatchFormat.fromName(initialFormat)
            state = FormationEditState(
                format = format,
                hasKeeper = format.keeperByDefault,
                slots = defaultSlots(format, format.keeperByDefault)
            )
        } else {
            viewModelScope.launch {
                repository.getFormation(formationId)?.let { entry ->
                    loaded = entry.formation
                    state = FormationEditState(
                        name = entry.formation.name,
                        format = entry.formation.format,
                        hasKeeper = entry.formation.hasKeeper,
                        slots = entry.orderedSlots
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) {
        state = state.copy(name = value, nameError = null)
    }

    fun onFormatChange(format: MatchFormat) {
        // Changing format changes how many markers are needed, so rebuild the shape.
        val keeper = format.keeperByDefault
        state = state.copy(
            format = format,
            hasKeeper = keeper,
            slots = defaultSlots(format, keeper),
            selectedSlotIndex = null
        )
    }

    fun onKeeperChange(hasKeeper: Boolean) {
        state = state.copy(
            hasKeeper = hasKeeper,
            slots = defaultSlots(state.format, hasKeeper),
            selectedSlotIndex = null
        )
    }

    fun onSlotMoved(slotIndex: Int, x: Float, y: Float) {
        state = state.copy(
            slots = state.slots.map {
                if (it.slotIndex == slotIndex) it.copy(x = x, y = y) else it
            }
        )
    }

    fun onSlotSelected(slotIndex: Int?) {
        state = state.copy(selectedSlotIndex = slotIndex)
    }

    /** Toggles selection. Lives here so the tap handler captures no screen state. */
    fun onSlotTapped(slotIndex: Int) {
        state = state.copy(
            selectedSlotIndex = if (state.selectedSlotIndex == slotIndex) null else slotIndex
        )
    }

    fun onSelectedRoleChange(role: Position) {
        val index = state.selectedSlotIndex ?: return
        state = state.copy(
            slots = state.slots.map {
                if (it.slotIndex == index) it.copy(role = role) else it
            }
        )
    }

    fun onSelectedLabelChange(label: String) {
        val index = state.selectedSlotIndex ?: return
        state = state.copy(
            slots = state.slots.map {
                if (it.slotIndex == index) it.copy(label = label.take(4)) else it
            }
        )
    }

    /** Spreads the required markers into evenly spaced lines as a starting point. */
    private fun defaultSlots(format: MatchFormat, hasKeeper: Boolean): List<FormationSlot> {
        val slots = mutableListOf<FormationSlot>()
        var index = 0

        if (hasKeeper) {
            slots += FormationSlot(
                formationId = formationId,
                slotIndex = index++,
                role = Position.GOALKEEPER,
                x = 0.5f,
                y = 0.93f
            )
        }

        val outfield = format.outfieldCount(hasKeeper)
        // Split roughly into thirds: back, middle, front.
        val back = (outfield + 2) / 3
        val front = outfield / 3
        val middle = outfield - back - front

        fun line(count: Int, y: Float, role: Position) {
            repeat(count) { i ->
                val x = (i + 1f) / (count + 1f)
                slots += FormationSlot(
                    formationId = formationId,
                    slotIndex = index++,
                    role = role,
                    x = x,
                    y = y
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
