package com.wheelscreener.domain.scoring

import com.wheelscreener.domain.model.StrategyConfig
import com.wheelscreener.domain.model.Underlying
import kotlinx.datetime.Clock
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FundamentalAnalyzerTest {
    
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
    fun `test positive FCF check`() {
        assertTrue(FundamentalAnalyzer.hasPositiveFCF(sampleUnderlying))
        
        val negativeFCFUnderlying = sampleUnderlying.copy(
            freeCashFlowTTM = -1_000_000_000.0
        )
        assertFalse(FundamentalAnalyzer.hasPositiveFCF(negativeFCFUnderlying))
    }
    
    @Test
    fun `test high net debt check`() {
        assertFalse(FundamentalAnalyzer.hasHighNetDebt(sampleUnderlying))
        
        val highDebtUnderlying = sampleUnderlying.copy(
            netDebt = 60_000_000_000.0 // 60% of market cap
        )
        assertTrue(FundamentalAnalyzer.hasHighNetDebt(highDebtUnderlying))
    }
    
    @Test
    fun `test market cap minimum check`() {
        assertTrue(FundamentalAnalyzer.meetsMarketCapMinimum(sampleUnderlying, config))
        
        val smallCapUnderlying = sampleUnderlying.copy(
            marketCap = 30_000_000_000L
        )
        assertFalse(FundamentalAnalyzer.meetsMarketCapMinimum(smallCapUnderlying, config))
    }
    
    @Test
    fun `test market cap preferred check`() {
        assertTrue(FundamentalAnalyzer.meetsMarketCapPreferred(sampleUnderlying, config))
        
        val mediumCapUnderlying = sampleUnderlying.copy(
            marketCap = 75_000_000_000L
        )
        assertFalse(FundamentalAnalyzer.meetsMarketCapPreferred(mediumCapUnderlying, config))
    }
    
    @Test
    fun `test fundamental flags generation`() {
        val flags = FundamentalAnalyzer.getFundamentalFlags(sampleUnderlying, config)
        // Should have no flags for healthy fundamentals
        assertTrue(flags.isEmpty())
        
        val weakFundamentalUnderlying = sampleUnderlying.copy(
            freeCashFlowTTM = -1_000_000_000.0,
            netDebt = 60_000_000_000.0,
            marketCap = 30_000_000_000L
        )
        val weakFlags = FundamentalAnalyzer.getFundamentalFlags(weakFundamentalUnderlying, config)
        assertTrue(weakFlags.contains(com.wheelscreener.domain.model.CandidateFlag.NEGATIVE_FREE_CASH_FLOW))
        assertTrue(weakFlags.contains(com.wheelscreener.domain.model.CandidateFlag.HIGH_NET_DEBT))
        assertTrue(weakFlags.contains(com.wheelscreener.domain.model.CandidateFlag.BELOW_MARKET_CAP_THRESHOLD))
    }
    
    @Test
    fun `test fundamental scoring`() {
        val score = FundamentalAnalyzer.scoreFundamentals(sampleUnderlying, config)
        assertTrue(score >= 0)
        assertTrue(score <= 15)
    }
    
    @Test
    fun `test fundamental scoring with strong fundamentals`() {
        val strongScore = FundamentalAnalyzer.scoreFundamentals(sampleUnderlying, config)
        assertTrue(strongScore > 10) // Should get good score for strong fundamentals
    }
    
    @Test
    fun `test fundamental scoring with weak fundamentals`() {
        val weakUnderlying = sampleUnderlying.copy(
            freeCashFlowTTM = -1_000_000_000.0,
            netDebt = 60_000_000_000.0,
            marketCap = 30_000_000_000L
        )
        val weakScore = FundamentalAnalyzer.scoreFundamentals(weakUnderlying, config)
        assertTrue(weakScore < 5) // Should be penalized for weak fundamentals
    }
    
    @Test
    fun `test fundamental scoring with net cash`() {
        val netCashUnderlying = sampleUnderlying.copy(
            netDebt = -5_000_000_000.0 // Negative net debt = net cash
        )
        val netCashScore = FundamentalAnalyzer.scoreFundamentals(netCashUnderlying, config)
        val normalScore = FundamentalAnalyzer.scoreFundamentals(sampleUnderlying, config)
        assertTrue(netCashScore >= normalScore) // Net cash should improve score
    }
    
    @Test
    fun `test fundamental assessment`() {
        val assessment = FundamentalAnalyzer.getFundamentalAssessment(sampleUnderlying, config)
        assertEquals(FundamentalQuality.HIGH, assessment.quality)
        assertTrue(assessment.score > 0)
        assertTrue(assessment.flags.isEmpty())
    }
    
    @Test
    fun `test fundamental assessment with weak fundamentals`() {
        val weakUnderlying = sampleUnderlying.copy(
            freeCashFlowTTM = -1_000_000_000.0,
            netDebt = 60_000_000_000.0,
            marketCap = 30_000_000_000L
        )
        val assessment = FundamentalAnalyzer.getFundamentalAssessment(weakUnderlying, config)
        assertEquals(FundamentalQuality.POOR, assessment.quality)
        assertTrue(assessment.flags.isNotEmpty())
    }
    
    @Test
    fun `test null fundamental values`() {
        val noDataUnderlying = sampleUnderlying.copy(
            freeCashFlowTTM = null,
            netDebt = null
        )
        assertFalse(FundamentalAnalyzer.hasPositiveFCF(noDataUnderlying))
        assertFalse(FundamentalAnalyzer.hasHighNetDebt(noDataUnderlying))
        
        val score = FundamentalAnalyzer.scoreFundamentals(noDataUnderlying, config)
        assertTrue(score >= 0) // Should still return a score even with missing data
    }
}