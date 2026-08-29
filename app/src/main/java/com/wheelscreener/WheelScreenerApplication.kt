package com.wheelscreener

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import com.wheelscreener.data.scheduler.PositionReminderScheduler

/**
 * Main application class with Hilt setup
 */
@HiltAndroidApp
class WheelScreenerApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var positionReminderScheduler: PositionReminderScheduler

    override fun onCreate() {
        super.onCreate()
        positionReminderScheduler.schedule()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
