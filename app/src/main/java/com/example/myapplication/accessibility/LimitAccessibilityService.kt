package com.example.myapplication.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.preferences.ProtectionMode
import com.example.myapplication.data.preferences.ProtectionPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LimitAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var database: AppDatabase
    private lateinit var protectionPreferences: ProtectionPreferences

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        database = AppDatabase.getInstance(applicationContext)
        protectionPreferences = ProtectionPreferences(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        if (packageName == applicationContext.packageName) return
        if (!::database.isInitialized) return

        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) return

        scope.launch {
            val app = database.trackedAppDao().getEnabledApps()
                .firstOrNull { it.packageName == packageName }
                ?: return@launch

            val usageManager = getSystemService(USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
            val startOfDay = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
            val usage = usageManager.queryAndAggregateUsageStats(startOfDay, System.currentTimeMillis())
                .getOrDefault(packageName, null)?.totalTimeInForeground ?: 0L
            val usedMinutes = (usage / 1000L / 60L).toInt()
            if (usedMinutes < app.dailyLimitMinutes) return@launch

            val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val mode = protectionPreferences.mode
            val graceUntil = protectionPreferences.getSoftGraceUntil(packageName, dateKey)

            if (mode == ProtectionMode.SOFT && graceUntil > System.currentTimeMillis()) return@launch

            val intent = Intent(this@LimitAccessibilityService, LimitBlockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(LimitBlockActivity.EXTRA_PACKAGE_NAME, packageName)
                putExtra(LimitBlockActivity.EXTRA_APP_NAME, app.appName)
                putExtra(LimitBlockActivity.EXTRA_HARD_BLOCK, mode == ProtectionMode.HARD)
                putExtra(LimitBlockActivity.EXTRA_USED_MINUTES, usedMinutes)
                putExtra(LimitBlockActivity.EXTRA_LIMIT_MINUTES, app.dailyLimitMinutes)
            }
            startActivity(intent)
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance === this) instance = null
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        @Volatile
        private var instance: LimitAccessibilityService? = null

        fun performHomeAction() {
            instance?.performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }
}