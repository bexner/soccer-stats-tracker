package com.bexner.soccerstats.stats

import com.bexner.soccerstats.data.entity.EventSide
import com.bexner.soccerstats.data.entity.EventType
import com.bexner.soccerstats.data.entity.Game
import com.bexner.soccerstats.data.entity.GameEvent
import com.bexner.soccerstats.data.entity.Player
import com.bexner.soccerstats.data.entity.PlayerStint
import com.bexner.soccerstats.data.entity.Position

/** Counts for one player, over one game or a whole season. */
data class PlayerStats(
    val player: Player,
    val gamesPlayed: Int = 0,
    val minutesMs: Long = 0,
    val goals: Int = 0,
    val assists: Int = 0,
    val shotsOn: Int = 0,
    val shotsOff: Int = 0,
    val saves: Int = 0,
    val tackles: Int = 0,
    val fiftyFifties: Int = 0,
    val fouls: Int = 0,
    val offsides: Int = 0,
    val yellowCards: Int = 0,
    val redCards: Int = 0,
    /** Match time in each position, keyed by role. */
    val minutesByPosition: Map<Position, Long> = emptyMap()
) {
    val shots: Int get() = shotsOn + shotsOff

    val minutes: Long get() = minutesMs / 60000

    /** Null rather than zero when nothing was attempted, so tables can show "—". */
    val shotAccuracy: Double?
        get() = if (shots == 0) null else shotsOn.toDouble() / shots

    operator fun plus(other: PlayerStats): PlayerStats {
        val merged = minutesByPosition.toMutableMap()
        other.minutesByPosition.forEach { (role, ms) ->
            merged[role] = (merged[role] ?: 0L) + ms
        }
        return copy(
            gamesPlayed = gamesPlayed + other.gamesPlayed,
            minutesMs = minutesMs + other.minutesMs,
            goals = goals + other.goals,
            assists = assists + other.assists,
            shotsOn = shotsOn + other.shotsOn,
            shotsOff = shotsOff + other.shotsOff,
            saves = saves + other.saves,
            tackles = tackles + other.tackles,
            fiftyFifties = fiftyFifties + other.fiftyFifties,
            fouls = fouls + other.fouls,
            offsides = offsides + other.offsides,
            yellowCards = yellowCards + other.yellowCards,
            redCards = redCards + other.redCards,
            minutesByPosition = merged
        )
    }
}

/** Team-level counts for and against. */
data class TeamTotals(
    val goalsFor: Int = 0,
    val goalsAgainst: Int = 0,
    val shotsFor: Int = 0,
    val shotsAgainst: Int = 0,
    val shotsOnFor: Int = 0,
    val shotsOnAgainst: Int = 0,
    val cornersFor: Int = 0,
    val cornersAgainst: Int = 0,
    val foulsFor: Int = 0,
    val foulsAgainst: Int = 0,
    val savesFor: Int = 0
) {
    operator fun plus(other: TeamTotals) = TeamTotals(
        goalsFor + other.goalsFor,
        goalsAgainst + other.goalsAgainst,
        shotsFor + other.shotsFor,
        shotsAgainst + other.shotsAgainst,
        shotsOnFor + other.shotsOnFor,
        shotsOnAgainst + other.shotsOnAgainst,
        cornersFor + other.cornersFor,
        cornersAgainst + other.cornersAgainst,
        foulsFor + other.foulsFor,
        foulsAgainst + other.foulsAgainst,
        savesFor + other.savesFor
    )
}

/** Everything computed for a single game. */
data class GameStats(
    val game: Game,
    val totals: TeamTotals,
    val players: List<PlayerStats>,
    val events: List<GameEvent>
) {
    val result: String
        get() = when {
            game.goalsFor > game.goalsAgainst -> "W"
            game.goalsFor < game.goalsAgainst -> "L"
            else -> "D"
        }
}

/** Season roll-up across every finished or in-progress game. */
data class SeasonStats(
    val teamName: String,
    val games: List<GameStats> = emptyList(),
    val players: List<PlayerStats> = emptyList()
) {
    val totals: TeamTotals
        get() = games.fold(TeamTotals()) { acc, g -> acc + g.totals }

    val wins: Int get() = games.count { it.result == "W" }
    val draws: Int get() = games.count { it.result == "D" }
    val losses: Int get() = games.count { it.result == "L" }

    val record: String get() = "$wins-$draws-$losses"

    val totalMinutesMs: Long get() = players.sumOf { it.minutesMs }

    val averageMinutesMs: Long
        get() = if (players.isEmpty()) 0L else totalMinutesMs / players.size
}

/**
 * Turns raw events and stints into counts.
 *
 * Kept as a pure function of its inputs — no database access — so the same code
 * serves the on-screen tables and the exported workbook, and so it can be
 * reasoned about (and checked) in isolation.
 */
object StatsCalculator {

    fun forGame(
        game: Game,
        players: List<Player>,
        stints: List<PlayerStint>,
        events: List<GameEvent>,
        /** Match time to measure open stints against; the final whistle by default. */
        elapsedMs: Long = game.clockElapsedMs
    ): GameStats {
        val ours = events.filter { it.side == EventSide.US }
        val theirs = events.filter { it.side == EventSide.THEM }

        fun count(list: List<GameEvent>, type: EventType) = list.count { it.type == type }

        val totals = TeamTotals(
            goalsFor = game.goalsFor,
            goalsAgainst = game.goalsAgainst,
            shotsFor = count(ours, EventType.SHOT_ON) + count(ours, EventType.SHOT_OFF),
            shotsAgainst = count(theirs, EventType.SHOT_ON) + count(theirs, EventType.SHOT_OFF),
            shotsOnFor = count(ours, EventType.SHOT_ON),
            shotsOnAgainst = count(theirs, EventType.SHOT_ON),
            cornersFor = count(ours, EventType.CORNER),
            cornersAgainst = count(theirs, EventType.CORNER),
            foulsFor = count(ours, EventType.FOUL),
            foulsAgainst = count(theirs, EventType.FOUL),
            savesFor = count(ours, EventType.SAVE)
        )

        val playerStats = players.map { player ->
            val own = ours.filter { it.playerId == player.id }
            val mine = stints.filter { it.playerId == player.id }
            val minutesMs = mine.sumOf { it.durationMsAt(elapsedMs) }

            PlayerStats(
                player = player,
                gamesPlayed = if (mine.isNotEmpty()) 1 else 0,
                minutesMs = minutesMs,
                goals = own.count { it.type == EventType.GOAL },
                // An assist is credited to the secondary player on a goal.
                assists = ours.count {
                    it.type == EventType.GOAL && it.secondaryPlayerId == player.id
                } + own.count { it.type == EventType.ASSIST },
                shotsOn = own.count { it.type == EventType.SHOT_ON },
                shotsOff = own.count { it.type == EventType.SHOT_OFF },
                saves = own.count { it.type == EventType.SAVE },
                tackles = own.count { it.type == EventType.TACKLE },
                fiftyFifties = own.count { it.type == EventType.FIFTY_FIFTY },
                fouls = own.count { it.type == EventType.FOUL },
                offsides = own.count { it.type == EventType.OFFSIDE },
                yellowCards = own.count { it.type == EventType.YELLOW_CARD },
                redCards = own.count { it.type == EventType.RED_CARD },
                minutesByPosition = mine
                    .groupBy { it.role }
                    .mapValues { (_, list) -> list.sumOf { it.durationMsAt(elapsedMs) } }
            )
        }

        return GameStats(game = game, totals = totals, players = playerStats, events = events)
    }

    /** Folds per-game stats into one row per player. */
    fun season(teamName: String, games: List<GameStats>): SeasonStats {
        val byPlayer = LinkedHashMap<Long, PlayerStats>()
        games.forEach { game ->
            game.players.forEach { stats ->
                // Only count a game for players who actually took the field.
                if (stats.gamesPlayed == 0 && stats.minutesMs == 0L) return@forEach
                val existing = byPlayer[stats.player.id]
                byPlayer[stats.player.id] = if (existing == null) stats else existing + stats
            }
        }
        return SeasonStats(
            teamName = teamName,
            games = games,
            players = byPlayer.values.sortedByDescending { it.minutesMs }
        )
    }
}
