package com.bexner.soccerstats.data.entity

/** Primary position a player lines up in. */
enum class Position(val label: String, val abbreviation: String) {
    GOALKEEPER("Goalkeeper", "GK"),
    DEFENDER("Defender", "DEF"),
    MIDFIELDER("Midfielder", "MID"),
    FORWARD("Forward", "FWD"),
    UNASSIGNED("Unassigned", "--");

    companion object {
        /** Order used in roster sorting: keepers first, then back to front. */
        val selectable: List<Position> = listOf(GOALKEEPER, DEFENDER, MIDFIELDER, FORWARD, UNASSIGNED)
    }
}
