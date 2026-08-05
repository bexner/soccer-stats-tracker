package com.bexner.soccerstats.data.entity

import androidx.room.Embedded

/** Game row plus availability counts, for the schedule list. */
data class GameWithCounts(
    @Embedded val game: Game,
    val availableCount: Int,
    val lineupCount: Int
)
