package com.bexner.soccerstats.ui.games

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bexner.soccerstats.data.SoccerRepository
import com.bexner.soccerstats.data.entity.Game
import com.bexner.soccerstats.data.entity.Venue
import com.bexner.soccerstats.ui.navigation.Routes
import kotlinx.coroutines.launch
import java.util.Calendar

data class GameFormState(
    val opponent: String = "",
    val venue: Venue = Venue.HOME,
    val kickoffAt: Long = nextSaturdayMorning(),
    val location: String = "",
    val periodMinutes: String = "30",
    val opponentError: String? = null
) {
    val isValid: Boolean get() = opponent.isNotBlank()
}

/** Defaults a new game to the coming Saturday at 9am, the usual youth slot. */
private fun nextSaturdayMorning(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 9)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
        cal.add(Calendar.DAY_OF_MONTH, 1)
    }
    return cal.timeInMillis
}

class GameEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: SoccerRepository
) : ViewModel() {

    private val teamId: Long = savedStateHandle[Routes.TEAM_ID_ARG] ?: 0L
    private val gameId: Long = savedStateHandle[Routes.GAME_ID_ARG] ?: 0L

    val isNew: Boolean = gameId == 0L

    var form by mutableStateOf(GameFormState())
        private set

    private var loaded: Game? = null

    init {
        if (!isNew) {
            viewModelScope.launch {
                repository.getGame(gameId)?.let { game ->
                    loaded = game
                    form = GameFormState(
                        opponent = game.opponent,
                        venue = game.venue,
                        kickoffAt = game.kickoffAt,
                        location = game.location,
                        periodMinutes = game.periodMinutes.toString()
                    )
                }
            }
        }
    }

    fun onOpponentChange(value: String) {
        form = form.copy(opponent = value, opponentError = null)
    }

    fun onVenueChange(venue: Venue) {
        form = form.copy(venue = venue)
    }

    fun onLocationChange(value: String) {
        form = form.copy(location = value)
    }

    fun onPeriodMinutesChange(value: String) {
        form = form.copy(periodMinutes = value.filter { it.isDigit() }.take(3))
    }

    /** Keeps the existing time of day when only the date changes. */
    fun onDateChange(dateMillisUtc: Long) {
        val existing = Calendar.getInstance().apply { timeInMillis = form.kickoffAt }
        // The date picker reports UTC midnight; read the fields in UTC to avoid
        // landing on the previous day for anyone west of Greenwich.
        val picked = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = dateMillisUtc
        }
        val merged = Calendar.getInstance().apply {
            set(Calendar.YEAR, picked.get(Calendar.YEAR))
            set(Calendar.MONTH, picked.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, picked.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, existing.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, existing.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        form = form.copy(kickoffAt = merged.timeInMillis)
    }

    fun onTimeChange(hour: Int, minute: Int) {
        val merged = Calendar.getInstance().apply {
            timeInMillis = form.kickoffAt
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        form = form.copy(kickoffAt = merged.timeInMillis)
    }

    fun save(onSaved: (Long) -> Unit) {
        if (form.opponent.isBlank()) {
            form = form.copy(opponentError = "Opponent is required")
            return
        }
        viewModelScope.launch {
            val minutes = form.periodMinutes.toIntOrNull()?.coerceIn(1, 60) ?: 30
            val existing = loaded
            if (existing == null) {
                val newId = repository.addGame(
                    Game(
                        teamId = teamId,
                        opponent = form.opponent.trim(),
                        venue = form.venue,
                        kickoffAt = form.kickoffAt,
                        location = form.location.trim(),
                        periodMinutes = minutes
                    )
                )
                onSaved(newId)
            } else {
                repository.updateGame(
                    existing.copy(
                        opponent = form.opponent.trim(),
                        venue = form.venue,
                        kickoffAt = form.kickoffAt,
                        location = form.location.trim(),
                        periodMinutes = minutes
                    )
                )
                onSaved(existing.id)
            }
        }
    }
}
