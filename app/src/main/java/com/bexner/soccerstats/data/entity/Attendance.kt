package com.bexner.soccerstats.data.entity

/** Whether a player is expected at a game. */
enum class Attendance(val label: String, val symbol: String) {
    YES("Yes", "Y"),
    MAYBE("Maybe", "?"),
    NO("No", "N"),
    UNKNOWN("Not set", "–");

    /** Yes and Maybe players are offerable to the lineup. */
    val isAvailable: Boolean get() = this == YES || this == MAYBE

    companion object {
        /** Order shown in the attendance toggle. */
        val selectable: List<Attendance> = listOf(YES, MAYBE, NO)

        fun fromName(value: String): Attendance =
            runCatching { valueOf(value) }.getOrDefault(UNKNOWN)
    }
}
