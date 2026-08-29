package com.wheelscreener.data.scheduler

import com.wheelscreener.domain.repository.MarketDataRepository
import kotlinx.datetime.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Resolves trading days in the U.S. market time zone, falling back to weekdays offline. */
@Singleton
class MarketCalendarManager @Inject constructor(
    private val marketDataRepository: MarketDataRepository
) {
    suspend fun isTradingDay(instant: Instant): Boolean {
        val date = java.time.Instant.ofEpochMilli(instant.toEpochMilliseconds())
            .atZone(MARKET_ZONE)
            .toLocalDate()
        val start = date.atStartOfDay(MARKET_ZONE).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(MARKET_ZONE).toInstant().toEpochMilli()
        val calendar = marketDataRepository.getMarketCalendar(
            Instant.fromEpochMilliseconds(start),
            Instant.fromEpochMilliseconds(end)
        ).getOrNull()

        return calendar?.tradingDays?.any { day ->
            day.toEpochMilliseconds() in start until end
        } ?: isWeekday(date)
    }

    private fun isWeekday(date: java.time.LocalDate): Boolean =
        date.dayOfWeek != java.time.DayOfWeek.SATURDAY && date.dayOfWeek != java.time.DayOfWeek.SUNDAY

    private companion object {
        val MARKET_ZONE: ZoneId = ZoneId.of("America/New_York")
    }
}
