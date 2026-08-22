package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.repository.SavedTimeRepository
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

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val database =
            AppDatabase.getInstance(
                applicationContext
            )

        val repository =
            UsageRepository(
                context = applicationContext,
                trackedAppDao =
                    database.trackedAppDao()
            )

        val savedTimeRepository =
            SavedTimeRepository(
                dailyProgressDao =
                    database.dailyProgressDao(),
                usageRepository =
                    repository
            )

        val factory =
            HomeViewModelFactory(
                repository = repository,
                savedTimeRepository = savedTimeRepository
            )

        val homeViewModel =
            ViewModelProvider(
                this,
                factory
            )[HomeViewModel::class.java]

        val profileFactory =
            ProfileViewModelFactory(
                context = applicationContext,
                repository = repository
            )

        val profileViewModel =
            ViewModelProvider(
                this,
                profileFactory
            )[ProfileViewModel::class.java]

        val statisticsViewModel =
            ViewModelProvider(
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

                        startActivity(
                            Intent(
                                Settings.ACTION_USAGE_ACCESS_SETTINGS
                            )
                        )
                    }
                )
            }
        }
    }
}