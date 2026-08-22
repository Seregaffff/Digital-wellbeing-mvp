package com.example.myapplication.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.repository.UsageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class LimitNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                if (android.os.Build.VERSION.SDK_INT >= 33 &&
                    appContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) return@launch

                val database = AppDatabase.getInstance(appContext)
                val usageRepository = UsageRepository(appContext, database.trackedAppDao())
                val trackedApps = usageRepository.getTrackedApps()
                val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
                val preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

                trackedApps.forEach { app ->
                    val used = usageRepository.getUsageMinutes(app.packageName)
                    val remaining = app.dailyLimitMinutes - used
                    val threshold = when {
                        used >= app.dailyLimitMinutes -> 0
                        remaining <= 5 -> 5
                        remaining <= 15 -> 15
                        else -> null
                    } ?: return@forEach

                    val key = "$dateKey|${app.packageName}|${app.dailyLimitMinutes}|$threshold"
                    if (preferences.getBoolean(key, false)) return@forEach

                    val title: String
                    val text: String
                    when (threshold) {
                        15 -> {
                            title = "⏳ ${app.appName}: осталось 15 минут"
                            text = "До дневного лимита осталось около 15 минут."
                        }
                        5 -> {
                            title = "⚠️ ${app.appName}: осталось 5 минут"
                            text = "До дневного лимита осталось около 5 минут."
                        }
                        else -> {
                            title = "🛑 ${app.appName}: лимит достигнут"
                            text = "Дневной лимит достигнут или превышен."
                        }
                    }

                    showNotification(appContext, app.packageName.hashCode() + threshold, title, text)
                    preferences.edit().putBoolean(key, true).apply()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, id: Int, title: String, text: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Лимиты приложений",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Напоминания о приближении к дневному лимиту экранного времени"
                }
            )
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            id,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(id, notification)
    }

    companion object {
        private const val CHANNEL_ID = "screen_time_limits"
        private const val PREFS_NAME = "limit_notifications"
    }
}
