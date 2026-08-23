package com.wheelscreener.domain.scoring

import com.wheelscreener.data.remote.CorporateEvent
import com.wheelscreener.data.remote.EventType
import com.wheelscreener.domain.model.*
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimePeriod
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ScoringEngineTest {
    
    private lateinit var config: StrategyConfig
    private lateinit var sampleContract: OptionContract
    private lateinit var sampleUnderlying: Underlying
    private lateinit var sampleEvents: List<CorporateEvent>
    
    @Before
    fun setup() {
        config = StrategyConfig.default()
        val now = Clock.System.now()
        
        sampleContract = OptionContract(
            symbol = "TEST_20250101_100P",
            underlyingSymbol = "TEST",
            contractType = ContractType.PUT,
            strike = 95.0,
            expiration = now.plus(DateTimePeriod(days = 10)),
            bid = 2.50,
            ask = 2.55,
            last = 2.52,
            volume = 500,
            openInterest = 1000,
            delta = -0.25,
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
        
        sampleEvents = listOf(
            CorporateEvent("TEST", EventType.EARNINGS, 
                now.plus(DateTimePeriod(days = 25)), "AMC", "Q4 Earnings")
        )
    }
    
    @Test
    fun `test CSP candidate scoring`() {
        val result = ScoringEngine.scoreCSPCandidate(
            sampleContract, sampleUnderlying, sampleEvents, config
        )
        
        assertNotNull(result)
        assertEquals(10, result.dte)
        assertEquals(0.25, result.absoluteDelta, 0.01)
        assertTrue(result.isCore) // High market cap
        assertTrue(result.scoreComponents.compositeScore > 0)
        assertTrue(result.scoreComponents.compositeScore <= 100)
    }
    
    @Test
    fun `test CSP candidate scoring with exclusion`() {
        val exclusionEvents = listOf(
            CorporateEvent("TEST", EventType.EARNINGS,
                sampleContract.expiration, "AMC", "Q4 Earnings")
        )
        
        val result = ScoringEngine.scoreCSPCandidate(
            sampleContract, sampleUnderlying, exclusionEvents, config
        )
        
        assertNotNull(result.exclusionReason)
        assertTrue(result.exclusionReason!!.contains("Earnings"))
    }
    
    @Test
    fun `test CC candidate scoring`() {
        val callContract = sampleContract.copy(
            symbol = "TEST_20250101_105C",
            contractType = ContractType.CALL,
            strike = 105.0,
            delta = 0.25
        )
        
        val result = ScoringEngine.scoreCCCandidate(
            callContract, sampleUnderlying, sampleEvents,
            shareCount = 100,
            costBasis = 100.0,
            config
        )
        
        assertNotNull(result)
        assertEquals(10, result.dte)
        assertEquals(0.25, result.absoluteDelta, 0.01)
        assertFalse(result.isIncomeFirst) // Strike above cost basis
        assertTrue(result.returnIfCalled > 0)
        assertTrue(result.scoreComponents.compositeScore > 0)
    }
    
    @Test
    fun `test CC candidate scoring with income first`() {
        val callContract = sampleContract.copy(
            symbol = "TEST_20250101_95C",
            contractType = ContractType.CALL,
            strike = 95.0,
            delta = 0.25
        )
        
        val result = ScoringEngine.scoreCCCandidate(
            callContract, sampleUnderlying, sampleEvents,
            shareCount = 100,
            costBasis = 100.0,
            config
        )
        
        assertTrue(result.isIncomeFirst) // Strike below cost basis
    }
    
    @Test
    fun `test confidence level determination`() {
        val highConfidenceResult = ScoringEngine.scoreCSPCandidate(
            sampleContract, sampleUnderlying, sampleEvents, config
        )
        assertEquals(DataConfidence.HIGH, highConfidenceResult.confidence)
        
        val lowDataContract = sampleContract.copy(
            delta = null,
            iv = null,
            ivRank = null
        )
        val lowConfidenceResult = ScoringEngine.scoreCSPCandidate(
            lowDataContract, sampleUnderlying, sampleEvents, config
        )
        assertEquals(DataConfidence.LOW, lowConfidenceResult.confidence)
    }
    
    @Test
    fun `test score components aggregation`() {
        val result = ScoringEngine.scoreCSPCandidate(
            sampleContract, sampleUnderlying, sampleEvents, config
        )
        
        // Verify individual components are within expected ranges
        assertTrue(result.scoreComponents.liquidityScore >= 0 && result.scoreComponents.liquidityScore <= 25)
        assertTrue(result.scoreComponents.ivScore >= 0 && result.scoreComponents.ivScore <= 20)
        assertTrue(result.scoreComponents.pullbackScore >= 0 && result.scoreComponents.pullbackScore <= 20)
        assertTrue(result.scoreComponents.fundamentalScore >= 0 && result.scoreComponents.fundamentalScore <= 15)
        assertTrue(result.scoreComponents.technicalScore >= 0 && result.scoreComponents.technicalScore <= 10)
        assertTrue(result.scoreComponents.diversificationScore >= 0 && result.scoreComponents.diversificationScore <= 10)
        
        // Verify composite score equals sum of components
        val expectedComposite = result.scoreComponents.liquidityScore +
                              result.scoreComponents.ivScore +
                              result.scoreComponents.pullbackScore +
                              result.scoreComponents.fundamentalScore +
                              result.scoreComponents.technicalScore +
                              result.scoreComponents.diversificationScore
        assertEquals(expectedComposite, result.scoreComponents.compositeScore, 0.01)
    }
    
    @Test
    fun `test flag collection`() {
        val result = ScoringEngine.scoreCSPCandidate(
            sampleContract, sampleUnderlying, sampleEvents, config
        )
        
        // Should have some flags based on the setup
        assertTrue(result.flags.isNotEmpty())
        
        // Verify specific flag types are present
        val hasPullbackFlag = result.flags.any { 
            it == CandidateFlag.SYMBOL_SPECIFIC_PULLBACK || 
            it == CandidateFlag.BROAD_MARKET_LED_PULLBACK 
        }
        assertTrue(hasPullbackFlag)
    }
    
    @Test
    fun `test ranking CSP candidates`() {
        val results = listOf(
            ScoringEngine.scoreCSPCandidate(
                sampleContract.copy(ivRank = 30.0),
                sampleUnderlying, sampleEvents, config
            ),
            ScoringEngine.scoreCSPCandidate(
                sampleContract.copy(ivRank = 50.0),
                sampleUnderlying, sampleEvents, config
            ),
            ScoringEngine.scoreCSPCandidate(
                sampleContract.copy(ivRank = 70.0),
                sampleUnderlying, sampleEvents, config
            )
        )
        
        val ranked = ScoringEngine.rankCSPCandidates(results)
        
        // Should be sorted by composite score descending
        assertTrue(ranked[0].scoreComponents.compositeScore >= ranked[1].scoreComponents.compositeScore)
        assertTrue(ranked[1].scoreComponents.compositeScore >= ranked[2].scoreComponents.compositeScore)
    }
    
    @Test
    fun `test ranking excludes filtered candidates`() {
        val exclusionEvents = listOf(
            CorporateEvent("TEST", EventType.EARNINGS,
                sampleContract.expiration, "AMC", "Q4 Earnings")
        )
        
        val results = listOf(
            ScoringEngine.scoreCSPCandidate(
                sampleContract,
                sampleUnderlying, exclusionEvents, config
            ),
            ScoringEngine.scoreCSPCandidate(
                sampleContract.copy(strike = 90.0),
                sampleUnderlying, sampleEvents, config
            )
        )
        
        val ranked = ScoringEngine.rankCSPCandidates(results)
        
        // Should only include non-excluded candidates
        assertEquals(1, ranked.size)
        assertNull(ranked[0].exclusionReason)
    }
    
    @Test
    fun `test satellite symbol classification`() {
        val satelliteUnderlying = sampleUnderlying.copy(
            marketCap = 30_000_000_000L // Below preferred threshold
        )
        
        val result = ScoringEngine.scoreCSPCandidate(
            sampleContract, satelliteUnderlying, sampleEvents, config
        )
        
        assertFalse(result.isCore)
    }
    
    @Test
    fun `test high IV satellite classification`() {
        val highIVContract = sampleContract.copy(ivRank = 60.0)
        
        val result = ScoringEngine.scoreCSPCandidate(
            highIVContract, sampleUnderlying, sampleEvents, config
        )
        
        // High IV should make it satellite even with good market cap
        assertFalse(result.isCore)
    }
}