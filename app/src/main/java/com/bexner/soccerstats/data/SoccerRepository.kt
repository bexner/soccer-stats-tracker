package com.bexner.soccerstats.data

import com.bexner.soccerstats.data.dao.FormationDao
import com.bexner.soccerstats.data.dao.PlayerDao
import com.bexner.soccerstats.data.dao.TeamDao
import com.bexner.soccerstats.data.entity.Formation
import com.bexner.soccerstats.data.entity.FormationSlot
import com.bexner.soccerstats.data.entity.FormationWithSlots
import com.bexner.soccerstats.data.entity.MatchFormat
import com.bexner.soccerstats.data.entity.Player
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
    private val formationDao: FormationDao
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
}
