package com.bexner.soccerstats.data.entity

import androidx.room.Embedded
import androidx.room.Relation

/** A formation and both of its shapes, loaded together. */
data class FormationWithSlots(
    @Embedded val formation: Formation,
    @Relation(
        parentColumn = "id",
        entityColumn = "formationId"
    )
    val slots: List<FormationSlot>
) {
    /** All slots in draw order; Room does not guarantee ordering on @Relation. */
    val orderedSlots: List<FormationSlot>
        get() = slots.sortedBy { it.slotIndex }

    fun slotsFor(phase: ShapePhase): List<FormationSlot> =
        slots.filter { it.phase == phase }.sortedBy { it.slotIndex }

    val defendingSlots: List<FormationSlot> get() = slotsFor(ShapePhase.DEFENDING)

    val attackingSlots: List<FormationSlot> get() = slotsFor(ShapePhase.ATTACKING)

    /** True once both shapes have been laid out. */
    val hasBothPhases: Boolean
        get() = defendingSlots.isNotEmpty() && attackingSlots.isNotEmpty()

    /**
     * Reads a shape back out as counts per line, e.g. "2-4-1-1".
     * Keeper is excluded, matching how coaches say it out loud.
     */
    fun summaryFor(phase: ShapePhase): String {
        val outfield = slotsFor(phase).filter { it.role != Position.GOALKEEPER }
        if (outfield.isEmpty()) return ""
        val byBand = outfield.groupBy { band(it.y) }
        return byBand.keys
            .sortedDescending()
            .joinToString("-") { key -> byBand.getValue(key).size.toString() }
    }

    /** Defending shape is the headline; falls back to attacking if only that exists. */
    val shapeSummary: String
        get() = summaryFor(ShapePhase.DEFENDING).ifBlank { summaryFor(ShapePhase.ATTACKING) }

    /** Rough band down the pitch, back line first. */
    private fun band(y: Float): Int = when {
        y >= 0.70f -> 3   // defensive third
        y >= 0.50f -> 2   // defensive midfield
        y >= 0.32f -> 1   // attacking midfield
        else -> 0         // forward line
    }
}
