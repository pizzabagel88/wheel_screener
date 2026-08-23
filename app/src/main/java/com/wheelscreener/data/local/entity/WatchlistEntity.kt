package com.wheelscreener.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Watchlist entity for storing symbols and their tags
 */
@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey
    val symbol: String,
    val tags: String, // Comma-separated tags: "Core,Satellite,ETF"
    val addedAt: Long,
    val isActive: Boolean = true
)