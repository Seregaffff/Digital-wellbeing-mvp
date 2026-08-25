package com.example.myapplication.ui.profile

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.accessibility.LimitAccessibilityService
import com.example.myapplication.data.local.TrackedAppEntity
import com.example.myapplication.data.preferences.ProtectionMode
import com.example.myapplication.data.preferences.ProtectionPreferences

@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var nameInput by remember(uiState.userName) { mutableStateOf(uiState.userName) }
    val context = LocalContext.current
    val protectionPreferences = remember { ProtectionPreferences(context) }
    var protectionMode by remember { mutableStateOf(protectionPreferences.mode) }
    var accessibilityEnabled by remember { mutableStateOf(isLimitAccessibilityEnabled(context)) }

    LaunchedEffect(Unit) {
        viewModel.loadProfileData()
        accessibilityEnabled = isLimitAccessibilityEnabled(context)
    }

    val visibleAchievements = uiState.achievements.take(3)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 20.dp, bottom = 32.dp)
    ) {
        item {
            Text("Мой профиль", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        item { Text("Имя", fontWeight = FontWeight.SemiBold) }
        item {
            OutlinedTextField(
                value = nameInput,
                onValueChange = {
                    nameInput = it.filter { char ->
                        char in 'A'..'Z' || char in 'a'..'z' || char in 'А'..'Я' || char in 'а'..'я' ||
                            char == 'Ё' || char == 'ё' || char == ' ' || char == '-'
                    }.take(30)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Как к тебе обращаться?") },
                supportingText = { Text("До 30 символов: буквы, пробел и дефис") },
                trailingIcon = {
                    TextButton(
                        onClick = { viewModel.saveUserName(nameInput) },
                        enabled = nameInput.trim() != uiState.userName
                    ) { Text("Сохранить") }
                }
            )
        }
        item { Text("Email", style = MaterialTheme.typography.bodyMedium) }

        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Защита лимитов", fontWeight = FontWeight.Bold)
                    Text(
                        "После достижения лимита приложение будет показывать экран блокировки.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        if (accessibilityEnabled) "Служба доступности включена" else "Служба доступности не включена",
                        color = if (accessibilityEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                protectionMode = ProtectionMode.SOFT
                                protectionPreferences.mode = ProtectionMode.SOFT
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Мягкая") }
                        OutlinedButton(
                            onClick = {
                                protectionMode = ProtectionMode.HARD
                                protectionPreferences.mode = ProtectionMode.HARD
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Жёсткая") }
                    }
                    Text(
                        if (protectionMode == ProtectionMode.SOFT) {
                            "Мягкая: после лимита можно один раз продолжить на 5 минут."
                        } else {
                            "Жёсткая: после лимита продолжение недоступно до следующего дня."
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (accessibilityEnabled) "Открыть настройки доступности" else "Включить защиту")
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Мои приложения", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Отслеживается ${uiState.trackedApps.size} из 3", style = MaterialTheme.typography.bodySmall)
                }
                Button(onClick = viewModel::openAppPicker, enabled = uiState.canAddApp) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Добавить")
                }
            }
        }

        if (uiState.trackedApps.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Text("Пока нет отслеживаемых приложений. Добавьте до 3 приложений.", modifier = Modifier.padding(16.dp))
                }
            }
        } else {
            items(uiState.trackedApps, key = { it.id }) { app ->
                TrackedAppCard(
                    app = app,
                    onLimit = { viewModel.openLimitEditor(app.id) },
                    onReplace = { viewModel.openAppPicker(app.id) },
                    onDelete = { viewModel.deleteApp(app) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Мои достижения", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }

        items(visibleAchievements, key = { it.key }) { achievement ->
            AchievementCard(achievement)
        }

        if (uiState.achievements.size > 3) {
            item {
                TextButton(onClick = viewModel::showAllAchievements, modifier = Modifier.fillMaxWidth()) {
                    Text("Посмотреть все")
                }
            }
        }
    }

    if (uiState.showAllAchievements) {
        AlertDialog(
            onDismissRequest = viewModel::hideAllAchievements,
            title = { Text("Все достижения") },
            text = {
                LazyColumn(
                    modifier = Modifier.height(420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.achievements, key = { it.key }) { achievement ->
                        AchievementCard(achievement)
                    }
                }
            },
            confirmButton = { TextButton(onClick = viewModel::hideAllAchievements) { Text("Закрыть") } }
        )
    }

    if (uiState.isAppPickerVisible) {
        AppPickerDialog(uiState.installedApps, viewModel::closeAppPicker, viewModel::selectApp)
    }

    if (uiState.isLimitDialogVisible) {
        val app = uiState.trackedApps.firstOrNull { it.id == uiState.editingAppId }
        if (app != null) {
            LimitDialog(app, viewModel::closeLimitEditor, viewModel::saveLimit)
        }
    }

    uiState.errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            title = { Text("Не удалось выполнить действие") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = viewModel::clearError) { Text("OK") } }
        )
    }

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    }
}

@Composable
private fun AchievementCard(achievement: ProfileAchievementUi) {
    val iconTint = if (achievement.unlocked) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(achievement.title, fontWeight = FontWeight.Bold)
                Text(
                    if (achievement.unlocked) "🏆 Достижение получено!" else achievement.description,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun TrackedAppCard(app: TrackedAppEntity, onLimit: () -> Unit, onReplace: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val iconBitmap = remember(app.packageName) { packageManager.getApplicationIcon(app.packageName).toBitmapSafely(96) }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Image(iconBitmap.asImageBitmap(), app.appName, Modifier.size(52.dp))
                Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
                    Text(app.appName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    Text("Лимит: ${formatMinutes(app.dailyLimitMinutes)} в день", style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить ${app.appName}")
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onLimit, modifier = Modifier.weight(1f)) { Text("Лимит") }
                TextButton(onClick = onReplace, modifier = Modifier.weight(1f)) { Text("Заменить") }
            }
        }
    }
}

@Composable
private fun LimitDialog(app: TrackedAppEntity, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var value by remember(app.id) { mutableStateOf(app.dailyLimitMinutes.toString()) }
    val minutes = value.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Лимит: ${app.appName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Укажи дневной лимит в минутах.")
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it.filter(Char::isDigit).take(4) },
                    singleLine = true,
                    label = { Text("Минут в день") }
                )
                Text("Примеры: 30 = полчаса, 60 = 1 час, 120 = 2 часа", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (minutes != null && minutes in 1..1440) onSave(minutes) },
                enabled = minutes != null && minutes in 1..1440
            ) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun AppPickerDialog(apps: List<InstalledAppUi>, onDismiss: () -> Unit, onAppSelected: (InstalledAppUi) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = remember(apps, searchQuery) {
        val query = searchQuery.trim().lowercase()
        if (query.isEmpty()) apps else apps.filter { it.appName.lowercase().contains(query) || it.packageName.lowercase().contains(query) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выберите приложение") },
        text = {
            Column {
                OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Поиск") })
                Spacer(Modifier.height(12.dp))
                if (filteredApps.isEmpty()) {
                    Text(if (apps.isEmpty()) "Не удалось найти установленные пользовательские приложения." else "По вашему запросу ничего не найдено.", modifier = Modifier.padding(vertical = 24.dp))
                } else {
                    LazyColumn(modifier = Modifier.height(360.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(filteredApps, key = { it.packageName }) { app ->
                            Row(Modifier.fillMaxWidth().clickable { onAppSelected(app) }.padding(vertical = 10.dp)) {
                                Text(app.appName, Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

private fun isLimitAccessibilityEnabled(context: android.content.Context): Boolean {
    val manager = context.getSystemService(AccessibilityManager::class.java) ?: return false
    return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).any { info ->
        info.resolveInfo.serviceInfo.packageName == context.packageName &&
            info.resolveInfo.serviceInfo.name == LimitAccessibilityService::class.java.name
    }
}

private fun formatMinutes(minutes: Int): String {
    val hours = minutes / 60
    val remaining = minutes % 60
    return when {
        hours > 0 && remaining > 0 -> "$hours ч $remaining мин"
        hours > 0 -> "$hours ч"
        else -> "$remaining мин"
    }
}

private fun Drawable.toBitmapSafely(size: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap
}