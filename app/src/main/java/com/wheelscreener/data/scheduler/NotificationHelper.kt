package com.wheelscreener.data.scheduler

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    companion object {
        const val CHANNEL_ID_SCAN = "scan_channel"
        const val CHANNEL_ID_CANDIDATES = "candidates_channel"
        const val NOTIFICATION_ID_SCAN_COMPLETE = 1001
        const val NOTIFICATION_ID_CANDIDATES = 1002
    }
    
    init {
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val scanChannel = NotificationChannel(
                CHANNEL_ID_SCAN,
                "Scan Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows scan completion status"
            }
            
            val candidatesChannel = NotificationChannel(
                CHANNEL_ID_CANDIDATES,
                "Top Candidates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows high-quality candidates found during scan"
            }
            
            notificationManager.createNotificationChannel(scanChannel)
            notificationManager.createNotificationChannel(candidatesChannel)
        }
    }
    
    fun showScanCompleteNotification(candidateCount: Int, topCandidates: List<String>) {
        val contentText = if (candidateCount > 0) {
            "Found $candidateCount candidates. Top: ${topCandidates.firstOrNull() ?: "None"}"
        } else {
            "Scan complete. No high-quality candidates found."
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SCAN)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Wheel Screener Scan Complete")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID_SCAN_COMPLETE, notification)
    }
    
    fun showHighQualityCandidatesNotification(candidates: List<String>) {
        if (candidates.isEmpty()) return
        
        val contentText = candidates.take(3).joinToString("\n")
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_CANDIDATES)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Top Wheel Candidates Found")
            .setContentText(candidates.first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID_CANDIDATES, notification)
    }
}
