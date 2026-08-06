package com.bexner.soccerstats.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.bexner.soccerstats.data.entity.Attendance
import com.bexner.soccerstats.data.entity.Game
import com.bexner.soccerstats.data.entity.GameAttendance
import com.bexner.soccerstats.data.entity.GameEvent
import com.bexner.soccerstats.data.entity.GameWithCounts
import com.bexner.soccerstats.data.entity.LineupSlot
import com.bexner.soccerstats.data.entity.PlayerStint
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {

    // ----- Games -----

    @Query(
        """
        SELECT g.*,
               (SELECT COUNT(*) FROM game_attendance a
                 WHERE a.gameId = g.id AND a.status IN ('YES','MAYBE')) AS availableCount,
               (SELECT COUNT(*) FROM lineup_slots l WHERE l.gameId = g.id) AS lineupCount
        FROM games g
        WHERE g.teamId = :teamId
        ORDER BY g.kickoffAt ASC
        """
    )
    fun observeByTeam(teamId: Long): Flow<List<GameWithCounts>>

    @Query("SELECT * FROM games WHERE id = :gameId")
    fun observeById(gameId: Long): Flow<Game?>

    @Query("SELECT * FROM games WHERE id = :gameId")
    suspend fun getById(gameId: Long): Game?

    /** Games worth aggregating: anything past the scheduled stage. */
    @Query(
        """
        SELECT * FROM games
        WHERE teamId = :teamId AND status IN ('IN_PROGRESS','FINAL')
        ORDER BY kickoffAt ASC
        """
    )
    suspend fun gamesForStats(teamId: Long): List<Game>

    @Query("SELECT * FROM player_stints WHERE gameId = :gameId ORDER BY onAtMs ASC")
    suspend fun stintsForGame(gameId: Long): List<PlayerStint>

    @Query("SELECT * FROM game_events WHERE gameId = :gameId ORDER BY clockMs ASC")
    suspend fun eventsForGame(gameId: Long): List<GameEvent>

    @Insert
    suspend fun insert(game: Game): Long

    @Update
    suspend fun update(game: Game)

    @Delete
    suspend fun delete(game: Game)

    // ----- Attendance -----

    @Query("SELECT * FROM game_attendance WHERE gameId = :gameId")
    fun observeAttendance(gameId: Long): Flow<List<GameAttendance>>

    @Query("SELECT * FROM game_attendance WHERE gameId = :gameId AND playerId = :playerId")
    suspend fun getAttendance(gameId: Long, playerId: Long): GameAttendance?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttendance(row: GameAttendance)

    @Query("UPDATE game_attendance SET status = :status WHERE gameId = :gameId AND playerId = :playerId")
    suspend fun updateAttendance(gameId: Long, playerId: Long, status: Attendance)

    /** Rows are created lazily, so set means insert-or-update. */
    @Transaction
    suspend fun setAttendance(gameId: Long, playerId: Long, status: Attendance) {
        val existing = getAttendance(gameId, playerId)
        if (existing == null) {
            upsertAttendance(GameAttendance(gameId = gameId, playerId = playerId, status = status))
        } else {
            updateAttendance(gameId, playerId, status)
        }
    }

    // ----- Lineup -----

    @Query("SELECT * FROM lineup_slots WHERE gameId = :gameId ORDER BY slotIndex ASC")
    fun observeLineup(gameId: Long): Flow<List<LineupSlot>>

    @Query("SELECT * FROM lineup_slots WHERE gameId = :gameId ORDER BY slotIndex ASC")
    suspend fun getLineup(gameId: Long): List<LineupSlot>

    @Query("DELETE FROM lineup_slots WHERE gameId = :gameId")
    suspend fun clearLineup(gameId: Long)

    @Insert
    suspend fun insertLineupSlots(slots: List<LineupSlot>)

    @Transaction
    suspend fun replaceLineup(gameId: Long, slots: List<LineupSlot>) {
        clearLineup(gameId)
        insertLineupSlots(slots.map { it.copy(id = 0, gameId = gameId) })
    }

    // ----- Stints -----

    @Query("SELECT * FROM player_stints WHERE gameId = :gameId ORDER BY onAtMs ASC")
    fun observeStints(gameId: Long): Flow<List<PlayerStint>>

    @Query("SELECT * FROM player_stints WHERE gameId = :gameId AND offAtMs IS NULL")
    suspend fun getOpenStints(gameId: Long): List<PlayerStint>

    @Query(
        """
        SELECT * FROM player_stints
        WHERE gameId = :gameId AND playerId = :playerId AND offAtMs IS NULL
        LIMIT 1
        """
    )
    suspend fun getOpenStint(gameId: Long, playerId: Long): PlayerStint?

    @Insert
    suspend fun insertStint(stint: PlayerStint): Long

    @Insert
    suspend fun insertStints(stints: List<PlayerStint>)

    @Update
    suspend fun updateStint(stint: PlayerStint)

    @Query("DELETE FROM player_stints WHERE gameId = :gameId")
    suspend fun clearStints(gameId: Long)

    // ----- Events -----

    @Query("SELECT * FROM game_events WHERE gameId = :gameId ORDER BY clockMs DESC, id DESC")
    fun observeEvents(gameId: Long): Flow<List<GameEvent>>

    @Query("SELECT * FROM game_events WHERE gameId = :gameId ORDER BY clockMs DESC, id DESC LIMIT 1")
    suspend fun getLastEvent(gameId: Long): GameEvent?

    @Insert
    suspend fun insertEvent(event: GameEvent): Long

    @Delete
    suspend fun deleteEvent(event: GameEvent)
}
