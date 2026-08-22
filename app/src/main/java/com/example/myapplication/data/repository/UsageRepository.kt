package com.example.myapplication.data.repository

import android.app.usage.UsageStatsManager
import android.content.Context
import com.example.myapplication.data.local.TrackedAppDao
import com.example.myapplication.data.local.TrackedAppEntity
import java.util.Calendar

data class TrackedAppUsage(
    val app: TrackedAppEntity,
    val usedMinutes: Int
)

data class DailyTrackedUsage(
    val date: Long,
    val usageByPackage: Map<String, Int>
)

class UsageRepository(
    private val context: Context,
    private val trackedAppDao: TrackedAppDao
) {

    suspend fun getTrackedApps(): List<TrackedAppEntity> {
        return trackedAppDao.getEnabledApps()
    }

    suspend fun saveApp(
        app: TrackedAppEntity
    ) {
        trackedAppDao.insertApp(app)
    }

    suspend fun updateApp(
        app: TrackedAppEntity
    ) {
        trackedAppDao.updateApp(app)
    }

    suspend fun deleteApp(
        app: TrackedAppEntity
    ) {
        trackedAppDao.deleteApp(app)
    }

    /**
     * Возвращает экранное время конкретного приложения
     * с начала текущего дня.
     */
    fun getUsageMinutes(
        packageName: String
    ): Int {

        val usageStatsManager =
            context.getSystemService(
                Context.USAGE_STATS_SERVICE
            ) as UsageStatsManager

        val startTime = getStartOfToday()

        val endTime =
            System.currentTimeMillis()

        val usageStats =
            usageStatsManager.queryAndAggregateUsageStats(
                startTime,
                endTime
            )

        val milliseconds =
            usageStats[packageName]
                ?.totalTimeInForeground
                ?: 0L

        return (
                milliseconds / 1000 / 60
                ).toInt()
    }

    /**
     * Возвращает общее экранное время устройства
     * с начала текущего дня.
     */
    fun getTodayTotalUsageMinutes(): Int {

        val usageStatsManager =
            context.getSystemService(
                Context.USAGE_STATS_SERVICE
            ) as UsageStatsManager

        val startTime = getStartOfToday()

        val endTime =
            System.currentTimeMillis()

        val usageStats =
            usageStatsManager.queryAndAggregateUsageStats(
                startTime,
                endTime
            )

        val totalMilliseconds =
            usageStats.values.sumOf {
                it.totalTimeInForeground
            }

        return (
                totalMilliseconds / 1000 / 60
                ).toInt()
    }

    /**
     * Возвращает использование всех отслеживаемых
     * приложений за сегодня.
     */
    suspend fun getTrackedAppsUsage(): List<TrackedAppUsage> {

        val trackedApps =
            getTrackedApps()

        return trackedApps.map { app ->

            TrackedAppUsage(
                app = app,
                usedMinutes =
                    getUsageMinutes(
                        app.packageName
                    )
            )
        }
    }

    /**
     * Возвращает экранное время конкретного приложения за указанную дату.
     */
    fun getUsageMinutesForDate(
        packageName: String,
        date: Calendar
    ): Int {
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val start = (date.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val end = (date.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val usageStats = usageStatsManager.queryAndAggregateUsageStats(start, end)
        val milliseconds = usageStats[packageName]?.totalTimeInForeground ?: 0L
        return (milliseconds / 1000L / 60L).toInt()
    }

    /**
     * Возвращает использование отслеживаемых приложений за последние 7 календарных дней,
     * включая сегодня. Дни идут от самого старого к текущему.
     */
    suspend fun getWeeklyTrackedUsage(): List<DailyTrackedUsage> {
        val trackedApps = getTrackedApps()
        val today = Calendar.getInstance()
        val result = mutableListOf<DailyTrackedUsage>()

        for (offset in 6 downTo 0) {
            val date = (today.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, -offset)
            }
            val values = trackedApps.associate { app ->
                app.packageName to getUsageMinutesForDate(app.packageName, date)
            }
            result += DailyTrackedUsage(
                date = date.timeInMillis,
                usageByPackage = values
            )
        }

        return result
    }

    /**
     * Начало текущего дня.
     */
    private fun getStartOfToday(): Long {

        val calendar =
            Calendar.getInstance()

        calendar.set(
            Calendar.HOUR_OF_DAY,
            0
        )

        calendar.set(
            Calendar.MINUTE,
            0
        )

        calendar.set(
            Calendar.SECOND,
            0
        )

        calendar.set(
            Calendar.MILLISECOND,
            0
        )

        return calendar.timeInMillis
    }
}