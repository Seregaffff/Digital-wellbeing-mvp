package com.example.myapplication.data.preferences

import android.content.Context

class UserPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun getUserName(): String = preferences.getString(KEY_USER_NAME, "") ?: ""

    fun setUserName(name: String) {
        preferences.edit().putString(KEY_USER_NAME, name.trim()).apply()
    }

    fun isFirstStepsAchievementUnlocked(): Boolean =
        preferences.getBoolean(KEY_FIRST_STEPS_ACHIEVEMENT, false)

    fun unlockFirstStepsAchievement() {
        preferences.edit().putBoolean(KEY_FIRST_STEPS_ACHIEVEMENT, true).apply()
    }

    fun isAchievementUnlocked(key: String): Boolean =
        preferences.getBoolean("$KEY_ACHIEVEMENT_PREFIX$key", false)

    fun unlockAchievement(key: String) {
        preferences.edit().putBoolean("$KEY_ACHIEVEMENT_PREFIX$key", true).apply()
    }

    fun getCurrentStreak(): Int = preferences.getInt(KEY_CURRENT_STREAK, 0)
    fun setCurrentStreak(value: Int) = preferences.edit().putInt(KEY_CURRENT_STREAK, value).apply()

    fun getLongestStreak(): Int = preferences.getInt(KEY_LONGEST_STREAK, 0)
    fun setLongestStreak(value: Int) = preferences.edit().putInt(KEY_LONGEST_STREAK, value).apply()

    fun getLastProcessedStreakDate(): String? = preferences.getString(KEY_LAST_PROCESSED_STREAK_DATE, null)
    fun setLastProcessedStreakDate(value: String) = preferences.edit().putString(KEY_LAST_PROCESSED_STREAK_DATE, value).apply()

    fun getWeeklyShields(): Int = preferences.getInt(KEY_WEEKLY_SHIELDS, 0).coerceIn(0, 1)
    fun setWeeklyShields(value: Int) = preferences.edit().putInt(KEY_WEEKLY_SHIELDS, value.coerceIn(0, 1)).apply()

    fun getStreakShields(): Int = preferences.getInt(KEY_STREAK_SHIELDS, 0).coerceAtLeast(0)
    fun setStreakShields(value: Int) = preferences.edit().putInt(KEY_STREAK_SHIELDS, value.coerceAtLeast(0)).apply()
    fun addStreakShields(value: Int) = setStreakShields(getStreakShields() + value)

    fun getLastWeeklyShieldWeek(): String? = preferences.getString(KEY_LAST_WEEKLY_SHIELD_WEEK, null)
    fun setLastWeeklyShieldWeek(value: String) = preferences.edit().putString(KEY_LAST_WEEKLY_SHIELD_WEEK, value).apply()

    fun getLastStreakStatus(): String? = preferences.getString(KEY_LAST_STREAK_STATUS, null)
    fun setLastStreakStatus(value: String?) {
        preferences.edit().apply {
            if (value == null) remove(KEY_LAST_STREAK_STATUS) else putString(KEY_LAST_STREAK_STATUS, value)
        }.apply()
    }

    companion object {
        private const val PREFS_NAME = "user_preferences"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_FIRST_STEPS_ACHIEVEMENT = "achievement_first_steps"
        private const val KEY_ACHIEVEMENT_PREFIX = "achievement_"
        private const val KEY_CURRENT_STREAK = "current_streak"
        private const val KEY_LONGEST_STREAK = "longest_streak"
        private const val KEY_LAST_PROCESSED_STREAK_DATE = "last_processed_streak_date"
        private const val KEY_WEEKLY_SHIELDS = "weekly_shields"
        private const val KEY_STREAK_SHIELDS = "streak_shields"
        private const val KEY_LAST_WEEKLY_SHIELD_WEEK = "last_weekly_shield_week"
        private const val KEY_LAST_STREAK_STATUS = "last_streak_status"
    }
}
