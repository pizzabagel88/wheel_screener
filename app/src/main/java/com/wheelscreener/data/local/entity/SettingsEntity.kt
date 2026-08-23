package com.wheelscreener.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Settings entity for storing user preferences and strategy configuration
 */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey
    val key: String,
    val value: String, // JSON string for complex values
    val updatedAt: Long
)