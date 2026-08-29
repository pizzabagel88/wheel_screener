package com.wheelscreener.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wheelscreener.data.local.dao.WatchlistDao
import com.wheelscreener.data.local.entity.WatchlistEntity
import com.wheelscreener.presentation.ui.state.WatchlistUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val watchlistDao: WatchlistDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(WatchlistUiState(isLoading = true))
    val uiState: StateFlow<WatchlistUiState> = _uiState.asStateFlow()

    init {
        loadWatchlist()
    }

    private fun loadWatchlist() {
        viewModelScope.launch {
            watchlistDao.getActiveWatchlist()
                .catch { e ->
                    _uiState.value = WatchlistUiState(error = "Failed to load watchlist: ${e.message}")
                }
                .collect { list ->
                    _uiState.value = WatchlistUiState(watchlist = list, isLoading = false)
                }
        }
    }

    fun addSymbol(symbol: String, tags: String = "Custom") {
        viewModelScope.launch {
            if (symbol.isBlank()) return@launch
            val normalized = symbol.trim().uppercase()
            val entity = WatchlistEntity(
                symbol = normalized,
                tags = tags,
                addedAt = Clock.System.now().toEpochMilliseconds(),
                isActive = true
            )
            watchlistDao.insertSymbol(entity)
        }
    }

    fun deleteSymbol(symbol: String) {
        viewModelScope.launch {
            watchlistDao.deleteSymbol(symbol)
        }
    }

    fun importFromCsv(csvContent: String) {
        viewModelScope.launch {
            try {
                val lines = csvContent.lines()
                val entities = lines.mapNotNull { line ->
                    val parts = line.split(",")
                    if (parts.isEmpty() || parts[0].isBlank()) return@mapNotNull null
                    val symbol = parts[0].trim().uppercase()
                    val tags = if (parts.size > 1) parts.drop(1).joinToString(",").trim() else "Imported"
                    WatchlistEntity(
                        symbol = symbol,
                        tags = tags,
                        addedAt = Clock.System.now().toEpochMilliseconds(),
                        isActive = true
                    )
                }
                if (entities.isNotEmpty()) {
                    watchlistDao.insertSymbols(entities)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "CSV Import failed: ${e.message}")
            }
        }
    }

    fun exportToCsv(): String {
        return _uiState.value.watchlist.joinToString("\n") { "${it.symbol},${it.tags}" }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            watchlistDao.clearAll()
            val defaultSymbols = listOf(
                "AMZN", "GOOGL", "META", "AMD", "UBER", "JPM",
                "FSLR", "TSLA", "NFLX", "XOM", "CVX", "SPY", "QQQ", "IWM"
            )
            
            defaultSymbols.forEach { symbol ->
                val tags = when (symbol) {
                    "AMZN", "GOOGL", "META", "AMD", "NFLX" -> "Core,Technology"
                    "UBER" -> "Satellite,Communication"
                    "JPM" -> "Core,Financial"
                    "FSLR", "TSLA" -> "Satellite,Energy"
                    "XOM", "CVX" -> "Core,Energy"
                    "SPY", "QQQ", "IWM" -> "ETF"
                    else -> "Custom"
                }
                
                watchlistDao.insertSymbol(
                    WatchlistEntity(
                        symbol = symbol,
                        tags = tags,
                        addedAt = Clock.System.now().toEpochMilliseconds(),
                        isActive = true
                    )
                )
            }
        }
    }
}
