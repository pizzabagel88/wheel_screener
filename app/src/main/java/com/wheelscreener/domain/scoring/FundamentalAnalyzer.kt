package com.wheelscreener.domain.scoring

import com.wheelscreener.domain.model.CandidateFlag
import com.wheelscreener.domain.model.Underlying

/**
 * Pure functions for fundamental analysis
 * All functions are deterministic and testable
 */
object FundamentalAnalyzer {
    
    /**
     * Check if company has positive free cash flow
     */
    fun hasPositiveFCF(underlying: Underlying): Boolean {
        val fcf = underlying.freeCashFlowTTM ?: return false
        return fcf > 0
    }
    
    /**
     * Check if net debt is concerning
     * Simple heuristic: net debt > 50% of market cap
     */
    fun hasHighNetDebt(underlying: Underlying): Boolean {
        val netDebt = underlying.netDebt ?: return false
        if (netDebt <= 0) return false
        
        val netDebtToMarketCap = netDebt / underlying.marketCap
        return netDebtToMarketCap > 0.5
    }
    
    /**
     * Check if market cap meets minimum threshold
     */
    fun meetsMarketCapMinimum(
        underlying: Underlying,
        config: com.wheelscreener.domain.model.StrategyConfig
    ): Boolean {
        return underlying.marketCap >= config.marketCapMin
    }
    
    /**
     * Check if market cap meets preferred threshold
     */
    fun meetsMarketCapPreferred(
        underlying: Underlying,
        config: com.wheelscreener.domain.model.StrategyConfig
    ): Boolean {
        return underlying.marketCap >= config.marketCapPreferred
    }
    
    /**
     * Get fundamental-related flags
     */
    fun getFundamentalFlags(
        underlying: Underlying,
        config: com.wheelscreener.domain.model.StrategyConfig
    ): List<CandidateFlag> {
        val flags = mutableListOf<CandidateFlag>()
        
        if (!hasPositiveFCF(underlying)) {
            flags.add(CandidateFlag.NEGATIVE_FREE_CASH_FLOW)
        }
        
        if (hasHighNetDebt(underlying)) {
            flags.add(CandidateFlag.HIGH_NET_DEBT)
        }
        
        if (!meetsMarketCapMinimum(underlying, config)) {
            flags.add(CandidateFlag.BELOW_MARKET_CAP_THRESHOLD)
        }
        
        return flags
    }
    
    /**
     * Score fundamental quality (0-15 scale as per strategy)
     * Higher score = better fundamentals
     */
    fun scoreFundamentals(
        underlying: Underlying,
        config: com.wheelscreener.domain.model.StrategyConfig
    ): Double {
        var score = 0.0
        
        // Market cap (6 points)
        val marketCapScore = when {
            meetsMarketCapPreferred(underlying, config) -> 6.0
            meetsMarketCapMinimum(underlying, config) -> 4.0
            else -> 0.0
        }
        score += marketCapScore
        
        // Free cash flow (5 points)
        val fcfScore = if (hasPositiveFCF(underlying)) 5.0 else 0.0
        score += fcfScore
        
        // Debt profile (4 points)
        val debtScore = when {
            underlying.netDebt == null -> 2.0 // No data
            underlying.netDebt <= 0 -> 4.0 // Net cash
            !hasHighNetDebt(underlying) -> 3.0 // Manageable debt
            else -> 0.0 // High debt
        }
        score += debtScore
        
        return score
    }
    
    /**
     * Get fundamental assessment
     */
    fun getFundamentalAssessment(
        underlying: Underlying,
        config: com.wheelscreener.domain.model.StrategyConfig
    ): FundamentalAssessment {
        val flags = getFundamentalFlags(underlying, config)
        val score = scoreFundamentals(underlying, config)
        
        val quality = when {
            score >= 12.0 -> FundamentalQuality.HIGH
            score >= 8.0 -> FundamentalQuality.MEDIUM
            score >= 4.0 -> FundamentalQuality.LOW
            else -> FundamentalQuality.POOR
        }
        
        return FundamentalAssessment(
            quality = quality,
            score = score,
            flags = flags
        )
    }
}

data class FundamentalAssessment(
    val quality: FundamentalQuality,
    val score: Double,
    val flags: List<com.wheelscreener.domain.model.CandidateFlag>
)

enum class FundamentalQuality {
    HIGH,    // Strong fundamentals
    MEDIUM,  // Adequate fundamentals
    LOW,     // Weak fundamentals
    POOR     // Poor fundamentals
}