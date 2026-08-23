package com.wheelscreener.domain.scoring

import com.wheelscreener.data.remote.CorporateEvent
import com.wheelscreener.data.remote.EventType
import com.wheelscreener.domain.model.CandidateFlag
import kotlinx.datetime.Instant

/**
 * Pure functions for event-based exclusion
 * All functions are deterministic and testable
 */
object EventExclusion {
    
    /**
     * Check if expiration date includes an earnings date
     */
    fun hasEarningsInExpiration(
        expiration: Instant,
        events: List<CorporateEvent>,
        bufferDays: Int = 3
    ): Boolean {
        return events.any { event ->
            event.eventType == EventType.EARNINGS &&
            isDateNearExpiration(event.eventDate, expiration, bufferDays)
        }
    }
    
    /**
     * Check if expiration date includes any major binary event
     */
    fun hasBinaryEventInExpiration(
        expiration: Instant,
        events: List<CorporateEvent>,
        bufferDays: Int = 3
    ): Boolean {
        val binaryEventTypes = setOf(
            EventType.EARNINGS,
            EventType.FDA_DECISION,
            EventType.COURT_RULING,
            EventType.INVESTOR_DAY
        )
        
        return events.any { event ->
            event.eventType in binaryEventTypes &&
            isDateNearExpiration(event.eventDate, expiration, bufferDays)
        }
    }
    
    /**
     * Check if a date is near expiration (within buffer days)
     */
    private fun isDateNearExpiration(
        eventDate: Instant,
        expiration: Instant,
        bufferDays: Int
    ): Boolean {
        val diff = kotlin.math.abs((expiration - eventDate).inWholeDays)
        return diff <= bufferDays
    }
    
    /**
     * Check for dividend assignment risk
     * Risk is elevated when ex-dividend date is near expiration
     * and option has low extrinsic value
     */
    fun hasDividendAssignmentRisk(
        expiration: Instant,
        events: List<CorporateEvent>,
        optionExtrinsicValue: Double,
        bufferDays: Int = 5
    ): Boolean {
        val dividendEvent = events.find { it.eventType == EventType.DIVIDEND }
            ?: return false
        
        if (!isDateNearExpiration(dividendEvent.eventDate, expiration, bufferDays)) {
            return false
        }
        
        // High risk if extrinsic value is low (< $0.50)
        return optionExtrinsicValue < 0.50
    }
    
    /**
     * Get exclusion reason for events
     */
    fun getEventExclusionReason(
        expiration: Instant,
        events: List<CorporateEvent>,
        optionExtrinsicValue: Double = 1.0
    ): String? {
        if (hasEarningsInExpiration(expiration, events)) {
            return "Earnings date within expiration window"
        }
        
        if (hasBinaryEventInExpiration(expiration, events)) {
            return "Major binary event within expiration window"
        }
        
        if (hasDividendAssignmentRisk(expiration, events, optionExtrinsicValue)) {
            return "Dividend assignment risk (ex-dividend near expiration)"
        }
        
        return null
    }
    
    /**
     * Get event-related flags
     */
    fun getEventFlags(
        expiration: Instant,
        events: List<CorporateEvent>,
        optionExtrinsicValue: Double = 1.0
    ): List<CandidateFlag> {
        val flags = mutableListOf<CandidateFlag>()
        
        if (hasEarningsInExpiration(expiration, events)) {
            flags.add(CandidateFlag.EARNINGS_IN_EXPIRATION)
        }
        
        if (hasBinaryEventInExpiration(expiration, events)) {
            flags.add(CandidateFlag.MAJOR_BINARY_EVENT)
        }
        
        if (hasDividendAssignmentRisk(expiration, events, optionExtrinsicValue)) {
            flags.add(CandidateFlag.DIVIDEND_ASSIGNMENT_RISK)
        }
        
        return flags
    }
    
    /**
     * Check if events data is sufficient
     */
    fun hasSufficientEventData(events: List<CorporateEvent>): Boolean {
        // Should have at least earnings data
        return events.any { it.eventType == EventType.EARNINGS }
    }
    
    /**
     * Filter expirations by event exclusion
     */
    fun filterExpirationsByEvents(
        expirations: List<Instant>,
        events: List<CorporateEvent>,
        optionExtrinsicValue: Double = 1.0
    ): List<Instant> {
        return expirations.filter { expiration ->
            getEventExclusionReason(expiration, events, optionExtrinsicValue) == null
        }
    }
    
    /**
     * Score event safety (0-1 scale)
     * Higher score = safer (no upcoming events)
     */
    fun scoreEventSafety(
        expiration: Instant,
        events: List<CorporateEvent>,
        optionExtrinsicValue: Double = 1.0
    ): Double {
        if (getEventExclusionReason(expiration, events, optionExtrinsicValue) != null) {
            return 0.0
        }
        
        // Check if any events are coming up (but not in exclusion window)
        val upcomingEvents = events.count { event ->
            val daysToEvent = (event.eventDate - expiration).inWholeDays
            daysToEvent in 4..30 // 4-30 days after expiration
        }
        
        // Deduct points for upcoming events
        return when (upcomingEvents) {
            0 -> 1.0
            1 -> 0.8
            2 -> 0.6
            else -> 0.4
        }
    }
}