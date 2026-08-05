package com.bexner.soccerstats.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One position marker within a formation.
 *
 * [x] and [y] are normalized 0f..1f so the shape renders identically on any
 * screen size. Origin is the top-left of the pitch as the coach views it:
 * y = 0f is the opponent's goal line, y = 1f is your own goal line. So a keeper
 * sits near y = 0.95f and strikers near y = 0.15f.
 */
@Entity(
    tableName = "formation_slots",
    foreignKeys = [
        ForeignKey(
            entity = Formation::class,
            parentColumns = ["id"],
            childColumns = ["formationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("formationId"), Index("formationId", "phase")]
)
data class FormationSlot(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val formationId: Long,
    /** Which shape this marker belongs to. */
    val phase: ShapePhase = ShapePhase.DEFENDING,
    /** Draw/tab order within the phase, 0-based. Keeper is 0 when there is one. */
    val slotIndex: Int,
    val role: Position,
    val x: Float,
    val y: Float,
    /** Optional short tag the coach can set, e.g. "LB", "CDM". */
    val label: String = ""
) {
    /** Falls back to the role abbreviation when no custom label is set. */
    val displayLabel: String
        get() = label.ifBlank { role.abbreviation }
}
