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

    companion object {
        private const val PREFS_NAME = "user_preferences"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_FIRST_STEPS_ACHIEVEMENT = "achievement_first_steps"
    }
}
