package com.wheelscreener.domain.model

import kotlinx.datetime.Instant

/**
 * Represents a single option contract
 */
data class OptionContract(
    val symbol: String,
    val underlyingSymbol: String,
    val contractType: ContractType, // CALL or PUT
    val strike: Double,
    val expiration: Instant,
    
    // Pricing
    val bid: Double,
    val ask: Double,
    val last: Double?,
    val volume: Int,
    val openInterest: Int,
    
    // Greeks
    val delta: Double?,
    val gamma: Double?,
    val theta: Double?,
    val vega: Double?,
    val iv: Double?,
    val ivRank: Double?, // 0-100
    val ivPercentile: Double?, // 0-100
    
    // Timestamps
    val lastUpdate: Instant,
    val isStale: Boolean = false
)

enum class ContractType {
    CALL,
    PUT
}