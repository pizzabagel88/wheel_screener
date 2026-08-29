package com.wheelscreener.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.wheelscreener.data.local.dao.SettingsDao
import com.wheelscreener.data.local.dao.PaperPositionDao
import com.wheelscreener.data.local.entity.PaperPositionEntity
import com.wheelscreener.data.local.dao.WatchlistDao
import com.wheelscreener.domain.model.StrategyConfig
import com.wheelscreener.domain.repository.MarketDataRepository
import com.wheelscreener.domain.scoring.ScoringEngine
import com.wheelscreener.presentation.ui.state.CandidateDetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import javax.inject.Inject

@HiltViewModel
class CandidateDetailViewModel @Inject constructor(
    private val marketDataRepository: MarketDataRepository,
    private val watchlistDao: WatchlistDao,
    private val settingsDao: SettingsDao,
    private val paperPositionDao: PaperPositionDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(CandidateDetailUiState())
    val uiState: StateFlow<CandidateDetailUiState> = _uiState.asStateFlow()

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val configAdapter = moshi.adapter(StrategyConfig::class.java)

    fun loadCandidate(
        symbol: String,
        strike: Double,
        expirationEpoch: Long,
        type: String
    ) {
        viewModelScope.launch {
            _uiState.value = CandidateDetailUiState(isLoading = true)
            try {
                // 1. Get watchlist status
                val isSaved = watchlistDao.getSymbol(symbol) != null

                // 2. Load strategy config
                val configEntity = settingsDao.getSetting("strategy_config")
                val config = if (configEntity != null) {
                    configAdapter.fromJson(configEntity.value) ?: StrategyConfig.default()
                } else {
                    StrategyConfig.default()
                }

                // 3. Fetch data
                val underlyingResult = marketDataRepository.getQuote(symbol)
                val expiration = Instant.fromEpochMilliseconds(expirationEpoch)
                val optionChainResult = marketDataRepository.getOptionChain(symbol, expiration)
                val eventsResult = marketDataRepository.getUpcomingEvents(symbol)

                if (underlyingResult.isSuccess && optionChainResult.isSuccess) {
                    val underlying = underlyingResult.getOrThrow()
                    val chain = optionChainResult.getOrThrow()
                    val events = eventsResult.getOrDefault(emptyList())

                    val contract = chain.contracts.firstOrNull {
                        kotlin.math.abs(it.strike - strike) < 0.01 &&
                                it.contractType.name.equals(type, ignoreCase = true)
                    }

                    if (contract != null) {
                        if (type.equals("PUT", ignoreCase = true)) {
                            val result = ScoringEngine.scoreCSPCandidate(
                                contract = contract,
                                underlying = underlying,
                                events = events,
                                config = config
                            )
                            _uiState.value = CandidateDetailUiState(
                                cspCandidate = result,
                                isSavedInWatchlist = isSaved
                            )
                        } else {
                            val result = ScoringEngine.scoreCCCandidate(
                                contract = contract,
                                underlying = underlying,
                                events = events,
                                shareCount = 100,
                                costBasis = underlying.price * 0.95,
                                config = config
                            )
                            _uiState.value = CandidateDetailUiState(
                                ccCandidate = result,
                                isSavedInWatchlist = isSaved
                            )
                        }
                    } else {
                        _uiState.value = CandidateDetailUiState(error = "Contract not found in option chain")
                    }
                } else {
                    _uiState.value = CandidateDetailUiState(error = "Failed to load market data for candidate")
                }
            } catch (e: Exception) {
                _uiState.value = CandidateDetailUiState(error = "Error loading candidate: ${e.message}")
            }
        }
    }

    fun openPaperPosition() {
        val csp = _uiState.value.cspCandidate
        val cc = _uiState.value.ccCandidate
        val contract = csp?.contract ?: cc?.contract ?: return
        val underlying = csp?.underlying ?: cc?.underlying ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSavingPosition = true, positionSaved = false)
            runCatching {
                paperPositionDao.insert(
                    PaperPositionEntity(
                        underlyingSymbol = underlying.symbol,
                        contractSymbol = contract.symbol,
                        strategy = if (csp != null) "CSP" else "CC",
                        optionType = contract.contractType.name,
                        strike = contract.strike,
                        expiration = contract.expiration.toEpochMilliseconds(),
                        quantity = 1,
                        entryCredit = contract.bid,
                        entryUnderlyingPrice = underlying.price,
                        entryDelta = contract.delta,
                        openedAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                    )
                )
            }.onSuccess {
                _uiState.value = _uiState.value.copy(isSavingPosition = false, positionSaved = true)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isSavingPosition = false, error = "Unable to open paper position: ${it.message}")
            }
        }
    }
}
