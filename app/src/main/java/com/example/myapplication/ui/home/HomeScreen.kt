package com.example.myapplication.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    userName: String,
    onOpenUsageSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        while (true) {
            viewModel.loadUsage()
            delay(60_000L)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            top = 20.dp,
            bottom = 32.dp
        )
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (userName.isBlank()) "Добрый день 👋" else "Добрый день, $userName 👋",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Text("Посмотрим, как проходит твой день?")

                ScreenTimeCard(totalMinutes = state.totalScreenTime)

                SavedTimeCard(savedMinutes = state.savedTimeMinutes)

                Text(
                    text = "Твои приложения",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                if (state.apps.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Пока нет отслеживаемых приложений",
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Добавь до 3 приложений, за которыми хочешь следить.")
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        state.apps.forEach { usage ->
                            AppUsageCard(
                                appName = usage.app.appName,
                                usedMinutes = usage.usedMinutes,
                                limitMinutes = usage.app.dailyLimitMinutes
                            )
                        }
                    }
                }

                Button(
                    onClick = { viewModel.loadUsage() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Обновить данные")
                }

                Button(
                    onClick = onOpenUsageSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Настройки доступа")
                }
            }
        }
    }
}

@Composable
private fun SavedTimeCard(savedMinutes: Int) {
    val hours = savedMinutes / 60
    val minutes = savedMinutes % 60
    val timeText = if (hours > 0) {
        if (minutes > 0) "$hours ч $minutes мин" else "$hours ч"
    } else {
        "$minutes мин"
    }

    val filmFraction = savedMinutes / 120f
    val filmText = when {
        savedMinutes <= 0 -> "Пока нечего возвращать — попробуй удержаться в рамках лимита."
        filmFraction >= 1f -> "Это примерно ${filmFraction.toInt()} полнометражный фильм(а) по 2 часа."
        else -> "Это примерно ${Math.round(filmFraction * 100)}% двухчасового фильма."
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Вы сэкономили", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(timeText, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(filmText)
            if (savedMinutes > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Расчёт: сумма неиспользованного времени по дневным лимитам выбранных приложений.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun ScreenTimeCard(totalMinutes: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Сегодня")
            Spacer(modifier = Modifier.height(8.dp))
            Text(formatTime(totalMinutes), fontSize = 42.sp, fontWeight = FontWeight.Bold)
            Text("общее экранное время")
            Text(
                "по всем приложениям на устройстве",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun AppUsageCard(appName: String, usedMinutes: Int, limitMinutes: Int) {
    val exceeded = limitMinutes > 0 && usedMinutes > limitMinutes
    val progress = if (limitMinutes > 0) {
        (usedMinutes.toFloat() / limitMinutes).coerceIn(0f, 1f)
    } else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (exceeded) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(appName, fontWeight = FontWeight.Bold)
                Text(
                    text = if (exceeded) "Лимит превышен" else "$usedMinutes / $limitMinutes мин",
                    color = if (exceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = if (exceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            if (exceeded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Превышение: ${usedMinutes - limitMinutes} мин",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun formatTime(minutes: Int): String {
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return if (hours > 0) "$hours ч $remainingMinutes мин" else "$remainingMinutes мин"
}