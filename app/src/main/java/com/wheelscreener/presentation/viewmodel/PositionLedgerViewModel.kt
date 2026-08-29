package com.wheelscreener.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wheelscreener.data.local.dao.PaperPositionDao
import com.wheelscreener.domain.model.PaperPositionAnalytics
import com.wheelscreener.domain.model.PaperPositionStatus
import com.wheelscreener.presentation.ui.state.PositionLedgerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject

@HiltViewModel
class PositionLedgerViewModel @Inject constructor(
    private val paperPositionDao: PaperPositionDao
) : ViewModel() {
    private val _uiState = MutableStateFlow(PositionLedgerUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            paperPositionDao.observeAllPositions().collectLatest { positions ->
                _uiState.value = _uiState.value.copy(
                    positions = positions,
                    reminders = positions.flatMap { PaperPositionAnalytics.reminders(it, Clock.System.now()) }
                )
            }
        }
    }

    fun closeWorthless(positionId: Long) = updatePosition(positionId) {
        it.copy(status = PaperPositionStatus.CLOSED.name, closeDebit = 0.0, closedAt = Clock.System.now().toEpochMilliseconds())
    }

    fun markAssigned(positionId: Long) = updatePosition(positionId) {
        it.copy(status = PaperPositionStatus.ASSIGNED.name, closedAt = Clock.System.now().toEpochMilliseconds(), assignmentPrice = it.strike)
    }

    fun markRolled(positionId: Long) = updatePosition(positionId) {
        it.copy(status = PaperPositionStatus.ROLLED.name, closedAt = Clock.System.now().toEpochMilliseconds())
    }

    fun exportCsv() {
        _uiState.value = _uiState.value.copy(exportedCsv = PaperPositionAnalytics.toCsv(_uiState.value.positions))
    }

    private fun updatePosition(positionId: Long, update: (com.wheelscreener.data.local.entity.PaperPositionEntity) -> com.wheelscreener.data.local.entity.PaperPositionEntity) {
        viewModelScope.launch {
            runCatching { paperPositionDao.getById(positionId)?.let { paperPositionDao.update(update(it)) } }
                .onFailure { _uiState.value = _uiState.value.copy(error = it.message ?: "Unable to update position") }
        }
    }
}
