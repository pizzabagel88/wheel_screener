package com.wheelscreener.data.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.squareup.moshi.Moshi
import com.wheelscreener.data.local.dao.SettingsDao
import com.wheelscreener.domain.model.StrategyConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Schedules one exact scan at a time, with periodic WorkManager as a fallback. */
@Singleton
class ScanScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDao: SettingsDao,
    moshi: Moshi
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val workManager = WorkManager.getInstance(context)
    private val configAdapter = moshi.adapter(StrategyConfig::class.java)

    fun schedule(config: StrategyConfig) {
        cancelScheduledWork()
        if (!config.scanEnabled) return

        if (canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calculateNextTriggerTime(config),
                createScanPendingIntent()
            )
        } else {
            scheduleWorkManagerFallback(config)
        }
    }

    suspend fun reschedule() {
        val entity = settingsDao.getSetting(CONFIG_KEY)
        val config = entity?.let { configAdapter.fromJson(it.value) } ?: StrategyConfig.default()
        schedule(config)
    }

    fun cancel() = cancelScheduledWork()

    private fun cancelScheduledWork() {
        alarmManager.cancel(createScanPendingIntent())
        workManager.cancelUniqueWork(SCAN_WORK_NAME)
    }

    private fun canScheduleExactAlarms(): Boolean = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> alarmManager.canScheduleExactAlarms()
        else -> true
    }

    private fun calculateNextTriggerTime(config: StrategyConfig): Long {
        val zone = java.time.ZoneId.of(config.scanTimeZone)
        val now = ZonedDateTime.now(zone)
        var date = now.toLocalDate()
        if (!date.atTime(config.scanHourOfDay, config.scanMinute).atZone(zone).isAfter(now)) {
            date = date.plusDays(1)
        }
        while (config.scanWeekdaysOnly && date.dayOfWeek.value >= 6) {
            date = date.plusDays(1)
        }
        return date.atTime(config.scanHourOfDay, config.scanMinute).atZone(zone).toInstant().toEpochMilli()
    }

    private fun scheduleWorkManagerFallback(config: StrategyConfig) {
        val delay = (calculateNextTriggerTime(config) - System.currentTimeMillis()).coerceAtLeast(0)
        val workRequest = PeriodicWorkRequestBuilder<ScanWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(SCAN_WORK_TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            SCAN_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    private fun createScanPendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        SCAN_REQUEST_CODE,
        Intent(context, ScanReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    companion object {
        const val CONFIG_KEY = "strategy_config"
        const val SCAN_WORK_NAME = "daily_scan_work"
        const val IMMEDIATE_SCAN_WORK_NAME = "alarm_scan_work"
        const val SCAN_WORK_TAG = "daily_scan"
        const val SCAN_REQUEST_CODE = 1001
    }
}
