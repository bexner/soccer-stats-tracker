package com.bexner.soccerstats.data.entity

/**
 * Number of players a side fields, including the keeper where one is used.
 * Youth soccer steps up through these as players age.
 */
enum class MatchFormat(
    val label: String,
    val playersOnField: Int,
    /** 4v4 is commonly played without a keeper, so this is a hint, not a rule. */
    val keeperByDefault: Boolean
) {
    FOUR_V_FOUR("4v4", 4, keeperByDefault = false),
    SEVEN_V_SEVEN("7v7", 7, keeperByDefault = true),
    NINE_V_NINE("9v9", 9, keeperByDefault = true),
    ELEVEN_V_ELEVEN("11v11", 11, keeperByDefault = true);

    /** Outfield slots available once the keeper is accounted for. */
    fun outfieldCount(hasKeeper: Boolean): Int =
        if (hasKeeper) playersOnField - 1 else playersOnField

    companion object {
        val all: List<MatchFormat> = entries.toList()

        fun fromName(value: String): MatchFormat =
            runCatching { valueOf(value) }.getOrDefault(SEVEN_V_SEVEN)
    }
}
