package com.example.myapplication.data.local

import androidx.room.Entity

/**
 * Daily snapshot of a tracked application's screen-time goal.
 *
 * One row represents one application on one calendar day.
 * The daily limit is stored together with the result so changing the
 * current limit later does not rewrite the historical result.
 */
@Entity(
    tableName = "daily_progress",
    primaryKeys = ["dateKey", "packageName"]
)
data class DailyProgressEntity(
    /** Calendar date in the user's local timezone, formatted as yyyy-MM-dd. */
    val dateKey: String,
    val packageName: String,
    val appName: String,
    val limitMinutes: Int,
    val usedMinutes: Int,
    val savedMinutes: Int,
    val completed: Boolean,
    val finalized: Boolean = false
)