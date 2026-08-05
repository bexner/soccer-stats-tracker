package com.bexner.soccerstats.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Binds a player to one position in the game's formation.
 *
 * Keyed on [slotIndex] rather than the marker's label, because a formation's two
 * shapes share slot indexes — so a single lineup drives both the defending and
 * attacking view without reassigning anyone.
 */
@Entity(
    tableName = "lineup_slots",
    foreignKeys = [
        ForeignKey(
            entity = Game::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Player::class,
            parentColumns = ["id"],
            childColumns = ["playerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["gameId", "slotIndex"], unique = true), Index("playerId")]
)
data class LineupSlot(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gameId: Long,
    val slotIndex: Int,
    val playerId: Long
)
