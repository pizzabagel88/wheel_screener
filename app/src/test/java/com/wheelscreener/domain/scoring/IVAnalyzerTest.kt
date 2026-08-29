package com.wheelscreener.domain.scoring

import com.wheelscreener.domain.model.ContractType
import com.wheelscreener.domain.model.OptionContract
import com.wheelscreener.domain.model.StrategyConfig
import kotlinx.datetime.Clock
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class IVAnalyzerTest {
    
    private lateinit var config: StrategyConfig
    private lateinit var sampleContract: OptionContract
    
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
    }
    
    @Test
    fun `test IV rank in range check`() {
        assertTrue(IVAnalyzer.isIVRankInRange(45.0, config))
        assertTrue(IVAnalyzer.isIVRankInRange(25.0, config))
        assertTrue(IVAnalyzer.isIVRankInRange(65.0, config))
        assertFalse(IVAnalyzer.isIVRankInRange(20.0, config))
        assertFalse(IVAnalyzer.isIVRankInRange(70.0, config))
    }
    
    @Test
    fun `test IV rank in preferred range check`() {
        assertTrue(IVAnalyzer.isIVRankInPreferredRange(45.0, config))
        assertTrue(IVAnalyzer.isIVRankInPreferredRange(40.0, config))
        assertTrue(IVAnalyzer.isIVRankInPreferredRange(55.0, config))
        assertFalse(IVAnalyzer.isIVRankInPreferredRange(35.0, config))
        assertFalse(IVAnalyzer.isIVRankInPreferredRange(60.0, config))
    }
    
    @Test
    fun `test high IV rank detection`() {
        assertFalse(IVAnalyzer.isHighIVRank(45.0))
        assertFalse(IVAnalyzer.isHighIVRank(65.0))
        assertTrue(IVAnalyzer.isHighIVRank(70.0))
        assertTrue(IVAnalyzer.isHighIVRank(80.0))
    }
    
    @Test
    fun `test low IV rank detection`() {
        assertTrue(IVAnalyzer.isLowIVRank(20.0))
        assertTrue(IVAnalyzer.isLowIVRank(15.0))
        assertFalse(IVAnalyzer.isLowIVRank(25.0))
        assertFalse(IVAnalyzer.isLowIVRank(30.0))
    }
    
    @Test
    fun `test IV score calculation`() {
        val perfectScore = IVAnalyzer.calculateIVScore(45.0, config)
        val acceptableScore = IVAnalyzer.calculateIVScore(30.0, config)
        val unacceptableScore = IVAnalyzer.calculateIVScore(20.0, config)
        
        assertEquals(20.0, perfectScore, 0.1) // Max score for preferred range
        assertTrue(acceptableScore > 0)
        assertTrue(acceptableScore < 20.0)
        assertEquals(5.0, unacceptableScore, 0.1) // Minimum score for too low
    }
    
    @Test
    fun `test IV score with high IV rank`() {
        val configNoHigh = config.copy(allowHighIvRankWithoutEvent = false)
        val highIVScore = IVAnalyzer.calculateIVScore(70.0, configNoHigh)
        assertEquals(0.0, highIVScore, 0.1) // Should be excluded
        
        val configAllowHigh = config.copy(allowHighIvRankWithoutEvent = true)
        val highIVScoreAllowed = IVAnalyzer.calculateIVScore(70.0, configAllowHigh)
        assertEquals(10.0, highIVScoreAllowed, 0.1) // Should get some score
    }
    
    @Test
    fun `test IV score with null IV rank`() {
        val nullScore = IVAnalyzer.calculateIVScore(null, config)
        assertEquals(0.0, nullScore, 0.1)
    }
    
    @Test
    fun `test IV flags generation`() {
        val flags = IVAnalyzer.getIVFlags(sampleContract, config)
        assertTrue(flags.isEmpty()) // Should have no flags for normal IV
        
        val highIVContract = sampleContract.copy(ivRank = 70.0)
        val highIVFlags = IVAnalyzer.getIVFlags(highIVContract, config)
        assertTrue(highIVFlags.contains(com.wheelscreener.domain.model.CandidateFlag.HIGH_IV_RANK))
    }
    
    @Test
    fun `test IV flags with missing data`() {
        val noIVContract = sampleContract.copy(ivRank = null, iv = null)
        val flags = IVAnalyzer.getIVFlags(noIVContract, config)
        assertTrue(flags.contains(com.wheelscreener.domain.model.CandidateFlag.IV_DATA_MISSING))
    }
    
    @Test
    fun `test IV percentile score calculation`() {
        val perfectScore = IVAnalyzer.calculateIVPercentileScore(50.0)
        assertEquals(1.0, perfectScore, 0.01)
        
        val goodScore = IVAnalyzer.calculateIVPercentileScore(45.0)
        assertTrue(goodScore > 0.8)
        
        val poorScore = IVAnalyzer.calculateIVPercentileScore(5.0)
        assertTrue(poorScore < 0.4)
    }
    
    @Test
    fun `test sufficient IV data check`() {
        assertTrue(IVAnalyzer.hasSufficientIVData(sampleContract))
        
        val noIVContract = sampleContract.copy(iv = null, ivRank = null)
        assertFalse(IVAnalyzer.hasSufficientIVData(noIVContract))
    }
    
    @Test
    fun `test IV opportunity scoring`() {
        val score = IVAnalyzer.scoreIVOpportunity(sampleContract, config)
        assertTrue(score >= 0)
        assertTrue(score <= 20)
    }
    
    @Test
    fun `test IV opportunity scoring with missing data`() {
        val noIVContract = sampleContract.copy(iv = null, ivRank = null)
        val score = IVAnalyzer.scoreIVOpportunity(noIVContract, config)
        assertEquals(0.0, score, 0.1)
    }
    
    @Test
    fun `test IV assessment`() {
        val assessment = IVAnalyzer.getIVAssessment(sampleContract, config)
        assertEquals(IVRegime.OPTIMAL, assessment.regime)
        assertTrue(assessment.score > 0)
        assertTrue(assessment.flags.isEmpty())
    }
    
    @Test
    fun `test IV assessment with high IV`() {
        val highIVContract = sampleContract.copy(ivRank = 70.0)
        val assessment = IVAnalyzer.getIVAssessment(highIVContract, config)
        assertEquals(IVRegime.HIGH, assessment.regime)
        assertTrue(assessment.flags.contains(com.wheelscreener.domain.model.CandidateFlag.HIGH_IV_RANK))
    }
    
    @Test
    fun `test IV assessment with missing data`() {
        val noIVContract = sampleContract.copy(iv = null, ivRank = null)
        val assessment = IVAnalyzer.getIVAssessment(noIVContract, config)
        assertEquals(IVRegime.UNKNOWN, assessment.regime)
        assertTrue(assessment.flags.contains(com.wheelscreener.domain.model.CandidateFlag.IV_DATA_MISSING))
    }
    
    @Test
    fun `test IV assessment with optimal IV`() {
        val optimalContract = sampleContract.copy(ivRank = 45.0)
        val configOptimal = config.copy(ivRankTarget = 40, ivRankTargetMax = 55)
        val assessment = IVAnalyzer.getIVAssessment(optimalContract, configOptimal)
        assertEquals(IVRegime.OPTIMAL, assessment.regime)
        assertEquals(20.0, assessment.score, 0.1) // Max score
    }
}
