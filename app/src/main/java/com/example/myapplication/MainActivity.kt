package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.preferences.UserPreferences
import com.example.myapplication.data.repository.GamificationRepository
import com.example.myapplication.data.repository.SavedTimeRepository
import com.example.myapplication.data.repository.SavingsRepository
import com.example.myapplication.data.repository.UsageRepository
import com.example.myapplication.navigation.AppNavigation
import com.example.myapplication.ui.home.HomeViewModel
import com.example.myapplication.ui.home.HomeViewModelFactory
import com.example.myapplication.ui.profile.ProfileViewModel
import com.example.myapplication.ui.profile.ProfileViewModelFactory
import com.example.myapplication.ui.statistics.StatisticsViewModel
import com.example.myapplication.ui.statistics.StatisticsViewModelFactory
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)
        val repository = UsageRepository(
            context = applicationContext,
            trackedAppDao = database.trackedAppDao()
        )
        val savedTimeRepository = SavedTimeRepository(
            dailyProgressDao = database.dailyProgressDao(),
            usageRepository = repository
        )
        val gamificationRepository = GamificationRepository(
            dailyProgressDao = database.dailyProgressDao(),
            userPreferences = UserPreferences(applicationContext)
        )
        val savingsRepository = SavingsRepository(
            allocationDao = database.savingsAllocationDao(),
            savedTimeRepository = savedTimeRepository
        )

        val homeViewModel = ViewModelProvider(
            this,
            HomeViewModelFactory(
                repository = repository,
                savedTimeRepository = savedTimeRepository,
                gamificationRepository = gamificationRepository,
                savingsRepository = savingsRepository
            )
        )[HomeViewModel::class.java]

        val profileViewModel = ViewModelProvider(
            this,
            ProfileViewModelFactory(
                context = applicationContext,
                repository = repository,
                gamificationRepository = gamificationRepository
            )
        )[ProfileViewModel::class.java]

        val statisticsViewModel = ViewModelProvider(
            this,
            StatisticsViewModelFactory(repository)
        )[StatisticsViewModel::class.java]

        setContent {
            MyApplicationTheme {
                AppNavigation(
                    homeViewModel = homeViewModel,
                    profileViewModel = profileViewModel,
                    statisticsViewModel = statisticsViewModel,
                    onOpenUsageSettings = {
                        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }
                )
            }
        }
    }
}