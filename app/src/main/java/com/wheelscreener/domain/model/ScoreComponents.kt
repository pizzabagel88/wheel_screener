package com.wheelscreener.domain.model

/**
 * Individual scoring components for transparency
 */
data class ScoreComponents(
    // Liquidity and execution quality (25 points)
    val liquidityScore: Double, // 0-25
    
    // IV opportunity (20 points)
    val ivScore: Double, // 0-20
    
    // Pullback quality (20 points)
    val pullbackScore: Double, // 0-20
    
    // Business/fundamental quality (15 points)
    val fundamentalScore: Double, // 0-15
    
    // Technical/trend safety (10 points)
    val technicalScore: Double, // 0-10
    
    // Portfolio diversification (10 points)
    val diversificationScore: Double, // 0-10
    
    // Composite score
    val compositeScore: Double // 0-100
) {
    companion object {
        fun fromComponents(
            liquidityScore: Double,
            ivScore: Double,
            pullbackScore: Double,
            fundamentalScore: Double,
            technicalScore: Double,
            diversificationScore: Double
        ): ScoreComponents {
            val compositeScore = liquidityScore + ivScore + pullbackScore + 
                               fundamentalScore + technicalScore + diversificationScore
            return ScoreComponents(
                liquidityScore = liquidityScore,
                ivScore = ivScore,
                pullbackScore = pullbackScore,
                fundamentalScore = fundamentalScore,
                technicalScore = technicalScore,
                diversificationScore = diversificationScore,
                compositeScore = compositeScore
            )
        }
    }
}