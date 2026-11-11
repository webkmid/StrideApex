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
import com.example.domain.GoalTrainingPlanEngine
import com.example.domain.RacePredictionEngine
import com.example.domain.ScheduledTrainingSession

object GoalReminderNotificationHelper {

    private const val CHANNEL_ID = "strideapex_goal_reminders"
    private const val CHANNEL_NAME = "Race Goal & Pacing Reminders"
    private const val PREFS_NAME = "strideapex_training_schedule_prefs"

    fun ensureChannelCreated(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily training schedule alerts, session prescriptions, and race goal reminders."
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

        val meta = GoalTrainingPlanEngine.parseGoalMetadata(goal)
        val targetTimeFormatted = RacePredictionEngine.formatDuration(goal.targetTimeSeconds)
        val remainingKm = (goal.weeklyMileageTargetKm - currentWeeklyKm).coerceAtLeast(0.0)
        val bodyText = if (remainingKm > 0.1) {
            "Goal: ${meta.cleanTitle} (${meta.targetDateDisplay} • $targetTimeFormatted). Sisa ${"%.1f".format(remainingKm)} km untuk mencapai target ${goal.weeklyMileageTargetKm} km minggu ini!"
        } else {
            "Target mingguan ${goal.weeklyMileageTargetKm} km tercapai! Pertahankan performa menuju ${meta.cleanTitle} (${meta.targetDateDisplay})."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("StrideApex • ${meta.cleanTitle}")
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

    /**
     * Automatically sends a notification if today is a scheduled training day for the goal
     * and the session has not been completed yet (once per day per goal).
     */
    fun notifyIfTodayIsTrainingDay(
        context: Context,
        goal: TrainingGoal,
        todaySession: ScheduledTrainingSession,
        forceNotify: Boolean = false
    ): Boolean {
        ensureChannelCreated(context)
        if (!hasNotificationPermission(context)) return false

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val prefKey = "notified_${goal.id.ifBlank { goal.title }}_${todaySession.dateKey}"
        if (!forceNotify && prefs.getBoolean(prefKey, false)) {
            return false
        }

        val meta = GoalTrainingPlanEngine.parseGoalMetadata(goal)
        val title = "Jadwal Latihan Hari Ini (${todaySession.dayName}) • ${meta.cleanTitle}"
        val body = "Sesi: ${todaySession.sessionTitle} — ${todaySession.sessionDescription} Jangan sampai terlewat (Skipped)!"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify((goal.id + todaySession.dateKey).hashCode(), notification)
            prefs.edit().putBoolean(prefKey, true).apply()
            true
        } catch (e: SecurityException) {
            false
        }
    }
}
