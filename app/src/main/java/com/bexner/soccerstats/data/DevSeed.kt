package com.bexner.soccerstats.data

import com.bexner.soccerstats.data.entity.Formation
import com.bexner.soccerstats.data.entity.FormationSlot
import com.bexner.soccerstats.data.entity.MatchFormat
import com.bexner.soccerstats.data.entity.Player
import com.bexner.soccerstats.data.entity.Position
import com.bexner.soccerstats.data.entity.ShapePhase
import com.bexner.soccerstats.data.entity.Team

/**
 * Real team data loaded on debug builds so a fresh install isn't a blank slate.
 *
 * This is developer convenience only — [SoccerStatsApplication] gates it behind
 * `BuildConfig.DEBUG`, so a release build never ships anyone's roster. It's also
 * idempotent: it checks for the team by name and does nothing if it already
 * exists, so editing seeded data by hand won't get overwritten on next launch.
 *
 * Formation coordinates follow the usual convention: y = 1f is your own goal
 * line, y = 0f is the opponent's, so the lists read keeper-first, striker-last.
 */
object DevSeed {

    const val TEAM_NAME = "Blackhawks Bronze"

    private val roster = listOf(
        Triple("Mika", "Regent", 0),
        Triple("Joshua", "Horvath", 1),
        Triple("Charbel", "Ashaka", 7),
        Triple("Parker", "Haupt", 8),
        Triple("Helios", "Myrtaj", 10),
        Triple("Victor", "Padigela", 14),
        Triple("Lucas", "Merino", 19),
        Triple("Leandro", "Espedido", 20),
        Triple("Karson", "Stafiej", 31),
        Triple("Grayson", "Exner", 42),
        Triple("Viyansh", "Bandi", 55)
    )

    // ---- Formation building blocks ----
    // Each marker is (positional number, role, x, y). The numbers are shirt
    // positions in the system, not the players' actual jersey numbers.

    private data class Spot(val number: String, val role: Position, val x: Float, val y: Float)

    private fun gk(number: String = "1") = Spot(number, Position.GOALKEEPER, 0.5f, 0.93f)
    private fun def(number: String, x: Float, y: Float) = Spot(number, Position.DEFENDER, x, y)
    private fun mid(number: String, x: Float, y: Float) = Spot(number, Position.MIDFIELDER, x, y)
    private fun fwd(number: String, x: Float, y: Float) = Spot(number, Position.FORWARD, x, y)

    private data class SystemSpec(
        val name: String,
        val notes: String,
        val defending: List<Spot>,
        val attacking: List<Spot>
    )

    private val systems: List<SystemSpec> = listOf(

        // 1 GK - 2 Def - 4 Mid - 1 Attacking Mid - 1 Fwd
        SystemSpec(
            name = "Base",
            notes = "In defense, the 11 and 7 recover into the midfield line so we are compact " +
                "and hard to play through. When we win the ball, the 11 and 7 get wide and " +
                "higher, the 10 supports the 9, and the 6/8 stay connected underneath so we can " +
                "either play forward or keep possession.",
            defending = listOf(
                gk(),
                def("5", 0.35f, 0.76f), def("4", 0.65f, 0.76f),
                mid("11", 0.14f, 0.52f), mid("8", 0.38f, 0.52f),
                mid("6", 0.62f, 0.52f), mid("7", 0.86f, 0.52f),
                mid("10", 0.50f, 0.34f),
                fwd("9", 0.50f, 0.14f)
            ),
            // Same shirt-number order as the defending shape, so slot N is the
            // same player in both phases.
            attacking = listOf(
                gk(),
                def("5", 0.35f, 0.76f), def("4", 0.65f, 0.76f),
                mid("11", 0.12f, 0.34f), mid("8", 0.38f, 0.56f),
                mid("6", 0.62f, 0.56f), mid("7", 0.88f, 0.34f),
                mid("10", 0.50f, 0.36f),
                fwd("9", 0.50f, 0.14f)
            )
        ),

        // 1 GK - 2 Def - 3 Mid - 2 Wide/Attacking Mid - 1 Fwd
        SystemSpec(
            name = "Aggressive",
            notes = "This is the most aggressive look because the 11, 9, and 7 can get higher " +
                "and threaten the back line earlier. The key is that the 8, 10, and 6 stay " +
                "connected underneath them so we do not get stretched. If we lose the ball, the " +
                "nearest attacking player pressures immediately while the midfield line squeezes " +
                "the center.",
            defending = listOf(
                gk(),
                def("5", 0.35f, 0.76f), def("4", 0.65f, 0.76f),
                mid("8", 0.25f, 0.55f), mid("10", 0.50f, 0.55f), mid("6", 0.75f, 0.55f),
                mid("11", 0.22f, 0.34f), mid("7", 0.78f, 0.34f),
                fwd("9", 0.50f, 0.14f)
            ),
            attacking = listOf(
                gk(),
                def("5", 0.35f, 0.76f), def("4", 0.65f, 0.76f),
                mid("8", 0.25f, 0.50f), mid("10", 0.50f, 0.50f), mid("6", 0.75f, 0.50f),
                fwd("11", 0.15f, 0.18f), fwd("7", 0.85f, 0.18f), fwd("9", 0.50f, 0.13f)
            )
        ),

        // 1 GK - 3 Def - 4 Mid - 1 Fwd
        SystemSpec(
            name = "Conservative",
            notes = "This is the most conservative look. The back three helps us stay goal-side " +
                "and protect the center. When we win the ball, the 11 and 7 provide width, while " +
                "the 8 and 6 support underneath the 9. The outside backs can step forward only " +
                "when the ball is secure.",
            defending = listOf(
                gk(),
                def("3", 0.22f, 0.76f), def("4", 0.50f, 0.79f), def("2", 0.78f, 0.76f),
                mid("11", 0.14f, 0.50f), mid("8", 0.38f, 0.50f),
                mid("6", 0.62f, 0.50f), mid("7", 0.86f, 0.50f),
                fwd("9", 0.50f, 0.16f)
            ),
            attacking = listOf(
                gk(),
                def("3", 0.22f, 0.74f), def("4", 0.50f, 0.78f), def("2", 0.78f, 0.74f),
                mid("11", 0.12f, 0.42f), mid("8", 0.37f, 0.44f),
                mid("6", 0.63f, 0.44f), mid("7", 0.88f, 0.42f),
                fwd("9", 0.50f, 0.14f)
            )
        )
    )

    fun team(): Team = Team(name = TEAM_NAME, ageGroup = "U11", season = "")

    fun players(teamId: Long): List<Player> = roster.map { (first, last, number) ->
        Player(
            teamId = teamId,
            firstName = first,
            lastName = last,
            jerseyNumber = number,
            position = Position.UNASSIGNED
        )
    }

    /** The three systems, each carrying both a defending and an attacking shape. */
    fun formations(): List<Pair<Formation, List<FormationSlot>>> = systems.map { spec ->
        val formation = Formation(
            name = spec.name,
            format = MatchFormat.NINE_V_NINE,
            hasKeeper = true,
            isPreset = false,
            notes = spec.notes
        )
        val slots = toSlots(spec.defending, ShapePhase.DEFENDING) +
            toSlots(spec.attacking, ShapePhase.ATTACKING)
        formation to slots
    }

    private fun toSlots(spots: List<Spot>, phase: ShapePhase): List<FormationSlot> =
        spots.mapIndexed { index, spot ->
            FormationSlot(
                formationId = 0,
                phase = phase,
                slotIndex = index,
                role = spot.role,
                x = spot.x,
                y = spot.y,
                label = spot.number
            )
        }

    /**
     * Catches a spec that doesn't add up to a full 9v9 side, or whose two shapes
     * disagree slot-for-slot. That second check matters: a lineup binds a player
     * to a slot index, so slot 4 must be the same shirt number in both phases or
     * toggling phase would silently move players around.
     */
    fun validate(): List<String> = systems.flatMap { spec ->
        val orderMismatch = spec.defending.map { it.number } != spec.attacking.map { it.number }
        val orderProblem = if (orderMismatch) {
            listOf(
                "${spec.name}: shirt order differs between phases — " +
                    "defending ${spec.defending.map { it.number }}, " +
                    "attacking ${spec.attacking.map { it.number }}"
            )
        } else {
            emptyList()
        }

        orderProblem + listOf(
            ShapePhase.DEFENDING to spec.defending,
            ShapePhase.ATTACKING to spec.attacking
        )
            .mapNotNull { (phase, spots) ->
                val keepers = spots.count { it.role == Position.GOALKEEPER }
                val numbers = spots.map { it.number }
                when {
                    spots.size != 9 ->
                        "${spec.name} ${phase.label}: ${spots.size} players, expected 9"
                    keepers != 1 ->
                        "${spec.name} ${phase.label}: $keepers keepers, expected 1"
                    numbers.size != numbers.toSet().size ->
                        "${spec.name} ${phase.label}: duplicate shirt numbers"
                    else -> null
                }
            }
    }
}
