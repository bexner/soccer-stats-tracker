package com.bexner.soccerstats.ui.games

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Shared date/time formatting so every game screen reads the same. */
object GameFormat {
    private val dayFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val fullFormat = SimpleDateFormat("EEE, MMM d · h:mm a", Locale.getDefault())

    fun day(epochMs: Long): String = dayFormat.format(Date(epochMs))
    fun time(epochMs: Long): String = timeFormat.format(Date(epochMs))
    fun full(epochMs: Long): String = fullFormat.format(Date(epochMs))

    /** mm:ss from match elapsed milliseconds. */
    fun clock(elapsedMs: Long): String {
        val totalSeconds = (elapsedMs / 1000).coerceAtLeast(0)
        return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
    }

    /** Whole minutes, for playing-time summaries. */
    fun minutes(elapsedMs: Long): String = "${(elapsedMs / 60000).coerceAtLeast(0)}m"
}
