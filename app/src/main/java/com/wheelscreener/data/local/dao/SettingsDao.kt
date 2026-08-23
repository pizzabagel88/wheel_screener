package com.wheelscreener.data.local.dao

import androidx.room.*
import com.wheelscreener.data.local.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    
    @Query("SELECT * FROM settings WHERE key = :key")
    suspend fun getSetting(key: String): SettingsEntity?
    
    @Query("SELECT * FROM settings")
    fun getAllSettings(): Flow<List<SettingsEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: SettingsEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: List<SettingsEntity>)
    
    @Update
    suspend fun updateSetting(setting: SettingsEntity)
    
    @Query("DELETE FROM settings WHERE key = :key")
    suspend fun deleteSetting(key: String)
}