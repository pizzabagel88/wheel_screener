package com.wheelscreener.domain.repository

import com.wheelscreener.data.remote.MarketDataProvider
import com.wheelscreener.domain.model.OptionChain
import com.wheelscreener.domain.model.Underlying
import kotlinx.datetime.Instant

/**
 * Repository interface for market data
 * Abstracts the data provider implementation
 */
interface MarketDataRepository {
    
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
    ): Result<List<com.wheelscreener.data.remote.HistoricalBar>>
    
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
     * Get upcoming events for a symbol
     */
    suspend fun getUpcomingEvents(symbol: String): Result<List<com.wheelscreener.data.remote.CorporateEvent>>
    
    /**
     * Get market calendar for date range
     */
    suspend fun getMarketCalendar(
        startDate: Instant,
        endDate: Instant
    ): Result<com.wheelscreener.data.remote.MarketCalendar>
    
    /**
     * Check if market data provider is available
     */
    suspend fun isProviderAvailable(): Boolean
    
    /**
     * Get provider name
     */
    fun getProviderName(): String
}