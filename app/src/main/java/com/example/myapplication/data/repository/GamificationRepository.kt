package com.example.myapplication.data.repository

import com.example.myapplication.data.local.DailyProgressDao
import com.example.myapplication.data.local.DailyProgressEntity
import com.example.myapplication.data.preferences.UserPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Stage 2 gamification engine.
 *
 * A finalized day is processed exactly once. A successful day increments the
 * streak. A failed day consumes a shield if one exists; otherwise the streak
 * resets. Streak milestone shields are stackable, while the weekly shield is
 * capped at one.
 */
class GamificationRepository(
    private val dailyProgressDao: DailyProgressDao,
    private val userPreferences: UserPreferences
) {

    suspend fun sync(): GamificationSyncResult {
        grantWeeklyShieldIfNeeded()

        val all = dailyProgressDao.getAll()
            .filter { it.finalized }
            .groupBy { it.dateKey }
            .toSortedMap()

        var currentStreak = userPreferences.getCurrentStreak()
        var longestStreak = userPreferences.getLongestStreak()
        var lastProcessedDate = userPreferences.getLastProcessedStreakDate()
        val newlyUnlocked = mutableListOf<String>()
        var shieldBurned = false
        var streakBroken = false

        for ((dateKey, rows) in all) {
            if (lastProcessedDate != null && dateKey <= lastProcessedDate) continue

            val successful = rows.isNotEmpty() && rows.all { it.completed }
            if (successful) {
                currentStreak += 1
                longestStreak = maxOf(longestStreak, currentStreak)

                STREAK_MILESTONES.forEach { milestone ->
                    if (currentStreak >= milestone.days && !userPreferences.isAchievementUnlocked(milestone.key)) {
                        userPreferences.unlockAchievement(milestone.key)
                        userPreferences.addStreakShields(1)
                        newlyUnlocked += milestone.title
                    }
                }
            } else {
                if (consumeShield()) {
                    shieldBurned = true
                } else {
                    currentStreak = 0
                    streakBroken = true
                }
            }

            lastProcessedDate = dateKey
        }

        userPreferences.setCurrentStreak(currentStreak)
        userPreferences.setLongestStreak(longestStreak)
        if (lastProcessedDate != null) {
            userPreferences.setLastProcessedStreakDate(lastProcessedDate)
        }

        val totalSavedMinutes = dailyProgressDao.getTotalFinalizedSavedMinutes()
        SAVED_TIME_MILESTONES.forEach { milestone ->
            if (totalSavedMinutes >= milestone.minutes && !userPreferences.isAchievementUnlocked(milestone.key)) {
                userPreferences.unlockAchievement(milestone.key)
                newlyUnlocked += milestone.title
            }
        }

        return GamificationSyncResult(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            weeklyShields = userPreferences.getWeeklyShields(),
            streakShields = userPreferences.getStreakShields(),
            totalSavedMinutes = totalSavedMinutes,
            newlyUnlockedAchievements = newlyUnlocked,
            shieldBurned = shieldBurned,
            streakBroken = streakBroken
        )
    }

    fun getAchievements(): List<AchievementDefinition> =
        listOf(
            AchievementDefinition(
                key = "first_steps",
                title = "Первые шаги",
                description = "Выберите приложение для отслеживания экранного времени и настройте для него дневной лимит."
            ),
            *STREAK_MILESTONES.map {
                AchievementDefinition(it.key, it.title, "Серия без превышения лимита: ${it.days} ${dayWord(it.days)}.")
            }.toTypedArray(),
            *SAVED_TIME_MILESTONES.map {
                AchievementDefinition(it.key, it.title, "Накоплено ${formatMinutes(it.minutes)} сэкономленного времени.")
            }.toTypedArray(),
            AchievementDefinition(
                key = "walks_weekly_challenge",
                title = "Маршрут на неделю",
                description = "Конвертируйте 3 часа за неделю в прогулки."
            )
        )

    fun isAchievementUnlocked(key: String): Boolean =
        userPreferences.isAchievementUnlocked(key)

    private fun consumeShield(): Boolean {
        val weekly = userPreferences.getWeeklyShields()
        if (weekly > 0) {
            userPreferences.setWeeklyShields(weekly - 1)
            return true
        }

        val streak = userPreferences.getStreakShields()
        if (streak > 0) {
            userPreferences.setStreakShields(streak - 1)
            return true
        }
        return false
    }

    private fun grantWeeklyShieldIfNeeded() {
        val weekKey = currentWeekKey()
        if (userPreferences.getLastWeeklyShieldWeek() != weekKey) {
            userPreferences.setLastWeeklyShieldWeek(weekKey)
            if (userPreferences.getWeeklyShields() == 0) {
                userPreferences.setWeeklyShields(1)
            }
        }
    }

    private fun currentWeekKey(): String {
        val calendar = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            minimalDaysInFirstWeek = 4
        }
        return "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.WEEK_OF_YEAR)}"
    }

    private fun formatMinutes(minutes: Int): String {
        val hours = minutes / 60
        val remaining = minutes % 60
        return when {
            hours > 0 && remaining > 0 -> "$hours ч $remaining мин"
            hours > 0 -> "$hours ч"
            else -> "$remaining мин"
        }
    }

    private fun dayWord(days: Int): String = when (days) {
        1 -> "день"
        2, 3, 4 -> "дня"
        else -> "дней"
    }

    companion object {
        private val STREAK_MILESTONES = listOf(
            StreakMilestone("streak_1", "Щит новичка", 1),
            StreakMilestone("streak_3", "Три дня контроля", 3),
            StreakMilestone("streak_7", "Неделя под контролем", 7),
            StreakMilestone("streak_14", "Две недели фокуса", 14),
            StreakMilestone("streak_30", "Месяц без перегруза", 30)
        )

        private val SAVED_TIME_MILESTONES = listOf(
            SavedTimeMilestone("saved_1h", "Первый возвращённый час", 60),
            SavedTimeMilestone("saved_5h", "Пять часов свободы", 300),
            SavedTimeMilestone("saved_10h", "Десять часов для себя", 600),
            SavedTimeMilestone("saved_25h", "День возвращённого времени", 1500),
            SavedTimeMilestone("saved_50h", "Мастер времени", 3000)
        )
    }
}

data class GamificationSyncResult(
    val currentStreak: Int,
    val longestStreak: Int,
    val weeklyShields: Int,
    val streakShields: Int,
    val totalSavedMinutes: Int,
    val newlyUnlockedAchievements: List<String>,
    val shieldBurned: Boolean,
    val streakBroken: Boolean
) {
    val totalShields: Int get() = weeklyShields + streakShields
}

data class AchievementDefinition(
    val key: String,
    val title: String,
    val description: String
)

private data class StreakMilestone(val key: String, val title: String, val days: Int)
private data class SavedTimeMilestone(val key: String, val title: String, val minutes: Int)
