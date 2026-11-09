package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.TrainingGoal
import com.example.domain.RacePredictionEngine

object GoalReminderNotificationHelper {

    private const val CHANNEL_ID = "strideapex_goal_reminders"
    private const val CHANNEL_NAME = "Race Goal & Pacing Reminders"

    fun ensureChannelCreated(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily training volume targets and race goal pacing reminders."
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun sendGoalPushReminder(
        context: Context,
        goal: TrainingGoal,
        currentWeeklyKm: Double
    ): Boolean {
        ensureChannelCreated(context)
        if (!hasNotificationPermission(context)) return false

        val targetTimeFormatted = RacePredictionEngine.formatDuration(goal.targetTimeSeconds)
        val remainingKm = (goal.weeklyMileageTargetKm - currentWeeklyKm).coerceAtLeast(0.0)
        val bodyText = if (remainingKm > 0.1) {
            "Goal: ${goal.title} ($targetTimeFormatted). You have ${"%.1f".format(remainingKm)} km left to hit your ${goal.weeklyMileageTargetKm} km weekly target!"
        } else {
            "Weekly target of ${goal.weeklyMileageTargetKm} km achieved! Stay sharp for ${goal.title} ($targetTimeFormatted)."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("StrideApex Goal Push • ${goal.targetDistanceCategory}")
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(goal.id.hashCode(), notification)
            true
        } catch (e: SecurityException) {
            false
        }
    }
}
