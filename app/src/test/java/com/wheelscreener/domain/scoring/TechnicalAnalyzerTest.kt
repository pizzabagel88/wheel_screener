package com.wheelscreener.domain.scoring

import com.wheelscreener.domain.model.StrategyConfig
import com.wheelscreener.domain.model.Underlying
import kotlinx.datetime.Clock
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TechnicalAnalyzerTest {
    
    private lateinit var config: StrategyConfig
    private lateinit var sampleUnderlying: Underlying
    
    @Before
    fun setup() {
        config = StrategyConfig.default()
        val now = Clock.System.now()
        
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
    fun `test above 200 SMA check`() {
        assertTrue(TechnicalAnalyzer.isAbove200SMA(sampleUnderlying))
        
        val belowSMAUnderlying = sampleUnderlying.copy(
            price = 85.0,
            sma200 = 90.0
        )
        assertFalse(TechnicalAnalyzer.isAbove200SMA(belowSMAUnderlying))
    }
    
    @Test
    fun `test within 200 SMA tolerance`() {
        assertTrue(TechnicalAnalyzer.isWithin200SMATolerance(sampleUnderlying, 12.0))
        
        val farUnderlying = sampleUnderlying.copy(
            price = 80.0,
            sma200 = 90.0
        )
        assertFalse(TechnicalAnalyzer.isWithin200SMATolerance(farUnderlying, 5.0))
    }
    
    @Test
    fun `test 20 above 50 SMA check`() {
        assertTrue(TechnicalAnalyzer.is20Above50SMA(sampleUnderlying))
        
        val downtrendUnderlying = sampleUnderlying.copy(
            sma20 = 92.0,
            sma50 = 95.0
        )
        assertFalse(TechnicalAnalyzer.is20Above50SMA(downtrendUnderlying))
    }
    
    @Test
    fun `test downtrend detection`() {
        assertFalse(TechnicalAnalyzer.isDowntrend(sampleUnderlying))
        
        val downtrendUnderlying = sampleUnderlying.copy(
            sma20 = 92.0,
            sma50 = 95.0
        )
        assertTrue(TechnicalAnalyzer.isDowntrend(downtrendUnderlying))
    }
    
    @Test
    fun `test distance from 200 SMA calculation`() {
        val distance = TechnicalAnalyzer.calculateDistanceFrom200SMA(sampleUnderlying)
        assertNotNull(distance)
        assertEquals(11.11, distance!!, 0.1) // (100-90)/90 * 100
    }
    
    @Test
    fun `test trend flags generation`() {
        val flags = TechnicalAnalyzer.getTrendFlags(sampleUnderlying, config)
        // Should have no flags for healthy setup
        assertTrue(flags.isEmpty())
        
        val downtrendUnderlying = sampleUnderlying.copy(
            price = 85.0,
            sma20 = 92.0,
            sma50 = 95.0,
            sma200 = 90.0
        )
        val downtrendFlags = TechnicalAnalyzer.getTrendFlags(downtrendUnderlying, config)
        assertTrue(downtrendFlags.contains(com.wheelscreener.domain.model.CandidateFlag.BELOW_200_SMA))
        assertTrue(downtrendFlags.contains(com.wheelscreener.domain.model.CandidateFlag.DOWNTREND_RISK))
    }
    
    @Test
    fun `test technical scoring`() {
        val score = TechnicalAnalyzer.scoreTechnical(sampleUnderlying, config)
        assertTrue(score >= 0)
        assertTrue(score <= 10)
    }
    
    @Test
    fun `test technical scoring with uptrend`() {
        val uptrendScore = TechnicalAnalyzer.scoreTechnical(sampleUnderlying, config)
        assertTrue(uptrendScore > 5) // Should get good score for uptrend
    }
    
    @Test
    fun `test technical scoring with downtrend`() {
        val downtrendUnderlying = sampleUnderlying.copy(
            price = 85.0,
            sma20 = 92.0,
            sma50 = 95.0,
            sma200 = 90.0,
            changePercent = -3.0
        )
        val downtrendScore = TechnicalAnalyzer.scoreTechnical(downtrendUnderlying, config)
        assertTrue(downtrendScore < 5) // Should be penalized for downtrend
    }
    
    @Test
    fun `test relative strength calculation`() {
        val relativeStrength = TechnicalAnalyzer.calculateRelativeStrength(2.0, 0.0)
        assertEquals(2.0, relativeStrength, 0.01)
        
        val negativeRS = TechnicalAnalyzer.calculateRelativeStrength(-1.0, -3.0)
        assertEquals(2.0, negativeRS, 0.01) // Outperforming market
    }
    
    @Test
    fun `test improving relative strength check`() {
        val configWithRS = config.copy(requireImprovingRelativeStrength = true)
        assertTrue(TechnicalAnalyzer.isImprovingRelativeStrength(2.0, configWithRS))
        assertFalse(TechnicalAnalyzer.isImprovingRelativeStrength(-1.0, configWithRS))
    }
    
    @Test
    fun `test technical assessment`() {
        val assessment = TechnicalAnalyzer.getTechnicalAssessment(sampleUnderlying, config)
        assertEquals(Trend.UPTREND, assessment.trend)
        assertTrue(assessment.score > 0)
        assertTrue(assessment.flags.isEmpty())
    }
    
    @Test
    fun `test null SMA values`() {
        val noSMAUnderlying = sampleUnderlying.copy(
            sma20 = null,
            sma50 = null,
            sma200 = null
        )
        assertFalse(TechnicalAnalyzer.isAbove200SMA(noSMAUnderlying))
        assertFalse(TechnicalAnalyzer.is20Above50SMA(noSMAUnderlying))
        assertNull(TechnicalAnalyzer.calculateDistanceFrom200SMA(noSMAUnderlying))
    }
}
