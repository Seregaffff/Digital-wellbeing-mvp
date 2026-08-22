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

    companion object {
        private const val PREFS_NAME = "user_preferences"
        private const val KEY_USER_NAME = "user_name"
    }
}
