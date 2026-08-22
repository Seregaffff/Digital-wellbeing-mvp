package com.example.myapplication.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface TrackedAppDao {

    @Query("SELECT * FROM tracked_apps WHERE enabled = 1")
    suspend fun getEnabledApps(): List<TrackedAppEntity>

    @Query("SELECT * FROM tracked_apps")
    suspend fun getAllApps(): List<TrackedAppEntity>

    @Insert
    suspend fun insertApp(app: TrackedAppEntity)

    @Update
    suspend fun updateApp(app: TrackedAppEntity)

    @Delete
    suspend fun deleteApp(app: TrackedAppEntity)

    @Query("DELETE FROM tracked_apps")
    suspend fun deleteAllApps()
}