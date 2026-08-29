package com.wheelscreener.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "paper_positions")
data class PaperPositionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val underlyingSymbol: String,
    val contractSymbol: String,
    val strategy: String,
    val optionType: String,
    val strike: Double,
    val expiration: Long,
    val quantity: Int,
    val entryCredit: Double,
    val entryUnderlyingPrice: Double,
    val entryDelta: Double?,
    val openedAt: Long,
    val status: String = "OPEN",
    val closeDebit: Double? = null,
    val closedAt: Long? = null,
    val assignmentPrice: Double? = null,
    val notes: String = ""
)
