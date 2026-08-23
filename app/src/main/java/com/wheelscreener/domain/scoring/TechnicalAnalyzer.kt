package com.wheelscreener.domain.scoring

import com.wheelscreener.domain.model.CandidateFlag
import com.wheelscreener.domain.model.Underlying

/**
 * Pure functions for technical analysis
 * All functions are deterministic and testable
 */
object TechnicalAnalyzer {
    
    /**
     * Check if price is above 200-day SMA
     */
    fun isAbove200SMA(underlying: Underlying): Boolean {
        val sma200 = underlying.sma200 ?: return false
        return underlying.price >= sma200
    }
    
    /**
     * Check if price is within tolerance of 200-day SMA
     */
    fun isWithin200SMATolerance(underlying: Underlying, tolerance: Double): Boolean {
        val sma200 = underlying.sma200 ?: return false
        val distancePercent = kotlin.math.abs((underlying.price - sma200) / sma200) * 100
        return distancePercent <= tolerance
    }
    
    /**
     * Check if 20-day SMA is above 50-day SMA (uptrend)
     */
    fun is20Above50SMA(underlying: Underlying): Boolean {
        val sma20 = underlying.sma20 ?: return false
        val sma50 = underlying.sma50 ?: return false
        return sma20 >= sma50
    }
    
    /**
     * Check for downtrend (20 SMA < 50 SMA)
     */
    fun isDowntrend(underlying: Underlying): Boolean {
        return !is20Above50SMA(underlying)
    }
    
    /**
     * Calculate distance from 200-day SMA as percentage
     */
    fun calculateDistanceFrom200SMA(underlying: Underlying): Double? {
        val sma200 = underlying.sma200 ?: return null
        return ((underlying.price - sma200) / sma200) * 100
    }
    
    /**
     * Get trend-related flags
     */
    fun getTrendFlags(
        underlying: Underlying,
        config: com.wheelscreener.domain.model.StrategyConfig
    ): List<CandidateFlag> {
        val flags = mutableListOf<CandidateFlag>()
        
        if (!isAbove200SMA(underlying)) {
            flags.add(CandidateFlag.BELOW_200_SMA)
        }
        
        if (isDowntrend(underlying)) {
            flags.add(CandidateFlag.DOWNTREND_RISK)
        }
        
        return flags
    }
    
    /**
     * Score technical quality (0-10 scale as per strategy)
     * Higher score = better technical setup
     */
    fun scoreTechnical(
        underlying: Underlying,
        config: com.wheelscreener.domain.model.StrategyConfig
    ): Double {
        var score = 0.0
        
        // 200-day SMA relationship (5 points)
        val sma200Score = when {
            isAbove200SMA(underlying) -> 5.0
            isWithin200SMATolerance(underlying, config.allowBelow200SmaTolerance) -> 3.0
            else -> 0.0
        }
        score += sma200Score
        
        // 20/50-day regime (3 points)
        val regimeScore = if (is20Above50SMA(underlying)) 3.0 else 0.0
        score += regimeScore
        
        // Recent momentum (2 points)
        val momentumScore = when {
            underlying.changePercent > 0 -> 2.0
            underlying.changePercent > -2.0 -> 1.0
            else -> 0.0
        }
        score += momentumScore
        
        return score
    }
    
    /**
     * Calculate relative strength vs SPY (simplified)
     * Real implementation would compare to actual SPY data
     */
    fun calculateRelativeStrength(
        underlyingChangePercent: Double,
        spyChangePercent: Double = 0.0
    ): Double {
        return underlyingChangePercent - spyChangePercent
    }
    
    /**
     * Check if relative strength is improving
     */
    fun isImprovingRelativeStrength(
        relativeStrength: Double,
        config: com.wheelscreener.domain.model.StrategyConfig
    ): Boolean {
        if (!config.requireImprovingRelativeStrength) return true
        return relativeStrength > 0
    }
    
    /**
     * Get technical assessment
     */
    fun getTechnicalAssessment(
        underlying: Underlying,
        config: com.wheelscreener.domain.model.StrategyConfig
    ): TechnicalAssessment {
        val flags = getTrendFlags(underlying, config)
        val score = scoreTechnical(underlying, config)
        
        val trend = when {
            isDowntrend(underlying) -> Trend.DOWNTREND
            isAbove200SMA(underlying) && is20Above50SMA(underlying) -> Trend.UPTREND
            else -> Trend.SIDEWAYS
        }
        
        return TechnicalAssessment(
            trend = trend,
            score = score,
            flags = flags
        )
    }
}

data class TechnicalAssessment(
    val trend: Trend,
    val score: Double,
    val flags: List<com.wheelscreener.domain.model.CandidateFlag>
)

enum class Trend {
    UPTREND,    // Above 200 SMA, 20 SMA > 50 SMA
    DOWNTREND,  // Below 200 SMA, 20 SMA < 50 SMA
    SIDEWAYS    // Mixed signals
}