package com.example.myapplication.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.UsageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val totalScreenTime: Int = 0,
    val apps: List<AppUsageUi> = emptyList(),
    val savedTimeMinutes: Int = 0,
    val isLoading: Boolean = false
)

data class AppUsageUi(
    val app: TrackedAppUi,
    val usedMinutes: Int
)

data class TrackedAppUi(
    val packageName: String,
    val appName: String,
    val dailyLimitMinutes: Int
)

class HomeViewModel(
    private val repository: UsageRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(HomeUiState())

    val uiState: StateFlow<HomeUiState> =
        _uiState.asStateFlow()

    init {
        loadUsage()
    }

    fun loadUsage() {

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isLoading = true
                )

            try {

                val totalMinutes =
                    repository.getTodayTotalUsageMinutes()

                val trackedApps =
                    repository.getTrackedAppsUsage()

                val apps =
                    trackedApps.map { usage ->

                        AppUsageUi(
                            app = TrackedAppUi(
                                packageName =
                                    usage.app.packageName,

                                appName =
                                    usage.app.appName,

                                dailyLimitMinutes =
                                    usage.app.dailyLimitMinutes
                            ),

                            usedMinutes =
                                usage.usedMinutes
                        )
                    }

                val savedTimeMinutes = trackedApps.sumOf { usage ->
                    (usage.app.dailyLimitMinutes - usage.usedMinutes).coerceAtLeast(0)
                }

                _uiState.value =
                    HomeUiState(
                        totalScreenTime =
                            totalMinutes,

                        apps = apps,

                        savedTimeMinutes = savedTimeMinutes,

                        isLoading = false
                    )

            } catch (e: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false
                    )
            }
        }
    }
}