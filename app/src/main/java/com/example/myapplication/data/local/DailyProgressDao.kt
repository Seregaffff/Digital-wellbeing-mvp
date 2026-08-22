package com.example.myapplication.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DailyProgressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: DailyProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(progress: List<DailyProgressEntity>)

    @Query("SELECT * FROM daily_progress WHERE dateKey = :dateKey ORDER BY appName")
    suspend fun getForDate(dateKey: String): List<DailyProgressEntity>

    @Query("SELECT * FROM daily_progress WHERE dateKey = :dateKey AND packageName = :packageName LIMIT 1")
    suspend fun getForApp(dateKey: String, packageName: String): DailyProgressEntity?

    @Query("SELECT * FROM daily_progress ORDER BY dateKey DESC, appName")
    suspend fun getAll(): List<DailyProgressEntity>

    @Query("SELECT * FROM daily_progress WHERE dateKey >= :fromDateKey AND dateKey <= :toDateKey ORDER BY dateKey DESC, appName")
    suspend fun getBetween(fromDateKey: String, toDateKey: String): List<DailyProgressEntity>

    @Query("SELECT COALESCE(SUM(savedMinutes), 0) FROM daily_progress WHERE finalized = 1")
    suspend fun getTotalFinalizedSavedMinutes(): Int

    @Query("SELECT COUNT(DISTINCT dateKey) FROM daily_progress WHERE finalized = 1 AND completed = 1")
    suspend fun getSuccessfulDayCount(): Int
}