package com.example.myapplication.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.TrackedAppEntity
import com.example.myapplication.data.repository.DailyTrackedUsage
import com.example.myapplication.data.repository.UsageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

 data class WeeklyDayUi(
    val date: Long,
    val label: String,
    val totalMinutes: Int
)

data class AppWeeklyUsageUi(
    val app: TrackedAppEntity,
    val totalMinutes: Int,
    val averageMinutes: Int,
    val days: List<WeeklyDayUi>
)

data class StatisticsUiState(
    val apps: List<AppWeeklyUsageUi> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class StatisticsViewModel(
    private val repository: UsageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        loadStatistics()
    }

    fun loadStatistics() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val apps = repository.getTrackedApps()
                val weekly = repository.getWeeklyTrackedUsage()
                _uiState.value = StatisticsUiState(
                    apps = apps.map { app ->
                        val days = weekly.map { day ->
                            WeeklyDayUi(
                                date = day.date,
                                label = dayLabel(day.date),
                                totalMinutes = day.usageByPackage[app.packageName] ?: 0
                            )
                        }
                        val total = days.sumOf { it.totalMinutes }
                        AppWeeklyUsageUi(
                            app = app,
                            totalMinutes = total,
                            averageMinutes = if (days.isEmpty()) 0 else total / days.size,
                            days = days
                        )
                    },
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = StatisticsUiState(
                    isLoading = false,
                    errorMessage = e.message ?: "Не удалось загрузить статистику."
                )
            }
        }
    }

    private fun dayLabel(timestamp: Long): String {
        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
        return when (calendar.get(java.util.Calendar.DAY_OF_WEEK)) {
            java.util.Calendar.MONDAY -> "Пн"
            java.util.Calendar.TUESDAY -> "Вт"
            java.util.Calendar.WEDNESDAY -> "Ср"
            java.util.Calendar.THURSDAY -> "Чт"
            java.util.Calendar.FRIDAY -> "Пт"
            java.util.Calendar.SATURDAY -> "Сб"
            else -> "Вс"
        }
    }
}

class StatisticsViewModelFactory(
    private val repository: UsageRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatisticsViewModel::class.java)) {
            return StatisticsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
