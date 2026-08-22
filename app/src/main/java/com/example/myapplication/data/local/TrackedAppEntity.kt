package com.example.myapplication.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracked_apps")
data class TrackedAppEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val packageName: String,

    val appName: String,

    val dailyLimitMinutes: Int,

    val enabled: Boolean = true
)