package com.bexner.soccerstats.data.entity

import androidx.room.Embedded
import androidx.room.Relation

/** A formation and its position markers, loaded together. */
data class FormationWithSlots(
    @Embedded val formation: Formation,
    @Relation(
        parentColumn = "id",
        entityColumn = "formationId"
    )
    val slots: List<FormationSlot>
) {
    /** Slots in draw order; Room does not guarantee ordering on @Relation. */
    val orderedSlots: List<FormationSlot>
        get() = slots.sortedBy { it.slotIndex }

    /**
     * Reads the shape back out as counts per line, e.g. "4-4-2".
     * Keeper is excluded, matching how coaches say it out loud.
     */
    val shapeSummary: String
        get() {
            val outfield = orderedSlots.filter { it.role != Position.GOALKEEPER }
            if (outfield.isEmpty()) return ""
            // Group by rough band down the pitch, back line first.
            val byBand = outfield.groupBy { band(it.y) }
            return byBand.keys
                .sortedDescending()
                .joinToString("-") { band -> byBand.getValue(band).size.toString() }
        }

    private fun band(y: Float): Int = when {
        y >= 0.70f -> 3   // defensive third
        y >= 0.50f -> 2   // defensive midfield
        y >= 0.32f -> 1   // attacking midfield
        else -> 0         // forward line
    }
}
