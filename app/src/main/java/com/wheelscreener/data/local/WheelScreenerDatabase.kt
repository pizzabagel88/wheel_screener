package com.wheelscreener.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.wheelscreener.data.local.dao.ScanResultDao
import com.wheelscreener.data.local.dao.SettingsDao
import com.wheelscreener.data.local.dao.WatchlistDao
import com.wheelscreener.data.local.entity.ScanResultEntity
import com.wheelscreener.data.local.entity.SettingsEntity
import com.wheelscreener.data.local.entity.WatchlistEntity

/**
 * Room database for Wheel Screener
 */
@Database(
    entities = [
        WatchlistEntity::class,
        ScanResultEntity::class,
        SettingsEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class WheelScreenerDatabase : RoomDatabase() {
    
    abstract fun watchlistDao(): WatchlistDao
    abstract fun scanResultDao(): ScanResultDao
    abstract fun settingsDao(): SettingsDao
}