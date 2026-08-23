package com.wheelscreener.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wheelscreener.data.local.dao.WatchlistDao
import com.wheelscreener.data.local.entity.WatchlistEntity
import com.wheelscreener.domain.repository.MarketDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val marketDataRepository: MarketDataRepository,
    private val watchlistDao: WatchlistDao
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    init {
        loadInitialData()
    }
    
    private fun loadInitialData() {
        viewModelScope.launch {
            // Check provider availability
            val isAvailable = marketDataRepository.isProviderAvailable()
            val providerName = marketDataRepository.getProviderName()
            
            _uiState.value = _uiState.value.copy(
                isProviderAvailable = isAvailable,
                providerName = providerName
            )
            
            // Load or initialize default watchlist
            initializeDefaultWatchlist()
            
            // Load watchlist for display
            watchlistDao.getActiveWatchlist().collect { watchlist ->
                _uiState.value = _uiState.value.copy(
                    watchlist = watchlist.map { it.symbol }
                )
            }
        }
    }
    
    private suspend fun initializeDefaultWatchlist() {
        val defaultSymbols = listOf(
            "AMZN", "GOOGL", "META", "AMD", "UBER", "JPM",
            "FSLR", "TSLA", "NFLX", "XOM", "CVX", "SPY", "QQQ", "IWM"
        )
        
        defaultSymbols.forEach { symbol ->
            val existing = watchlistDao.getSymbol(symbol)
            if (existing == null) {
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
    
    fun runDemoScan() {
        viewModelScope.launch {
            try {
                val results = mutableListOf<String>()
                val watchlist = _uiState.value.watchlist.take(5) // Scan first 5 for demo
                
                watchlist.forEach { symbol ->
                    val quoteResult = marketDataRepository.getQuote(symbol)
                    quoteResult.onSuccess { quote ->
                        results.add("$symbol: $${quote.price} (${String.format("%.2f", quote.changePercent)}%)")
                    }.onFailure { error ->
                        results.add("$symbol: Error - ${error.message}")
                    }
                }
                
                _uiState.value = _uiState.value.copy(
                    lastScanResults = results,
                    lastScanTimestamp = Clock.System.now()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    lastScanResults = listOf("Scan failed: ${e.message}")
                )
            }
        }
    }
}

data class DashboardUiState(
    val isProviderAvailable: Boolean = false,
    val providerName: String = "",
    val watchlist: List<String> = emptyList(),
    val lastScanResults: List<String> = emptyList(),
    val lastScanTimestamp: kotlinx.datetime.Instant? = null
)