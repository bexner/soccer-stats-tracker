package com.bexner.soccerstats.data.entity

import androidx.room.Embedded

/** Team row plus a roster size, used by the team list screen. */
data class TeamWithPlayerCount(
    @Embedded val team: Team,
    val playerCount: Int
)
