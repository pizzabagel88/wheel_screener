package com.wheelscreener.data.local.dao

import androidx.room.*
import com.wheelscreener.data.local.entity.WatchlistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {
    
    @Query("SELECT * FROM watchlist WHERE isActive = 1 ORDER BY symbol ASC")
    fun getActiveWatchlist(): Flow<List<WatchlistEntity>>
    
    @Query("SELECT * FROM watchlist WHERE symbol = :symbol")
    suspend fun getSymbol(symbol: String): WatchlistEntity?
    
    @Query("SELECT * FROM watchlist WHERE tags LIKE '%' || :tag || '%' AND isActive = 1")
    fun getSymbolsByTag(tag: String): Flow<List<WatchlistEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymbol(symbol: WatchlistEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymbols(symbols: List<WatchlistEntity>)
    
    @Update
    suspend fun updateSymbol(symbol: WatchlistEntity)
    
    @Query("UPDATE watchlist SET isActive = 0 WHERE symbol = :symbol")
    suspend fun deactivateSymbol(symbol: String)
    
    @Query("DELETE FROM watchlist WHERE symbol = :symbol")
    suspend fun deleteSymbol(symbol: String)
    
    @Query("DELETE FROM watchlist")
    suspend fun clearAll()
}