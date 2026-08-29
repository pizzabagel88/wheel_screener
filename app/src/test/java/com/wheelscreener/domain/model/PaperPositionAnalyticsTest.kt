package com.wheelscreener.domain.model

import com.wheelscreener.data.local.entity.PaperPositionEntity
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaperPositionAnalyticsTest {
    private val now = Clock.System.now()

    @Test
    fun `calculates short-option unrealized and realized PnL`() {
        val position = position(entryCredit = 2.0)
        assertEquals(150.0, PaperPositionAnalytics.unrealizedPnl(position, 0.50), 0.001)
        assertEquals(200.0, PaperPositionAnalytics.realizedPnl(position.copy(status = "CLOSED", closeDebit = 0.0))!!, 0.001)
    }

    @Test
    fun `creates expiration and assignment reminders`() {
        val position = position(expiration = now.plus(2, DateTimeUnit.DAY, kotlinx.datetime.TimeZone.of("America/New_York")).toEpochMilliseconds(), entryDelta = -0.45)
        val reminders = PaperPositionAnalytics.reminders(position, now)
        assertTrue(reminders.any { it.type == PositionReminderType.EXPIRING_SOON })
        assertTrue(reminders.any { it.type == PositionReminderType.ASSIGNMENT_RISK })
    }

    @Test
    fun `exports CSV with escaped notes`() {
        val csv = PaperPositionAnalytics.toCsv(listOf(position(notes = "review, then \"roll\"")))
        assertTrue(csv.contains("\"review, then \"\"roll\"\"\""))
    }

    private fun position(
        entryCredit: Double = 1.0,
        expiration: Long = now.plus(10, DateTimeUnit.DAY, kotlinx.datetime.TimeZone.of("America/New_York")).toEpochMilliseconds(),
        entryDelta: Double? = -0.25,
        notes: String = ""
    ) = PaperPositionEntity(
        id = 1,
        underlyingSymbol = "SPY",
        contractSymbol = "SPY-test",
        strategy = "CSP",
        optionType = "PUT",
        strike = 500.0,
        expiration = expiration,
        quantity = 1,
        entryCredit = entryCredit,
        entryUnderlyingPrice = 500.0,
        entryDelta = entryDelta,
        openedAt = now.toEpochMilliseconds(),
        notes = notes
    )
}
