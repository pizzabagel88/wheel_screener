package com.wheelscreener.domain.scoring

import com.wheelscreener.domain.model.OptionContract
import com.wheelscreener.domain.model.StrategyConfig
import kotlinx.datetime.Instant

/**
 * Pure functions for DTE (Days To Expiration) selection
 * All functions are deterministic and testable
 */
object DteSelector {
    
    /**
     * Calculate DTE from current time to expiration
     */
    fun calculateDTE(currentTime: Instant, expiration: Instant): Int {
        val zone = java.time.ZoneId.of("America/New_York")
        val currentDate = java.time.Instant.ofEpochMilli(currentTime.toEpochMilliseconds())
            .atZone(zone)
            .toLocalDate()
        val expirationDate = java.time.Instant.ofEpochMilli(expiration.toEpochMilliseconds())
            .atZone(zone)
            .toLocalDate()
        return java.time.temporal.ChronoUnit.DAYS.between(currentDate, expirationDate).toInt()
    }
    
    /**
     * Check if DTE is within target range
     */
    fun isDTEInRange(dte: Int, config: StrategyConfig): Boolean {
        return dte >= config.dteMin && dte <= config.dteMax
    }
    
    /**
     * Filter contracts by DTE range
     */
    fun filterByDTE(
        contracts: List<OptionContract>,
        currentTime: Instant,
        config: StrategyConfig
    ): List<OptionContract> {
        return contracts.filter { contract ->
            val dte = calculateDTE(currentTime, contract.expiration)
            isDTEInRange(dte, config)
        }
    }
    
    /**
     * Find contracts with specific DTE (for testing/targeting)
     */
    fun findContractsByDTE(
        contracts: List<OptionContract>,
        currentTime: Instant,
        targetDTE: Int,
        tolerance: Int = 2
    ): List<OptionContract> {
        return contracts.filter { contract ->
            val dte = calculateDTE(currentTime, contract.expiration)
            dte in (targetDTE - tolerance)..(targetDTE + tolerance)
        }
    }
    
    /**
     * Get optimal DTE for a specific symbol based on volatility
     * Higher volatility = shorter DTE for risk management
     */
    fun getOptimalDTE(
        ivRank: Double?,
        ivPercentile: Double?,
        config: StrategyConfig
    ): Int {
        // If IV data is missing, use middle of range
        if (ivRank == null || ivPercentile == null) {
            return (config.dteMin + config.dteMax) / 2
        }
        
        // High IV (>65) - prefer shorter DTE
        return when {
            ivRank > 65 -> config.dteMin
            ivRank > 50 -> config.dteMin + 1
            ivRank < 30 -> config.dteMax
            else -> (config.dteMin + config.dteMax) / 2
        }
    }
    
    /**
     * Check if contract has weekly expiration
     * For this implementation, we assume DTE <= 8 indicates weekly
     */
    fun isWeeklyExpiration(dte: Int): Boolean {
        return dte <= 8
    }
    
    /**
     * Score DTE suitability (0-1 scale)
     * Higher score = better fit for strategy
     */
    fun scoreDTESuitability(dte: Int, config: StrategyConfig): Double {
        if (!isDTEInRange(dte, config)) return 0.0
        
        // Prefer middle of range for flexibility
        val range = config.dteMax - config.dteMin
        val middle = config.dteMin + range / 2.0
        val distanceFromMiddle = kotlin.math.abs(dte - middle)
        
        // Linear scoring: closer to middle = higher score
        return (1.0 - (distanceFromMiddle / (range / 2.0))).coerceAtLeast(0.1)
    }
}
