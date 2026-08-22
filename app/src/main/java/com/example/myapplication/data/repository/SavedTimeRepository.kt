package com.example.myapplication.data.repository

import com.example.myapplication.data.local.DailyProgressEntity
import com.example.myapplication.data.local.DailyProgressDao
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Stores and reads the persistent daily Saved Time history.
 *
 * The current day is kept as a live snapshot (finalized = false). A previous
 * calendar day is finalized when it is first accessed after that day ended.
 */
class SavedTimeRepository(
    private val dailyProgressDao: DailyProgressDao,
    private val usageRepository: UsageRepository
) {

    suspend fun updateToday(): DailySavedTimeSummary {
        val today = Calendar.getInstance()
        val dateKey = dateKey(today)
        val apps = usageRepository.getTrackedApps()

        val progress = apps.map { app ->
            val usedMinutes = usageRepository.getUsageMinutes(app.packageName)
            val savedMinutes =
                (app.dailyLimitMinutes - usedMinutes).coerceAtLeast(0)

            DailyProgressEntity(
                dateKey = dateKey,
                packageName = app.packageName,
                appName = app.appName,
                limitMinutes = app.dailyLimitMinutes,
                usedMinutes = usedMinutes,
                savedMinutes = savedMinutes,
                completed = usedMinutes <= app.dailyLimitMinutes,
                finalized = false
            )
        }

        dailyProgressDao.upsertAll(progress)

        return summaryFor(dateKey)
    }

    suspend fun finalizePreviousDays() {
        val all = dailyProgressDao.getAll()
        val todayKey = dateKey(Calendar.getInstance())

        all.filter { it.dateKey < todayKey && !it.finalized }
            .forEach { progress ->
                dailyProgressDao.upsert(
                    progress.copy(finalized = true)
                )
            }
    }

    suspend fun getToday(): List<DailyProgressEntity> {
        return dailyProgressDao.getForDate(dateKey(Calendar.getInstance()))
    }

    suspend fun getTotalSavedTimeMinutes(): Int {
        return dailyProgressDao.getTotalFinalizedSavedMinutes()
    }

    suspend fun getSuccessfulDayCount(): Int {
        return dailyProgressDao.getSuccessfulDayCount()
    }

    suspend fun getHistory(from: Calendar, to: Calendar): List<DailyProgressEntity> {
        return dailyProgressDao.getBetween(
            dateKey(from),
            dateKey(to)
        )
    }

    private suspend fun summaryFor(dateKey: String): DailySavedTimeSummary {
        val rows = dailyProgressDao.getForDate(dateKey)
        return DailySavedTimeSummary(
            dateKey = dateKey,
            savedMinutes = rows.sumOf { it.savedMinutes },
            usedMinutes = rows.sumOf { it.usedMinutes },
            limitMinutes = rows.sumOf { it.limitMinutes },
            completed = rows.isNotEmpty() && rows.all { it.completed }
        )
    }

    private fun dateKey(calendar: Calendar): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
    }
}

data class DailySavedTimeSummary(
    val dateKey: String,
    val savedMinutes: Int,
    val usedMinutes: Int,
    val limitMinutes: Int,
    val completed: Boolean
)