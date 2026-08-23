package com.wheelscreener.domain.scoring

import com.wheelscreener.domain.model.StrategyConfig
import com.wheelscreener.domain.model.Underlying
import kotlinx.datetime.Clock
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PullbackAnalyzerTest {
    
    private lateinit var config: StrategyConfig
    private lateinit var sampleUnderlying: Underlying
    
    @Before
    fun setup() {
        config = StrategyConfig.default()
        val now = Clock.System.now()
        
        sampleUnderlying = Underlying(
            symbol = "TEST",
            price = 100.0,
            change = -2.0,
            changePercent = -2.0,
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
    fun `test 20-day pullback calculation`() {
        val pullback = PullbackAnalyzer.calculatePullback20d(sampleUnderlying)
        assertNotNull(pullback)
        assertEquals(9.09, pullback, 0.1) // (110-100)/110 * 100
    }
    
    @Test
    fun `test 60-day pullback calculation`() {
        val pullback = PullbackAnalyzer.calculatePullback60d(sampleUnderlying)
        assertNotNull(pullback)
        assertEquals(13.04, pullback, 0.1) // (115-100)/115 * 100
    }
    
    @Test
    fun `test pullback in range check 20d`() {
        val inRangeUnderlying = sampleUnderlying.copy(
            price = 100.0,
            high20d = 110.0
        )
        assertTrue(PullbackAnalyzer.isPullbackInRange20d(
            PullbackAnalyzer.calculatePullback20d(inRangeUnderlying), config
        ))
        
        val outOfRangeUnderlying = sampleUnderlying.copy(
            price = 100.0,
            high20d = 105.0
        )
        assertFalse(PullbackAnalyzer.isPullbackInRange20d(
            PullbackAnalyzer.calculatePullback20d(outOfRangeUnderlying), config
        ))
    }
    
    @Test
    fun `test breakdown detection`() {
        val breakdownUnderlying = sampleUnderlying.copy(
            price = 85.0,
            high20d = 110.0
        )
        assertTrue(PullbackAnalyzer.isBreakdownDecline(breakdownUnderlying, config))
        
        val normalUnderlying = sampleUnderlying.copy(
            price = 100.0,
            high20d = 110.0
        )
        assertFalse(PullbackAnalyzer.isBreakdownDecline(normalUnderlying, config))
    }
    
    @Test
    fun `test pullback categorization`() {
        val idealUnderlying = sampleUnderlying.copy(
            price = 100.0,
            high20d = 110.0,
            high60d = 115.0
        )
        assertEquals(PullbackCategory.IDEAL, 
            PullbackAnalyzer.categorizePullback(idealUnderlying, config))
        
        val breakdownUnderlying = sampleUnderlying.copy(
            price = 85.0,
            high20d = 110.0
        )
        assertEquals(PullbackCategory.BREAKDOWN,
            PullbackAnalyzer.categorizePullback(breakdownUnderlying, config))
    }
    
    @Test
    fun `test pullback type determination`() {
        val symbolSpecificUnderlying = sampleUnderlying.copy(changePercent = -5.0)
        assertEquals(PullbackType.SYMBOL_SPECIFIC,
            PullbackAnalyzer.determinePullbackType(symbolSpecificUnderlying, 0.0))
        
        val marketLedUnderlying = sampleUnderlying.copy(changePercent = -1.0)
        assertEquals(PullbackType.BROAD_MARKET_LED,
            PullbackAnalyzer.determinePullbackType(marketLedUnderlying, -3.0))
    }
    
    @Test
    fun `test pullback flags generation`() {
        val flags = PullbackAnalyzer.getPullbackFlags(sampleUnderlying, config)
        // Should have flags based on the 2% decline
        assertTrue(flags.isNotEmpty())
    }
    
    @Test
    fun `test pullback scoring`() {
        val score = PullbackAnalyzer.scorePullback(sampleUnderlying, config)
        assertTrue(score >= 0)
        assertTrue(score <= 20)
    }
    
    @Test
    fun `test pullback scoring with ideal setup`() {
        val idealUnderlying = sampleUnderlying.copy(
            price = 100.0,
            high20d = 110.0,
            high60d = 115.0,
            changePercent = -8.0
        )
        val score = PullbackAnalyzer.scorePullback(idealUnderlying, config)
        assertTrue(score > 10) // Should get good score for ideal pullback
    }
    
    @Test
    fun `test pullback scoring with breakdown`() {
        val breakdownUnderlying = sampleUnderlying.copy(
            price = 85.0,
            high20d = 110.0
        )
        val score = PullbackAnalyzer.scorePullback(breakdownUnderlying, config)
        assertTrue(score < 10) // Should be penalized for breakdown
    }
    
    @Test
    fun `test null high values`() {
        val noHighUnderlying = sampleUnderlying.copy(
            high20d = null,
            high60d = null
        )
        assertNull(PullbackAnalyzer.calculatePullback20d(noHighUnderlying))
        assertNull(PullbackAnalyzer.calculatePullback60d(noHighUnderlying))
    }
}