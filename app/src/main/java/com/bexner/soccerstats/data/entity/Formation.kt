package com.bexner.soccerstats.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A reusable shape (4-4-2, 2-3-1, ...) held in a shared library rather than
 * owned by one team, so the same formation serves every squad of that format.
 *
 * Slot positions live in [FormationSlot]. Lineups will later bind real players
 * to those slots for a specific game.
 */
@Entity(tableName = "formations")
data class Formation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val format: MatchFormat,
    val hasKeeper: Boolean,
    /** Preset formations ship with the app and are restored if all are deleted. */
    val isPreset: Boolean = false,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
