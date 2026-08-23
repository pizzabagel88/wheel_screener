package com.wheelscreener.domain.scoring

import com.wheelscreener.domain.model.CandidateFlag
import com.wheelscreener.domain.model.OptionContract
import com.wheelscreener.domain.model.StrategyConfig
import com.wheelscreener.domain.model.Underlying

/**
 * Pure functions for liquidity filtering and rejection
 * All functions are deterministic and testable
 */
object LiquidityFilter {
    
    /**
     * Check if option contract meets minimum open interest
     */
    fun hasMinimumOpenInterest(
        contract: OptionContract,
        config: StrategyConfig
    ): Boolean {
        return contract.openInterest >= config.minOpenInterest
    }
    
    /**
     * Check if option contract meets minimum volume
     */
    fun hasMinimumVolume(
        contract: OptionContract,
        config: StrategyConfig
    ): Boolean {
        return contract.volume >= config.minContractVolume
    }
    
    /**
     * Calculate bid-ask spread percentage
     */
    fun calculateSpreadPercent(bid: Double, ask: Double): Double {
        if (bid <= 0) return Double.MAX_VALUE
        return ((ask - bid) / bid) * 100
    }
    
    /**
     * Check if spread is acceptable
     */
    fun hasAcceptableSpread(
        contract: OptionContract,
        config: StrategyConfig
    ): Boolean {
        val spreadPercent = calculateSpreadPercent(contract.bid, contract.ask)
        val spreadAbsolute = contract.ask - contract.bid
        
        // Must meet both criteria
        val percentOk = spreadPercent <= config.maxSpreadPercent
        val absoluteOk = spreadAbsolute <= config.maxSpreadAbsolute
        
        return percentOk && absoluteOk
    }
    
    /**
     * Check if underlying has sufficient average dollar volume
     */
    fun hasSufficientDollarVolume(
        underlying: Underlying,
        config: StrategyConfig
    ): Boolean {
        val avgDollarVolume = underlying.averageVolume20d * underlying.price
        return avgDollarVolume >= config.minAverageDollarVolume
    }
    
    /**
     * Check if weekly expiration is available
     */
    fun hasWeeklyExpiration(
        dte: Int,
        config: StrategyConfig
    ): Boolean {
        if (!config.requireWeeklyExpiration) return true
        return DteSelector.isWeeklyExpiration(dte)
    }
    
    /**
     * Comprehensive liquidity check
     * Returns list of flags if any criteria fail
     */
    fun checkLiquidity(
        contract: OptionContract,
        underlying: Underlying,
        dte: Int,
        config: StrategyConfig
    ): List<CandidateFlag> {
        val flags = mutableListOf<CandidateFlag>()
        
        if (!hasMinimumOpenInterest(contract, config)) {
            flags.add(CandidateFlag.LOW_OPEN_INTEREST)
        }
        
        if (!hasMinimumVolume(contract, config)) {
            flags.add(CandidateFlag.LOW_VOLUME)
        }
        
        if (!hasAcceptableSpread(contract, config)) {
            flags.add(CandidateFlag.WIDE_SPREAD)
        }
        
        if (!hasWeeklyExpiration(dte, config)) {
            flags.add(CandidateFlag.NO_WEEKLY_EXPIRATION)
        }
        
        if (!hasSufficientDollarVolume(underlying, config)) {
            flags.add(CandidateFlag.BELOW_MARKET_CAP_THRESHOLD)
        }
        
        return flags
    }
    
    /**
     * Check if contract passes all liquidity requirements
     */
    fun passesLiquidityRequirements(
        contract: OptionContract,
        underlying: Underlying,
        dte: Int,
        config: StrategyConfig
    ): Boolean {
        return checkLiquidity(contract, underlying, dte, config).isEmpty()
    }
    
    /**
     * Score liquidity quality (0-25 scale as per strategy)
     * Based on spread, OI, and volume
     */
    fun scoreLiquidity(
        contract: OptionContract,
        underlying: Underlying,
        dte: Int,
        config: StrategyConfig
    ): Double {
        var score = 0.0
        
        // Spread quality (10 points)
        val spreadPercent = calculateSpreadPercent(contract.bid, contract.ask)
        val spreadScore = when {
            spreadPercent <= 1.0 -> 10.0
            spreadPercent <= 2.0 -> 8.0
            spreadPercent <= 3.0 -> 6.0
            spreadPercent <= config.maxSpreadPercent -> 4.0
            else -> 0.0
        }
        score += spreadScore
        
        // Open interest quality (8 points)
        val oiScore = when {
            contract.openInterest >= 5000 -> 8.0
            contract.openInterest >= 2000 -> 6.0
            contract.openInterest >= 1000 -> 4.0
            contract.openInterest >= config.minOpenInterest -> 2.0
            else -> 0.0
        }
        score += oiScore
        
        // Volume quality (4 points)
        val volumeScore = when {
            contract.volume >= 1000 -> 4.0
            contract.volume >= 500 -> 3.0
            contract.volume >= 200 -> 2.0
            contract.volume >= config.minContractVolume -> 1.0
            else -> 0.0
        }
        score += volumeScore
        
        // Weekly availability (3 points)
        val weeklyScore = if (hasWeeklyExpiration(dte, config)) 3.0 else 0.0
        score += weeklyScore
        
        return score
    }
    
    /**
     * Filter contracts by liquidity requirements
     */
    fun filterByLiquidity(
        contracts: List<OptionContract>,
        underlying: Underlying,
        currentTime: kotlinx.datetime.Instant,
        config: StrategyConfig
    ): List<OptionContract> {
        return contracts.filter { contract ->
            val dte = DteSelector.calculateDTE(currentTime, contract.expiration)
            passesLiquidityRequirements(contract, underlying, dte, config)
        }
    }
}