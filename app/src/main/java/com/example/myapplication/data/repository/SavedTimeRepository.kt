package com.example.myapplication.data.repository

import com.example.myapplication.data.local.DailyProgressDao
import com.example.myapplication.data.local.DailyProgressEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Stores and reads the persistent daily Saved Time history.
 *
 * The current day is kept as a live snapshot (finalized = false). Previous
 * days are recalculated from UsageStats before they become permanent history.
 */
class SavedTimeRepository(
    private val dailyProgressDao: DailyProgressDao,
    private val usageRepository: UsageRepository
) {

    suspend fun updateToday(): DailySavedTimeSummary {
        val today = Calendar.getInstance()
        val dateKey = dateKey(today)
        val apps = usageRepository.getTrackedApps()

        // Rebuild today's live snapshot from the current tracking settings.
        // This prevents Saved Time from accumulating after app/limit changes.
        dailyProgressDao.deleteLiveForDate(dateKey)

        val progress = apps.map { app ->
            val usedMinutes = usageRepository.getUsageMinutes(app.packageName)
            DailyProgressEntity(
                dateKey = dateKey,
                packageName = app.packageName,
                appName = app.appName,
                limitMinutes = app.dailyLimitMinutes,
                usedMinutes = usedMinutes,
                savedMinutes = (app.dailyLimitMinutes - usedMinutes).coerceAtLeast(0),
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
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        withContext(Dispatchers.IO) {
            for (progress in all) {
                if (progress.finalized || progress.dateKey >= todayKey) continue

                val parsedDate = dateFormat.parse(progress.dateKey) ?: continue
                val calendar = Calendar.getInstance().apply { time = parsedDate }
                val usedMinutes = usageRepository.getUsageMinutesForDate(progress.packageName, calendar)

                dailyProgressDao.upsert(
                    progress.copy(
                        usedMinutes = usedMinutes,
                        savedMinutes = (progress.limitMinutes - usedMinutes).coerceAtLeast(0),
                        completed = usedMinutes <= progress.limitMinutes,
                        finalized = true
                    )
                )
            }
        }
    }

    suspend fun getToday(): List<DailyProgressEntity> =
        dailyProgressDao.getForDate(dateKey(Calendar.getInstance()))

    suspend fun getTotalSavedTimeMinutes(): Int =
        dailyProgressDao.getTotalFinalizedSavedMinutes()

    suspend fun getSuccessfulDayCount(): Int =
        dailyProgressDao.getSuccessfulDayCount()

    suspend fun getHistory(from: Calendar, to: Calendar): List<DailyProgressEntity> =
        dailyProgressDao.getBetween(dateKey(from), dateKey(to))

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

    private fun dateKey(calendar: Calendar): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
}

data class DailySavedTimeSummary(
    val dateKey: String,
    val savedMinutes: Int,
    val usedMinutes: Int,
    val limitMinutes: Int,
    val completed: Boolean
)