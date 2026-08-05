package com.bexner.soccerstats.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.bexner.soccerstats.data.entity.Player
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {

    @Query(
        """
        SELECT * FROM players
        WHERE teamId = :teamId
        ORDER BY isActive DESC,
                 CASE WHEN jerseyNumber IS NULL THEN 1 ELSE 0 END,
                 jerseyNumber ASC,
                 lastName COLLATE NOCASE ASC,
                 firstName COLLATE NOCASE ASC
        """
    )
    fun observeByTeam(teamId: Long): Flow<List<Player>>

    @Query("SELECT * FROM players WHERE id = :playerId")
    suspend fun getById(playerId: Long): Player?

    /** Used to warn about a duplicate jersey number within the same team. */
    @Query(
        """
        SELECT * FROM players
        WHERE teamId = :teamId AND jerseyNumber = :jerseyNumber AND id != :excludePlayerId
        LIMIT 1
        """
    )
    suspend fun findByJerseyNumber(teamId: Long, jerseyNumber: Int, excludePlayerId: Long): Player?

    @Query("SELECT COUNT(*) FROM players WHERE teamId = :teamId")
    fun observeCountByTeam(teamId: Long): Flow<Int>

    @Insert
    suspend fun insert(player: Player): Long

    @Insert
    suspend fun insertAll(players: List<Player>)

    @Update
    suspend fun update(player: Player)

    @Delete
    suspend fun delete(player: Player)
}
