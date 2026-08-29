package com.wheelscreener.domain.model

import com.wheelscreener.data.remote.EventType
import org.junit.Assert.*
import org.junit.Test

class ModelTest {
    
    @Test
    fun `test score components calculation`() {
        val components = ScoreComponents.fromComponents(
            liquidityScore = 20.0,
            ivScore = 15.0,
            pullbackScore = 18.0,
            fundamentalScore = 12.0,
            technicalScore = 8.0,
            diversificationScore = 7.0
        )
        
        assertEquals(20.0, components.liquidityScore, 0.01)
        assertEquals(15.0, components.ivScore, 0.01)
        assertEquals(18.0, components.pullbackScore, 0.01)
        assertEquals(12.0, components.fundamentalScore, 0.01)
        assertEquals(8.0, components.technicalScore, 0.01)
        assertEquals(7.0, components.diversificationScore, 0.01)
        assertEquals(80.0, components.compositeScore, 0.01)
    }
    
    @Test
    fun `test strategy config defaults`() {
        val config = StrategyConfig.default()
        
        assertEquals(1, config.version)
        assertEquals(7, config.dteMin)
        assertEquals(14, config.dteMax)
        assertEquals(0.20, config.cspDeltaMinCore, 0.01)
        assertEquals(0.30, config.cspDeltaMaxCore, 0.01)
        assertEquals(50_000_000_000L, config.marketCapMin)
        assertEquals(100_000_000_000L, config.marketCapPreferred)
        assertEquals(500, config.minOpenInterest)
        assertEquals(100, config.minContractVolume)
        assertEquals(25, config.ivRankMin)
        assertEquals(65, config.ivRankMax)
    }
    
    @Test
    fun `test data confidence enum values`() {
        assertEquals(3, DataConfidence.values().size)
        assertTrue(DataConfidence.values().contains(DataConfidence.HIGH))
        assertTrue(DataConfidence.values().contains(DataConfidence.MEDIUM))
        assertTrue(DataConfidence.values().contains(DataConfidence.LOW))
    }
    
    @Test
    fun `test contract type enum values`() {
        assertEquals(2, ContractType.values().size)
        assertTrue(ContractType.values().contains(ContractType.CALL))
        assertTrue(ContractType.values().contains(ContractType.PUT))
    }
    
    @Test
    fun `test assignment risk enum values`() {
        assertEquals(3, AssignmentRisk.values().size)
        assertTrue(AssignmentRisk.values().contains(AssignmentRisk.LOWER))
        assertTrue(AssignmentRisk.values().contains(AssignmentRisk.MODERATE))
        assertTrue(AssignmentRisk.values().contains(AssignmentRisk.ELEVATED))
    }
    
    @Test
    fun `test candidate flag enum covers all categories`() {
        val flags = CandidateFlag.values()
        
        // Trend flags
        assertTrue(flags.contains(CandidateFlag.BELOW_200_SMA))
        assertTrue(flags.contains(CandidateFlag.DOWNTREND_RISK))
        assertTrue(flags.contains(CandidateFlag.POSSIBLE_TREND_BREAKDOWN))
        
        // Pullback flags
        assertTrue(flags.contains(CandidateFlag.SHARP_RECENT_BREAKDOWN))
        assertTrue(flags.contains(CandidateFlag.BROAD_MARKET_LED_PULLBACK))
        assertTrue(flags.contains(CandidateFlag.SYMBOL_SPECIFIC_PULLBACK))
        
        // Liquidity flags
        assertTrue(flags.contains(CandidateFlag.LOW_OPEN_INTEREST))
        assertTrue(flags.contains(CandidateFlag.LOW_VOLUME))
        assertTrue(flags.contains(CandidateFlag.WIDE_SPREAD))
        assertTrue(flags.contains(CandidateFlag.NO_WEEKLY_EXPIRATION))
        
        // Event flags
        assertTrue(flags.contains(CandidateFlag.EARNINGS_IN_EXPIRATION))
        assertTrue(flags.contains(CandidateFlag.MAJOR_BINARY_EVENT))
        assertTrue(flags.contains(CandidateFlag.DIVIDEND_ASSIGNMENT_RISK))
        
        // Fundamental flags
        assertTrue(flags.contains(CandidateFlag.NEGATIVE_FREE_CASH_FLOW))
        assertTrue(flags.contains(CandidateFlag.HIGH_NET_DEBT))
        assertTrue(flags.contains(CandidateFlag.BELOW_MARKET_CAP_THRESHOLD))
        
        // IV flags
        assertTrue(flags.contains(CandidateFlag.HIGH_IV_RANK))
        assertTrue(flags.contains(CandidateFlag.IV_DATA_MISSING))
        
        // Data quality flags
        assertTrue(flags.contains(CandidateFlag.STALE_QUOTE))
        assertTrue(flags.contains(CandidateFlag.EVENT_DATA_MISSING))
        assertTrue(flags.contains(CandidateFlag.FUNDAMENTAL_DATA_MISSING))
        assertTrue(flags.contains(CandidateFlag.UNRELIABLE_DATA))
    }
    
    @Test
    fun `test event type enum values`() {
        val eventTypes = EventType.values()
        
        assertTrue(eventTypes.contains(EventType.EARNINGS))
        assertTrue(eventTypes.contains(EventType.INVESTOR_DAY))
        assertTrue(eventTypes.contains(EventType.FDA_DECISION))
        assertTrue(eventTypes.contains(EventType.COURT_RULING))
        assertTrue(eventTypes.contains(EventType.DIVIDEND))
        assertTrue(eventTypes.contains(EventType.OTHER))
    }
}
