package com.wheelscreener.domain.backtest

import com.wheelscreener.data.remote.HistoricalBar
import com.wheelscreener.domain.model.OptionContract
import com.wheelscreener.domain.model.Underlying
import com.wheelscreener.domain.scoring.DteSelector
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.minus
import kotlin.random.Random

/**
 * Generates synthetic historical data for backtesting
 * Creates realistic-looking historical option chains and price data
 */
object BacktestDataGenerator {
    
    private val random = Random(123) // Fixed seed for reproducibility
    
    /**
     * Generate synthetic historical price bars
     */
    fun generateHistoricalBars(
        symbol: String,
        startDate: Instant,
        endDate: Instant,
        basePrice: Double = 100.0,
        volatility: Double = 0.02
    ): List<HistoricalBar> {
        val bars = mutableListOf<HistoricalBar>()
        var currentDate = startDate
        var currentPrice = basePrice
        
        while (currentDate <= endDate) {
            // Random walk with drift
            val dailyReturn = (random.nextDouble() - 0.5) * volatility * 2
            currentPrice = currentPrice * (1 + dailyReturn)
            
            // Generate OHLC
            val high = currentPrice * (1 + random.nextDouble() * 0.01)
            val low = currentPrice * (1 - random.nextDouble() * 0.01)
            val open = low + random.nextDouble() * (high - low)
            val close = low + random.nextDouble() * (high - low)
            
            val volume = (5_000_000 + random.nextDouble() * 10_000_000).toLong()
            
            bars.add(
                HistoricalBar(
                    timestamp = currentDate,
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = volume
                )
            )
            
            currentDate = currentDate.plus(1, DateTimeUnit.DAY, TimeZone.UTC)
        }
        
        return bars
    }
    
    /**
     * Generate synthetic historical option chain
     */
    fun generateHistoricalOptionChain(
        symbol: String,
        underlyingPrice: Double,
        expiration: Instant,
        strikeCount: Int = 10,
        ivBase: Double = 0.30
    ): List<OptionContract> {
        val contracts = mutableListOf<OptionContract>()
        val now = kotlinx.datetime.Clock.System.now()
        
        // Generate strikes around ATM
        val strikeStep = when {
            underlyingPrice < 50 -> 2.5
            underlyingPrice < 100 -> 5.0
            underlyingPrice < 200 -> 10.0
            else -> 20.0
        }
        
        val atmStrike = (underlyingPrice / strikeStep).toInt() * strikeStep
        val halfCount = strikeCount / 2
        val strikes = ((-halfCount)..halfCount).map { atmStrike + it * strikeStep }.filter { it > 0 }
        
        val dte = DteSelector.calculateDTE(now, expiration)
        
        strikes.forEach { strike ->
            // Generate both calls and puts
            listOf(com.wheelscreener.domain.model.ContractType.CALL, com.wheelscreener.domain.model.ContractType.PUT).forEach { type ->
                val moneyness = when (type) {
                    com.wheelscreener.domain.model.ContractType.CALL -> (underlyingPrice - strike) / underlyingPrice
                    com.wheelscreener.domain.model.ContractType.PUT -> (strike - underlyingPrice) / underlyingPrice
                }
                
                val timeValue = (underlyingPrice * ivBase * (dte / 365.0)) * (0.8 + random.nextDouble() * 0.4)
                val intrinsic = maxOf(0.0, moneyness * underlyingPrice)
                
                val bid = maxOf(0.01, intrinsic + timeValue * 0.95)
                val ask = bid * (1.0 + random.nextDouble() * 0.05)
                
                val delta = when (type) {
                    com.wheelscreener.domain.model.ContractType.CALL -> {
                        when {
                            moneyness > 0.1 -> 0.7 + random.nextDouble() * 0.2
                            moneyness > -0.1 -> 0.4 + random.nextDouble() * 0.3
                            else -> 0.1 + random.nextDouble() * 0.2
                        }
                    }
                    com.wheelscreener.domain.model.ContractType.PUT -> {
                        when {
                            moneyness > 0.1 -> 0.1 + random.nextDouble() * 0.2
                            moneyness > -0.1 -> 0.4 + random.nextDouble() * 0.3
                            else -> 0.7 + random.nextDouble() * 0.2
                        }
                    }
                }
                
                val ivRank = 20 + random.nextDouble() * 60
                val ivPercentile = ivRank + random.nextDouble() * 10 - 5
                
                contracts.add(
                    OptionContract(
                        symbol = "$symbol${expiration.toString().take(8)}${strike.toInt()}${type.name[0]}",
                        underlyingSymbol = symbol,
                        contractType = type,
                        strike = strike,
                        expiration = expiration,
                        bid = bid,
                        ask = ask,
                        last = (bid + ask) / 2,
                        volume = (50 + random.nextInt(5000)).toInt(),
                        openInterest = (100 + random.nextInt(10000)).toInt(),
                        delta = delta * if (type == com.wheelscreener.domain.model.ContractType.PUT) -1 else 1,
                        gamma = random.nextDouble() * 0.1,
                        theta = -random.nextDouble() * 0.05,
                        vega = random.nextDouble() * 0.2,
                        iv = ivBase * (0.8 + random.nextDouble() * 0.4),
                        ivRank = ivRank,
                        ivPercentile = ivPercentile,
                        lastUpdate = now
                    )
                )
            }
        }
        
        return contracts
    }
    
    /**
     * Generate synthetic backtest scenario
     */
    fun generateBacktestScenario(
        symbol: String,
        days: Int = 90,
        basePrice: Double = 100.0,
        scenarioType: ScenarioType = ScenarioType.NORMAL
    ): BacktestScenario {
        val now = Clock.System.now()
        val startDate = now.minus(days, DateTimeUnit.DAY, TimeZone.UTC)
        val endDate = now
        
        val volatility = when (scenarioType) {
            ScenarioType.NORMAL -> 0.02
            ScenarioType.HIGH_VOLATILITY -> 0.04
            ScenarioType.LOW_VOLATILITY -> 0.01
            ScenarioType.TRENDING_UP -> 0.015
            ScenarioType.TRENDING_DOWN -> 0.025
        }
        
        val bars = generateHistoricalBars(symbol, startDate, endDate, basePrice, volatility)
        
        // Generate option chains for key dates
        val optionChains = mutableMapOf<Instant, List<OptionContract>>()
        bars.chunked(7).forEach { weekBars ->
            if (weekBars.isNotEmpty()) {
                val weekDate = weekBars.first().timestamp
                val weekPrice = weekBars.first().close
                val expiration = weekDate.plus(14, DateTimeUnit.DAY, TimeZone.UTC)
                optionChains[weekDate] = generateHistoricalOptionChain(
                    symbol, weekPrice, expiration
                )
            }
        }
        
        return BacktestScenario(
            symbol = symbol,
            scenarioType = scenarioType,
            startDate = startDate,
            endDate = endDate,
            historicalBars = bars,
            optionChains = optionChains,
            basePrice = basePrice
        )
    }
    
    /**
     * Generate multiple scenarios for comprehensive testing
     */
    fun generateComprehensiveTestSet(): List<BacktestScenario> {
        val scenarios = mutableListOf<BacktestScenario>()
        
        // Normal market
        scenarios.add(generateBacktestScenario("NORMAL_TEST", 90, 100.0, ScenarioType.NORMAL))
        
        // High volatility
        scenarios.add(generateBacktestScenario("HIGH_VOL_TEST", 90, 100.0, ScenarioType.HIGH_VOLATILITY))
        
        // Low volatility
        scenarios.add(generateBacktestScenario("LOW_VOL_TEST", 90, 100.0, ScenarioType.LOW_VOLATILITY))
        
        // Trending up
        scenarios.add(generateBacktestScenario("TREND_UP_TEST", 90, 100.0, ScenarioType.TRENDING_UP))
        
        // Trending down
        scenarios.add(generateBacktestScenario("TREND_DOWN_TEST", 90, 100.0, ScenarioType.TRENDING_DOWN))
        
        return scenarios
    }
}

enum class ScenarioType {
    NORMAL,
    HIGH_VOLATILITY,
    LOW_VOLATILITY,
    TRENDING_UP,
    TRENDING_DOWN
}

data class BacktestScenario(
    val symbol: String,
    val scenarioType: ScenarioType,
    val startDate: Instant,
    val endDate: Instant,
    val historicalBars: List<HistoricalBar>,
    val optionChains: Map<Instant, List<OptionContract>>,
    val basePrice: Double
)