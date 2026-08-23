package com.wheelscreener.data.remote

import com.wheelscreener.domain.model.DataConfidence
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MockMarketDataProviderTest {
    
    private lateinit var provider: MockMarketDataProvider
    
    @Before
    fun setup() {
        provider = MockMarketDataProvider()
    }
    
    @Test
    fun `test provider is always available`() = runTest {
        assertTrue(provider.isAvailable())
    }
    
    @Test
    fun `test provider name`() {
        assertEquals("Mock Data Provider", provider.getProviderName())
    }
    
    @Test
    fun `test get quote for known symbol`() = runTest {
        val result = provider.getQuote("AMZN")
        assertTrue(result.isSuccess)
        
        val quote = result.getOrNull()
        assertNotNull(quote)
        assertEquals("AMZN", quote?.symbol)
        assertTrue(quote?.price!! > 0)
        assertEquals(DataConfidence.HIGH, quote.confidence)
    }
    
    @Test
    fun `test get quote for unknown symbol`() = runTest {
        val result = provider.getQuote("UNKNOWN")
        assertTrue(result.isSuccess)
        
        val quote = result.getOrNull()
        assertNotNull(quote)
        assertEquals("UNKNOWN", quote?.symbol)
    }
    
    @Test
    fun `test get option chain`() = runTest {
        val result = provider.getOptionChain("AMZN")
        assertTrue(result.isSuccess)
        
        val chain = result.getOrNull()
        assertNotNull(chain)
        assertEquals("AMZN", chain?.symbol)
        assertTrue(chain?.contracts?.isNotEmpty() == true)
        
        // Verify we have both calls and puts
        val calls = chain?.contracts?.filter { it.contractType == ContractType.CALL }
        val puts = chain?.contracts?.filter { it.contractType == ContractType.PUT }
        
        assertTrue(calls?.isNotEmpty() == true)
        assertTrue(puts?.isNotEmpty() == true)
    }
    
    @Test
    fun `test get historical bars`() = runTest {
        val startDate = kotlinx.datetime.Clock.System.now()
        val endDate = startDate.plus(kotlinx.datetime.DateTimePeriod(days = 5))
        
        val result = provider.getHistoricalBars("AMZN", startDate, endDate)
        assertTrue(result.isSuccess)
        
        val bars = result.getOrNull()
        assertNotNull(bars)
        assertTrue(bars?.isNotEmpty() == true)
        
        // Verify bar structure
        val firstBar = bars?.first()
        assertNotNull(firstBar?.timestamp)
        assertTrue(firstBar?.open!! > 0)
        assertTrue(firstBar?.high!! > 0)
        assertTrue(firstBar?.low!! > 0)
        assertTrue(firstBar?.close!! > 0)
        assertTrue(firstBar?.volume!! > 0)
    }
    
    @Test
    fun `test get upcoming events`() = runTest {
        val result = provider.getUpcomingEvents("AMZN")
        assertTrue(result.isSuccess)
        
        val events = result.getOrNull()
        assertNotNull(events)
        assertTrue(events?.isNotEmpty() == true)
        
        // Should have at least earnings event
        val earningsEvent = events?.find { it.eventType == EventType.EARNINGS }
        assertNotNull(earningsEvent)
    }
    
    @Test
    fun `test get market calendar`() = runTest {
        val startDate = kotlinx.datetime.Clock.System.now()
        val endDate = startDate.plus(kotlinx.datetime.DateTimePeriod(days = 30))
        
        val result = provider.getMarketCalendar(startDate, endDate)
        assertTrue(result.isSuccess)
        
        val calendar = result.getOrNull()
        assertNotNull(calendar)
        assertTrue(calendar?.tradingDays?.isNotEmpty() == true)
    }
    
    @Test
    fun `test add custom symbol`() = runTest {
        provider.addCustomSymbol("CUSTOM", 150.0, "Technology")
        
        val result = provider.getQuote("CUSTOM")
        assertTrue(result.isSuccess)
        
        val quote = result.getOrNull()
        assertNotNull(quote)
        assertEquals("CUSTOM", quote?.symbol)
        assertEquals("Technology", quote?.sector)
    }
    
    @Test
    fun `test option contract greeks are populated`() = runTest {
        val result = provider.getOptionChain("AMZN")
        assertTrue(result.isSuccess)
        
        val chain = result.getOrNull()
        val firstContract = chain?.contracts?.first()
        
        assertNotNull(firstContract?.delta)
        assertNotNull(firstContract?.gamma)
        assertNotNull(firstContract?.theta)
        assertNotNull(firstContract?.vega)
        assertNotNull(firstContract?.iv)
        assertNotNull(firstContract?.ivRank)
        assertNotNull(firstContract?.ivPercentile)
    }
    
    @Test
    fun `test option contract liquidity data`() = runTest {
        val result = provider.getOptionChain("AMZN")
        assertTrue(result.isSuccess)
        
        val chain = result.getOrNull()
        val firstContract = chain?.contracts?.first()
        
        assertTrue(firstContract?.bid!! > 0)
        assertTrue(firstContract?.ask!! > firstContract?.bid!!)
        assertTrue(firstContract?.volume!! >= 0)
        assertTrue(firstContract?.openInterest!! >= 0)
    }
}