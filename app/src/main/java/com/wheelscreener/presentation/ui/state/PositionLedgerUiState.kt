package com.wheelscreener.presentation.ui.state

import com.wheelscreener.data.local.entity.PaperPositionEntity
import com.wheelscreener.domain.model.PositionReminder

data class PositionLedgerUiState(
    val positions: List<PaperPositionEntity> = emptyList(),
    val reminders: List<PositionReminder> = emptyList(),
    val exportedCsv: String? = null,
    val error: String? = null
)
