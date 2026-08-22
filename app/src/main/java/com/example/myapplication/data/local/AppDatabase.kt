package com.example.myapplication.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TrackedAppEntity::class,
        DailyProgressEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun trackedAppDao(): TrackedAppDao

    abstract fun dailyProgressDao(): DailyProgressDao

    companion object {

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS daily_progress (
                        dateKey TEXT NOT NULL,
                        packageName TEXT NOT NULL,
                        appName TEXT NOT NULL,
                        limitMinutes INTEGER NOT NULL,
                        usedMinutes INTEGER NOT NULL,
                        savedMinutes INTEGER NOT NULL,
                        completed INTEGER NOT NULL,
                        finalized INTEGER NOT NULL,
                        PRIMARY KEY(dateKey, packageName)
                    )
                    """.trimIndent()
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "digital_wellbeing_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance

                instance
            }
        }
    }
}