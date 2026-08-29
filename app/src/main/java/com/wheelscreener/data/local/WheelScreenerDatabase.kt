package com.wheelscreener.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.wheelscreener.data.local.dao.ScanResultDao
import com.wheelscreener.data.local.dao.PaperPositionDao
import com.wheelscreener.data.local.dao.SettingsDao
import com.wheelscreener.data.local.dao.WatchlistDao
import com.wheelscreener.data.local.entity.ScanResultEntity
import com.wheelscreener.data.local.entity.PaperPositionEntity
import com.wheelscreener.data.local.entity.SettingsEntity
import com.wheelscreener.data.local.entity.WatchlistEntity

/**
 * Room database for Wheel Screener
 */
@Database(
    entities = [
        WatchlistEntity::class,
        ScanResultEntity::class,
        SettingsEntity::class,
        PaperPositionEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class WheelScreenerDatabase : RoomDatabase() {
    
    abstract fun watchlistDao(): WatchlistDao
    abstract fun scanResultDao(): ScanResultDao
    abstract fun settingsDao(): SettingsDao
    abstract fun paperPositionDao(): PaperPositionDao
}
