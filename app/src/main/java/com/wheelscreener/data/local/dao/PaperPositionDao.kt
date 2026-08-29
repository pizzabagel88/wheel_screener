package com.wheelscreener.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wheelscreener.data.local.entity.PaperPositionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaperPositionDao {
    @Query("SELECT * FROM paper_positions WHERE status = 'OPEN' ORDER BY expiration ASC")
    fun observeOpenPositions(): Flow<List<PaperPositionEntity>>

    @Query("SELECT * FROM paper_positions ORDER BY openedAt DESC")
    fun observeAllPositions(): Flow<List<PaperPositionEntity>>

    @Query("SELECT * FROM paper_positions WHERE id = :id")
    suspend fun getById(id: Long): PaperPositionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(position: PaperPositionEntity): Long

    @Update
    suspend fun update(position: PaperPositionEntity)

    @Query("DELETE FROM paper_positions WHERE id = :id")
    suspend fun delete(id: Long)
}
