package com.bexner.soccerstats.ui.formations

import androidx.compose.ui.graphics.Color
import com.bexner.soccerstats.data.entity.FormationSlot
import com.bexner.soccerstats.data.entity.FormationWithSlots
import com.bexner.soccerstats.data.entity.Position
import com.bexner.soccerstats.ui.components.PitchMarker

/** Consistent colour per line, so a shape reads at a glance. */
fun Position.markerColor(): Color = when (this) {
    Position.GOALKEEPER -> Color(0xFFF9A825)
    Position.DEFENDER -> Color(0xFF1565C0)
    Position.MIDFIELDER -> Color(0xFF6A1B9A)
    Position.FORWARD -> Color(0xFFC62828)
    Position.UNASSIGNED -> Color(0xFF546E7A)
}

fun FormationSlot.toMarker(): PitchMarker = PitchMarker(
    id = if (id != 0L) id else slotIndex.toLong() + 1L,
    x = x,
    y = y,
    label = displayLabel,
    color = role.markerColor()
)

fun FormationWithSlots.toMarkers(): List<PitchMarker> = orderedSlots.map { it.toMarker() }
