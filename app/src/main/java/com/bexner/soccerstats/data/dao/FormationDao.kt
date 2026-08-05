package com.bexner.soccerstats.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.bexner.soccerstats.data.entity.Formation
import com.bexner.soccerstats.data.entity.FormationSlot
import com.bexner.soccerstats.data.entity.FormationWithSlots
import com.bexner.soccerstats.data.entity.MatchFormat
import com.bexner.soccerstats.data.entity.ShapePhase
import kotlinx.coroutines.flow.Flow

@Dao
interface FormationDao {

    @Transaction
    @Query("SELECT * FROM formations ORDER BY format ASC, name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<FormationWithSlots>>

    @Transaction
    @Query(
        """
        SELECT * FROM formations
        WHERE format = :format
        ORDER BY isPreset DESC, name COLLATE NOCASE ASC
        """
    )
    fun observeByFormat(format: MatchFormat): Flow<List<FormationWithSlots>>

    @Transaction
    @Query("SELECT * FROM formations WHERE id = :formationId")
    fun observeById(formationId: Long): Flow<FormationWithSlots?>

    @Transaction
    @Query("SELECT * FROM formations WHERE id = :formationId")
    suspend fun getById(formationId: Long): FormationWithSlots?

    @Query("SELECT COUNT(*) FROM formations")
    suspend fun count(): Int

    @Insert
    suspend fun insertFormation(formation: Formation): Long

    @Update
    suspend fun updateFormation(formation: Formation)

    @Delete
    suspend fun deleteFormation(formation: Formation)

    @Insert
    suspend fun insertSlots(slots: List<FormationSlot>)

    @Query("DELETE FROM formation_slots WHERE formationId = :formationId")
    suspend fun deleteSlotsFor(formationId: Long)

    @Query("DELETE FROM formation_slots WHERE formationId = :formationId AND phase = :phase")
    suspend fun deleteSlotsFor(formationId: Long, phase: ShapePhase)

    /** Replaces every marker on a formation — used when saving both shapes at once. */
    @Transaction
    suspend fun replaceSlots(formationId: Long, slots: List<FormationSlot>) {
        deleteSlotsFor(formationId)
        insertSlots(slots.map { it.copy(id = 0, formationId = formationId) })
    }

    /** Replaces just one shape, leaving the other phase untouched. */
    @Transaction
    suspend fun replaceSlots(formationId: Long, phase: ShapePhase, slots: List<FormationSlot>) {
        deleteSlotsFor(formationId, phase)
        insertSlots(slots.map { it.copy(id = 0, formationId = formationId, phase = phase) })
    }

    @Transaction
    suspend fun insertFormationWithSlots(formation: Formation, slots: List<FormationSlot>): Long {
        val newId = insertFormation(formation)
        insertSlots(slots.map { it.copy(id = 0, formationId = newId) })
        return newId
    }
}
