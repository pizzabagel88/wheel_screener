package com.wheelscreener.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Scan result entity for storing historical scan results
 */
@Entity(tableName = "scan_results")
data class ScanResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val scanTimestamp: Long,
    val symbol: String,
    val candidateType: String, // "CSP" or "CC"
    val strike: Double,
    val expiration: Long,
    val dte: Int,
    val bid: Double,
    val credit: Double,
    val collateral: Double,
    val premiumYield: Double,
    val delta: Double,
    val ivRank: Double?,
    val compositeScore: Double,
    val scoreComponentsJson: String, // JSON of ScoreComponents
    val flagsJson: String, // JSON array of CandidateFlag
    val exclusionReason: String?,
    val confidence: String // DataConfidence enum name
)