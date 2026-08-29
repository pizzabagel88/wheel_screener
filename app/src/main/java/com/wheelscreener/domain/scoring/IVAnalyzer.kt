package com.wheelscreener.domain.scoring

import com.wheelscreener.domain.model.CandidateFlag
import com.wheelscreener.domain.model.OptionContract

/**
 * Pure functions for IV (Implied Volatility) analysis
 * All functions are deterministic and testable
 */
object IVAnalyzer {
    
    /**
     * Check if IV rank is in target range
     */
    fun isIVRankInRange(
        ivRank: Double?,
        config: com.wheelscreener.domain.model.StrategyConfig
    ): Boolean {
        if (ivRank == null) return false
        return ivRank >= config.ivRankMin && ivRank <= config.ivRankMax
    }
    
    /**
     * Check if IV rank is in preferred target range
     */
    fun isIVRankInPreferredRange(
        ivRank: Double?,
        config: com.wheelscreener.domain.model.StrategyConfig
    ): Boolean {
        if (ivRank == null) return false
        return ivRank >= config.ivRankTarget && ivRank <= config.ivRankTargetMax
    }
    
    /**
     * Check if IV rank is elevated (>65)
     */
    fun isHighIVRank(ivRank: Double?): Boolean {
        if (ivRank == null) return false
        return ivRank > 65
    }
    
    /**
     * Check if IV rank is low (<25)
     */
    fun isLowIVRank(ivRank: Double?): Boolean {
        if (ivRank == null) return false
        return ivRank < 25
    }
    
    /**
     * Calculate IV score based on distance from target
     * Higher score = closer to preferred range
     */
    fun calculateIVScore(
        ivRank: Double?,
        config: com.wheelscreener.domain.model.StrategyConfig
    ): Double {
        if (ivRank == null) return 0.0
        
        // Perfect score if in preferred range
        if (isIVRankInPreferredRange(ivRank, config)) {
            return 20.0 // Max score for IV component
        }
        
        // Still good if in acceptable range
        if (isIVRankInRange(ivRank, config)) {
            // Linear scoring based on distance from preferred range
            val distanceFromPreferred = when {
                ivRank < config.ivRankTarget -> config.ivRankTarget - ivRank
                else -> ivRank - config.ivRankTargetMax
            }
            
            val maxDistance = maxOf(
                config.ivRankTarget - config.ivRankMin,
                config.ivRankMax - config.ivRankTargetMax
            )
            
            val normalizedDistance = distanceFromPreferred / maxDistance
            return 20.0 * (1.0 - normalizedDistance)
        }
        
        // Outside acceptable range - minimal score
        return when {
            ivRank < config.ivRankMin -> 5.0 // Too low
            ivRank > config.ivRankMax -> {
                // High IV rank - check if acceptable without event risk
                if (config.allowHighIvRankWithoutEvent) 10.0 else 0.0
            }
            else -> 0.0
        }
    }
    
    /**
     * Get IV-related flags
     */
    fun getIVFlags(
        contract: OptionContract,
        config: com.wheelscreener.domain.model.StrategyConfig,
        hasEventRisk: Boolean = false
    ): List<CandidateFlag> {
        val flags = mutableListOf<CandidateFlag>()
        
        if (contract.ivRank == null) {
            flags.add(CandidateFlag.IV_DATA_MISSING)
            return flags
        }
        
        if (isHighIVRank(contract.ivRank)) {
            flags.add(CandidateFlag.HIGH_IV_RANK)
        }
        
        return flags
    }
    
    /**
     * Calculate IV percentile score
     * Similar to IV rank but using percentile
     */
    fun calculateIVPercentileScore(ivPercentile: Double?): Double {
        if (ivPercentile == null) return 0.0
        
        // Prefer 40-60 percentile range
        return when {
            ivPercentile in 40.0..60.0 -> 1.0
            ivPercentile in 30.0..70.0 -> 0.8
            ivPercentile in 20.0..80.0 -> 0.6
            ivPercentile in 10.0..90.0 -> 0.4
            else -> 0.2
        }
    }
    
    /**
     * Check if IV data is sufficient
     */
    fun hasSufficientIVData(contract: OptionContract): Boolean {
        return contract.iv != null && contract.ivRank != null
    }
    
    /**
     * Score IV opportunity (0-20 scale as per strategy)
     * This is the main IV scoring function
     */
    fun scoreIVOpportunity(
        contract: OptionContract,
        config: com.wheelscreener.domain.model.StrategyConfig,
        hasEventRisk: Boolean = false
    ): Double {
        if (!hasSufficientIVData(contract)) {
            return 0.0
        }
        
        return calculateIVScore(contract.ivRank, config)
    }
    
    /**
     * Get IV assessment
     */
    fun getIVAssessment(
        contract: OptionContract,
        config: com.wheelscreener.domain.model.StrategyConfig,
        hasEventRisk: Boolean = false
    ): IVAssessment {
        val flags = getIVFlags(contract, config, hasEventRisk)
        val score = scoreIVOpportunity(contract, config, hasEventRisk)
        
        val regime = when {
            contract.ivRank == null -> IVRegime.UNKNOWN
            isHighIVRank(contract.ivRank) -> IVRegime.HIGH
            isLowIVRank(contract.ivRank) -> IVRegime.LOW
            isIVRankInPreferredRange(contract.ivRank, config) -> IVRegime.OPTIMAL
            else -> IVRegime.NORMAL
        }
        
        return IVAssessment(
            regime = regime,
            score = score,
            flags = flags
        )
    }
}

data class IVAssessment(
    val regime: IVRegime,
    val score: Double,
    val flags: List<com.wheelscreener.domain.model.CandidateFlag>
)

enum class IVRegime {
    OPTIMAL,    // 40-55 IV rank (preferred range)
    NORMAL,     // 25-65 IV rank (acceptable range)
    HIGH,       // >65 IV rank (elevated)
    LOW,        // <25 IV rank (depressed)
    UNKNOWN     // Data missing
}
