package com.wheelscreener.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.wheelscreener.data.local.dao.WatchlistDao
import com.wheelscreener.data.local.entity.WatchlistEntity
import com.wheelscreener.domain.model.StrategyConfig
import com.wheelscreener.domain.repository.MarketDataRepository
import com.wheelscreener.domain.scoring.CSPScoringResult
import com.wheelscreener.domain.usecase.RunScanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val marketDataRepository: MarketDataRepository,
    private val watchlistDao: WatchlistDao,
    private val runScanUseCase: RunScanUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }
    
    private fun loadInitialData() {
        viewModelScope.launch {
            val isAvailable = marketDataRepository.isProviderAvailable()
            val providerName = marketDataRepository.getProviderName()
            
            _uiState.value = _uiState.value.copy(
                isProviderAvailable = isAvailable,
                providerName = providerName
            )
            
            initializeDefaultWatchlist()
            
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
            _uiState.value = _uiState.value.copy(isScanning = true)
            try {
                val ranked = runScanUseCase()
                val summaryList = ranked.take(5).map { 
                    "${it.underlying.symbol} ${it.contract.strike}P (DTE: ${it.dte}) - Score: ${String.format("%.1f", it.scoreComponents.compositeScore)}"
                }
                
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    lastScanResults = if (summaryList.isEmpty()) listOf("No high-quality candidates found") else summaryList,
                    lastScanTimestamp = Clock.System.now()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
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
    val lastScanTimestamp: kotlinx.datetime.Instant? = null,
    val isScanning: Boolean = false
)