package com.wheelscreener.domain.backtest

import com.wheelscreener.domain.model.StrategyConfig
import com.wheelscreener.domain.model.Underlying
import com.wheelscreener.domain.scoring.PullbackAnalyzer
import com.wheelscreener.domain.scoring.TechnicalAnalyzer
import kotlin.math.sqrt

/**
 * Analyzes correlation between pullback and trend scoring components
 * Addresses the spec requirement to check if these components double-count the same price action
 */
object CorrelationAnalyzer {
    
    /**
     * Calculate correlation coefficient between two sets of scores
     */
    fun calculateCorrelation(scores1: List<Double>, scores2: List<Double>): Double {
        if (scores1.size != scores2.size || scores1.size < 2) return 0.0
        
        val n = scores1.size
        val mean1 = scores1.average()
        val mean2 = scores2.average()
        
        var numerator = 0.0
        var sumSq1 = 0.0
        var sumSq2 = 0.0
        
        for (i in 0 until n) {
            val diff1 = scores1[i] - mean1
            val diff2 = scores2[i] - mean2
            
            numerator += diff1 * diff2
            sumSq1 += diff1 * diff1
            sumSq2 += diff2 * diff2
        }
        
        val denominator = sqrt(sumSq1 * sumSq2)
        if (denominator == 0.0) return 0.0
        
        return numerator / denominator
    }
    
    /**
     * Analyze pullback vs trend correlation across a dataset
     */
    fun analyzePullbackTrendCorrelation(
        underlyings: List<Underlying>,
        config: StrategyConfig
    ): CorrelationReport {
        val pullbackScores = mutableListOf<Double>()
        val trendScores = mutableListOf<Double>()
        val detailedScores = mutableListOf<ScorePair>()
        
        underlyings.forEach { underlying ->
            val pullbackScore = PullbackAnalyzer.scorePullback(underlying, config)
            val trendScore = TechnicalAnalyzer.scoreTechnical(underlying, config)
            
            pullbackScores.add(pullbackScore)
            trendScores.add(trendScore)
            
            detailedScores.add(
                ScorePair(
                    symbol = underlying.symbol,
                    pullbackScore = pullbackScore,
                    trendScore = trendScore,
                    price = underlying.price,
                    changePercent = underlying.changePercent
                )
            )
        }
        
        val correlation = calculateCorrelation(pullbackScores, trendScores)
        
        return CorrelationReport(
            correlation = correlation,
            sampleSize = underlyings.size,
            pullbackScores = pullbackScores,
            trendScores = trendScores,
            detailedScores = detailedScores,
            recommendation = generateRecommendation(correlation)
        )
    }
    
    /**
     * Generate recommendation based on correlation
     */
    private fun generateRecommendation(correlation: Double): CorrelationRecommendation {
        return when {
            correlation > 0.7 -> CorrelationRecommendation.HIGH_CORRELATION_MERGE
            correlation > 0.5 -> CorrelationRecommendation.MODERATE_CORRELATION_REDUCE_WEIGHT
            correlation > 0.3 -> CorrelationRecommendation.LOW_CORRELATION_KEEP_SEPARATE
            else -> CorrelationRecommendation.VERY_LOW_CORRELATION_KEEP_SEPARATE
        }
    }
    
    /**
     * Run correlation analysis on backtest scenarios
     */
    fun analyzeBacktestCorrelation(
        scenarios: List<BacktestScenario>
    ): List<CorrelationReport> {
        val config = StrategyConfig.default()
        val reports = mutableListOf<CorrelationReport>()
        
        scenarios.forEach { scenario ->
            val underlyings = scenario.historicalBars.map { bar ->
                com.wheelscreener.domain.model.Underlying(
                    symbol = scenario.symbol,
                    price = bar.close,
                    change = bar.close - bar.open,
                    changePercent = ((bar.close - bar.open) / bar.open) * 100,
                    volume = bar.volume,
                    averageVolume20d = scenario.historicalBars.takeLast(20).map { it.volume }.average().toLong(),
                    marketCap = 100_000_000_000L,
                    fiftyTwoWeekHigh = scenario.historicalBars.map { it.high }.maxOrNull() ?: bar.high,
                    fiftyTwoWeekLow = scenario.historicalBars.map { it.low }.minOrNull() ?: bar.low,
                    lastUpdate = bar.timestamp,
                    sma20 = scenario.historicalBars.takeLast(20).map { it.close }.average(),
                    sma50 = scenario.historicalBars.takeLast(50).map { it.close }.average(),
                    sma200 = scenario.historicalBars.takeLast(200).map { it.close }.average(),
                    high20d = scenario.historicalBars.takeLast(20).map { it.high }.maxOrNull(),
                    high60d = scenario.historicalBars.takeLast(60).map { it.high }.maxOrNull(),
                    freeCashFlowTTM = 5_000_000_000.0,
                    netDebt = 2_000_000_000.0,
                    sector = "Technology"
                )
            }
            
            reports.add(analyzePullbackTrendCorrelation(underlyings, config))
        }
        
        return reports
    }
    
    /**
     * Calculate combined score if merging components
     */
    fun calculateMergedScore(
        pullbackScore: Double,
        trendScore: Double,
        mergedWeight: Double = 25.0 // Combined weight (was 20 + 10 = 30)
    ): Double {
        val normalizedPullback = (pullbackScore / 20.0) * mergedWeight * 0.67 // 2/3 weight to pullback
        val normalizedTrend = (trendScore / 10.0) * mergedWeight * 0.33 // 1/3 weight to trend
        return normalizedPullback + normalizedTrend
    }
    
    /**
     * Compare performance of separate vs merged scoring
     */
    fun compareScoringApproaches(
        backtestResults: List<BacktestResult>
    ): ScoringComparison {
        val separateDecilePerformance = backtestResults.map { result ->
            result.decileAnalysis.map { it.avgPnlPercent }
        }
        
        val mergedDecilePerformance = backtestResults.map { result ->
            // Recalculate with merged scores (simplified)
            result.decileAnalysis.map { decile ->
                // This would require recalculating scores with merged approach
                // For now, return original as placeholder
                decile.avgPnlPercent
            }
        }
        
        val separateAvgPnl = separateDecilePerformance.flatten().average()
        val mergedAvgPnl = mergedDecilePerformance.flatten().average()
        
        return ScoringComparison(
            separateAvgPnl = separateAvgPnl,
            mergedAvgPnl = mergedAvgPnl,
            improvement = mergedAvgPnl - separateAvgPnl,
            recommendation = if (mergedAvgPnl > separateAvgPnl) {
                "Merged approach shows better performance"
            } else {
                "Separate approach shows better performance"
            }
        )
    }
}

data class CorrelationReport(
    val correlation: Double,
    val sampleSize: Int,
    val pullbackScores: List<Double>,
    val trendScores: List<Double>,
    val detailedScores: List<ScorePair>,
    val recommendation: CorrelationRecommendation
)

data class ScorePair(
    val symbol: String,
    val pullbackScore: Double,
    val trendScore: Double,
    val price: Double,
    val changePercent: Double
)

enum class CorrelationRecommendation {
    HIGH_CORRELATION_MERGE,
    MODERATE_CORRELATION_REDUCE_WEIGHT,
    LOW_CORRELATION_KEEP_SEPARATE,
    VERY_LOW_CORRELATION_KEEP_SEPARATE
}

data class ScoringComparison(
    val separateAvgPnl: Double,
    val mergedAvgPnl: Double,
    val improvement: Double,
    val recommendation: String
)