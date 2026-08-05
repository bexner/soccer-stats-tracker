package com.bexner.soccerstats.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A player on a team's roster. Deleting a team deletes its players.
 */
@Entity(
    tableName = "players",
    foreignKeys = [
        ForeignKey(
            entity = Team::class,
            parentColumns = ["id"],
            childColumns = ["teamId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("teamId")]
)
data class Player(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val teamId: Long,
    val firstName: String,
    val lastName: String = "",
    val jerseyNumber: Int? = null,
    val position: Position = Position.UNASSIGNED,
    /** Inactive players stay on the roster but are excluded from game lineups. */
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    val fullName: String
        get() = if (lastName.isBlank()) firstName else "$firstName $lastName"

    val displayNumber: String
        get() = jerseyNumber?.toString() ?: "-"
}
