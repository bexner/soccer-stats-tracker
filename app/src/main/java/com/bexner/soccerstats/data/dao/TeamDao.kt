package com.bexner.soccerstats.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.bexner.soccerstats.data.entity.Team
import com.bexner.soccerstats.data.entity.TeamWithPlayerCount
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamDao {

    @Query("SELECT * FROM teams ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<Team>>

    @Query(
        """
        SELECT t.*, (SELECT COUNT(*) FROM players p WHERE p.teamId = t.id) AS playerCount
        FROM teams t
        ORDER BY t.name COLLATE NOCASE ASC
        """
    )
    fun observeAllWithPlayerCount(): Flow<List<TeamWithPlayerCount>>

    @Query("SELECT * FROM teams WHERE id = :teamId")
    fun observeById(teamId: Long): Flow<Team?>

    @Query("SELECT * FROM teams WHERE id = :teamId")
    suspend fun getById(teamId: Long): Team?

    @Insert
    suspend fun insert(team: Team): Long

    @Update
    suspend fun update(team: Team)

    @Delete
    suspend fun delete(team: Team)

    @Query("DELETE FROM teams WHERE id = :teamId")
    suspend fun deleteById(teamId: Long)
}
