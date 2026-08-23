package com.wheelscreener.domain.model

import kotlinx.datetime.Instant

/**
 * Represents stock/ETF underlying data
 */
data class Underlying(
    val symbol: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val volume: Long,
    val averageVolume20d: Long,
    val marketCap: Long,
    val fiftyTwoWeekHigh: Double,
    val fiftyTwoWeekLow: Double,
    val lastUpdate: Instant,
    
    // Technical indicators
    val sma20: Double? = null,
    val sma50: Double? = null,
    val sma200: Double? = null,
    val high20d: Double? = null,
    val high60d: Double? = null,
    
    // Fundamentals
    val freeCashFlowTTM: Double? = null,
    val netDebt: Double? = null,
    val sector: String? = null,
    
    // Events
    val nextEarningsDate: Instant? = null,
    val nextEarningsTime: String? = null, // "AMC", "BMO", etc.
    
    // Data quality
    val isStale: Boolean = false,
    val confidence: DataConfidence = DataConfidence.MEDIUM
)

enum class DataConfidence {
    HIGH,    // All data fresh and complete
    MEDIUM,  // One non-critical input missing
    LOW      // Stale or multiple inputs missing
}