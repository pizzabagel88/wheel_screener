package com.wheelscreener.domain.model

import kotlinx.datetime.Instant

/**
 * Cash-Secured Put candidate with all calculated metrics
 */
data class CspCandidate(
    // Basic info
    val symbol: String,
    val underlyingPrice: Double,
    val strike: Double,
    val expiration: Instant,
    val dte: Int,
    
    // Option pricing
    val bid: Double,
    val ask: Double,
    val midpoint: Double,
    val credit: Double, // Using bid for conservative estimate
    
    // Calculated metrics
    val collateral: Double, // strike * 100
    val premiumYield: Double, // credit * 100 / collateral
    val breakEven: Double, // strike - credit
    val distanceToBreakEvenPercent: Double, // (breakEven - underlyingPrice) / underlyingPrice
    
    // Greeks
    val delta: Double,
    val absoluteDelta: Double,
    val iv: Double?,
    val ivRank: Double?,
    val ivPercentile: Double?,
    
    // Liquidity
    val openInterest: Int,
    val volume: Int,
    val bidAskSpread: Double,
    val bidAskSpreadPercent: Double,
    
    // Risk labels
    val assignmentRisk: AssignmentRisk,
    
    // Scoring
    val scoreComponents: ScoreComponents,
    
    // Flags and exclusions
    val flags: List<CandidateFlag>,
    val exclusionReason: String? = null,
    
    // Data quality
    val confidence: DataConfidence,
    val scanTimestamp: Instant
)

enum class AssignmentRisk {
    LOWER,
    MODERATE,
    ELEVATED
}