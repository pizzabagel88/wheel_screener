package com.wheelscreener.data.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ScanReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent?) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val workRequest = OneTimeWorkRequestBuilder<ScanWorker>()
            .setConstraints(constraints)
            .addTag(ScanScheduler.SCAN_WORK_TAG)
            .build()
        
        WorkManager.getInstance(context).enqueueUniqueWork(
            ScanScheduler.IMMEDIATE_SCAN_WORK_NAME,
            androidx.work.ExistingWorkPolicy.KEEP,
            workRequest
        )
    }
}
