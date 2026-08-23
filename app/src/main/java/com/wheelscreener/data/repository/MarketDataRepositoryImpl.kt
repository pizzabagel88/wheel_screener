package com.wheelscreener.data.repository

import com.wheelscreener.data.remote.MarketDataProvider
import com.wheelscreener.domain.model.OptionChain
import com.wheelscreener.domain.model.Underlying
import com.wheelscreener.domain.repository.MarketDataRepository
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of market data repository
 * Wraps the market data provider with additional business logic
 */
@Singleton
class MarketDataRepositoryImpl @Inject constructor(
    private val marketDataProvider: MarketDataProvider
) : MarketDataRepository {
    
    override suspend fun getQuote(symbol: String): Result<Underlying> {
        return marketDataProvider.getQuote(symbol)
    }
    
    override suspend fun getHistoricalBars(
        symbol: String,
        startDate: Instant,
        endDate: Instant
    ): Result<List<com.wheelscreener.data.remote.HistoricalBar>> {
        return marketDataProvider.getHistoricalBars(symbol, startDate, endDate)
    }
    
    override suspend fun getOptionChain(symbol: String): Result<OptionChain> {
        return marketDataProvider.getOptionChain(symbol)
    }
    
    override suspend fun getOptionChain(
        symbol: String,
        expiration: Instant
    ): Result<OptionChain> {
        return marketDataProvider.getOptionChain(symbol, expiration)
    }
    
    override suspend fun getUpcomingEvents(symbol: String): Result<List<com.wheelscreener.data.remote.CorporateEvent>> {
        return marketDataProvider.getUpcomingEvents(symbol)
    }
    
    override suspend fun getMarketCalendar(
        startDate: Instant,
        endDate: Instant
    ): Result<com.wheelscreener.data.remote.MarketCalendar> {
        return marketDataProvider.getMarketCalendar(startDate, endDate)
    }
    
    override suspend fun isProviderAvailable(): Boolean {
        return marketDataProvider.isAvailable()
    }
    
    override fun getProviderName(): String {
        return marketDataProvider.getProviderName()
    }
}