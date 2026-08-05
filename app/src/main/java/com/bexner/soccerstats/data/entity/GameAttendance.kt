package com.bexner.soccerstats.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One player's availability for one game. Rows are created lazily — a player
 * with no row is [Attendance.UNKNOWN].
 */
@Entity(
    tableName = "game_attendance",
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
    indices = [Index(value = ["gameId", "playerId"], unique = true), Index("playerId")]
)
data class GameAttendance(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gameId: Long,
    val playerId: Long,
    val status: Attendance = Attendance.UNKNOWN
)
