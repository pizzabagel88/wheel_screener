package com.wheelscreener.domain.backtest

import com.wheelscreener.data.remote.CorporateEvent
import com.wheelscreener.data.remote.EventType
import com.wheelscreener.data.remote.HistoricalBar
import com.wheelscreener.domain.model.*
import com.wheelscreener.domain.scoring.ScoringEngine
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus

/**
 * Backtest engine for validating scoring model performance
 * Replays scoring engine against historical data to check if higher scores correlate with better outcomes
 */
object BacktestEngine {
    
    /**
     * Run backtest on a historical scenario
     */
    fun runBacktest(
        scenario: BacktestScenario,
        config: StrategyConfig
    ): BacktestResult {
        val candidateResults = mutableListOf<BacktestCandidateResult>()
        
        // Process each week in the scenario
        scenario.optionChains.forEach { (date, optionChain) ->
            val underlyingAtDate = createUnderlyingFromBars(scenario.historicalBars, date)
            val events = generateSampleEvents(date)
            
            // Score each contract in the chain
            optionChain.forEach { contract ->
                // Only score puts for CSP backtest
                if (contract.contractType == ContractType.PUT) {
                    val scoringResult = ScoringEngine.scoreCSPCandidate(
                        contract, underlyingAtDate, events, config, date
                    )
                    
                    if (scoringResult.exclusionReason == null) {
                        val outcome = simulateOutcome(
                            contract, underlyingAtDate, scenario.historicalBars, date
                        )
                        
                        candidateResults.add(
                            BacktestCandidateResult(
                                date = date,
                                contract = contract,
                                underlying = underlyingAtDate,
                                scoreComponents = scoringResult.scoreComponents,
                                compositeScore = scoringResult.scoreComponents.compositeScore,
                                outcome = outcome
                            )
                        )
                    }
                }
            }
        }
        
        // Analyze results by score decile
        val decileAnalysis = analyzeByDecile(candidateResults)
        
        return BacktestResult(
            scenario = scenario,
            candidateResults = candidateResults,
            decileAnalysis = decileAnalysis,
            totalCandidates = candidateResults.size
        )
    }
    
    /**
     * Create underlying data from historical bars
     */
    private fun createUnderlyingFromBars(
        bars: List<HistoricalBar>,
        date: Instant
    ): Underlying {
        val barsUpToDate = bars.filter { it.timestamp <= date }
        if (barsUpToDate.isEmpty()) {
            return createDefaultUnderlying()
        }
        
        val recentBars = barsUpToDate.takeLast(20)
        val latestBar = recentBars.last()
        
        val sma20 = if (recentBars.size >= 20) recentBars.takeLast(20).map { bar -> bar.close }.average() else null
        val sma50 = if (barsUpToDate.size >= 50) barsUpToDate.takeLast(50).map { bar -> bar.close }.average() else null
        val sma200 = if (barsUpToDate.size >= 200) barsUpToDate.takeLast(200).map { bar -> bar.close }.average() else null
        
        val high20d = recentBars.map { bar -> bar.high }.maxOrNull()
        val high60d = if (barsUpToDate.size >= 60) barsUpToDate.takeLast(60).map { bar -> bar.high }.maxOrNull() else null
        
        return Underlying(
            symbol = "TEST",
            price = latestBar.close,
            change = latestBar.close - (recentBars.getOrNull(recentBars.size - 2)?.close ?: latestBar.close),
            changePercent = 0.0, // Simplified
            volume = latestBar.volume,
            averageVolume20d = recentBars.map { bar -> bar.volume }.average().toLong(),
            marketCap = 100_000_000_000L, // Simplified
            fiftyTwoWeekHigh = barsUpToDate.map { bar -> bar.high }.maxOrNull() ?: latestBar.high,
            fiftyTwoWeekLow = barsUpToDate.map { bar -> bar.low }.minOrNull() ?: latestBar.low,
            lastUpdate = date,
            sma20 = sma20,
            sma50 = sma50,
            sma200 = sma200,
            high20d = high20d,
            high60d = high60d,
            freeCashFlowTTM = 5_000_000_000.0, // Simplified
            netDebt = 2_000_000_000.0,
            sector = "Technology"
        )
    }
    
    /**
     * Create default underlying when no data available
     */
    private fun createDefaultUnderlying(): Underlying {
        val now = Clock.System.now()
        return Underlying(
            symbol = "TEST",
            price = 100.0,
            change = 0.0,
            changePercent = 0.0,
            volume = 10_000_000L,
            averageVolume20d = 8_000_000L,
            marketCap = 100_000_000_000L,
            fiftyTwoWeekHigh = 120.0,
            fiftyTwoWeekLow = 80.0,
            lastUpdate = now,
            sma20 = 98.0,
            sma50 = 95.0,
            sma200 = 90.0,
            high20d = 110.0,
            high60d = 115.0,
            freeCashFlowTTM = 5_000_000_000.0,
            netDebt = 2_000_000_000.0,
            sector = "Technology"
        )
    }
    
    /**
     * Generate sample corporate events
     */
    private fun generateSampleEvents(date: Instant): List<CorporateEvent> {
        // Generate earnings date 3-4 weeks out
        val earningsDate = date.plus(21 + (0..7).random(), DateTimeUnit.DAY, TimeZone.UTC)
        return listOf(
            CorporateEvent("TEST", EventType.EARNINGS, earningsDate, "AMC", "Quarterly Earnings")
        )
    }
    
    /**
     * Simulate option outcome at expiration
     */
    private fun simulateOutcome(
        contract: OptionContract,
        underlying: Underlying,
        historicalBars: List<HistoricalBar>,
        entryDate: Instant
    ): OptionOutcome {
        // Find underlying price at expiration
        val barsAtExpiration = historicalBars.filter { it.timestamp == contract.expiration }
        val priceAtExpiration = if (barsAtExpiration.isNotEmpty()) {
            barsAtExpiration.first().close
        } else {
            // Estimate from nearest available bar
            val nearestBars = historicalBars.minByOrNull { 
                kotlin.math.abs((it.timestamp - contract.expiration).inWholeDays) 
            }
            nearestBars?.close ?: underlying.price
        }
        
        val isITM = when (contract.contractType) {
            ContractType.PUT -> priceAtExpiration < contract.strike
            ContractType.CALL -> priceAtExpiration > contract.strike
        }
        
        val credit = contract.bid
        val collateral = contract.strike * 100
        
        val pnl = if (isITM) {
            when (contract.contractType) {
                ContractType.PUT -> {
                    // Put assigned: buy shares at strike
                    val lossPerShare = contract.strike - priceAtExpiration
                    credit - (lossPerShare * 100)
                }
                ContractType.CALL -> {
                    // Call assigned: sell shares at strike
                    val gainPerShare = contract.strike - priceAtExpiration
                    credit + (gainPerShare * 100)
                }
            }
        } else {
            // Expired worthless
            credit * 100
        }
        
        val pnlPercent = (pnl / collateral) * 100
        
        return OptionOutcome(
            isITM = isITM,
            priceAtExpiration = priceAtExpiration,
            pnl = pnl,
            pnlPercent = pnlPercent,
            credit = credit,
            collateral = collateral
        )
    }
    
    /**
     * Analyze results by score decile
     */
    private fun analyzeByDecile(results: List<BacktestCandidateResult>): List<DecileAnalysis> {
        if (results.isEmpty()) return emptyList()
        
        // Sort by composite score
        val sortedResults = results.sortedByDescending { it.compositeScore }
        
        // Split into deciles
        val decileSize = maxOf(1, sortedResults.size / 10)
        val decileAnalyses = mutableListOf<DecileAnalysis>()
        
        for (i in 0 until 10) {
            val startIndex = i * decileSize
            val endIndex = minOf((i + 1) * decileSize, sortedResults.size)
            
            if (startIndex < sortedResults.size) {
                val decileResults = sortedResults.subList(startIndex, endIndex)
                
                val avgPnlPercent = decileResults.map { it.outcome.pnlPercent }.average()
                val winRate = decileResults.count { it.outcome.pnl > 0 }.toDouble() / decileResults.size
                val avgScore = decileResults.map { it.compositeScore }.average()
                
                decileAnalyses.add(
                    DecileAnalysis(
                        decile = i + 1,
                        count = decileResults.size,
                        avgScore = avgScore,
                        avgPnlPercent = avgPnlPercent,
                        winRate = winRate,
                        totalPnl = decileResults.sumOf { it.outcome.pnl }
                    )
                )
            }
        }
        
        return decileAnalyses
    }
    
    /**
     * Export backtest results to CSV format
     */
    fun exportToCSV(result: BacktestResult): String {
        val csv = StringBuilder()
        
        // Header
        csv.appendLine("Date,Symbol,Strike,Expiration,Type,Delta,IV Rank,Composite Score,PnL Percent,ITM")
        
        // Data rows
        result.candidateResults.forEach { candidate ->
            csv.appendLine(
                "${candidate.date}," +
                "${candidate.contract.underlyingSymbol}," +
                "${candidate.contract.strike}," +
                "${candidate.contract.expiration}," +
                "${candidate.contract.contractType}," +
                "${candidate.contract.delta}," +
                "${candidate.contract.ivRank}," +
                "${candidate.compositeScore}," +
                "${candidate.outcome.pnlPercent}," +
                "${candidate.outcome.isITM}"
            )
        }
        
        return csv.toString()
    }
    
    /**
     * Export decile analysis to CSV format
     */
    fun exportDecileAnalysisToCSV(result: BacktestResult): String {
        val csv = StringBuilder()
        
        // Header
        csv.appendLine("Decile,Count,Avg Score,Avg PnL %,Win Rate,Total PnL")
        
        // Data rows
        result.decileAnalysis.forEach { decile ->
            csv.appendLine(
                "${decile.decile}," +
                "${decile.count}," +
                "${decile.avgScore}," +
                "${decile.avgPnlPercent}," +
                "${decile.winRate}," +
                "${decile.totalPnl}"
            )
        }
        
        return csv.toString()
    }
}

data class BacktestResult(
    val scenario: BacktestScenario,
    val candidateResults: List<BacktestCandidateResult>,
    val decileAnalysis: List<DecileAnalysis>,
    val totalCandidates: Int
)

data class BacktestCandidateResult(
    val date: Instant,
    val contract: OptionContract,
    val underlying: Underlying,
    val scoreComponents: ScoreComponents,
    val compositeScore: Double,
    val outcome: OptionOutcome
)

data class OptionOutcome(
    val isITM: Boolean,
    val priceAtExpiration: Double,
    val pnl: Double,
    val pnlPercent: Double,
    val credit: Double,
    val collateral: Double
)

data class DecileAnalysis(
    val decile: Int,
    val count: Int,
    val avgScore: Double,
    val avgPnlPercent: Double,
    val winRate: Double,
    val totalPnl: Double
)