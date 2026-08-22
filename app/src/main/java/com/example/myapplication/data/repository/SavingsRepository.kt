package com.example.myapplication.data.repository

import com.example.myapplication.data.local.SavingsAllocationDao
import com.example.myapplication.data.local.SavingsAllocationEntity
import com.example.myapplication.data.preferences.UserPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class SavingsCategory(val key: String, val title: String, val description: String, val targetMinutes: Int) {
    BOOKS("books", "Книги", "1 страница ≈ 1,25 минуты", 375),
    MOVIES("movies", "Кино", "До «Зелёной мили» — 189 минут", 189),
    WALKS("walks", "Прогулки", "45 минут ≈ 3,5 км", 45)
}

data class SavingsSnapshot(
    val totalSavedMinutes: Int,
    val allocatedMinutes: Int,
    val availableMinutes: Int,
    val booksMinutes: Int,
    val moviesMinutes: Int,
    val walksMinutes: Int,
    val weeklyWalkMinutes: Int
)

class SavingsRepository(
    private val allocationDao: SavingsAllocationDao,
    private val savedTimeRepository: SavedTimeRepository,
    private val userPreferences: UserPreferences
) {

    suspend fun getSnapshot(): SavingsSnapshot {
        val totalSaved = savedTimeRepository.getTotalSavedTimeMinutes()
        val allocated = allocationDao.getTotalAllocatedMinutes()
        return SavingsSnapshot(
            totalSavedMinutes = totalSaved,
            allocatedMinutes = allocated,
            availableMinutes = (totalSaved - allocated).coerceAtLeast(0),
            booksMinutes = allocationDao.getAllocatedMinutes(SavingsCategory.BOOKS.key),
            moviesMinutes = allocationDao.getAllocatedMinutes(SavingsCategory.MOVIES.key),
            walksMinutes = allocationDao.getAllocatedMinutes(SavingsCategory.WALKS.key),
            weeklyWalkMinutes = allocationDao.getAllocatedBetweenForCategory(
                weekStartKey(),
                todayKey(),
                SavingsCategory.WALKS.key
            )
        )
    }

    suspend fun allocate(category: SavingsCategory, minutes: Int): Boolean {
        val safeMinutes = minutes.coerceIn(1, 24 * 60)
        val snapshot = getSnapshot()
        if (safeMinutes > snapshot.availableMinutes) return false

        allocationDao.insert(
            SavingsAllocationEntity(
                dateKey = todayKey(),
                category = category.key,
                minutes = safeMinutes
            )
        )

        if (category == SavingsCategory.WALKS && getSnapshot().weeklyWalkMinutes >= 180) {
            userPreferences.unlockAchievement("walks_weekly_challenge")
        }
        return true
    }

    private fun todayKey(): String = dateKey(Calendar.getInstance())

    private fun weekStartKey(): String {
        val calendar = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }
        return dateKey(calendar)
    }

    private fun dateKey(calendar: Calendar): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
}