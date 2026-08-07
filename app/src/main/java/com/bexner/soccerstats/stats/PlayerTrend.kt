package com.bexner.soccerstats.stats

import com.bexner.soccerstats.data.entity.Game

/** One game's line in a player's history. */
data class PlayerGameLine(
    val game: Game,
    val stats: PlayerStats
)

/** Raw counts, or counts normalized to a full game's worth of minutes. */
enum class RateMode(val label: String) {
    TOTAL("Total"),
    PER_60("Per 60 min")
}

/**
 * A stat that can be charted over time.
 *
 * [keeperOnly] metrics are measured against time in goal rather than total
 * minutes, and are only offered for players who actually kept goal — a striker's
 * "goals conceded per 60" would be meaningless.
 */
enum class TrendMetric(
    val label: String,
    val keeperOnly: Boolean = false,
    private val extract: (PlayerStats) -> Int
) {
    GOALS("Goals", extract = { it.goals }),
    ASSISTS("Assists", extract = { it.assists }),
    SHOTS_ON("Shots on goal", extract = { it.shotsOn }),
    SHOTS("Shots", extract = { it.shots }),
    TACKLES("Tackles", extract = { it.tackles }),
    FIFTY_FIFTIES("50/50s", extract = { it.fiftyFifties }),
    SAVES("Saves", keeperOnly = true, extract = { it.saves }),
    GOALS_CONCEDED("Goals conceded", keeperOnly = true, extract = { it.goalsConceded }),
    MINUTES("Minutes", extract = { it.minutes.toInt() });

    fun rawValue(stats: PlayerStats): Int = extract(stats)

    /**
     * The minutes this metric should be measured against. Keeper metrics use
     * time in goal; everything else uses total time on the pitch.
     */
    fun baseMs(stats: PlayerStats): Long =
        if (keeperOnly) stats.keeperMs else stats.minutesMs

    /**
     * Value in the requested mode. Returns null when a rate can't be computed —
     * no minutes played means no rate, which a chart should show as a gap rather
     * than as zero.
     */
    fun value(stats: PlayerStats, mode: RateMode): Double? = when (mode) {
        RateMode.TOTAL -> rawValue(stats).toDouble()
        RateMode.PER_60 -> {
            val base = baseMs(stats)
            // Minutes-as-a-metric has no meaningful rate against itself.
            if (base <= 0L || this == MINUTES) null
            else rawValue(stats).toDouble() * 60.0 / (base / 60000.0)
        }
    }

    companion object {
        /** Metrics worth offering for this player, keeper ones only if relevant. */
        fun availableFor(stats: PlayerStats): List<TrendMetric> =
            entries.filter { !it.keeperOnly || stats.playedInGoal }
    }
}

/** A player's whole season, game by game, with the totals already folded. */
data class PlayerTrend(
    val playerName: String,
    val jerseyNumber: Int?,
    val lines: List<PlayerGameLine>,
    val total: PlayerStats
) {
    /** Games where the player actually took the field, oldest first. */
    val playedLines: List<PlayerGameLine>
        get() = lines.filter { it.stats.minutesMs > 0 }.sortedBy { it.game.kickoffAt }

    val keptGoal: Boolean get() = total.playedInGoal

    fun series(metric: TrendMetric, mode: RateMode): List<Pair<PlayerGameLine, Double?>> =
        playedLines.map { it to metric.value(it.stats, mode) }

    /**
     * Season figure for a metric. Computed from the folded totals rather than by
     * averaging per-game rates, which would over-weight short appearances.
     */
    fun overall(metric: TrendMetric, mode: RateMode): Double? = metric.value(total, mode)

    companion object {
        fun from(playerId: Long, season: SeasonStats): PlayerTrend? {
            val lines = season.games.mapNotNull { game ->
                game.players.firstOrNull { it.player.id == playerId }
                    ?.let { PlayerGameLine(game.game, it) }
            }
            val any = lines.firstOrNull() ?: return null
            val total = lines
                .map { it.stats }
                .filter { it.minutesMs > 0 || it.gamesPlayed > 0 }
                .reduceOrNull { a, b -> a + b }
                ?: any.stats
            return PlayerTrend(
                playerName = any.stats.player.fullName,
                jerseyNumber = any.stats.player.jerseyNumber,
                lines = lines,
                total = total
            )
        }
    }
}
