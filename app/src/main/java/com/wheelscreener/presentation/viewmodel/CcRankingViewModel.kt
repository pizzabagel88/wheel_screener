package com.wheelscreener.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.wheelscreener.data.local.dao.SettingsDao
import com.wheelscreener.data.local.dao.WatchlistDao
import com.wheelscreener.domain.model.StrategyConfig
import com.wheelscreener.domain.repository.MarketDataRepository
import com.wheelscreener.domain.scoring.CCScoringResult
import com.wheelscreener.domain.scoring.ScoringEngine
import com.wheelscreener.presentation.ui.state.CcRankingUiState
import com.wheelscreener.presentation.ui.state.CcSortOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CcRankingViewModel @Inject constructor(
    private val marketDataRepository: MarketDataRepository,
    private val watchlistDao: WatchlistDao,
    private val settingsDao: SettingsDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(CcRankingUiState())
    val uiState: StateFlow<CcRankingUiState> = _uiState.asStateFlow()

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val configAdapter = moshi.adapter(StrategyConfig::class.java)

    init {
        runScan()
    }

    fun runScan() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // 1. Load configuration
                val configEntity = settingsDao.getSetting("strategy_config")
                val config = if (configEntity != null) {
                    configAdapter.fromJson(configEntity.value) ?: StrategyConfig.default()
                } else {
                    StrategyConfig.default()
                }

                // 2. Load watchlist symbols
                val watchlist = watchlistDao.getActiveWatchlist().first()
                if (watchlist.isEmpty()) {
                    _uiState.value = _uiState.value.copy(isLoading = false, candidates = emptyList())
                    return@launch
                }

                // 3. Scan each symbol
                val scoredResults = mutableListOf<CCScoringResult>()
                for (item in watchlist) {
                    val symbol = item.symbol
                    val underlyingResult = marketDataRepository.getQuote(symbol)
                    val optionChainResult = marketDataRepository.getOptionChain(symbol)
                    val eventsResult = marketDataRepository.getUpcomingEvents(symbol)

                    if (underlyingResult.isSuccess && optionChainResult.isSuccess) {
                        val underlying = underlyingResult.getOrThrow()
                        val optionChain = optionChainResult.getOrThrow()
                        val events = eventsResult.getOrDefault(emptyList())

                        // Mock portfolio cost basis: 95% of current price for testing CC scoring
                        val mockCostBasis = underlying.price * 0.95
                        val mockShareCount = 100

                        // Score each CALL contract in the chain
                        for (contract in optionChain.contracts) {
                            if (contract.contractType == com.wheelscreener.domain.model.ContractType.CALL) {
                                val scoreResult = ScoringEngine.scoreCCCandidate(
                                    contract = contract,
                                    underlying = underlying,
                                    events = events,
                                    shareCount = mockShareCount,
                                    costBasis = mockCostBasis,
                                    config = config
                                )
                                scoredResults.add(scoreResult)
                            }
                        }
                    }
                }

                // 4. Rank candidates
                val ranked = ScoringEngine.rankCCCandidates(scoredResults)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    candidates = ranked
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Scan failed: ${e.message}"
                )
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setMinScore(score: Float) {
        _uiState.value = _uiState.value.copy(minScore = score)
    }

    fun setSortBy(option: CcSortOption) {
        _uiState.value = _uiState.value.copy(sortBy = option)
    }

    fun getFilteredAndSortedCandidates(): List<CCScoringResult> {
        val state = _uiState.value
        return state.candidates
            .filter { result ->
                result.underlying.symbol.contains(state.searchQuery, ignoreCase = true) &&
                        result.scoreComponents.compositeScore >= state.minScore
            }
            .sortedWith { a, b ->
                when (state.sortBy) {
                    CcSortOption.SCORE -> b.scoreComponents.compositeScore.compareTo(a.scoreComponents.compositeScore)
                    CcSortOption.RETURN_IF_CALLED -> b.returnIfCalled.compareTo(a.returnIfCalled)
                    CcSortOption.DTE -> a.dte.compareTo(b.dte)
                    CcSortOption.IV_RANK -> {
                        val aIV = a.contract.ivRank ?: 0.0
                        val bIV = b.contract.ivRank ?: 0.0
                        bIV.compareTo(aIV)
                    }
                }
            }
    }
}
