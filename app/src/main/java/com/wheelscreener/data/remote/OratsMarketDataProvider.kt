package com.wheelscreener.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.wheelscreener.domain.model.ContractType
import com.wheelscreener.domain.model.DataConfidence
import com.wheelscreener.domain.model.OptionChain
import com.wheelscreener.domain.model.OptionContract
import com.wheelscreener.domain.model.Underlying
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Instant
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import java.io.IOException
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/** Exception returned when ORATS rejects a request or returns an unusable payload. */
class OratsApiException(message: String) : IOException(message)

class OratsRateLimitException(message: String) : IOException(message)

/**
 * Real ORATS-backed market data provider.
 *
 * It is enabled only when `ORATS_API_KEY` is supplied through a non-versioned local
 * Gradle property. ORATS supports selected-column responses, therefore each mapping
 * validates required data rather than relying on a rigid response schema.
 */
class OratsMarketDataProvider(
    private val apiKey: String,
    baseUrl: String,
    private val api: OratsApiService = createApi(baseUrl)
) : MarketDataProvider {
    private val requestMutex = Mutex()
    private var lastRequestAtMillis = 0L
    private val mapAdapter = Moshi.Builder().build().adapter<Map<String, Any?>>(
        Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    )

    override suspend fun getQuote(symbol: String): Result<Underlying> = runProviderCall {
        mapCore(firstRow(fetchCores(symbol)), symbol)
    }

    override suspend fun getHistoricalBars(
        symbol: String,
        startDate: Instant,
        endDate: Instant
    ): Result<List<HistoricalBar>> = runProviderCall {
        val rows = requestRows {
            api.getHistoricalCores(
                token = apiKey,
                ticker = symbol.normalizedSymbol(),
                startDate = startDate.toMarketDate(),
                endDate = endDate.toMarketDate()
            )
        }
        rows.mapNotNull(::mapHistoricalBar).ifEmpty {
            throw OratsApiException("ORATS returned no historical core data for $symbol")
        }
    }

    override suspend fun getOptionChain(symbol: String): Result<OptionChain> = runProviderCall {
        val normalizedSymbol = symbol.normalizedSymbol()
        val core = firstRow(fetchCores(normalizedSymbol))
        val underlying = mapCore(core, normalizedSymbol)
        val rows = requestRows { api.getStrikes(apiKey, normalizedSymbol) }
        val contracts = rows.flatMap { mapContracts(it, underlying, core) }
        if (contracts.isEmpty()) {
            throw OratsApiException("ORATS returned no usable option contracts for $normalizedSymbol")
        }
        OptionChain(
            symbol = normalizedSymbol,
            underlyingPrice = underlying.price,
            contracts = contracts,
            lastUpdate = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        )
    }

    override suspend fun getOptionChain(symbol: String, expiration: Instant): Result<OptionChain> =
        getOptionChain(symbol).map { chain ->
            val requestedDate = expiration.toMarketLocalDate()
            chain.copy(contracts = chain.contracts.filter { it.expiration.toMarketLocalDate() == requestedDate })
        }

    override suspend fun getUpcomingEvents(symbol: String): Result<List<CorporateEvent>> = runProviderCall {
        val core = firstRow(fetchCores(symbol))
        val earningsDate = core.stringValue("earningsDate", "nextEarningsDate")?.toMarketInstantOrNull()
        earningsDate?.let {
            listOf(
                CorporateEvent(
                    symbol = symbol.normalizedSymbol(),
                    eventType = EventType.EARNINGS,
                    eventDate = it,
                    eventTime = core.stringValue("earningsTime"),
                    description = "ORATS earnings date"
                )
            )
        } ?: emptyList()
    }

    override suspend fun getMarketCalendar(startDate: Instant, endDate: Instant): Result<MarketCalendar> = runProviderCall {
        // ORATS focuses on market and options data rather than an exchange calendar.
        // The repository still gets a deterministic weekday calendar when this provider is active.
        val tradingDays = generateSequence(startDate.toMarketLocalDate()) { it.plusDays(1) }
            .takeWhile { !it.isAfter(endDate.toMarketLocalDate()) }
            .filter { it.dayOfWeek.value < 6 }
            .map { it.toMarketInstant() }
            .toList()
        MarketCalendar(tradingDays = tradingDays, holidays = emptyList())
    }

    override suspend fun isAvailable(): Boolean = getQuote("SPY").isSuccess

    override fun getProviderName(): String = "ORATS"

    private suspend fun fetchCores(symbol: String): List<Map<String, Any?>> =
        requestRows { api.getCores(apiKey, symbol.normalizedSymbol()) }

    private suspend fun requestRows(request: suspend () -> Response<okhttp3.ResponseBody>): List<Map<String, Any?>> {
        var lastError: Exception? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                throttle()
                val response = request()
                if (response.code() == 429) {
                    throw OratsRateLimitException("ORATS rate limit reached")
                }
                if (response.code() in 500..599) {
                    throw IOException("ORATS service failure (${response.code()})")
                }
                if (!response.isSuccessful) {
                    throw OratsApiException("ORATS request failed (${response.code()}): ${response.errorBody()?.string().orEmpty()}")
                }
                val payload = response.body()?.string()
                    ?: throw OratsApiException("ORATS returned an empty response")
                return extractRows(payload)
            } catch (error: Exception) {
                lastError = error
                if (error is OratsApiException || error is OratsRateLimitException || attempt == MAX_ATTEMPTS - 1) {
                    throw error
                }
                delay(RETRY_DELAY_MILLIS * (attempt + 1))
            }
        }
        throw lastError ?: OratsApiException("ORATS request failed")
    }

    private suspend fun throttle() = requestMutex.withLock {
        val elapsed = System.currentTimeMillis() - lastRequestAtMillis
        if (elapsed < MIN_REQUEST_INTERVAL_MILLIS) {
            delay(MIN_REQUEST_INTERVAL_MILLIS - elapsed)
        }
        lastRequestAtMillis = System.currentTimeMillis()
    }

    private fun extractRows(payload: String): List<Map<String, Any?>> {
        val root = mapAdapter.fromJson(payload)
            ?: throw OratsApiException("ORATS returned malformed JSON")
        val values = root["data"] ?: root["results"] ?: root["rows"]
            ?: throw OratsApiException("ORATS response did not contain data rows")
        @Suppress("UNCHECKED_CAST")
        return (values as? List<*>)
            ?.mapNotNull { it as? Map<String, Any?> }
            ?.takeIf { it.isNotEmpty() }
            ?: throw OratsApiException("ORATS response contained no data rows")
    }

    private fun firstRow(rows: List<Map<String, Any?>>): Map<String, Any?> =
        rows.firstOrNull() ?: throw OratsApiException("ORATS response contained no data rows")

    private fun mapCore(row: Map<String, Any?>, fallbackSymbol: String): Underlying {
        val price = row.doubleValue("stockPrice", "close", "price")
            ?: throw OratsApiException("ORATS core data is missing stockPrice")
        val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        val marketCap = row.doubleValue("marketCap", "stockMarketCap")?.toLong() ?: 0L
        return Underlying(
            symbol = row.stringValue("ticker", "symbol")?.normalizedSymbol() ?: fallbackSymbol,
            price = price,
            change = row.doubleValue("stockPriceChange", "change") ?: 0.0,
            changePercent = row.doubleValue("stockPriceChangePct", "changePercent") ?: 0.0,
            volume = row.doubleValue("stockVolume", "volume")?.toLong() ?: 0L,
            averageVolume20d = row.doubleValue("stockAvgVolume", "averageVolume20d")?.toLong() ?: 0L,
            marketCap = marketCap,
            fiftyTwoWeekHigh = row.doubleValue("stockHigh52", "high52Week") ?: price,
            fiftyTwoWeekLow = row.doubleValue("stockLow52", "low52Week") ?: price,
            lastUpdate = now,
            sma20 = row.doubleValue("stockSma20", "sma20"),
            sma50 = row.doubleValue("stockSma50", "sma50"),
            sma200 = row.doubleValue("stockSma200", "sma200"),
            high20d = row.doubleValue("stockHigh20", "high20d"),
            high60d = row.doubleValue("stockHigh60", "high60d"),
            freeCashFlowTTM = row.doubleValue("freeCashFlow", "freeCashFlowTTM"),
            netDebt = row.doubleValue("netDebt"),
            sector = row.stringValue("sector"),
            nextEarningsDate = row.stringValue("earningsDate", "nextEarningsDate")?.toMarketInstantOrNull(),
            nextEarningsTime = row.stringValue("earningsTime"),
            confidence = if (marketCap > 0L) DataConfidence.HIGH else DataConfidence.MEDIUM
        )
    }

    private fun mapHistoricalBar(row: Map<String, Any?>): HistoricalBar? {
        val close = row.doubleValue("stockPrice", "close") ?: return null
        val timestamp = row.stringValue("tradeDate", "date")?.toMarketInstantOrNull() ?: return null
        return HistoricalBar(
            timestamp = timestamp,
            open = row.doubleValue("open") ?: close,
            high = row.doubleValue("high") ?: close,
            low = row.doubleValue("low") ?: close,
            close = close,
            volume = row.doubleValue("stockVolume", "volume")?.toLong() ?: 0L
        )
    }

    private fun mapContracts(
        row: Map<String, Any?>,
        underlying: Underlying,
        core: Map<String, Any?>
    ): List<OptionContract> {
        val strike = row.doubleValue("strike") ?: return emptyList()
        val expiration = row.stringValue("expirDate", "expirationDate")?.toMarketInstantOrNull()
            ?: row.doubleValue("dte")?.toLong()?.let {
                underlying.lastUpdate.toMarketLocalDate().plusDays(it).toMarketInstant()
            }
            ?: return emptyList()
        val ivRank = row.doubleValue("ivRank") ?: core.doubleValue("ivRank")
        val ivPercentile = row.doubleValue("ivPercentile") ?: core.doubleValue("ivPercentile")
        return listOfNotNull(
            optionContract(row, underlying, strike, expiration, ContractType.CALL, ivRank, ivPercentile),
            optionContract(row, underlying, strike, expiration, ContractType.PUT, ivRank, ivPercentile)
        )
    }

    private fun optionContract(
        row: Map<String, Any?>,
        underlying: Underlying,
        strike: Double,
        expiration: Instant,
        type: ContractType,
        ivRank: Double?,
        ivPercentile: Double?
    ): OptionContract? {
        val prefix = if (type == ContractType.CALL) "call" else "put"
        val bid = row.doubleValue("${prefix}BidPrice", "${prefix}Bid") ?: return null
        val ask = row.doubleValue("${prefix}AskPrice", "${prefix}Ask") ?: return null
        return OptionContract(
            symbol = row.stringValue("${prefix}Symbol")
                ?: "${underlying.symbol}-${expiration.toMarketDate()}-$strike-${type.name.first()}",
            underlyingSymbol = underlying.symbol,
            contractType = type,
            strike = strike,
            expiration = expiration,
            bid = bid,
            ask = ask,
            last = row.doubleValue("${prefix}LastPrice", "${prefix}Last"),
            volume = row.doubleValue("${prefix}Volume")?.toInt() ?: 0,
            openInterest = row.doubleValue("${prefix}OpenInterest", "${prefix}Oi")?.toInt() ?: 0,
            delta = row.doubleValue("${prefix}Delta"),
            gamma = row.doubleValue("${prefix}Gamma"),
            theta = row.doubleValue("${prefix}Theta"),
            vega = row.doubleValue("${prefix}Vega"),
            iv = row.doubleValue("${prefix}Iv", "${prefix}ImpliedVolatility"),
            ivRank = ivRank,
            ivPercentile = ivPercentile,
            lastUpdate = underlying.lastUpdate
        )
    }

    private suspend fun <T> runProviderCall(block: suspend () -> T): Result<T> =
        runCatching { block() }

    private fun Map<String, Any?>.doubleValue(vararg keys: String): Double? = keys.firstNotNullOfOrNull { key ->
        when (val value = this[key]) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull()
            else -> null
        }
    }

    private fun Map<String, Any?>.stringValue(vararg keys: String): String? = keys.firstNotNullOfOrNull { key ->
        this[key]?.toString()?.takeIf(String::isNotBlank)
    }

    private fun String.normalizedSymbol(): String = trim().uppercase()

    private fun String.toMarketInstantOrNull(): Instant? = runCatching {
        java.time.LocalDate.parse(take(10), DATE_FORMAT)
            .atStartOfDay(MARKET_ZONE)
            .toInstant()
            .toEpochMilli()
            .let(Instant::fromEpochMilliseconds)
    }.getOrNull()

    private fun Instant.toMarketLocalDate(): java.time.LocalDate =
        java.time.Instant.ofEpochMilli(toEpochMilliseconds()).atZone(MARKET_ZONE).toLocalDate()

    private fun Instant.toMarketDate(): String = toMarketLocalDate().format(DATE_FORMAT)

    private fun java.time.LocalDate.toMarketInstant(): Instant = Instant.fromEpochMilliseconds(
        atStartOfDay(MARKET_ZONE).toInstant().toEpochMilli()
    )

    private companion object {
        const val MAX_ATTEMPTS = 3
        const val MIN_REQUEST_INTERVAL_MILLIS = 250L
        const val RETRY_DELAY_MILLIS = 500L
        val MARKET_ZONE: ZoneId = ZoneId.of("America/New_York")
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

        fun createApi(baseUrl: String): OratsApiService {
            require(baseUrl.startsWith("https://")) { "ORATS base URL must use HTTPS" }
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
            return Retrofit.Builder()
                .baseUrl(if (baseUrl.endsWith('/')) baseUrl else "$baseUrl/")
                .client(client)
                .build()
                .create(OratsApiService::class.java)
        }
    }
}
