package com.bexner.soccerstats.ui.roster

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bexner.soccerstats.data.SoccerRepository
import com.bexner.soccerstats.data.entity.Player
import com.bexner.soccerstats.data.entity.Position
import com.bexner.soccerstats.ui.navigation.Routes
import kotlinx.coroutines.launch

data class PlayerFormState(
    val firstName: String = "",
    val lastName: String = "",
    val jerseyNumber: String = "",
    val position: Position = Position.UNASSIGNED,
    val isActive: Boolean = true,
    val firstNameError: String? = null,
    val jerseyError: String? = null
) {
    val isValid: Boolean get() = firstName.isNotBlank()
}

class PlayerEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: SoccerRepository
) : ViewModel() {

    private val teamId: Long = savedStateHandle[Routes.TEAM_ID_ARG] ?: 0L
    private val playerId: Long = savedStateHandle[Routes.PLAYER_ID_ARG] ?: 0L

    val isNewPlayer: Boolean = playerId == 0L

    var form by mutableStateOf(PlayerFormState())
        private set

    private var loadedPlayer: Player? = null

    init {
        if (!isNewPlayer) {
            viewModelScope.launch {
                repository.getPlayer(playerId)?.let { player ->
                    loadedPlayer = player
                    form = PlayerFormState(
                        firstName = player.firstName,
                        lastName = player.lastName,
                        jerseyNumber = player.jerseyNumber?.toString() ?: "",
                        position = player.position,
                        isActive = player.isActive
                    )
                }
            }
        }
    }

    fun onFirstNameChange(value: String) {
        form = form.copy(firstName = value, firstNameError = null)
    }

    fun onLastNameChange(value: String) {
        form = form.copy(lastName = value)
    }

    fun onJerseyChange(value: String) {
        // Numbers only, max three digits.
        val filtered = value.filter { it.isDigit() }.take(3)
        form = form.copy(jerseyNumber = filtered, jerseyError = null)
    }

    fun onPositionChange(position: Position) {
        form = form.copy(position = position)
    }

    fun onActiveChange(active: Boolean) {
        form = form.copy(isActive = active)
    }

    fun save(onSaved: () -> Unit) {
        if (form.firstName.isBlank()) {
            form = form.copy(firstNameError = "First name is required")
            return
        }

        viewModelScope.launch {
            val number = form.jerseyNumber.toIntOrNull()
            if (number != null && repository.isJerseyTaken(teamId, number, playerId)) {
                form = form.copy(jerseyError = "#$number is already taken on this team")
                return@launch
            }

            val existing = loadedPlayer
            if (existing == null) {
                repository.addPlayer(
                    Player(
                        teamId = teamId,
                        firstName = form.firstName.trim(),
                        lastName = form.lastName.trim(),
                        jerseyNumber = number,
                        position = form.position,
                        isActive = form.isActive
                    )
                )
            } else {
                repository.updatePlayer(
                    existing.copy(
                        firstName = form.firstName.trim(),
                        lastName = form.lastName.trim(),
                        jerseyNumber = number,
                        position = form.position,
                        isActive = form.isActive
                    )
                )
            }
            onSaved()
        }
    }
}
