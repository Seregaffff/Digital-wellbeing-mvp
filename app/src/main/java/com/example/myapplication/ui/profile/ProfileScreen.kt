package com.example.myapplication.ui.profile

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.local.TrackedAppEntity

@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var nameInput by remember(uiState.userName) { mutableStateOf(uiState.userName) }

    LaunchedEffect(Unit) { viewModel.loadTrackedApps() }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Мой профиль", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Text("Имя", fontWeight = FontWeight.SemiBold)
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

        Text("Email", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(4.dp))

        Text(
            "Мои достижения",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        FirstStepsAchievement(unlocked = uiState.firstStepsAchievementUnlocked)

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

        if (uiState.trackedApps.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Text("Пока нет отслеживаемых приложений. Добавьте до 3 приложений.", modifier = Modifier.padding(16.dp))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.trackedApps, key = { it.id }) { app ->
                    TrackedAppCard(
                        app = app,
                        onLimit = { viewModel.openLimitEditor(app.id) },
                        onReplace = { viewModel.openAppPicker(app.id) },
                        onDelete = { viewModel.deleteApp(app) }
                    )
                }
            }
        }
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
private fun FirstStepsAchievement(unlocked: Boolean) {
    val iconTint = if (unlocked) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Первые шаги", fontWeight = FontWeight.Bold)
                Text(
                    if (unlocked) "Достижение получено!"
                    else "Выберите приложение для отслеживания экранного времени и настройте для него дневной лимит.",
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
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(iconBitmap.asImageBitmap(), app.appName, Modifier.size(44.dp))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(app.appName, fontWeight = FontWeight.SemiBold)
                Text("Лимит: ${formatMinutes(app.dailyLimitMinutes)} в день", style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onLimit) { Text("Лимит") }
            TextButton(onClick = onReplace) { Text("Заменить") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Удалить ${app.appName}") }
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
                            Row(Modifier.fillMaxWidth().clickable { onAppSelected(app) }.padding(vertical = 10.dp)) { Text(app.appName, Modifier.weight(1f)) }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
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
