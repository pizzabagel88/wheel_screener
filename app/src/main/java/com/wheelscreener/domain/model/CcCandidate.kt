package com.wheelscreener.domain.model

import kotlinx.datetime.Instant

/**
 * Covered Call candidate with all calculated metrics
 */
data class CcCandidate(
    // Basic info
    val symbol: String,
    val underlyingPrice: Double,
    val strike: Double,
    val expiration: Instant,
    val dte: Int,
    
    // Position details
    val shareCount: Int,
    val costBasis: Double,
    val availableShares: Int,
    
    // Option pricing
    val bid: Double,
    val ask: Double,
    val midpoint: Double,
    val credit: Double, // Using bid for conservative estimate
    
    // Calculated metrics
    val effectiveExitPrice: Double, // strike + credit
    val returnIfCalled: Double, // (effectiveExitPrice - costBasis) / costBasis
    val downsideRemaining: Double, // underlyingPrice - breakEven
    val breakEven: Double, // costBasis - credit
    
    // Income-first mode
    val isIncomeFirst: Boolean, // strike < costBasis
    val incomeFirstWarning: Boolean,
    
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
    
    // Dividend risk
    val exDividendDate: Instant?,
    val dividendAssignmentRisk: Boolean,
    
    // Scoring
    val scoreComponents: ScoreComponents,
    
    // Flags and exclusions
    val flags: List<CandidateFlag>,
    val exclusionReason: String? = null,
    
    // Data quality
    val confidence: DataConfidence,
    val scanTimestamp: Instant
)