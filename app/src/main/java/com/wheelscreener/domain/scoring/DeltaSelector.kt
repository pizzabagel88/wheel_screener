package com.wheelscreener.domain.scoring

import com.wheelscreener.domain.model.OptionContract
import com.wheelscreener.domain.model.StrategyConfig

/**
 * Pure functions for delta selection
 * All functions are deterministic and testable
 */
object DeltaSelector {
    
    /**
     * Get absolute delta value
     */
    fun getAbsoluteDelta(delta: Double?): Double {
        return kotlin.math.abs(delta ?: 0.0)
    }
    
    /**
     * Check if delta is within target range for CSP
     */
    fun isCSPDeltaInRange(
        absoluteDelta: Double,
        isCore: Boolean,
        config: StrategyConfig
    ): Boolean {
        val (min, max) = if (isCore) {
            config.cspDeltaMinCore to config.cspDeltaMaxCore
        } else {
            config.cspDeltaMinSatellite to config.cspDeltaMaxSatellite
        }
        return absoluteDelta in min..max
    }
    
    /**
     * Check if delta is within target range for CC
     */
    fun isCCDeltaInRange(
        absoluteDelta: Double,
        isCore: Boolean,
        config: StrategyConfig
    ): Boolean {
        val (min, max) = if (isCore) {
            config.ccDeltaMinCore to config.ccDeltaMaxCore
        } else {
            config.ccDeltaMinSatellite to config.ccDeltaMaxSatellite
        }
        return absoluteDelta in min..max
    }
    
    /**
     * Find contract with delta closest to target
     */
    fun findClosestDelta(
        contracts: List<OptionContract>,
        targetDelta: Double,
        config: StrategyConfig
    ): OptionContract? {
        return contracts
            .filter { it.delta != null }
            .minByOrNull { kotlin.math.abs(it.delta!! - targetDelta) }
    }
    
    /**
     * Find CSP contract closest to target delta range
     */
    fun findCSPCandidate(
        contracts: List<OptionContract>,
        isCore: Boolean,
        config: StrategyConfig
    ): OptionContract? {
        val (minDelta, maxDelta) = if (isCore) {
            config.cspDeltaMinCore to config.cspDeltaMaxCore
        } else {
            config.cspDeltaMinSatellite to config.cspDeltaMaxSatellite
        }
        
        val targetDelta = (minDelta + maxDelta) / 2.0
        
        return contracts
            .filter { it.contractType == com.wheelscreener.domain.model.ContractType.PUT }
            .filter { it.delta != null }
            .filter { getAbsoluteDelta(it.delta) in minDelta..maxDelta }
            .minByOrNull { kotlin.math.abs(it.delta!! - targetDelta) }
    }
    
    /**
     * Find CC contract closest to target delta range
     */
    fun findCCCandidate(
        contracts: List<OptionContract>,
        isCore: Boolean,
        config: StrategyConfig
    ): OptionContract? {
        val (minDelta, maxDelta) = if (isCore) {
            config.ccDeltaMinCore to config.ccDeltaMaxCore
        } else {
            config.ccDeltaMinSatellite to config.ccDeltaMaxSatellite
        }
        
        val targetDelta = (minDelta + maxDelta) / 2.0
        
        return contracts
            .filter { it.contractType == com.wheelscreener.domain.model.ContractType.CALL }
            .filter { it.delta != null }
            .filter { getAbsoluteDelta(it.delta) in minDelta..maxDelta }
            .minByOrNull { kotlin.math.abs(it.delta!! - targetDelta) }
    }
    
    /**
     * Check if delta indicates elevated assignment risk
     * Higher absolute delta = higher assignment probability
     */
    fun hasElevatedAssignmentRisk(absoluteDelta: Double): Boolean {
        return absoluteDelta > 0.4
    }
    
    /**
     * Score delta suitability (0-1 scale)
     * Higher score = better fit for strategy
     */
    fun scoreDeltaSuitability(
        absoluteDelta: Double,
        isCore: Boolean,
        config: StrategyConfig
    ): Double {
        val (minDelta, maxDelta) = if (isCore) {
            config.cspDeltaMinCore to config.cspDeltaMaxCore
        } else {
            config.cspDeltaMinSatellite to config.cspDeltaMaxSatellite
        }
        
        if (absoluteDelta !in minDelta..maxDelta) return 0.0
        
        // Prefer middle of range
        val targetDelta = (minDelta + maxDelta) / 2.0
        val distanceFromTarget = kotlin.math.abs(absoluteDelta - targetDelta)
        val rangeWidth = maxDelta - minDelta
        
        return 1.0 - (distanceFromTarget / (rangeWidth / 2.0))
    }
    
    /**
     * Determine if symbol should be treated as core or satellite
     * Based on market cap and volatility
     */
    fun classifySymbol(
        marketCap: Long,
        ivRank: Double?,
        config: StrategyConfig
    ): Boolean {
        // Above preferred market cap = core
        if (marketCap >= config.marketCapPreferred) return true
        
        // Below minimum = satellite
        if (marketCap < config.marketCapMin) return false
        
        // In between - check IV
        // High IV symbols treated as satellite for risk management
        if (ivRank != null && ivRank > 50) return false
        
        return true
    }
}