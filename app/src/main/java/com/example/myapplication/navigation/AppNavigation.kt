package com.example.myapplication.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.home.HomeScreen
import com.example.myapplication.ui.home.HomeViewModel
import com.example.myapplication.ui.profile.ProfileScreen
import com.example.myapplication.ui.profile.ProfileViewModel
import com.example.myapplication.ui.progress.ProgressScreen
import com.example.myapplication.ui.progress.ProgressViewModel
import com.example.myapplication.ui.statistics.StatisticsScreen
import com.example.myapplication.ui.statistics.StatisticsViewModel

private const val HOME = "home"
private const val STATISTICS = "statistics"
private const val PROGRESS = "progress"
private const val PROFILE = "profile"

@Composable
fun AppNavigation(
    homeViewModel: HomeViewModel,
    profileViewModel: ProfileViewModel,
    statisticsViewModel: StatisticsViewModel,
    progressViewModel: ProgressViewModel,
    onOpenUsageSettings: () -> Unit
) {
    val navController = rememberNavController()
    val profileState by profileViewModel.uiState.collectAsState()

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController, homeViewModel)
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = HOME,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(HOME) {
                HomeScreen(
                    viewModel = homeViewModel,
                    userName = profileState.userName,
                    onOpenUsageSettings = onOpenUsageSettings
                )
            }
            composable(STATISTICS) {
                StatisticsScreen(statisticsViewModel)
            }
            composable(PROGRESS) {
                ProgressScreen(progressViewModel)
            }
            composable(PROFILE) {
                ProfileScreen(viewModel = profileViewModel)
            }
        }
    }
}

@Composable
private fun BottomNavigationBar(
    navController: NavHostController,
    homeViewModel: HomeViewModel
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    LaunchedEffect(currentRoute) {
        if (currentRoute == HOME) {
            homeViewModel.loadUsage()
        }
    }

    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == HOME,
            onClick = {
                navController.navigate(HOME) {
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(Icons.Default.Home, contentDescription = "Главная") },
            label = { Text("Главная") }
        )

        NavigationBarItem(
            selected = currentRoute == STATISTICS,
            onClick = {
                navController.navigate(STATISTICS) {
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(Icons.Default.ShowChart, contentDescription = "Статистика") },
            label = { Text("Статистика") }
        )

        NavigationBarItem(
            selected = currentRoute == PROGRESS,
            onClick = {
                navController.navigate(PROGRESS) {
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(Icons.Default.Star, contentDescription = "Прогресс") },
            label = { Text("Прогресс") }
        )

        NavigationBarItem(
            selected = currentRoute == PROFILE,
            onClick = {
                navController.navigate(PROFILE) {
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(Icons.Default.Person, contentDescription = "Профиль") },
            label = { Text("Профиль") }
        )
    }
}
