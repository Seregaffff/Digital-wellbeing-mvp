package com.example.myapplication.ui.profile

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.TrackedAppEntity
import com.example.myapplication.data.preferences.UserPreferences
import com.example.myapplication.data.repository.UsageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Информация об установленном пользовательском приложении, доступном для выбора.
 */
data class InstalledAppUi(
    val packageName: String,
    val appName: String
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
    val editingAppId: Int? = null
) {
    val canAddApp: Boolean
        get() = trackedApps.size < MAX_TRACKED_APPS
}

private const val MAX_TRACKED_APPS = 3

class ProfileViewModel(
    private val applicationContext: Context,
    private val repository: UsageRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = _uiState.value.copy(userName = userPreferences.getUserName())
        loadTrackedApps()
    }

    fun loadTrackedApps() {
        viewModelScope.launch {
            val trackedApps = withContext(Dispatchers.IO) {
                repository.getTrackedApps()
            }

            _uiState.value = _uiState.value.copy(
                trackedApps = trackedApps,
                errorMessage = null
            )
        }
    }

    fun openAppPicker(replacingAppId: Int? = null) {
        if (replacingAppId == null && !_uiState.value.canAddApp) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "В бесплатной версии можно отслеживать максимум 3 приложения."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            val installedApps = withContext(Dispatchers.IO) {
                loadInstalledUserApps()
            }

            _uiState.value = _uiState.value.copy(
                installedApps = installedApps,
                isAppPickerVisible = true,
                replacingAppId = replacingAppId,
                isLoading = false
            )
        }
    }

    fun closeAppPicker() {
        _uiState.value = _uiState.value.copy(
            isAppPickerVisible = false,
            replacingAppId = null
        )
    }

    fun selectApp(app: InstalledAppUi) {
        viewModelScope.launch {
            val currentApps = repository.getTrackedApps()
            val replacingAppId = _uiState.value.replacingAppId

            if (currentApps.any { it.packageName == app.packageName && it.id != replacingAppId }) {
                _uiState.value = _uiState.value.copy(
                    isAppPickerVisible = false,
                    replacingAppId = null,
                    errorMessage = "Это приложение уже отслеживается."
                )
                return@launch
            }

            if (replacingAppId != null) {
                val currentApp = currentApps.firstOrNull { it.id == replacingAppId }

                if (currentApp == null) {
                    _uiState.value = _uiState.value.copy(
                        isAppPickerVisible = false,
                        replacingAppId = null,
                        errorMessage = "Не удалось найти приложение для замены."
                    )
                    return@launch
                }

                repository.updateApp(
                    currentApp.copy(
                        packageName = app.packageName,
                        appName = app.appName
                    )
                )
            } else {
                if (currentApps.size >= MAX_TRACKED_APPS) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "В бесплатной версии можно отслеживать максимум 3 приложения."
                    )
                    return@launch
                }

                repository.saveApp(
                    TrackedAppEntity(
                        packageName = app.packageName,
                        appName = app.appName,
                        dailyLimitMinutes = DEFAULT_DAILY_LIMIT_MINUTES
                    )
                )
            }

            _uiState.value = _uiState.value.copy(
                trackedApps = repository.getTrackedApps(),
                isAppPickerVisible = false,
                replacingAppId = null,
                errorMessage = null
            )
        }
    }

    fun deleteApp(app: TrackedAppEntity) {
        viewModelScope.launch {
            repository.deleteApp(app)
            _uiState.value = _uiState.value.copy(
                trackedApps = repository.getTrackedApps(),
                errorMessage = null
            )
        }
    }

    fun saveUserName(name: String) {
        val sanitized = name
            .filter { char ->
                char in 'A'..'Z' ||
                    char in 'a'..'z' ||
                    char in 'А'..'Я' ||
                    char in 'а'..'я' ||
                    char == 'Ё' ||
                    char == 'ё' ||
                    char == ' ' ||
                    char == '-'
            }
            .take(30)
            .trim()

        userPreferences.setUserName(sanitized)
        _uiState.value = _uiState.value.copy(userName = sanitized)
    }

    fun openLimitEditor(appId: Int) {
        _uiState.value = _uiState.value.copy(
            isLimitDialogVisible = true,
            editingAppId = appId
        )
    }

    fun closeLimitEditor() {
        _uiState.value = _uiState.value.copy(
            isLimitDialogVisible = false,
            editingAppId = null
        )
    }

    fun saveLimit(minutes: Int) {
        val appId = _uiState.value.editingAppId ?: return
        val safeMinutes = minutes.coerceIn(1, 24 * 60)
        viewModelScope.launch {
            val app = repository.getTrackedApps().firstOrNull { it.id == appId }
            if (app == null) {
                closeLimitEditor()
                return@launch
            }
            repository.updateApp(app.copy(dailyLimitMinutes = safeMinutes))
            _uiState.value = _uiState.value.copy(
                trackedApps = repository.getTrackedApps(),
                isLimitDialogVisible = false,
                editingAppId = null,
                errorMessage = null
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun loadInstalledUserApps(): List<InstalledAppUi> {
        val packageManager = applicationContext.packageManager
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }

        return packageManager
            .queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .asSequence()
            .mapNotNull { resolveInfo ->
                val appInfo = resolveInfo.activityInfo?.applicationInfo ?: return@mapNotNull null

                if (appInfo.packageName == applicationContext.packageName) {
                    return@mapNotNull null
                }

                val isSystemApp = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
                val isUpdatedSystemApp = appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0

                // YouTube can be delivered as a pre-installed system app on some devices.
                // It is still a normal user-facing, launchable app, so keep it selectable.
                val isUserFacingSystemApp = isUpdatedSystemApp ||
                    appInfo.packageName == YOUTUBE_PACKAGE

                if (isSystemApp && !isUserFacingSystemApp) {
                    return@mapNotNull null
                }

                InstalledAppUi(
                    packageName = appInfo.packageName,
                    appName = resolveInfo.loadLabel(packageManager).toString()
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.appName.lowercase() }
            .toList()
    }

    companion object {
        private const val DEFAULT_DAILY_LIMIT_MINUTES = 60
        private const val YOUTUBE_PACKAGE = "com.google.android.youtube"
    }
}

class ProfileViewModelFactory(
    private val context: Context,
    private val repository: UsageRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            return ProfileViewModel(
                applicationContext = context.applicationContext,
                repository = repository,
                userPreferences = UserPreferences(context.applicationContext)
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
