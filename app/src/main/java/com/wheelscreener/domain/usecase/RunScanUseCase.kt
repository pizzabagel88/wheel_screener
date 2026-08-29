package com.wheelscreener.domain.usecase

import com.wheelscreener.data.local.dao.SettingsDao
import com.wheelscreener.data.local.dao.WatchlistDao
import com.wheelscreener.data.local.entity.WatchlistEntity
import com.wheelscreener.domain.scoring.CSPScoringResult
import com.wheelscreener.domain.model.StrategyConfig
import com.wheelscreener.domain.repository.MarketDataRepository
import com.wheelscreener.domain.scoring.ScoringEngine
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RunScanUseCase @Inject constructor(
    private val marketDataRepository: MarketDataRepository,
    private val watchlistDao: WatchlistDao,
    private val settingsDao: SettingsDao
) {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val configAdapter = moshi.adapter(StrategyConfig::class.java)

    suspend operator fun invoke(): List<CSPScoringResult> {
        val configEntity = settingsDao.getSetting("strategy_config")
        val config = configEntity?.let {
            configAdapter.fromJson(it.value) ?: StrategyConfig.default()
        } ?: StrategyConfig.default()

        val watchlist = watchlistDao.getActiveWatchlist().first()
        val scoredResults = mutableListOf<CSPScoringResult>()

        val scanSymbols = watchlist.take(4)
        for (item in scanSymbols) {
            val symbol = item.symbol
            val underlyingResult = marketDataRepository.getQuote(symbol)
            val optionChainResult = marketDataRepository.getOptionChain(symbol)
            val eventsResult = marketDataRepository.getUpcomingEvents(symbol)

            if (underlyingResult.isSuccess && optionChainResult.isSuccess) {
                val underlying = underlyingResult.getOrThrow()
                val optionChain = optionChainResult.getOrThrow()
                val events = eventsResult.getOrDefault(emptyList())

                for (contract in optionChain.contracts) {
                    if (contract.contractType == com.wheelscreener.domain.model.ContractType.PUT) {
                        val scoreResult = ScoringEngine.scoreCSPCandidate(
                            contract = contract,
                            underlying = underlying,
                            events = events,
                            config = config
                        )
                        scoredResults.add(scoreResult)
                    }
                }
            }
        }

        return ScoringEngine.rankCSPCandidates(scoredResults)
    }
}
