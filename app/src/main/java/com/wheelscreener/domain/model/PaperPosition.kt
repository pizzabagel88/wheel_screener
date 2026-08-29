package com.wheelscreener.domain.model

import com.wheelscreener.data.local.entity.PaperPositionEntity
import kotlinx.datetime.Instant
import kotlin.math.abs

enum class PaperPositionStatus { OPEN, CLOSED, ASSIGNED, ROLLED }

enum class PositionReminderType { EXPIRING_SOON, ROLL_REVIEW, ASSIGNMENT_RISK }

data class PositionReminder(
    val positionId: Long,
    val symbol: String,
    val type: PositionReminderType,
    val message: String
)

object PaperPositionAnalytics {
    private const val CONTRACT_MULTIPLIER = 100.0

    fun unrealizedPnl(position: PaperPositionEntity, mark: Double): Double =
        (position.entryCredit - mark) * position.quantity * CONTRACT_MULTIPLIER

    fun realizedPnl(position: PaperPositionEntity): Double? {
        return when (position.status) {
            PaperPositionStatus.CLOSED.name -> position.closeDebit?.let {
                (position.entryCredit - it) * position.quantity * CONTRACT_MULTIPLIER
            }
            PaperPositionStatus.ASSIGNED.name, PaperPositionStatus.ROLLED.name ->
                (position.entryCredit - (position.closeDebit ?: 0.0)) * position.quantity * CONTRACT_MULTIPLIER
            else -> null
        }
    }

    fun daysToExpiration(position: PaperPositionEntity, now: Instant): Int {
        val zone = java.time.ZoneId.of("America/New_York")
        val today = java.time.Instant.ofEpochMilli(now.toEpochMilliseconds()).atZone(zone).toLocalDate()
        val expiration = java.time.Instant.ofEpochMilli(position.expiration).atZone(zone).toLocalDate()
        return java.time.temporal.ChronoUnit.DAYS.between(today, expiration).toInt()
    }

    fun reminders(position: PaperPositionEntity, now: Instant): List<PositionReminder> {
        if (position.status != PaperPositionStatus.OPEN.name) return emptyList()
        val dte = daysToExpiration(position, now)
        return buildList {
            if (dte <= 3) add(PositionReminder(position.id, position.underlyingSymbol, PositionReminderType.EXPIRING_SOON, "${position.underlyingSymbol} expires in $dte day(s)."))
            else if (dte <= 7) add(PositionReminder(position.id, position.underlyingSymbol, PositionReminderType.ROLL_REVIEW, "Review ${position.underlyingSymbol} for a possible roll in $dte days."))
            if (abs(position.entryDelta ?: 0.0) >= 0.40) {
                add(PositionReminder(position.id, position.underlyingSymbol, PositionReminderType.ASSIGNMENT_RISK, "${position.underlyingSymbol} has elevated assignment risk (delta ${"%.2f".format(position.entryDelta)})."))
            }
        }
    }

    fun toCsv(positions: List<PaperPositionEntity>): String = buildString {
        appendLine("Id,Symbol,Contract,Strategy,Type,Strike,Expiration,Quantity,Entry Credit,Close Debit,Status,Opened At,Closed At,Realized PnL,Notes")
        positions.forEach { position ->
            appendLine(listOf(
                position.id, position.underlyingSymbol, position.contractSymbol, position.strategy, position.optionType,
                position.strike, position.expiration, position.quantity, position.entryCredit, position.closeDebit ?: "",
                position.status, position.openedAt, position.closedAt ?: "", realizedPnl(position) ?: "", csvEscape(position.notes)
            ).joinToString(","))
        }
    }

    private fun csvEscape(value: String): String = "\"${value.replace("\"", "\"\"")}\""
}
