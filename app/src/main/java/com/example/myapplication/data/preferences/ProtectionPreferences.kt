package com.example.myapplication.data.preferences

import android.content.Context

/** Stores the user's app-protection mode and temporary soft-block grace periods. */
class ProtectionPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var mode: ProtectionMode
        get() = when (prefs.getString(KEY_MODE, ProtectionMode.SOFT.name)) {
            ProtectionMode.HARD.name -> ProtectionMode.HARD
            ProtectionMode.NONE.name -> ProtectionMode.NONE
            else -> ProtectionMode.SOFT
        }
        set(value) {
            prefs.edit().putString(KEY_MODE, value.name).apply()
        }

    fun getSoftGraceUntil(packageName: String, dateKey: String): Long =
        prefs.getLong("$KEY_GRACE:$dateKey:$packageName", 0L)

    fun setSoftGraceUntil(packageName: String, dateKey: String, untilMillis: Long) {
        prefs.edit().putLong("$KEY_GRACE:$dateKey:$packageName", untilMillis).apply()
    }

    /** Returns true when the user has granted the 5-minute soft extension for this app/day. */
    fun hasSoftGrace(packageName: String, dateKey: String): Boolean =
        prefs.getLong("$KEY_GRACE:$dateKey:$packageName", 0L) > 0L

    companion object {
        private const val PREFS_NAME = "protection_preferences"
        private const val KEY_MODE = "mode"
        private const val KEY_GRACE = "soft_grace"
    }
}

enum class ProtectionMode {
    NONE,
    SOFT,
    HARD
}
