package com.bexner.soccerstats.ui.stats

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bexner.soccerstats.data.SoccerRepository
import com.bexner.soccerstats.stats.PlayerTrend
import com.bexner.soccerstats.stats.RateMode
import com.bexner.soccerstats.stats.StatsCalculator
import com.bexner.soccerstats.stats.TrendMetric
import com.bexner.soccerstats.ui.navigation.Routes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerStatsViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: SoccerRepository
) : ViewModel() {

    private val teamId: Long = savedStateHandle[Routes.TEAM_ID_ARG] ?: 0L
    private val playerId: Long = savedStateHandle[Routes.PLAYER_ID_ARG] ?: 0L

    var trend by mutableStateOf<PlayerTrend?>(null)
        private set

    var metric by mutableStateOf(TrendMetric.GOALS)
        private set

    var rateMode by mutableStateOf(RateMode.TOTAL)
        private set

    var isLoading by mutableStateOf(true)
        private set

    init {
        viewModelScope.launch {
            isLoading = true
            val teamName = repository.getTeam(teamId)?.name.orEmpty()
            val players = repository.observeRoster(teamId).first()

            val computed = withContext(Dispatchers.Default) {
                val games = repository.playedGames(teamId).map { game ->
                    StatsCalculator.forGame(
                        game = game,
                        players = players,
                        stints = repository.stintsFor(game.id),
                        events = repository.eventsFor(game.id)
                    )
                }
                PlayerTrend.from(playerId, StatsCalculator.season(teamName, games))
            }
            trend = computed

            // Land on a metric that means something for this player rather than
            // defaulting a keeper to "Goals".
            computed?.let {
                if (it.keptGoal && it.total.goals == 0) metric = TrendMetric.SAVES
            }
            isLoading = false
        }
    }

    fun onMetricChange(value: TrendMetric) {
        metric = value
    }

    fun onRateModeChange(value: RateMode) {
        rateMode = value
    }
}
