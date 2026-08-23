package com.wheelscreener.data.local.dao

import androidx.room.*
import com.wheelscreener.data.local.entity.ScanResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanResultDao {
    
    @Query("SELECT * FROM scan_results ORDER BY scanTimestamp DESC LIMIT :limit")
    fun getRecentScanResults(limit: Int = 100): Flow<List<ScanResultEntity>>
    
    @Query("SELECT * FROM scan_results WHERE scanTimestamp = :timestamp ORDER BY compositeScore DESC")
    fun getScanResultsByTimestamp(timestamp: Long): Flow<List<ScanResultEntity>>
    
    @Query("SELECT * FROM scan_results WHERE candidateType = :type AND scanTimestamp = :timestamp ORDER BY compositeScore DESC LIMIT :limit")
    fun getTopCandidatesByType(type: String, timestamp: Long, limit: Int = 10): Flow<List<ScanResultEntity>>
    
    @Query("SELECT * FROM scan_results WHERE symbol = :symbol ORDER BY scanTimestamp DESC")
    fun getScanResultsBySymbol(symbol: String): Flow<List<ScanResultEntity>>
    
    @Query("SELECT DISTINCT scanTimestamp FROM scan_results ORDER BY scanTimestamp DESC")
    fun getScanTimestamps(): Flow<List<Long>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanResult(result: ScanResultEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanResults(results: List<ScanResultEntity>)
    
    @Query("DELETE FROM scan_results WHERE scanTimestamp < :timestamp")
    suspend fun deleteOldResults(timestamp: Long)
    
    @Query("DELETE FROM scan_results")
    suspend fun clearAll()
}