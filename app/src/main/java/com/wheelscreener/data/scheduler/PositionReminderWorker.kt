package com.wheelscreener.data.scheduler

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.wheelscreener.data.local.dao.PaperPositionDao
import com.wheelscreener.domain.model.PaperPositionAnalytics
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock

@HiltWorker
class PositionReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val paperPositionDao: PaperPositionDao,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = runCatching {
        val reminders = paperPositionDao.observeOpenPositions().first()
            .flatMap { PaperPositionAnalytics.reminders(it, Clock.System.now()) }
            .map { it.message }
        notificationHelper.showPositionReminders(reminders)
        Result.success()
    }.getOrElse { Result.retry() }
}
