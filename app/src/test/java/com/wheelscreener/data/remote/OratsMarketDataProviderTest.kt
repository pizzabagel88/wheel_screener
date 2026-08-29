package com.wheelscreener.data.remote

import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class OratsMarketDataProviderTest {
    @Test
    fun `maps validated core data to an underlying`() = runTest {
        val provider = providerWith(
            cores = """{"data":[{"ticker":"SPY","stockPrice":500.0,"stockVolume":1000,"stockAvgVolume":900,"marketCap":1000000000}]}"""
        )

        val quote = provider.getQuote("spy").getOrThrow()

        assertEquals("SPY", quote.symbol)
        assertEquals(500.0, quote.price, 0.0)
        assertEquals(1_000_000_000L, quote.marketCap)
    }

    @Test
    fun `rejects core data without a price`() = runTest {
        val provider = providerWith("""{"data":[{"ticker":"SPY"}]}""")

        assertTrue(provider.getQuote("SPY").isFailure)
    }

    @Test
    fun `maps both call and put contracts from ORATS strikes`() = runTest {
        val provider = providerWith(
            cores = """{"data":[{"ticker":"SPY","stockPrice":500.0,"ivRank":45}]}""",
            strikes = """{"data":[{"strike":500,"dte":7,"callBidPrice":4.0,"callAskPrice":4.2,"putBidPrice":3.8,"putAskPrice":4.0,"callDelta":0.5,"putDelta":-0.5,"callVolume":100,"putVolume":120,"callOpenInterest":1000,"putOpenInterest":1100}]}"""
        )

        val chain = provider.getOptionChain("SPY").getOrThrow()

        assertEquals(2, chain.contracts.size)
        assertTrue(chain.contracts.any { it.contractType.name == "CALL" })
        assertTrue(chain.contracts.any { it.contractType.name == "PUT" })
    }

    @Test
    fun `fails safely on rate limiting`() = runTest {
        val provider = OratsMarketDataProvider(
            apiKey = "test",
            baseUrl = "https://example.com/",
            api = FakeOratsApi(statusCode = 429)
        )

        assertFalse(provider.getQuote("SPY").isSuccess)
    }

    private fun providerWith(
        cores: String,
        strikes: String = """{"data":[]}"""
    ) = OratsMarketDataProvider(
        apiKey = "test",
        baseUrl = "https://example.com/",
        api = FakeOratsApi(cores = cores, strikes = strikes)
    )

    private class FakeOratsApi(
        private val cores: String = """{"data":[]}""",
        private val strikes: String = """{"data":[]}""",
        private val statusCode: Int = 200
    ) : OratsApiService {
        override suspend fun getCores(token: String, ticker: String, tradeDate: String?) = response(cores)

        override suspend fun getStrikes(token: String, ticker: String, tradeDate: String?) = response(strikes)

        override suspend fun getHistoricalCores(
            token: String,
            ticker: String,
            startDate: String,
            endDate: String
        ) = response(cores)

        private fun response(payload: String): Response<okhttp3.ResponseBody> =
            if (statusCode == 200) {
                Response.success(payload.toResponseBody("application/json".toMediaType()))
            } else {
                Response.error(statusCode, payload.toResponseBody("application/json".toMediaType()))
            }
    }
}
