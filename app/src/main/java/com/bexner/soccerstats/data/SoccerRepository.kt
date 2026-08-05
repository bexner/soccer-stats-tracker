package com.bexner.soccerstats.data

import com.bexner.soccerstats.data.dao.FormationDao
import com.bexner.soccerstats.data.dao.GameDao
import com.bexner.soccerstats.data.dao.PlayerDao
import com.bexner.soccerstats.data.dao.TeamDao
import com.bexner.soccerstats.data.entity.Attendance
import com.bexner.soccerstats.data.entity.EventSide
import com.bexner.soccerstats.data.entity.EventType
import com.bexner.soccerstats.data.entity.Formation
import com.bexner.soccerstats.data.entity.FormationSlot
import com.bexner.soccerstats.data.entity.FormationWithSlots
import com.bexner.soccerstats.data.entity.Game
import com.bexner.soccerstats.data.entity.GameAttendance
import com.bexner.soccerstats.data.entity.GameEvent
import com.bexner.soccerstats.data.entity.GameStatus
import com.bexner.soccerstats.data.entity.GameWithCounts
import com.bexner.soccerstats.data.entity.LineupSlot
import com.bexner.soccerstats.data.entity.MatchFormat
import com.bexner.soccerstats.data.entity.Player
import com.bexner.soccerstats.data.entity.PlayerStint
import com.bexner.soccerstats.data.entity.Position
import com.bexner.soccerstats.data.entity.ShapePhase
import com.bexner.soccerstats.data.entity.Team
import com.bexner.soccerstats.data.entity.TeamWithPlayerCount
import kotlinx.coroutines.flow.Flow

/**
 * Single entry point for team and roster data. Screens talk to this, never to
 * the DAOs directly, so schedules/formations/stats can be layered on later.
 */
class SoccerRepository(
    private val teamDao: TeamDao,
    private val playerDao: PlayerDao,
    private val formationDao: FormationDao,
    private val gameDao: GameDao
) {

    // ----- Teams -----

    fun observeTeams(): Flow<List<Team>> = teamDao.observeAll()

    fun observeTeamsWithPlayerCount(): Flow<List<TeamWithPlayerCount>> =
        teamDao.observeAllWithPlayerCount()

    fun observeTeam(teamId: Long): Flow<Team?> = teamDao.observeById(teamId)

    suspend fun getTeam(teamId: Long): Team? = teamDao.getById(teamId)

    suspend fun addTeam(team: Team): Long = teamDao.insert(team)

    suspend fun updateTeam(team: Team) = teamDao.update(team)

    suspend fun deleteTeam(team: Team) = teamDao.delete(team)

    // ----- Players -----

    fun observeRoster(teamId: Long): Flow<List<Player>> = playerDao.observeByTeam(teamId)

    suspend fun getPlayer(playerId: Long): Player? = playerDao.getById(playerId)

    suspend fun addPlayer(player: Player): Long = playerDao.insert(player)

    suspend fun updatePlayer(player: Player) = playerDao.update(player)

    suspend fun deletePlayer(player: Player) = playerDao.delete(player)

    /** True when another player on the same team already wears this number. */
    suspend fun isJerseyTaken(teamId: Long, jerseyNumber: Int, excludePlayerId: Long = 0): Boolean =
        playerDao.findByJerseyNumber(teamId, jerseyNumber, excludePlayerId) != null

    // ----- Formations -----

    fun observeFormations(): Flow<List<FormationWithSlots>> = formationDao.observeAll()

    fun observeFormations(format: MatchFormat): Flow<List<FormationWithSlots>> =
        formationDao.observeByFormat(format)

    fun observeFormation(formationId: Long): Flow<FormationWithSlots?> =
        formationDao.observeById(formationId)

    suspend fun getFormation(formationId: Long): FormationWithSlots? =
        formationDao.getById(formationId)

    suspend fun addFormation(formation: Formation, slots: List<FormationSlot>): Long =
        formationDao.insertFormationWithSlots(formation, slots)

    suspend fun updateFormation(formation: Formation, slots: List<FormationSlot>) {
        formationDao.updateFormation(formation)
        formationDao.replaceSlots(formation.id, slots)
    }

    suspend fun deleteFormation(formation: Formation) = formationDao.deleteFormation(formation)

    /**
     * Copies an existing formation, which is how a coach tweaks a preset without
     * losing the original.
     */
    suspend fun duplicateFormation(source: FormationWithSlots, newName: String): Long =
        formationDao.insertFormationWithSlots(
            source.formation.copy(id = 0, name = newName, isPreset = false),
            source.orderedSlots
        )

    /**
     * Inserts the built-in formations when the library is empty. Runs on every
     * launch but only does work once, which also restores presets if they were
     * all deleted.
     */
    suspend fun seedPresetFormationsIfEmpty() {
        if (formationDao.count() > 0) return
        FormationPresets.all().forEach { (formation, slots) ->
            formationDao.insertFormationWithSlots(formation, slots)
        }
    }

    /**
     * Loads the developer's own team, roster and systems on debug builds so a
     * fresh install isn't empty. Keyed on the team name, so hand-editing seeded
     * data survives the next launch instead of being clobbered.
     */
    suspend fun seedDevDataIfMissing() {
        val alreadySeeded = teamDao.findByName(DevSeed.TEAM_NAME) != null
        if (alreadySeeded) return

        val teamId = teamDao.insert(DevSeed.team())
        playerDao.insertAll(DevSeed.players(teamId))

        DevSeed.formations().forEach { (formation, slots) ->
            formationDao.insertFormationWithSlots(formation, slots)
        }
    }

    // ----- Games -----

    fun observeGames(teamId: Long): Flow<List<GameWithCounts>> = gameDao.observeByTeam(teamId)

    fun observeGame(gameId: Long): Flow<Game?> = gameDao.observeById(gameId)

    suspend fun getGame(gameId: Long): Game? = gameDao.getById(gameId)

    suspend fun addGame(game: Game): Long = gameDao.insert(game)

    suspend fun updateGame(game: Game) = gameDao.update(game)

    suspend fun deleteGame(game: Game) = gameDao.delete(game)

    // ----- Attendance -----

    fun observeAttendance(gameId: Long): Flow<List<GameAttendance>> =
        gameDao.observeAttendance(gameId)

    suspend fun setAttendance(gameId: Long, playerId: Long, status: Attendance) =
        gameDao.setAttendance(gameId, playerId, status)

    // ----- Lineup -----

    fun observeLineup(gameId: Long): Flow<List<LineupSlot>> = gameDao.observeLineup(gameId)

    /**
     * Saves the starting eleven (or nine). Also records the chosen formation on
     * the game, since the lineup is meaningless without the shape it belongs to.
     */
    suspend fun saveLineup(gameId: Long, formationId: Long, assignments: Map<Int, Long>) {
        gameDao.replaceLineup(
            gameId,
            assignments.map { (slotIndex, playerId) ->
                LineupSlot(gameId = gameId, slotIndex = slotIndex, playerId = playerId)
            }
        )
        gameDao.getById(gameId)?.let { gameDao.update(it.copy(formationId = formationId)) }
    }

    // ----- Match clock -----

    fun observeStints(gameId: Long): Flow<List<PlayerStint>> = gameDao.observeStints(gameId)

    fun observeEvents(gameId: Long): Flow<List<GameEvent>> = gameDao.observeEvents(gameId)

    /**
     * Starts or resumes the clock. On the very first start it opens a stint for
     * every player in the lineup, which is what makes minutes-played computable.
     */
    suspend fun startClock(gameId: Long) {
        val game = gameDao.getById(gameId) ?: return
        if (game.isClockRunning) return

        val openedFirstTime = gameDao.getOpenStints(gameId).isEmpty() && game.clockElapsedMs == 0L
        if (openedFirstTime) {
            openStintsForLineup(game)
            gameDao.insertEvent(
                GameEvent(
                    gameId = gameId,
                    type = EventType.PERIOD_START,
                    period = game.currentPeriod,
                    clockMs = game.clockElapsedMs
                )
            )
        }

        gameDao.update(
            game.copy(
                clockRunningSince = System.currentTimeMillis(),
                status = GameStatus.IN_PROGRESS
            )
        )
    }

    /** Stops the clock and banks the running time. */
    suspend fun stopClock(gameId: Long) {
        val game = gameDao.getById(gameId) ?: return
        val startedAt = game.clockRunningSince ?: return
        val banked = game.clockElapsedMs + (System.currentTimeMillis() - startedAt).coerceAtLeast(0L)
        gameDao.update(game.copy(clockElapsedMs = banked, clockRunningSince = null))
    }

    /** Ends the current period, stopping the clock and logging the boundary. */
    suspend fun endPeriod(gameId: Long) {
        stopClock(gameId)
        val game = gameDao.getById(gameId) ?: return
        gameDao.insertEvent(
            GameEvent(
                gameId = gameId,
                type = EventType.PERIOD_END,
                period = game.currentPeriod,
                clockMs = game.clockElapsedMs
            )
        )
        if (game.currentPeriod < game.periodCount) {
            gameDao.update(game.copy(currentPeriod = game.currentPeriod + 1))
        }
    }

    /** Closes every open stint and marks the game final. */
    suspend fun finishGame(gameId: Long) {
        stopClock(gameId)
        val game = gameDao.getById(gameId) ?: return
        gameDao.getOpenStints(gameId).forEach { stint ->
            gameDao.updateStint(stint.copy(offAtMs = game.clockElapsedMs))
        }
        gameDao.update(game.copy(status = GameStatus.FINAL))
    }

    private suspend fun openStintsForLineup(game: Game) {
        val lineup = gameDao.getLineup(game.id)
        if (lineup.isEmpty()) return

        val roles = game.formationId
            ?.let { formationDao.getById(it) }
            ?.slotsFor(ShapePhase.DEFENDING)
            ?.associate { it.slotIndex to it.role }
            .orEmpty()

        gameDao.insertStints(
            lineup.map { slot ->
                PlayerStint(
                    gameId = game.id,
                    playerId = slot.playerId,
                    slotIndex = slot.slotIndex,
                    role = roles[slot.slotIndex] ?: Position.UNASSIGNED,
                    onAtMs = game.clockElapsedMs
                )
            }
        )
    }

    /**
     * Swaps one player for another in the same slot. Closes the outgoing stint
     * and opens the incoming one at the same match time, so minutes always add
     * up to the time the position was actually occupied.
     */
    suspend fun substitute(gameId: Long, outPlayerId: Long, inPlayerId: Long) {
        val game = gameDao.getById(gameId) ?: return
        val now = game.elapsedMsAt()
        val open = gameDao.getOpenStint(gameId, outPlayerId) ?: return

        gameDao.updateStint(open.copy(offAtMs = now))
        gameDao.insertStint(
            PlayerStint(
                gameId = gameId,
                playerId = inPlayerId,
                slotIndex = open.slotIndex,
                role = open.role,
                onAtMs = now
            )
        )
        gameDao.insertEvent(
            GameEvent(
                gameId = gameId,
                type = EventType.SUBSTITUTION,
                playerId = outPlayerId,
                secondaryPlayerId = inPlayerId,
                period = game.currentPeriod,
                clockMs = now
            )
        )
    }

    /** Logs an event at the current match time, keeping the score in step. */
    suspend fun logEvent(
        gameId: Long,
        type: EventType,
        side: EventSide,
        playerId: Long? = null,
        secondaryPlayerId: Long? = null,
        pitchX: Float? = null,
        pitchY: Float? = null,
        goalX: Float? = null,
        goalY: Float? = null
    ) {
        val game = gameDao.getById(gameId) ?: return
        gameDao.insertEvent(
            GameEvent(
                gameId = gameId,
                type = type,
                side = side,
                playerId = playerId,
                secondaryPlayerId = secondaryPlayerId,
                period = game.currentPeriod,
                clockMs = game.elapsedMsAt(),
                pitchX = pitchX,
                pitchY = pitchY,
                goalX = goalX,
                goalY = goalY
            )
        )
        if (type == EventType.GOAL) {
            gameDao.update(
                if (side == EventSide.US) game.copy(goalsFor = game.goalsFor + 1)
                else game.copy(goalsAgainst = game.goalsAgainst + 1)
            )
        }
    }

    /** Removes an event, undoing its effect on the score. */
    suspend fun deleteEvent(event: GameEvent) {
        gameDao.deleteEvent(event)
        if (event.type == EventType.GOAL) {
            gameDao.getById(event.gameId)?.let { game ->
                gameDao.update(
                    if (event.side == EventSide.US) {
                        game.copy(goalsFor = (game.goalsFor - 1).coerceAtLeast(0))
                    } else {
                        game.copy(goalsAgainst = (game.goalsAgainst - 1).coerceAtLeast(0))
                    }
                )
            }
        }
    }
}
