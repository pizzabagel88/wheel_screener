package com.wheelscreener.data.scheduler

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.wheelscreener.data.local.dao.SettingsDao
import com.wheelscreener.domain.model.StrategyConfig
import com.wheelscreener.domain.usecase.RunScanUseCase
import com.squareup.moshi.Moshi
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ScanWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val runScanUseCase: RunScanUseCase,
    private val notificationHelper: NotificationHelper,
    private val marketCalendarManager: MarketCalendarManager,
    private val settingsDao: SettingsDao,
    private val scanScheduler: ScanScheduler,
    private val moshi: Moshi
) : CoroutineWorker(context, workerParams) {
    
    private val configAdapter = moshi.adapter(StrategyConfig::class.java)
    
    override suspend fun doWork(): Result {
        var config: StrategyConfig? = null
        return try {
            val scanConfig = loadConfig()
            config = scanConfig
            if (!scanConfig.scanEnabled) {
                return Result.success()
            }
            
            val now = kotlinx.datetime.Clock.System.now()
            val isTradingDay = marketCalendarManager.isTradingDay(now)
            if (!isTradingDay) {
                return Result.success()
            }
            
            val results = runScanUseCase()
            val topCandidates = results.take(5).map { result ->
                "${result.underlying.symbol} ${result.contract.strike}P (Score: ${String.format("%.1f", result.scoreComponents.compositeScore)})"
            }
            
            if (scanConfig.notifyOnScanComplete) {
                notificationHelper.showScanCompleteNotification(results.size, topCandidates)
            }
            
            if (scanConfig.notifyOnHighQualityCandidates && results.isNotEmpty()) {
                val highQuality = results.filter { result ->
                    result.scoreComponents.compositeScore >= scanConfig.minScoreForNotification
                }
                if (highQuality.isNotEmpty()) {
                    val highQualityTexts = highQuality.take(5).map { result ->
                        "${result.underlying.symbol} ${result.contract.strike}P (Score: ${String.format("%.1f", result.scoreComponents.compositeScore)})"
                    }
                    notificationHelper.showHighQualityCandidatesNotification(highQualityTexts)
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        } finally {
            // Exact alarms are one-shot. Schedule the following run after every attempt.
            config?.let { scanScheduler.schedule(it) }
        }
    }
    
    private suspend fun loadConfig(): StrategyConfig {
        val entity = settingsDao.getSetting("strategy_config")
        return if (entity != null) {
            configAdapter.fromJson(entity.value) ?: StrategyConfig.default()
        } else {
            StrategyConfig.default()
        }
    }
}
