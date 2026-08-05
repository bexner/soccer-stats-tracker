package com.bexner.soccerstats.data

import com.bexner.soccerstats.data.dao.PlayerDao
import com.bexner.soccerstats.data.dao.TeamDao
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
    private val playerDao: PlayerDao
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
}
