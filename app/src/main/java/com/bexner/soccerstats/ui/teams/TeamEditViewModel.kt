package com.bexner.soccerstats.ui.teams

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bexner.soccerstats.data.SoccerRepository
import com.bexner.soccerstats.data.entity.Team
import com.bexner.soccerstats.ui.navigation.Routes
import kotlinx.coroutines.launch

data class TeamFormState(
    val name: String = "",
    val ageGroup: String = "",
    val season: String = "",
    val nameError: String? = null
) {
    val isValid: Boolean get() = name.isNotBlank()
}

class TeamEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: SoccerRepository
) : ViewModel() {

    private val teamId: Long = savedStateHandle[Routes.TEAM_ID_ARG] ?: 0L

    val isNewTeam: Boolean = teamId == 0L

    var form by mutableStateOf(TeamFormState())
        private set

    private var loadedTeam: Team? = null

    init {
        if (!isNewTeam) {
            viewModelScope.launch {
                repository.getTeam(teamId)?.let { team ->
                    loadedTeam = team
                    form = TeamFormState(
                        name = team.name,
                        ageGroup = team.ageGroup,
                        season = team.season
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) {
        form = form.copy(name = value, nameError = null)
    }

    fun onAgeGroupChange(value: String) {
        form = form.copy(ageGroup = value)
    }

    fun onSeasonChange(value: String) {
        form = form.copy(season = value)
    }

    /** Returns true when the team was saved. */
    fun save(onSaved: () -> Unit) {
        if (form.name.isBlank()) {
            form = form.copy(nameError = "Team name is required")
            return
        }
        viewModelScope.launch {
            val existing = loadedTeam
            if (existing == null) {
                repository.addTeam(
                    Team(
                        name = form.name.trim(),
                        ageGroup = form.ageGroup.trim(),
                        season = form.season.trim()
                    )
                )
            } else {
                repository.updateTeam(
                    existing.copy(
                        name = form.name.trim(),
                        ageGroup = form.ageGroup.trim(),
                        season = form.season.trim()
                    )
                )
            }
            onSaved()
        }
    }
}
