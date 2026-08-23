package com.wheelscreener.domain.scoring

import com.wheelscreener.data.remote.CorporateEvent
import com.wheelscreener.data.remote.EventType
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimePeriod
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class EventExclusionTest {
    
    private lateinit var currentTime: kotlinx.datetime.Instant
    
    @Before
    fun setup() {
        currentTime = Clock.System.now()
    }
    
    @Test
    fun `test earnings in expiration detection`() {
        val expiration = currentTime.plus(DateTimePeriod(days = 10))
        val earningsDate = currentTime.plus(DateTimePeriod(days = 11))
        
        val events = listOf(
            CorporateEvent("TEST", EventType.EARNINGS, earningsDate, "AMC", "Q4 Earnings")
        )
        
        assertTrue(EventExclusion.hasEarningsInExpiration(expiration, events))
    }
    
    @Test
    fun `test earnings outside expiration window`() {
        val expiration = currentTime.plus(DateTimePeriod(days = 10))
        val earningsDate = currentTime.plus(DateTimePeriod(days = 20))
        
        val events = listOf(
            CorporateEvent("TEST", EventType.EARNINGS, earningsDate, "AMC", "Q4 Earnings")
        )
        
        assertFalse(EventExclusion.hasEarningsInExpiration(expiration, events))
    }
    
    @Test
    fun `test binary event detection`() {
        val expiration = currentTime.plus(DateTimePeriod(days = 10))
        val fdaDate = currentTime.plus(DateTimePeriod(days = 12))
        
        val events = listOf(
            CorporateEvent("TEST", EventType.FDA_DECISION, fdaDate, null, "FDA Decision")
        )
        
        assertTrue(EventExclusion.hasBinaryEventInExpiration(expiration, events))
    }
    
    @Test
    fun `test dividend assignment risk`() {
        val expiration = currentTime.plus(DateTimePeriod(days = 10))
        val exDivDate = currentTime.plus(DateTimePeriod(days = 12))
        
        val events = listOf(
            CorporateEvent("TEST", EventType.DIVIDEND, exDivDate, null, "Quarterly Dividend")
        )
        
        // High risk with low extrinsic value
        assertTrue(EventExclusion.hasDividendAssignmentRisk(expiration, events, 0.30))
        
        // Low risk with high extrinsic value
        assertFalse(EventExclusion.hasDividendAssignmentRisk(expiration, events, 1.50))
    }
    
    @Test
    fun `test event exclusion reason`() {
        val expiration = currentTime.plus(DateTimePeriod(days = 10))
        val earningsDate = currentTime.plus(DateTimePeriod(days = 11))
        
        val events = listOf(
            CorporateEvent("TEST", EventType.EARNINGS, earningsDate, "AMC", "Q4 Earnings")
        )
        
        val reason = EventExclusion.getEventExclusionReason(expiration, events)
        assertNotNull(reason)
        assertTrue(reason!!.contains("Earnings"))
    }
    
    @Test
    fun `test no exclusion reason when safe`() {
        val expiration = currentTime.plus(DateTimePeriod(days = 10))
        val earningsDate = currentTime.plus(DateTimePeriod(days = 20))
        
        val events = listOf(
            CorporateEvent("TEST", EventType.EARNINGS, earningsDate, "AMC", "Q4 Earnings")
        )
        
        val reason = EventExclusion.getEventExclusionReason(expiration, events)
        assertNull(reason)
    }
    
    @Test
    fun `test event flags generation`() {
        val expiration = currentTime.plus(DateTimePeriod(days = 10))
        val earningsDate = currentTime.plus(DateTimePeriod(days = 11))
        
        val events = listOf(
            CorporateEvent("TEST", EventType.EARNINGS, earningsDate, "AMC", "Q4 Earnings")
        )
        
        val flags = EventExclusion.getEventFlags(expiration, events)
        assertTrue(flags.contains(com.wheelscreener.domain.model.CandidateFlag.EARNINGS_IN_EXPIRATION))
    }
    
    @Test
    fun `test sufficient event data check`() {
        val eventsWithEarnings = listOf(
            CorporateEvent("TEST", EventType.EARNINGS, currentTime, "AMC", "Q4 Earnings")
        )
        assertTrue(EventExclusion.hasSufficientEventData(eventsWithEarnings))
        
        val eventsWithoutEarnings = listOf(
            CorporateEvent("TEST", EventType.DIVIDEND, currentTime, null, "Dividend")
        )
        assertFalse(EventExclusion.hasSufficientEventData(eventsWithoutEarnings))
    }
    
    @Test
    fun `test filter expirations by events`() {
        val exp1 = currentTime.plus(DateTimePeriod(days = 10))
        val exp2 = currentTime.plus(DateTimePeriod(days = 20))
        val exp3 = currentTime.plus(DateTimePeriod(days = 30))
        
        val earningsDate = currentTime.plus(DateTimePeriod(days = 11))
        val events = listOf(
            CorporateEvent("TEST", EventType.EARNINGS, earningsDate, "AMC", "Q4 Earnings")
        )
        
        val filtered = EventExclusion.filterExpirationsByEvents(listOf(exp1, exp2, exp3), events)
        
        // exp1 should be filtered out (near earnings)
        assertFalse(filtered.contains(exp1))
        assertTrue(filtered.contains(exp2))
        assertTrue(filtered.contains(exp3))
    }
    
    @Test
    fun `test event safety scoring`() {
        val expiration = currentTime.plus(DateTimePeriod(days = 10))
        val earningsDate = currentTime.plus(DateTimePeriod(days = 20))
        
        val events = listOf(
            CorporateEvent("TEST", EventType.EARNINGS, earningsDate, "AMC", "Q4 Earnings")
        )
        
        val score = EventExclusion.scoreEventSafety(expiration, events)
        assertTrue(score > 0)
        assertTrue(score <= 1.0)
    }
    
    @Test
    fun `test event safety scoring with exclusion`() {
        val expiration = currentTime.plus(DateTimePeriod(days = 10))
        val earningsDate = currentTime.plus(DateTimePeriod(days = 11))
        
        val events = listOf(
            CorporateEvent("TEST", EventType.EARNINGS, earningsDate, "AMC", "Q4 Earnings")
        )
        
        val score = EventExclusion.scoreEventSafety(expiration, events)
        assertEquals(0.0, score, 0.01)
    }
}