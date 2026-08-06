package com.bexner.soccerstats.ui.stats

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bexner.soccerstats.data.SoccerRepository
import com.bexner.soccerstats.stats.GameStats
import com.bexner.soccerstats.stats.SeasonStats
import com.bexner.soccerstats.stats.StatsCalculator
import com.bexner.soccerstats.stats.StatsExporter
import com.bexner.soccerstats.ui.navigation.Routes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Which set of numbers the screen is showing. */
enum class StatsScope(val label: String) {
    GAME("This game"),
    SEASON("Season")
}

class StatsViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: SoccerRepository
) : ViewModel() {

    /** Set when opened from a game; zero when opened from the team. */
    private val gameId: Long = savedStateHandle[Routes.GAME_ID_ARG] ?: 0L
    private val teamIdArg: Long = savedStateHandle[Routes.TEAM_ID_ARG] ?: 0L

    var scope by mutableStateOf(if (gameId != 0L) StatsScope.GAME else StatsScope.SEASON)
        private set

    var gameStats by mutableStateOf<GameStats?>(null)
        private set

    var seasonStats by mutableStateOf<SeasonStats?>(null)
        private set

    var isLoading by mutableStateOf(true)
        private set

    var message by mutableStateOf<String?>(null)
        private set

    private var teamName: String = ""

    init {
        refresh()
    }

    fun onScopeChange(value: StatsScope) {
        scope = value
    }

    fun dismissMessage() {
        message = null
    }

    /**
     * Aggregation runs off the main thread and is recomputed on demand rather
     * than observed — a finished game's numbers don't change while you look at
     * them, and recomputing a season on every database emission would be waste.
     */
    fun refresh() {
        viewModelScope.launch {
            isLoading = true
            val teamId = if (gameId != 0L) {
                repository.getGame(gameId)?.teamId ?: teamIdArg
            } else {
                teamIdArg
            }
            teamName = repository.getTeam(teamId)?.name.orEmpty()
            val players = repository.observeRoster(teamId).first()

            withContext(Dispatchers.Default) {
                if (gameId != 0L) {
                    repository.getGame(gameId)?.let { game ->
                        gameStats = StatsCalculator.forGame(
                            game = game,
                            players = players,
                            stints = repository.stintsFor(game.id),
                            events = repository.eventsFor(game.id)
                        )
                    }
                }

                val allGames = repository.playedGames(teamId).map { game ->
                    StatsCalculator.forGame(
                        game = game,
                        players = players,
                        stints = repository.stintsFor(game.id),
                        events = repository.eventsFor(game.id)
                    )
                }
                seasonStats = StatsCalculator.season(teamName, allGames)
            }
            isLoading = false
        }
    }

    fun exportCurrent(context: Context) {
        viewModelScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    if (scope == StatsScope.GAME) {
                        val stats = gameStats ?: return@withContext null
                        StatsExporter.writeGameWorkbook(context, stats, teamName)
                    } else {
                        val season = seasonStats ?: return@withContext null
                        StatsExporter.writeSeasonWorkbook(context, season)
                    }
                }
                if (file == null) {
                    message = "Nothing to export yet."
                } else {
                    StatsExporter.share(
                        context = context,
                        file = file,
                        subject = if (scope == StatsScope.GAME) "Game stats" else "Season stats"
                    )
                }
            } catch (e: Exception) {
                // Surfacing the reason beats a silent no-op when a share target
                // or the filesystem misbehaves.
                message = "Export failed: ${e.message ?: e::class.java.simpleName}"
            }
        }
    }
}
