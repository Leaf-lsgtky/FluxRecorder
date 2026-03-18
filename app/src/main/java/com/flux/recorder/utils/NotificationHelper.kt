package com.flux.recorder.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.flux.recorder.MainActivity
import com.flux.recorder.R
import com.flux.recorder.service.RecorderService

class NotificationHelper(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ID = "recording_channel"
        const val NOTIFICATION_ID = 1001
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun createRecordingNotification(
        durationMs: Long,
        isPaused: Boolean = false
    ): android.app.Notification {
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context, 0, contentIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val timeStr = formatDuration(durationMs)
        val title = if (isPaused) {
            context.getString(R.string.notification_paused_title, timeStr)
        } else {
            context.getString(R.string.notification_recording_title, timeStr)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setSmallIcon(R.drawable.ic_record)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setColor(0xFF000000.toInt())
            .setColorized(true)

        // Pause / Resume action
        if (isPaused) {
            val resumeIntent = Intent(context, RecorderService::class.java).apply {
                action = RecorderService.ACTION_RESUME_RECORDING
            }
            val resumePending = PendingIntent.getService(
                context, 1, resumeIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(R.drawable.ic_resume, context.getString(R.string.action_resume), resumePending)
        } else {
            val pauseIntent = Intent(context, RecorderService::class.java).apply {
                action = RecorderService.ACTION_PAUSE_RECORDING
            }
            val pausePending = PendingIntent.getService(
                context, 1, pauseIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(R.drawable.ic_pause, context.getString(R.string.action_pause), pausePending)
        }

        // Stop action
        val stopIntent = Intent(context, RecorderService::class.java).apply {
            action = RecorderService.ACTION_STOP_RECORDING
        }
        val stopPending = PendingIntent.getService(
            context, 2, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        builder.addAction(R.drawable.ic_stop, context.getString(R.string.action_stop), stopPending)

        return builder.build()
    }

    fun updateNotification(notification: android.app.Notification) {
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}
