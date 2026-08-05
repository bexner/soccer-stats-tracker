package com.bexner.soccerstats.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class Venue(val label: String) {
    HOME("Home"),
    AWAY("Away"),
    NEUTRAL("Neutral");

    companion object {
        val all: List<Venue> = entries.toList()
        fun fromName(value: String): Venue =
            runCatching { valueOf(value) }.getOrDefault(HOME)
    }
}

/** Where a game is in its lifecycle. Drives what the game screen offers. */
enum class GameStatus(val label: String) {
    SCHEDULED("Scheduled"),
    IN_PROGRESS("In progress"),
    FINAL("Final");

    companion object {
        fun fromName(value: String): GameStatus =
            runCatching { valueOf(value) }.getOrDefault(SCHEDULED)
    }
}

@Entity(
    tableName = "games",
    foreignKeys = [
        ForeignKey(
            entity = Team::class,
            parentColumns = ["id"],
            childColumns = ["teamId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("teamId"), Index("teamId", "kickoffAt")]
)
data class Game(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val teamId: Long,
    val opponent: String,
    val venue: Venue = Venue.HOME,
    /** Epoch millis of kickoff, local time as entered by the coach. */
    val kickoffAt: Long,
    val location: String = "",
    val notes: String = "",
    val status: GameStatus = GameStatus.SCHEDULED,
    /** Chosen when the lineup is built; null until then. */
    val formationId: Long? = null,
    val periodCount: Int = 2,
    val periodMinutes: Int = 30,
    /** Running score, updated as goals are logged. */
    val goalsFor: Int = 0,
    val goalsAgainst: Int = 0,
    // ---- Clock ----
    // Persisted so a phone lock, app kill or rotation can't lose match time.
    /** Running match time already banked, in ms. Excludes any active run. */
    val clockElapsedMs: Long = 0,
    /** Wall-clock ms when the clock was last started, or null when stopped. */
    val clockRunningSince: Long? = null,
    val currentPeriod: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
) {
    val totalMinutes: Int get() = periodCount * periodMinutes

    val isClockRunning: Boolean get() = clockRunningSince != null

    /** Match time right now, banked plus whatever the active run has added. */
    fun elapsedMsAt(now: Long = System.currentTimeMillis()): Long =
        clockElapsedMs + (clockRunningSince?.let { (now - it).coerceAtLeast(0L) } ?: 0L)
}
