package com.example.myapplication.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SavingsAllocationDao {

    @Insert
    suspend fun insert(allocation: SavingsAllocationEntity)

    @Query("SELECT COALESCE(SUM(minutes), 0) FROM savings_allocations")
    suspend fun getTotalAllocatedMinutes(): Int

    @Query("SELECT COALESCE(SUM(minutes), 0) FROM savings_allocations WHERE category = :category")
    suspend fun getAllocatedMinutes(category: String): Int

    @Query("SELECT COALESCE(SUM(minutes), 0) FROM savings_allocations WHERE dateKey >= :fromDateKey AND dateKey <= :toDateKey")
    suspend fun getAllocatedBetween(fromDateKey: String, toDateKey: String): Int

    @Query("SELECT COALESCE(SUM(minutes), 0) FROM savings_allocations WHERE dateKey >= :fromDateKey AND dateKey <= :toDateKey AND category = :category")
    suspend fun getAllocatedBetweenForCategory(fromDateKey: String, toDateKey: String, category: String): Int
}