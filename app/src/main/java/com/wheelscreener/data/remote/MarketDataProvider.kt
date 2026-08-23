package com.wheelscreener.data.remote

import com.wheelscreener.domain.model.OptionChain
import com.wheelscreener.domain.model.Underlying
import kotlinx.datetime.Instant

/**
 * Abstract interface for market data providers
 * This allows swapping between real APIs and mock implementations
 */
interface MarketDataProvider {
    
    /**
     * Get current quote for a symbol
     */
    suspend fun getQuote(symbol: String): Result<Underlying>
    
    /**
     * Get historical price bars for a symbol
     */
    suspend fun getHistoricalBars(
        symbol: String,
        startDate: Instant,
        endDate: Instant
    ): Result<List<HistoricalBar>>
    
    /**
     * Get option chain for a symbol
     */
    suspend fun getOptionChain(symbol: String): Result<OptionChain>
    
    /**
     * Get option chain for specific expiration
     */
    suspend fun getOptionChain(
        symbol: String,
        expiration: Instant
    ): Result<OptionChain>
    
    /**
     * Get upcoming events for a symbol (earnings, etc.)
     */
    suspend fun getUpcomingEvents(symbol: String): Result<List<CorporateEvent>>
    
    /**
     * Get market calendar for date range
     */
    suspend fun getMarketCalendar(
        startDate: Instant,
        endDate: Instant
    ): Result<MarketCalendar>
    
    /**
     * Check if provider is available
     */
    suspend fun isAvailable(): Boolean
    
    /**
     * Get provider name for display
     */
    fun getProviderName(): String
}

/**
 * Historical price bar
 */
data class HistoricalBar(
    val timestamp: Instant,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long
)

/**
 * Corporate event (earnings, investor day, etc.)
 */
data class CorporateEvent(
    val symbol: String,
    val eventType: EventType,
    val eventDate: Instant,
    val eventTime: String? = null, // "AMC", "BMO", etc.
    val description: String? = null
)

enum class EventType {
    EARNINGS,
    INVESTOR_DAY,
    FDA_DECISION,
    COURT_RULING,
    DIVIDEND,
    OTHER
}

/**
 * Market calendar with trading days and holidays
 */
data class MarketCalendar(
    val tradingDays: List<Instant>,
    val holidays: List<MarketHoliday>
)

data class MarketHoliday(
    val date: Instant,
    val name: String,
    val isMarketOpen: Boolean
)