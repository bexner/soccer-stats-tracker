package com.bexner.soccerstats.data

import com.bexner.soccerstats.data.entity.Formation
import com.bexner.soccerstats.data.entity.FormationSlot
import com.bexner.soccerstats.data.entity.MatchFormat
import com.bexner.soccerstats.data.entity.Position

/**
 * Formations the app ships with, so there is something usable before the coach
 * builds anything.
 *
 * Coordinates are normalized 0f..1f with y = 1f at your own goal line, so the
 * lists below read top-to-bottom the same way the pitch is drawn: keeper first,
 * strikers last.
 */
object FormationPresets {

    private data class Spec(
        val name: String,
        val format: MatchFormat,
        val hasKeeper: Boolean,
        val points: List<Triple<Position, Float, Float>>
    )

    private fun gk() = Triple(Position.GOALKEEPER, 0.5f, 0.93f)
    private fun def(x: Float, y: Float) = Triple(Position.DEFENDER, x, y)
    private fun mid(x: Float, y: Float) = Triple(Position.MIDFIELDER, x, y)
    private fun fwd(x: Float, y: Float) = Triple(Position.FORWARD, x, y)

    private val specs: List<Spec> = listOf(

        // ---- 4v4, no keeper (typical U6-U8) ----
        Spec("1-2-1 Diamond", MatchFormat.FOUR_V_FOUR, false, listOf(
            def(0.50f, 0.78f),
            mid(0.24f, 0.50f), mid(0.76f, 0.50f),
            fwd(0.50f, 0.22f)
        )),
        Spec("2-2 Box", MatchFormat.FOUR_V_FOUR, false, listOf(
            def(0.32f, 0.75f), def(0.68f, 0.75f),
            fwd(0.32f, 0.30f), fwd(0.68f, 0.30f)
        )),
        Spec("2-1-1", MatchFormat.FOUR_V_FOUR, false, listOf(
            def(0.32f, 0.78f), def(0.68f, 0.78f),
            mid(0.50f, 0.50f),
            fwd(0.50f, 0.22f)
        )),

        // ---- 4v4, with keeper ----
        Spec("1-1-1", MatchFormat.FOUR_V_FOUR, true, listOf(
            gk(),
            def(0.50f, 0.74f),
            mid(0.50f, 0.50f),
            fwd(0.50f, 0.24f)
        )),
        Spec("2-1", MatchFormat.FOUR_V_FOUR, true, listOf(
            gk(),
            def(0.32f, 0.72f), def(0.68f, 0.72f),
            fwd(0.50f, 0.28f)
        )),
        Spec("1-2", MatchFormat.FOUR_V_FOUR, true, listOf(
            gk(),
            def(0.50f, 0.72f),
            fwd(0.32f, 0.28f), fwd(0.68f, 0.28f)
        )),

        // ---- 7v7 (U9-U10) ----
        Spec("2-3-1", MatchFormat.SEVEN_V_SEVEN, true, listOf(
            gk(),
            def(0.30f, 0.75f), def(0.70f, 0.75f),
            mid(0.20f, 0.48f), mid(0.50f, 0.48f), mid(0.80f, 0.48f),
            fwd(0.50f, 0.20f)
        )),
        Spec("3-2-1", MatchFormat.SEVEN_V_SEVEN, true, listOf(
            gk(),
            def(0.20f, 0.75f), def(0.50f, 0.78f), def(0.80f, 0.75f),
            mid(0.33f, 0.48f), mid(0.67f, 0.48f),
            fwd(0.50f, 0.20f)
        )),
        Spec("2-2-2", MatchFormat.SEVEN_V_SEVEN, true, listOf(
            gk(),
            def(0.32f, 0.75f), def(0.68f, 0.75f),
            mid(0.32f, 0.50f), mid(0.68f, 0.50f),
            fwd(0.32f, 0.22f), fwd(0.68f, 0.22f)
        )),
        Spec("3-1-2", MatchFormat.SEVEN_V_SEVEN, true, listOf(
            gk(),
            def(0.20f, 0.75f), def(0.50f, 0.78f), def(0.80f, 0.75f),
            mid(0.50f, 0.50f),
            fwd(0.35f, 0.22f), fwd(0.65f, 0.22f)
        )),

        // ---- 9v9 (U11-U12) ----
        Spec("3-3-2", MatchFormat.NINE_V_NINE, true, listOf(
            gk(),
            def(0.20f, 0.76f), def(0.50f, 0.79f), def(0.80f, 0.76f),
            mid(0.22f, 0.50f), mid(0.50f, 0.50f), mid(0.78f, 0.50f),
            fwd(0.35f, 0.20f), fwd(0.65f, 0.20f)
        )),
        Spec("3-2-3", MatchFormat.NINE_V_NINE, true, listOf(
            gk(),
            def(0.20f, 0.76f), def(0.50f, 0.79f), def(0.80f, 0.76f),
            mid(0.33f, 0.50f), mid(0.67f, 0.50f),
            fwd(0.20f, 0.20f), fwd(0.50f, 0.18f), fwd(0.80f, 0.20f)
        )),
        Spec("2-3-3", MatchFormat.NINE_V_NINE, true, listOf(
            gk(),
            def(0.32f, 0.77f), def(0.68f, 0.77f),
            mid(0.20f, 0.50f), mid(0.50f, 0.50f), mid(0.80f, 0.50f),
            fwd(0.20f, 0.20f), fwd(0.50f, 0.18f), fwd(0.80f, 0.20f)
        )),
        Spec("3-4-1", MatchFormat.NINE_V_NINE, true, listOf(
            gk(),
            def(0.20f, 0.76f), def(0.50f, 0.79f), def(0.80f, 0.76f),
            mid(0.15f, 0.50f), mid(0.38f, 0.52f), mid(0.62f, 0.52f), mid(0.85f, 0.50f),
            fwd(0.50f, 0.20f)
        )),

        // ---- 11v11 (U13+) ----
        Spec("4-4-2", MatchFormat.ELEVEN_V_ELEVEN, true, listOf(
            gk(),
            def(0.15f, 0.76f), def(0.38f, 0.78f), def(0.62f, 0.78f), def(0.85f, 0.76f),
            mid(0.15f, 0.50f), mid(0.38f, 0.52f), mid(0.62f, 0.52f), mid(0.85f, 0.50f),
            fwd(0.38f, 0.20f), fwd(0.62f, 0.20f)
        )),
        Spec("4-3-3", MatchFormat.ELEVEN_V_ELEVEN, true, listOf(
            gk(),
            def(0.15f, 0.76f), def(0.38f, 0.78f), def(0.62f, 0.78f), def(0.85f, 0.76f),
            mid(0.28f, 0.52f), mid(0.50f, 0.55f), mid(0.72f, 0.52f),
            fwd(0.18f, 0.20f), fwd(0.50f, 0.16f), fwd(0.82f, 0.20f)
        )),
        Spec("4-2-3-1", MatchFormat.ELEVEN_V_ELEVEN, true, listOf(
            gk(),
            def(0.15f, 0.76f), def(0.38f, 0.78f), def(0.62f, 0.78f), def(0.85f, 0.76f),
            mid(0.35f, 0.60f), mid(0.65f, 0.60f),
            mid(0.20f, 0.38f), mid(0.50f, 0.38f), mid(0.80f, 0.38f),
            fwd(0.50f, 0.15f)
        )),
        Spec("3-5-2", MatchFormat.ELEVEN_V_ELEVEN, true, listOf(
            gk(),
            def(0.25f, 0.77f), def(0.50f, 0.79f), def(0.75f, 0.77f),
            mid(0.10f, 0.52f), mid(0.30f, 0.55f), mid(0.50f, 0.50f),
            mid(0.70f, 0.55f), mid(0.90f, 0.52f),
            fwd(0.38f, 0.18f), fwd(0.62f, 0.18f)
        )),
        Spec("5-3-2", MatchFormat.ELEVEN_V_ELEVEN, true, listOf(
            gk(),
            def(0.10f, 0.74f), def(0.30f, 0.78f), def(0.50f, 0.80f),
            def(0.70f, 0.78f), def(0.90f, 0.74f),
            mid(0.28f, 0.52f), mid(0.50f, 0.50f), mid(0.72f, 0.52f),
            fwd(0.38f, 0.18f), fwd(0.62f, 0.18f)
        ))
    )

    /** Every preset, paired with its slot markers, ready to insert. */
    fun all(): List<Pair<Formation, List<FormationSlot>>> = specs.map { spec ->
        val formation = Formation(
            name = spec.name,
            format = spec.format,
            hasKeeper = spec.hasKeeper,
            isPreset = true
        )
        val slots = spec.points.mapIndexed { index, (role, x, y) ->
            FormationSlot(
                formationId = 0,
                slotIndex = index,
                role = role,
                x = x,
                y = y
            )
        }
        formation to slots
    }

    /** Guards against a spec whose marker count doesn't match its format. */
    fun validate(): List<String> = specs.mapNotNull { spec ->
        val expected = spec.format.playersOnField
        val actual = spec.points.size
        val keepers = spec.points.count { it.first == Position.GOALKEEPER }
        when {
            actual != expected ->
                "${spec.format.label} ${spec.name}: $actual markers, expected $expected"
            spec.hasKeeper && keepers != 1 ->
                "${spec.format.label} ${spec.name}: expected exactly 1 keeper, found $keepers"
            !spec.hasKeeper && keepers != 0 ->
                "${spec.format.label} ${spec.name}: keeperless formation has $keepers keepers"
            else -> null
        }
    }
}
