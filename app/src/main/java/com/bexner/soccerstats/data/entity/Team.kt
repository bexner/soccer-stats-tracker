package com.bexner.soccerstats.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A team the coach manages. Rosters, schedules, formations and game stats
 * will all hang off of this record.
 */
@Entity(tableName = "teams")
data class Team(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val ageGroup: String = "",
    val season: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
