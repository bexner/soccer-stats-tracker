package com.bexner.soccerstats.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Which side an event belongs to. */
enum class EventSide(val label: String) {
    US("Us"),
    THEM("Them");

    companion object {
        val all: List<EventSide> = entries.toList()
        fun fromName(value: String): EventSide =
            runCatching { valueOf(value) }.getOrDefault(US)
    }
}

/**
 * Everything the live screen can record.
 *
 * [attributable] marks events where naming a player is meaningful — the live
 * screen prompts for one on those and skips the prompt otherwise, which keeps
 * taps down while a game is actually happening.
 */
enum class EventType(
    val label: String,
    val short: String,
    val attributable: Boolean = true
) {
    GOAL("Goal", "GOAL"),
    ASSIST("Assist", "AST"),
    SHOT_ON("Shot on target", "SOT"),
    SHOT_OFF("Shot off target", "SHOT"),
    SAVE("Save", "SAVE"),
    CORNER("Corner", "CK", attributable = false),
    FREE_KICK("Free kick", "FK", attributable = false),
    FOUL("Foul", "FOUL"),
    TACKLE("Tackle won", "TKL"),
    FIFTY_FIFTY("50/50 won", "50/50"),
    OFFSIDE("Offside", "OFF"),
    YELLOW_CARD("Yellow card", "YC"),
    RED_CARD("Red card", "RC"),
    SUBSTITUTION("Substitution", "SUB"),
    PERIOD_START("Period start", "START", attributable = false),
    PERIOD_END("Period end", "END", attributable = false);

    companion object {
        /** Buttons offered on the live screen, in tap-frequency order. */
        val loggable: List<EventType> = listOf(
            GOAL, SHOT_ON, SHOT_OFF, SAVE, CORNER, FREE_KICK,
            TACKLE, FIFTY_FIFTY, FOUL, OFFSIDE, YELLOW_CARD, RED_CARD
        )

        /**
         * Events worth pinning to a spot in the goal mouth — anything that
         * reached the net. Everything else skips that prompt.
         */
        val placementRelevant: Set<EventType> = setOf(GOAL, SHOT_ON, SAVE)

        fun fromName(value: String): EventType =
            runCatching { valueOf(value) }.getOrDefault(GOAL)
    }
}

@Entity(
    tableName = "game_events",
    foreignKeys = [
        ForeignKey(
            entity = Game::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("gameId"), Index(value = ["gameId", "clockMs"])]
)
data class GameEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gameId: Long,
    val type: EventType,
    val side: EventSide = EventSide.US,
    /**
     * Player credited. Nullable rather than a foreign key on purpose — opponent
     * events have no player of ours, and deleting a player shouldn't erase the
     * fact that a goal was scored.
     */
    val playerId: Long? = null,
    /** Second player, for assists on a goal or the incoming player on a sub. */
    val secondaryPlayerId: Long? = null,
    val period: Int = 1,
    /** Match elapsed ms when it happened. */
    val clockMs: Long,
    /**
     * Where on the pitch it happened, normalized 0f..1f, or null if it was
     * logged from the quick buttons. Always stored with **your attacking
     * direction upward** (y = 0f is the goal you're attacking) no matter which
     * end you're actually playing, so positions stay comparable across halves
     * and across games.
     */
    val pitchX: Float? = null,
    val pitchY: Float? = null,
    /**
     * Where it finished in the goal mouth, normalized 0f..1f with x = 0f at the
     * left post and y = 0f at the crossbar. Null unless placement was recorded.
     */
    val goalX: Float? = null,
    val goalY: Float? = null,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    /** mm:ss as it would read on the match clock. */
    val clockLabel: String
        get() {
            val totalSeconds = clockMs / 1000
            return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
        }

    val hasPitchPosition: Boolean get() = pitchX != null && pitchY != null

    val hasGoalPlacement: Boolean get() = goalX != null && goalY != null

    /** Which third of the pitch, named from your attacking perspective. */
    val pitchThird: String?
        get() = pitchY?.let {
            when {
                it >= 0.667f -> "Defensive third"
                it >= 0.333f -> "Middle third"
                else -> "Attacking third"
            }
        }

    /**
     * Nine-box description of where it hit the goal, e.g. "Top left".
     * Reads left-to-right from the shooter's point of view.
     */
    val goalZone: String?
        get() {
            val x = goalX ?: return null
            val y = goalY ?: return null
            val vertical = when {
                y < 0.333f -> "Top"
                y < 0.667f -> "Middle"
                else -> "Bottom"
            }
            val horizontal = when {
                x < 0.333f -> "left"
                x < 0.667f -> "centre"
                else -> "right"
            }
            return "$vertical $horizontal"
        }
}
