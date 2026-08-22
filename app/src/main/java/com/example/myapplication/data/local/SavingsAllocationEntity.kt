package com.example.myapplication.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "savings_allocations")
data class SavingsAllocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateKey: String,
    val category: String,
    val minutes: Int
)