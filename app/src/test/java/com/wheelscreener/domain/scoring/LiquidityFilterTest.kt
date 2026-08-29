package com.wheelscreener.domain.scoring

import com.wheelscreener.domain.model.*
import kotlinx.datetime.Clock
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class LiquidityFilterTest {
    
    private lateinit var config: StrategyConfig
    private lateinit var sampleContract: OptionContract
    private lateinit var sampleUnderlying: Underlying
    
    @Before
    fun setup() {
        config = StrategyConfig.default()
        
        val now = Clock.System.now()
        sampleContract = OptionContract(
            symbol = "TEST_20250101_100C",
            underlyingSymbol = "TEST",
            contractType = ContractType.CALL,
            strike = 100.0,
            expiration = now.plus(kotlinx.datetime.DateTimePeriod(days = 10)),
            bid = 2.50,
            ask = 2.55,
            last = 2.52,
            volume = 500,
            openInterest = 1000,
            delta = 0.25,
            gamma = 0.05,
            theta = -0.02,
            vega = 0.15,
            iv = 0.30,
            ivRank = 45.0,
            ivPercentile = 50.0,
            lastUpdate = now
        )
        
        sampleUnderlying = Underlying(
            symbol = "TEST",
            price = 100.0,
            change = 1.0,
            changePercent = 1.0,
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
    
    @Test
    fun `test minimum open interest check`() {
        assertTrue(LiquidityFilter.hasMinimumOpenInterest(sampleContract, config))
        
        val lowOIContract = sampleContract.copy(openInterest = 200)
        assertFalse(LiquidityFilter.hasMinimumOpenInterest(lowOIContract, config))
    }
    
    @Test
    fun `test minimum volume check`() {
        assertTrue(LiquidityFilter.hasMinimumVolume(sampleContract, config))
        
        val lowVolumeContract = sampleContract.copy(volume = 50)
        assertFalse(LiquidityFilter.hasMinimumVolume(lowVolumeContract, config))
    }
    
    @Test
    fun `test spread percentage calculation`() {
        val spreadPercent = LiquidityFilter.calculateSpreadPercent(2.50, 2.55)
        assertEquals(2.0, spreadPercent, 0.1)
    }
    
    @Test
    fun `test acceptable spread check`() {
        assertTrue(LiquidityFilter.hasAcceptableSpread(sampleContract, config))
        
        val wideSpreadContract = sampleContract.copy(bid = 2.50, ask = 2.75)
        assertFalse(LiquidityFilter.hasAcceptableSpread(wideSpreadContract, config))
    }
    
    @Test
    fun `test sufficient dollar volume check`() {
        assertTrue(LiquidityFilter.hasSufficientDollarVolume(sampleUnderlying, config))
        
        val lowVolumeUnderlying = sampleUnderlying.copy(
            averageVolume20d = 1_000_000L,
            price = 10.0
        )
        assertFalse(LiquidityFilter.hasSufficientDollarVolume(lowVolumeUnderlying, config))
    }
    
    @Test
    fun `test weekly expiration check`() {
        assertTrue(LiquidityFilter.hasWeeklyExpiration(5, config))
        assertFalse(LiquidityFilter.hasWeeklyExpiration(14, config))
        
        val configNoWeekly = config.copy(requireWeeklyExpiration = false)
        assertTrue(LiquidityFilter.hasWeeklyExpiration(14, configNoWeekly))
    }
    
    @Test
    fun `test comprehensive liquidity check`() {
        val flags = LiquidityFilter.checkLiquidity(
            sampleContract, sampleUnderlying, 5, config
        )
        assertTrue(flags.isEmpty())
        
        val lowOIContract = sampleContract.copy(openInterest = 200)
        val flagsWithLowOI = LiquidityFilter.checkLiquidity(
            lowOIContract, sampleUnderlying, 10, config
        )
        assertTrue(flagsWithLowOI.contains(CandidateFlag.LOW_OPEN_INTEREST))
    }
    
    @Test
    fun `test liquidity scoring`() {
        val score = LiquidityFilter.scoreLiquidity(
            sampleContract, sampleUnderlying, 10, config
        )
        assertTrue(score > 0)
        assertTrue(score <= 25)
    }
    
    @Test
    fun `test liquidity scoring with wide spread`() {
        val wideSpreadContract = sampleContract.copy(bid = 2.50, ask = 2.75)
        val score = LiquidityFilter.scoreLiquidity(
            wideSpreadContract, sampleUnderlying, 10, config
        )
        assertTrue(score < 25) // Should be penalized for wide spread
    }
    
    @Test
    fun `test liquidity scoring with high OI`() {
        val highOIContract = sampleContract.copy(openInterest = 10000)
        val score = LiquidityFilter.scoreLiquidity(
            highOIContract, sampleUnderlying, 10, config
        )
        val normalScore = LiquidityFilter.scoreLiquidity(
            sampleContract, sampleUnderlying, 10, config
        )
        assertTrue(score >= normalScore) // Higher OI should give better score
    }
}
