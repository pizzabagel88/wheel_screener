package com.wheelscreener.data.remote

import com.wheelscreener.domain.model.*
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlin.random.Random

/**
 * Mock market data provider for demo mode and testing
 * Generates realistic-looking synthetic data
 */
class MockMarketDataProvider : MarketDataProvider {
    
    private val random = Random(42) // Fixed seed for reproducibility
    private val defaultSymbols = listOf(
        "AMZN", "GOOGL", "META", "AMD", "UBER", "JPM",
        "FSLR", "TSLA", "NFLX", "XOM", "CVX", "SPY", "QQQ", "IWM"
    )
    
    private val symbolData = mutableMapOf<String, SymbolInfo>()
    
    init {
        initializeSymbolData()
    }
    
    private fun initializeSymbolData() {
        defaultSymbols.forEach { symbol ->
            symbolData[symbol] = generateSymbolInfo(symbol)
        }
    }
    
    private fun generateSymbolInfo(symbol: String): SymbolInfo {
        val basePrice = when (symbol) {
            "AMZN" -> 175.0
            "GOOGL" -> 140.0
            "META" -> 350.0
            "AMD" -> 120.0
            "UBER" -> 65.0
            "JPM" -> 155.0
            "FSLR" -> 180.0
            "TSLA" -> 240.0
            "NFLX" -> 450.0
            "XOM" -> 110.0
            "CVX" -> 155.0
            "SPY" -> 540.0
            "QQQ" -> 470.0
            "IWM" -> 200.0
            else -> 100.0
        }
        
        val sector = when (symbol) {
            "AMZN", "GOOGL", "META" -> "Technology"
            "AMD", "NFLX" -> "Technology"
            "UBER" -> "Communication"
            "JPM" -> "Financial"
            "FSLR" -> "Energy"
            "TSLA" -> "Consumer Discretionary"
            "XOM", "CVX" -> "Energy"
            "SPY", "QQQ", "IWM" -> "ETF"
            else -> "Other"
        }
        
        return SymbolInfo(
            basePrice = basePrice,
            sector = sector,
            marketCap = (basePrice * 1_000_000_000).toLong()
        )
    }
    
    override suspend fun getQuote(symbol: String): Result<Underlying> {
        return try {
            val info = symbolData[symbol] ?: generateSymbolInfo(symbol)
            val currentPrice = info.basePrice * (0.95 + random.nextDouble() * 0.1)
            val change = currentPrice * (random.nextDouble() * 0.04 - 0.02)
            
            val now = Clock.System.now()
            val underlying = Underlying(
                symbol = symbol,
                price = currentPrice,
                change = change,
                changePercent = (change / currentPrice) * 100,
                volume = (5_000_000 + random.nextDouble() * 20_000_000).toLong(),
                averageVolume20d = (8_000_000 + random.nextDouble() * 15_000_000).toLong(),
                marketCap = info.marketCap,
                fiftyTwoWeekHigh = currentPrice * 1.3,
                fiftyTwoWeekLow = currentPrice * 0.7,
                lastUpdate = now,
                sma20 = currentPrice * (0.95 + random.nextDouble() * 0.1),
                sma50 = currentPrice * (0.9 + random.nextDouble() * 0.15),
                sma200 = currentPrice * (0.85 + random.nextDouble() * 0.2),
                high20d = currentPrice * (1.05 + random.nextDouble() * 0.1),
                high60d = currentPrice * (1.1 + random.nextDouble() * 0.15),
                freeCashFlowTTM = info.marketCap * 0.1 * (0.8 + random.nextDouble() * 0.4),
                netDebt = info.marketCap * 0.2 * (0.0 + random.nextDouble() * 0.5),
                sector = info.sector,
                nextEarningsDate = now.plus(DateTimePeriod(days = 15 + random.nextInt(30))),
                nextEarningsTime = if (random.nextBoolean()) "AMC" else "BMO",
                isStale = false,
                confidence = DataConfidence.HIGH
            )
            Result.success(underlying)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getHistoricalBars(
        symbol: String,
        startDate: Instant,
        endDate: Instant
    ): Result<List<HistoricalBar>> {
        return try {
            val info = symbolData[symbol] ?: generateSymbolInfo(symbol)
            val bars = mutableListOf<HistoricalBar>()
            
            var currentDate = startDate
            val dayMillis = 24 * 60 * 60 * 1000L
            
            while (currentDate <= endDate) {
                val basePrice = info.basePrice * (0.9 + random.nextDouble() * 0.2)
                bars.add(
                    HistoricalBar(
                        timestamp = currentDate,
                        open = basePrice * (0.98 + random.nextDouble() * 0.02),
                        high = basePrice * (1.0 + random.nextDouble() * 0.03),
                        low = basePrice * (0.97 + random.nextDouble() * 0.02),
                        close = basePrice,
                        volume = (5_000_000 + random.nextDouble() * 20_000_000).toLong()
                    )
                )
                currentDate = currentDate.plus(dayMillis)
            }
            
            Result.success(bars)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getOptionChain(symbol: String): Result<OptionChain> {
        return try {
            val quote = getQuote(symbol).getOrThrow()
            val contracts = generateOptionContracts(symbol, quote.price)
            
            val optionChain = OptionChain(
                symbol = symbol,
                underlyingPrice = quote.price,
                contracts = contracts,
                lastUpdate = Clock.System.now(),
                isStale = false
            )
            Result.success(optionChain)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getOptionChain(symbol: String, expiration: Instant): Result<OptionChain> {
        return try {
            val quote = getQuote(symbol).getOrThrow()
            val contracts = generateOptionContracts(symbol, quote.price, expiration)
            
            val optionChain = OptionChain(
                symbol = symbol,
                underlyingPrice = quote.price,
                contracts = contracts,
                lastUpdate = Clock.System.now(),
                isStale = false
            )
            Result.success(optionChain)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun generateOptionContracts(
        symbol: String,
        underlyingPrice: Double,
        specificExpiration: Instant? = null
    ): List<OptionContract> {
        val contracts = mutableListOf<OptionContract>()
        val now = Clock.System.now()
        
        // Generate 3-4 expirations (weekly)
        val expirations = if (specificExpiration != null) {
            listOf(specificExpiration)
        } else {
            (0..3).map { i ->
                now.plus(DateTimePeriod(days = 7 * (i + 1)))
            }
        }
        
        // Generate strikes around ATM
        val strikeStep = when {
            underlyingPrice < 50 -> 2.5
            underlyingPrice < 100 -> 5.0
            underlyingPrice < 200 -> 10.0
            else -> 20.0
        }
        
        val atmStrike = (underlyingPrice / strikeStep).toInt() * strikeStep
        val strikes = ((atmStrike - 5 * strikeStep).toInt()..(atmStrike + 5 * strikeStep).toInt())
            .map { it.toDouble() * strikeStep }
            .filter { it > 0 }
        
        expirations.forEach { expiration ->
            val dte = ((expiration - now).inWholeDays).toInt()
            
            strikes.forEach { strike ->
                // Generate both calls and puts
                listOf(ContractType.CALL, ContractType.PUT).forEach { type ->
                    val moneyness = when (type) {
                        ContractType.CALL -> (underlyingPrice - strike) / underlyingPrice
                        ContractType.PUT -> (strike - underlyingPrice) / underlyingPrice
                    }
                    
                    val intrinsic = maxOf(0.0, moneyness * underlyingPrice)
                    val timeValue = (underlyingPrice * 0.02 * (dte / 365.0)) * (1 + random.nextDouble())
                    val iv = 0.2 + random.nextDouble() * 0.3
                    val ivRank = 20 + random.nextDouble() * 60
                    val ivPercentile = ivRank + random.nextDouble() * 10 - 5
                    
                    val bid = maxOf(0.01, intrinsic + timeValue * 0.95)
                    val ask = bid * (1.0 + random.nextDouble() * 0.05)
                    
                    val delta = when (type) {
                        ContractType.CALL -> {
                            when {
                                moneyness > 0.1 -> 0.8 + random.nextDouble() * 0.15
                                moneyness > -0.1 -> 0.4 + random.nextDouble() * 0.3
                                else -> 0.1 + random.nextDouble() * 0.2
                            }
                        }
                        ContractType.PUT -> {
                            when {
                                moneyness > 0.1 -> 0.1 + random.nextDouble() * 0.2
                                moneyness > -0.1 -> 0.4 + random.nextDouble() * 0.3
                                else -> 0.8 + random.nextDouble() * 0.15
                            }
                        }
                    }
                    
                    contracts.add(
                        OptionContract(
                            symbol = "$symbol${expiration.toString().take(8)}${strike.toInt()}${type.name[0]}",
                            underlyingSymbol = symbol,
                            contractType = type,
                            strike = strike,
                            expiration = expiration,
                            bid = bid,
                            ask = ask,
                            last = (bid + ask) / 2,
                            volume = (50 + random.nextInt(5000)).toInt(),
                            openInterest = (100 + random.nextInt(10000)).toInt(),
                            delta = delta,
                            gamma = random.nextDouble() * 0.1,
                            theta = -random.nextDouble() * 0.05,
                            vega = random.nextDouble() * 0.2,
                            iv = iv,
                            ivRank = ivRank,
                            ivPercentile = ivPercentile,
                            lastUpdate = now,
                            isStale = false
                        )
                    )
                }
            }
        }
        
        return contracts
    }
    
    override suspend fun getUpcomingEvents(symbol: String): Result<List<CorporateEvent>> {
        return try {
            val now = Clock.System.now()
            val events = mutableListOf<CorporateEvent>()
            
            // Add earnings event
            events.add(
                CorporateEvent(
                    symbol = symbol,
                    eventType = EventType.EARNINGS,
                    eventDate = now.plus(DateTimePeriod(days = 15 + random.nextInt(30))),
                    eventTime = if (random.nextBoolean()) "AMC" else "BMO",
                    description = "Quarterly Earnings Release"
                )
            )
            
            // Occasionally add other events
            if (random.nextBoolean()) {
                events.add(
                    CorporateEvent(
                        symbol = symbol,
                        eventType = EventType.DIVIDEND,
                        eventDate = now.plus(DateTimePeriod(days = 30 + random.nextInt(60))),
                        description = "Quarterly Dividend"
                    )
                )
            }
            
            Result.success(events)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getMarketCalendar(
        startDate: Instant,
        endDate: Instant
    ): Result<MarketCalendar> {
        return try {
            val tradingDays = mutableListOf<Instant>()
            val holidays = mutableListOf<MarketHoliday>()
            
            var currentDate = startDate
            val dayMillis = 24 * 60 * 60 * 1000L
            val nyTimeZone = TimeZone.of("America/New_York")
            
            while (currentDate <= endDate) {
                val dayOfWeek = currentDate.toLocalDateTime(nyTimeZone).dayOfWeek
                
                // Skip weekends
                if (dayOfWeek.name in listOf("SATURDAY", "SUNDAY")) {
                    currentDate = currentDate.plus(dayMillis)
                    continue
                }
                
                // Add some holidays
                val month = currentDate.toLocalDateTime(nyTimeZone).monthNumber
                val day = currentDate.toLocalDateTime(nyTimeZone).dayOfMonth
                
                if ((month == 1 && day == 1) || // New Year
                    (month == 7 && day == 4) || // Independence Day
                    (month == 12 && day == 25)) { // Christmas
                    holidays.add(
                        MarketHoliday(
                            date = currentDate,
                            name = when {
                                month == 1 && day == 1 -> "New Year's Day"
                                month == 7 && day == 4 -> "Independence Day"
                                else -> "Christmas Day"
                            },
                            isMarketOpen = false
                        )
                    )
                } else {
                    tradingDays.add(currentDate)
                }
                
                currentDate = currentDate.plus(dayMillis)
            }
            
            Result.success(MarketCalendar(tradingDays, holidays))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun isAvailable(): Boolean {
        return true // Mock is always available
    }
    
    override fun getProviderName(): String {
        return "Mock Data Provider"
    }
    
    /**
     * Add custom symbol for testing
     */
    fun addCustomSymbol(symbol: String, basePrice: Double, sector: String = "Other") {
        symbolData[symbol] = SymbolInfo(
            basePrice = basePrice,
            sector = sector,
            marketCap = (basePrice * 1_000_000_000).toLong()
        )
    }
    
    private data class SymbolInfo(
        val basePrice: Double,
        val sector: String,
        val marketCap: Long
    )
}