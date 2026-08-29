package com.wheelscreener.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Low-level ORATS Data API endpoints. Responses intentionally remain untyped here:
 * ORATS returns only the requested columns, so [OratsMarketDataProvider] validates
 * and maps the dynamic payload into the app's stable domain models.
 */
interface OratsApiService {
    @GET("datav2/cores")
    suspend fun getCores(
        @Query("token") token: String,
        @Query("ticker") ticker: String,
        @Query("tradeDate") tradeDate: String? = null
    ): Response<ResponseBody>

    @GET("datav2/strikes")
    suspend fun getStrikes(
        @Query("token") token: String,
        @Query("ticker") ticker: String,
        @Query("tradeDate") tradeDate: String? = null
    ): Response<ResponseBody>

    @GET("datav2/hist/cores")
    suspend fun getHistoricalCores(
        @Query("token") token: String,
        @Query("ticker") ticker: String,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<ResponseBody>
}
