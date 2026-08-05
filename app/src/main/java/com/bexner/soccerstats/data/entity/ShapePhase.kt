package com.bexner.soccerstats.data.entity

/**
 * A formation holds two shapes for the same players: where they stand when the
 * other team has the ball, and where they go once you win it.
 */
enum class ShapePhase(val label: String) {
    DEFENDING("Defending"),
    ATTACKING("Attacking");

    companion object {
        val all: List<ShapePhase> = entries.toList()

        fun fromName(value: String): ShapePhase =
            runCatching { valueOf(value) }.getOrDefault(DEFENDING)
    }
}
