package com.wheelscreener.domain.model

/**
 * Strategy configuration - all configurable parameters
 * This is versioned to allow for migration
 */
data class StrategyConfig(
    val version: Int = 1,
    
    // DTE configuration
    val dteMin: Int = 7,
    val dteMax: Int = 14,
    
    // Delta configuration
    val cspDeltaMinCore: Double = 0.20,
    val cspDeltaMaxCore: Double = 0.30,
    val cspDeltaMinSatellite: Double = 0.15,
    val cspDeltaMaxSatellite: Double = 0.25,
    
    val ccDeltaMinCore: Double = 0.20,
    val ccDeltaMaxCore: Double = 0.30,
    val ccDeltaMinSatellite: Double = 0.15,
    val ccDeltaMaxSatellite: Double = 0.25,
    
    // Market cap configuration
    val marketCapMin: Long = 50_000_000_000L, // $50B
    val marketCapPreferred: Long = 100_000_000_000L, // $100B
    
    // Liquidity configuration
    val minOpenInterest: Int = 500,
    val minContractVolume: Int = 100,
    val maxSpreadPercent: Double = 0.05, // 5%
    val maxSpreadAbsolute: Double = 0.10, // $0.10
    val minAverageDollarVolume: Long = 500_000_000L, // $500M/day
    val requireWeeklyExpiration: Boolean = true,
    
    // IV configuration
    val ivRankMin: Int = 25,
    val ivRankMax: Int = 65,
    val ivRankTarget: Int = 40,
    val ivRankTargetMax: Int = 55,
    val allowHighIvRankWithoutEvent: Boolean = true,
    
    // Pullback configuration
    val pullbackMin20d: Double = 0.05, // 5%
    val pullbackMax20d: Double = 0.15, // 15%
    val pullbackMin60d: Double = 0.08, // 8%
    val pullbackMax60d: Double = 0.20, // 20%
    val maxDecline20d: Double = 0.20, // 20% - breakdown threshold
    
    // Trend configuration
    val preferAbove200Sma: Boolean = true,
    val allowBelow200SmaTolerance: Double = 0.05, // 5%
    val requireImprovingRelativeStrength: Boolean = false,
    
    // Score weights
    val liquidityWeight: Double = 25.0,
    val ivWeight: Double = 20.0,
    val pullbackWeight: Double = 20.0,
    val fundamentalWeight: Double = 15.0,
    val technicalWeight: Double = 10.0,
    val diversificationWeight: Double = 10.0
) {
    companion object {
        fun default() = StrategyConfig()
    }
}