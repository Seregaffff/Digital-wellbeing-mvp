package com.example.myapplication.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.GamificationRepository
import com.example.myapplication.data.repository.SavedTimeRepository
import com.example.myapplication.data.repository.SavingsCategory
import com.example.myapplication.data.repository.SavingsRepository
import com.example.myapplication.data.repository.UsageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val totalScreenTime: Int = 0,
    val apps: List<AppUsageUi> = emptyList(),
    val savedTimeMinutes: Int = 0,
    val totalSavedTimeMinutes: Int = 0,
    val availableSavingsMinutes: Int = 0,
    val booksSavingsMinutes: Int = 0,
    val moviesSavingsMinutes: Int = 0,
    val walksSavingsMinutes: Int = 0,
    val weeklyWalkMinutes: Int = 0,
    val currentStreak: Int = 0,
    val totalShields: Int = 0,
    val streakShields: Int = 0,
    val weeklyShield: Boolean = false,
    val achievementMessage: String? = null,
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
    private val repository: UsageRepository,
    private val savedTimeRepository: SavedTimeRepository,
    private val gamificationRepository: GamificationRepository,
    private val savingsRepository: SavingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadUsage()
    }

    fun loadUsage() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                savedTimeRepository.finalizePreviousDays()
                val gamification = gamificationRepository.sync()
                val todaySavedTime = savedTimeRepository.updateToday()
                val savings = savingsRepository.getSnapshot()

                val totalMinutes = repository.getTodayTotalUsageMinutes()
                val trackedApps = repository.getTrackedAppsUsage()
                val apps = trackedApps.map { usage ->
                    AppUsageUi(
                        app = TrackedAppUi(
                            packageName = usage.app.packageName,
                            appName = usage.app.appName,
                            dailyLimitMinutes = usage.app.dailyLimitMinutes
                        ),
                        usedMinutes = usage.usedMinutes
                    )
                }

                val achievementMessage = when {
                    gamification.newlyUnlockedAchievements.isNotEmpty() ->
                        "🏆 Новое достижение: ${gamification.newlyUnlockedAchievements.first()}"
                    gamification.shieldBurned ->
                        "🛡️ Лимит превышен — щит сгорел. Серия сохранена."
                    gamification.streakBroken ->
                        "Лимит превышен — щит закончился, серия прервана."
                    else -> null
                }

                _uiState.value = HomeUiState(
                    totalScreenTime = totalMinutes,
                    apps = apps,
                    savedTimeMinutes = todaySavedTime.savedMinutes,
                    totalSavedTimeMinutes = gamification.totalSavedMinutes,
                    availableSavingsMinutes = savings.availableMinutes,
                    booksSavingsMinutes = savings.booksMinutes,
                    moviesSavingsMinutes = savings.moviesMinutes,
                    walksSavingsMinutes = savings.walksMinutes,
                    weeklyWalkMinutes = savings.weeklyWalkMinutes,
                    currentStreak = gamification.currentStreak,
                    totalShields = gamification.totalShields,
                    streakShields = gamification.streakShields,
                    weeklyShield = gamification.weeklyShields > 0,
                    achievementMessage = achievementMessage,
                    isLoading = false
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun allocateSavings(category: SavingsCategory, minutes: Int = 30) {
        viewModelScope.launch {
            savingsRepository.allocate(category, minutes)
            loadUsage()
        }
    }
}