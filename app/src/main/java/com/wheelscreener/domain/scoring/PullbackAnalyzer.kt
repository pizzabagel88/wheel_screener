package com.wheelscreener.domain.scoring

import com.wheelscreener.domain.model.CandidateFlag
import com.wheelscreener.domain.model.Underlying

/**
 * Pure functions for pullback analysis
 * All functions are deterministic and testable
 */
object PullbackAnalyzer {
    
    /**
     * Calculate pullback from 20-day high
     */
    fun calculatePullback20d(underlying: Underlying): Double? {
        val high20d = underlying.high20d ?: return null
        if (high20d <= 0) return null
        
        return ((high20d - underlying.price) / high20d) * 100
    }
    
    /**
     * Calculate pullback from 60-day high
     */
    fun calculatePullback60d(underlying: Underlying): Double? {
        val high60d = underlying.high60d ?: return null
        if (high60d <= 0) return null
        
        return ((high60d - underlying.price) / high60d) * 100
    }
    
    /**
     * Check if pullback is in target range (20-day)
     */
    fun isPullbackInRange20d(pullback: Double?, config: com.wheelscreener.domain.model.StrategyConfig): Boolean {
        if (pullback == null) return false
        return pullback in config.pullbackMin20d..config.pullbackMax20d
    }
    
    /**
     * Check if pullback is in target range (60-day)
     */
    fun isPullbackInRange60d(pullback: Double?, config: com.wheelscreener.domain.model.StrategyConfig): Boolean {
        if (pullback == null) return false
        return pullback in config.pullbackMin60d..config.pullbackMax60d
    }
    
    /**
     * Check if decline exceeds breakdown threshold
     */
    fun isBreakdownDecline(underlying: Underlying, config: com.wheelscreener.domain.model.StrategyConfig): Boolean {
        val pullback20d = calculatePullback20d(underlying) ?: return false
        return pullback20d > config.maxDecline20d
    }
    
    /**
     * Categorize pullback quality
     */
    fun categorizePullback(
        underlying: Underlying,
        config: com.wheelscreener.domain.model.StrategyConfig
    ): PullbackCategory {
        val pullback20d = calculatePullback20d(underlying)
        val pullback60d = calculatePullback60d(underlying)
        
        // Check for breakdown
        if (isBreakdownDecline(underlying, config)) {
            return PullbackCategory.BREAKDOWN
        }
        
        // Check for ideal pullback (in range for both 20d and 60d)
        val inRange20d = isPullbackInRange20d(pullback20d, config)
        val inRange60d = isPullbackInRange60d(pullback60d, config)
        
        return when {
            inRange20d && inRange60d -> PullbackCategory.IDEAL
            inRange20d || inRange60d -> PullbackCategory.MODERATE
            pullback20d != null && pullback20d > 0 -> PullbackCategory.SHALLOW
            else -> PullbackCategory.NONE
        }
    }
    
    /**
     * Determine if pullback is symbol-specific vs market-led
     * This is a simplified version - real implementation would compare to market index
     */
    fun determinePullbackType(
        underlying: Underlying,
        marketChangePercent: Double = 0.0
    ): PullbackType {
        val changePercent = underlying.changePercent
        
        return when {
            changePercent < marketChangePercent - 2.0 -> PullbackType.SYMBOL_SPECIFIC
            changePercent > marketChangePercent + 2.0 -> PullbackType.BROAD_MARKET_LED
            else -> PullbackType.MIXED
        }
    }
    
    /**
     * Get pullback-related flags
     */
    fun getPullbackFlags(
        underlying: Underlying,
        config: com.wheelscreener.domain.model.StrategyConfig,
        marketChangePercent: Double = 0.0
    ): List<CandidateFlag> {
        val flags = mutableListOf<CandidateFlag>()
        
        if (isBreakdownDecline(underlying, config)) {
            flags.add(CandidateFlag.POSSIBLE_TREND_BREAKDOWN)
        }
        
        val pullbackType = determinePullbackType(underlying, marketChangePercent)
        when (pullbackType) {
            PullbackType.SYMBOL_SPECIFIC -> flags.add(CandidateFlag.SYMBOL_SPECIFIC_PULLBACK)
            PullbackType.BROAD_MARKET_LED -> flags.add(CandidateFlag.BROAD_MARKET_LED_PULLBACK)
            PullbackType.MIXED -> {} // No flag for mixed
        }
        
        val category = categorizePullback(underlying, config)
        if (category == PullbackCategory.BREAKDOWN) {
            flags.add(CandidateFlag.SHARP_RECENT_BREAKDOWN)
        }
        
        return flags
    }
    
    /**
     * Score pullback quality (0-20 scale as per strategy)
     * Higher score = better pullback opportunity
     */
    fun scorePullback(
        underlying: Underlying,
        config: com.wheelscreener.domain.model.StrategyConfig,
        marketChangePercent: Double = 0.0
    ): Double {
        val pullback20d = calculatePullback20d(underlying) ?: return 0.0
        val pullback60d = calculatePullback60d(underlying) ?: return 0.0
        
        var score = 0.0
        
        // Pullback magnitude (12 points)
        val inRange20d = isPullbackInRange20d(pullback20d, config)
        val inRange60d = isPullbackInRange60d(pullback60d, config)
        
        val magnitudeScore = when {
            inRange20d && inRange60d -> 12.0
            inRange20d || inRange60d -> 8.0
            pullback20d > 0 -> 4.0
            else -> 0.0
        }
        score += magnitudeScore
        
        // No breakdown penalty (4 points)
        val breakdownPenalty = if (isBreakdownDecline(underlying, config)) 0.0 else 4.0
        score += breakdownPenalty
        
        // Symbol-specific pullback bonus (4 points)
        val pullbackType = determinePullbackType(underlying, marketChangePercent)
        val typeBonus = when (pullbackType) {
            PullbackType.SYMBOL_SPECIFIC -> 4.0
            PullbackType.BROAD_MARKET_LED -> 2.0
            PullbackType.MIXED -> 3.0
        }
        score += typeBonus
        
        return score
    }
}

enum class PullbackCategory {
    IDEAL,       // 5-15% pullback from 20d high, 8-20% from 60d high
    MODERATE,    // In range for one timeframe
    SHALLOW,     // Some pullback but not in target range
    BREAKDOWN,   // >20% decline over 20 days
    NONE         // No pullback
}

enum class PullbackType {
    SYMBOL_SPECIFIC,      // Worse than market
    BROAD_MARKET_LED,    // Following market
    MIXED                // Combination
}