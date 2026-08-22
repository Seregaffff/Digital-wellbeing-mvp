package com.example.myapplication.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.preferences.UserPreferences
import com.example.myapplication.data.repository.GamificationRepository
import com.example.myapplication.data.repository.SavedTimeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ProgressMilestoneUi(
    val days: Int,
    val title: String,
    val unlocked: Boolean
)

data class ProgressUiState(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val weeklyShields: Int = 0,
    val streakShields: Int = 0,
    val totalSavedMinutes: Int = 0,
    val milestones: List<ProgressMilestoneUi> = emptyList(),
    val achievements: List<String> = emptyList(),
    val statusMessage: String? = null,
    val isLoading: Boolean = false
) {
    val totalShields: Int get() = weeklyShields + streakShields
}

class ProgressViewModel(
    private val savedTimeRepository: SavedTimeRepository,
    private val gamificationRepository: GamificationRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                withContext(Dispatchers.IO) { savedTimeRepository.finalizePreviousDays() }
                val gamification = withContext(Dispatchers.IO) { gamificationRepository.sync() }
                val milestones = listOf(
                    ProgressMilestoneUi(1, "Щит новичка", userPreferences.isAchievementUnlocked("streak_1")),
                    ProgressMilestoneUi(3, "Три дня контроля", userPreferences.isAchievementUnlocked("streak_3")),
                    ProgressMilestoneUi(7, "Неделя под контролем", userPreferences.isAchievementUnlocked("streak_7")),
                    ProgressMilestoneUi(14, "Две недели фокуса", userPreferences.isAchievementUnlocked("streak_14")),
                    ProgressMilestoneUi(30, "Месяц без перегруза", userPreferences.isAchievementUnlocked("streak_30"))
                )
                val status = when {
                    gamification.streakBroken -> "Лимит превышен — щитов не осталось, серия прервана."
                    gamification.shieldBurned -> "Лимит превышен — щит сгорел. Серия сохранена."
                    else -> userPreferences.getLastStreakStatus()
                }
                _uiState.value = ProgressUiState(
                    currentStreak = gamification.currentStreak,
                    longestStreak = userPreferences.getLongestStreak(),
                    weeklyShields = gamification.weeklyShields,
                    streakShields = gamification.streakShields,
                    totalSavedMinutes = gamification.totalSavedMinutes,
                    milestones = milestones,
                    achievements = gamification.newlyUnlockedAchievements,
                    statusMessage = status,
                    isLoading = false
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}

class ProgressViewModelFactory(
    private val savedTimeRepository: SavedTimeRepository,
    private val gamificationRepository: GamificationRepository,
    private val userPreferences: UserPreferences
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProgressViewModel::class.java)) {
            return ProgressViewModel(savedTimeRepository, gamificationRepository, userPreferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
