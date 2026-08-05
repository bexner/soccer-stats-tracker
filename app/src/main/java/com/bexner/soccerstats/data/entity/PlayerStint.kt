package com.bexner.soccerstats.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A continuous spell on the pitch for one player in one position.
 *
 * Times are **match elapsed milliseconds** — banked running clock, not wall
 * clock — so stoppages don't inflate anyone's minutes. Substituting closes the
 * outgoing player's stint and opens one for whoever replaces them, which is what
 * makes minutes-by-position computable after the fact.
 */
@Entity(
    tableName = "player_stints",
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
    indices = [Index("gameId"), Index("playerId"), Index(value = ["gameId", "playerId"])]
)
data class PlayerStint(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gameId: Long,
    val playerId: Long,
    /** Formation slot occupied during this spell. */
    val slotIndex: Int,
    val role: Position = Position.UNASSIGNED,
    val onAtMs: Long,
    /** Null while the player is still on the pitch. */
    val offAtMs: Long? = null
) {
    fun durationMsAt(matchElapsedMs: Long): Long =
        ((offAtMs ?: matchElapsedMs) - onAtMs).coerceAtLeast(0L)

    val isOpen: Boolean get() = offAtMs == null
}
