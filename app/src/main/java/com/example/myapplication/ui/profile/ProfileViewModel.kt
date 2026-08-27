package com.example.myapplication.ui.profile

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.TrackedAppEntity
import com.example.myapplication.data.preferences.UserPreferences
import com.example.myapplication.data.repository.GamificationRepository
import com.example.myapplication.data.repository.UsageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class InstalledAppUi(val packageName: String, val appName: String)

data class ProfileAchievementUi(
    val key: String,
    val title: String,
    val description: String,
    val unlocked: Boolean
)

data class ProfileUiState(
    val trackedApps: List<TrackedAppEntity> = emptyList(),
    val installedApps: List<InstalledAppUi> = emptyList(),
    val isAppPickerVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val replacingAppId: Int? = null,
    val userName: String = "",
    val isLimitDialogVisible: Boolean = false,
    val editingAppId: Int? = null,
    val achievements: List<ProfileAchievementUi> = emptyList(),
    val currentStreak: Int = 0,
    val totalShields: Int = 0,
    val totalSavedMinutes: Int = 0,
    val showAllAchievements: Boolean = false,
    val recommendedLimitMinutes: Int? = null,
    val recommendationAverageMinutes: Int? = null,
    val recommendationDays: Int = 0
) {
    val canAddApp: Boolean get() = trackedApps.size < MAX_TRACKED_APPS
}

private const val MAX_TRACKED_APPS = 3
private const val MAX_DAILY_LIMIT_MINUTES = 12 * 60

class ProfileViewModel(
    private val applicationContext: Context,
    private val repository: UsageRepository,
    private val userPreferences: UserPreferences,
    private val gamificationRepository: GamificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = _uiState.value.copy(userName = userPreferences.getUserName())
        loadProfileData()
    }

    fun loadProfileData() {
        viewModelScope.launch {
            val gamification = withContext(Dispatchers.IO) { gamificationRepository.sync() }
            val trackedApps = withContext(Dispatchers.IO) { repository.getTrackedApps() }
            val achievements = gamificationRepository.getAchievements().map { definition ->
                val unlocked = if (definition.key == "first_steps") {
                    userPreferences.isFirstStepsAchievementUnlocked()
                } else {
                    gamificationRepository.isAchievementUnlocked(definition.key)
                }
                ProfileAchievementUi(definition.key, definition.title, definition.description, unlocked)
            }

            _uiState.value = _uiState.value.copy(
                trackedApps = trackedApps,
                achievements = achievements,
                currentStreak = gamification.currentStreak,
                totalShields = gamification.totalShields,
                totalSavedMinutes = gamification.totalSavedMinutes,
                errorMessage = null
            )
        }
    }

    fun loadTrackedApps() = loadProfileData()

    fun openAppPicker(replacingAppId: Int? = null) {
        if (replacingAppId == null && !_uiState.value.canAddApp) {
            _uiState.value = _uiState.value.copy(errorMessage = "В бесплатной версии можно отслеживать максимум 3 приложения.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val installedApps = withContext(Dispatchers.IO) { loadInstalledUserApps() }
            _uiState.value = _uiState.value.copy(
                installedApps = installedApps,
                isAppPickerVisible = true,
                replacingAppId = replacingAppId,
                isLoading = false
            )
        }
    }

    fun closeAppPicker() {
        _uiState.value = _uiState.value.copy(isAppPickerVisible = false, replacingAppId = null)
    }

    fun selectApp(app: InstalledAppUi) {
        viewModelScope.launch {
            val currentApps = repository.getTrackedApps()
            val replacingAppId = _uiState.value.replacingAppId
            if (currentApps.any { it.packageName == app.packageName && it.id != replacingAppId }) {
                _uiState.value = _uiState.value.copy(isAppPickerVisible = false, replacingAppId = null, errorMessage = "Это приложение уже отслеживается.")
                return@launch
            }

            if (replacingAppId != null) {
                val currentApp = currentApps.firstOrNull { it.id == replacingAppId }
                if (currentApp == null) {
                    _uiState.value = _uiState.value.copy(isAppPickerVisible = false, replacingAppId = null, errorMessage = "Не удалось найти приложение для замены.")
                    return@launch
                }
                repository.updateApp(currentApp.copy(packageName = app.packageName, appName = app.appName))
            } else {
                if (currentApps.size >= MAX_TRACKED_APPS) {
                    _uiState.value = _uiState.value.copy(errorMessage = "В бесплатной версии можно отслеживать максимум 3 приложения.")
                    return@launch
                }
                repository.saveApp(TrackedAppEntity(packageName = app.packageName, appName = app.appName, dailyLimitMinutes = DEFAULT_DAILY_LIMIT_MINUTES))
            }

            _uiState.value = _uiState.value.copy(trackedApps = repository.getTrackedApps(), isAppPickerVisible = false, replacingAppId = null, errorMessage = null)
        }
    }

    fun deleteApp(app: TrackedAppEntity) {
        viewModelScope.launch {
            repository.deleteApp(app)
            _uiState.value = _uiState.value.copy(trackedApps = repository.getTrackedApps(), errorMessage = null)
        }
    }

    fun saveUserName(name: String) {
        val sanitized = name.filter { char ->
            char in 'A'..'Z' || char in 'a'..'z' || char in 'А'..'Я' || char in 'а'..'я' || char == 'Ё' || char == 'ё' || char == ' ' || char == '-'
        }.take(30).trim()
        userPreferences.setUserName(sanitized)
        _uiState.value = _uiState.value.copy(userName = sanitized)
    }

    fun openLimitEditor(appId: Int) {
        viewModelScope.launch {
            val recommendation = calculateRecommendation(appId)
            _uiState.value = _uiState.value.copy(
                isLimitDialogVisible = true,
                editingAppId = appId,
                recommendedLimitMinutes = recommendation?.recommendedMinutes,
                recommendationAverageMinutes = recommendation?.averageMinutes,
                recommendationDays = recommendation?.days ?: 0
            )
        }
    }

    fun closeLimitEditor() {
        _uiState.value = _uiState.value.copy(
            isLimitDialogVisible = false,
            editingAppId = null,
            recommendedLimitMinutes = null,
            recommendationAverageMinutes = null,
            recommendationDays = 0
        )
    }

    fun saveLimit(minutes: Int) {
        val appId = _uiState.value.editingAppId ?: return
        val safeMinutes = minutes.coerceIn(1, MAX_DAILY_LIMIT_MINUTES)
        viewModelScope.launch {
            val app = repository.getTrackedApps().firstOrNull { it.id == appId }
            if (app == null) {
                closeLimitEditor()
                return@launch
            }
            repository.updateApp(app.copy(dailyLimitMinutes = safeMinutes))
            userPreferences.unlockFirstStepsAchievement()
            closeLimitEditor()
            loadProfileData()
        }
    }

    fun showAllAchievements() { _uiState.value = _uiState.value.copy(showAllAchievements = true) }
    fun hideAllAchievements() { _uiState.value = _uiState.value.copy(showAllAchievements = false) }
    fun clearError() { _uiState.value = _uiState.value.copy(errorMessage = null) }

    private suspend fun calculateRecommendation(appId: Int): Recommendation? = withContext(Dispatchers.IO) {
        val app = repository.getTrackedApps().firstOrNull { it.id == appId } ?: return@withContext null
        val week = repository.getWeeklyTrackedUsage()
        val values = week.mapNotNull { day -> day.usageByPackage[app.packageName] }.filter { it > 0 }
        if (values.size < 3) return@withContext null

        val average = values.average().toInt().coerceAtLeast(1)
        val reduction = if (values.size >= 7) 0.20 else 0.15
        val recommended = (average * (1.0 - reduction)).toInt().coerceIn(1, MAX_DAILY_LIMIT_MINUTES)
        Recommendation(average, recommended, values.size)
    }

    private fun loadInstalledUserApps(): List<InstalledAppUi> {
        val packageManager = applicationContext.packageManager
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply { addCategory(android.content.Intent.CATEGORY_LAUNCHER) }
        return packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .asSequence()
            .mapNotNull { resolveInfo ->
                val appInfo = resolveInfo.activityInfo?.applicationInfo ?: return@mapNotNull null
                if (appInfo.packageName == applicationContext.packageName) return@mapNotNull null
                val isSystemApp = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
                val isUpdatedSystemApp = appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
                val isUserFacingSystemApp = isUpdatedSystemApp || appInfo.packageName == YOUTUBE_PACKAGE
                if (isSystemApp && !isUserFacingSystemApp) return@mapNotNull null
                InstalledAppUi(appInfo.packageName, resolveInfo.loadLabel(packageManager).toString())
            }
            .distinctBy { it.packageName }
            .sortedBy { it.appName.lowercase() }
            .toList()
    }

    private data class Recommendation(val averageMinutes: Int, val recommendedMinutes: Int, val days: Int)

    companion object {
        private const val DEFAULT_DAILY_LIMIT_MINUTES = 60
        private const val YOUTUBE_PACKAGE = "com.google.android.youtube"
    }
}

class ProfileViewModelFactory(
    private val context: Context,
    private val repository: UsageRepository,
    private val gamificationRepository: GamificationRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            return ProfileViewModel(
                applicationContext = context.applicationContext,
                repository = repository,
                userPreferences = UserPreferences(context.applicationContext),
                gamificationRepository = gamificationRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
