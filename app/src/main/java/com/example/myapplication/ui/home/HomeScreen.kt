package com.example.myapplication.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.repository.SavingsCategory
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(viewModel: HomeViewModel, userName: String, onOpenUsageSettings: () -> Unit) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        while (true) {
            viewModel.loadUsage()
            delay(60_000L)
        }
    }

    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 32.dp)
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
                ScreenTimeCard(state.totalScreenTime)
                SavedTimeCard(state.savedTimeMinutes, state.totalSavedTimeMinutes)
                StreakCard(state.currentStreak, state.totalShields, state.streakShields, state.weeklyShield)
                state.achievementMessage?.let { AchievementNotice(it) }
                SavingsPotCard(state, viewModel::allocateSavings)
                SleepConversionCard(state.totalSavedTimeMinutes)

                Text("Твои приложения", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (state.apps.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Пока нет отслеживаемых приложений", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Добавь до 3 приложений, за которыми хочешь следить.")
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        state.apps.forEach { usage ->
                            AppUsageCard(usage.app.appName, usage.usedMinutes, usage.app.dailyLimitMinutes)
                        }
                    }
                }
                Button(onClick = viewModel::loadUsage, modifier = Modifier.fillMaxWidth()) { Text("Обновить данные") }
                Button(onClick = onOpenUsageSettings, modifier = Modifier.fillMaxWidth()) { Text("Настройки доступа") }
            }
        }
    }
}

@Composable
private fun SavedTimeCard(todaySavedMinutes: Int, totalSavedMinutes: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Вы возвращаете себе", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(formatTime(todaySavedMinutes), fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text("потенциально сегодня")
            Text("Всего в копилке: ${formatTime(totalSavedMinutes)}", fontWeight = FontWeight.SemiBold)
            Text(
                "Сегодняшнее значение меняется вместе с использованием. В копилку попадает только завершённое время.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun StreakCard(currentStreak: Int, totalShields: Int, streakShields: Int, weeklyShield: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("🔥 Streak", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                if (currentStreak == 0) "Начни серию сегодня" else "$currentStreak ${streakWord(currentStreak)} подряд",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text("🛡️ Щитов: $totalShields")
            Text("Недельный: ${if (weeklyShield) "1" else "0"} · за стрики: $streakShields", style = MaterialTheme.typography.bodySmall)
            Text(
                "День без превышения продолжает серию. При превышении щит защищает серию и сгорает.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun AchievementNotice(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Text(message, modifier = Modifier.padding(16.dp), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SavingsPotCard(state: HomeUiState, onAllocate: (SavingsCategory) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("⏱️ Копилка", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Доступно: ${formatTime(state.availableSavingsMinutes)}")
                }
                Text("${formatTime(state.totalSavedTimeMinutes)} накоплено", fontWeight = FontWeight.SemiBold)
            }

            SavingsGoalRow(
                emoji = "📚",
                title = "Книги",
                allocatedMinutes = state.booksSavingsMinutes,
                targetMinutes = SavingsCategory.BOOKS.targetMinutes,
                description = "≈ ${(state.booksSavingsMinutes / 1.25f).toInt()} страниц",
                enabled = state.availableSavingsMinutes >= 30,
                onAllocate = { onAllocate(SavingsCategory.BOOKS) }
            )
            SavingsGoalRow(
                emoji = "🎬",
                title = "Кино",
                allocatedMinutes = state.moviesSavingsMinutes,
                targetMinutes = SavingsCategory.MOVIES.targetMinutes,
                description = "До «Зелёной мили» — ${maxOf(0, SavingsCategory.MOVIES.targetMinutes - state.moviesSavingsMinutes)} мин",
                enabled = state.availableSavingsMinutes >= 30,
                onAllocate = { onAllocate(SavingsCategory.MOVIES) }
            )
            SavingsGoalRow(
                emoji = "🚶",
                title = "Прогулки",
                allocatedMinutes = state.walksSavingsMinutes,
                targetMinutes = SavingsCategory.WALKS.targetMinutes,
                description = "≈ ${(state.walksSavingsMinutes / 45f * 3.5f).toInt()} км",
                enabled = state.availableSavingsMinutes >= 30,
                onAllocate = { onAllocate(SavingsCategory.WALKS) }
            )

            Text("🎯 Задание недели: конвертируй 3 часа в прогулки — ${state.weeklyWalkMinutes}/180 мин", fontWeight = FontWeight.SemiBold)
            LinearProgressIndicator(
                progress = { (state.weeklyWalkMinutes / 180f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
            if (state.weeklyWalkMinutes >= 180) {
                Text("🏅 Задание выполнено!", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SavingsGoalRow(
    emoji: String,
    title: String,
    allocatedMinutes: Int,
    targetMinutes: Int,
    description: String,
    enabled: Boolean,
    onAllocate: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("$emoji $title", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Button(onClick = onAllocate, enabled = enabled, modifier = Modifier.size(width = 150.dp, height = 40.dp)) {
                Text("+30 мин")
            }
        }
        LinearProgressIndicator(
            progress = { (allocatedMinutes / targetMinutes.toFloat()).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth()
        )
        Text("$description · ${allocatedMinutes}/${targetMinutes} мин", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SleepConversionCard(totalSavedMinutes: Int) {
    val sleepBlocks = totalSavedMinutes / 30
    val weeklySleepHours = sleepBlocks * 0.5f * 7f
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("😴 Ещё один вариант", fontWeight = FontWeight.Bold)
            Text("Если ложиться на 30 минут раньше, за неделю можно вернуть до 3,5 часа сна.")
            if (totalSavedMinutes >= 30) {
                Text("Из твоей копилки это уже ${sleepBlocks} × 30 минут — до ${formatDecimalHours(weeklySleepHours)} сна за неделю.")
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
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Сегодня")
            Spacer(modifier = Modifier.height(8.dp))
            Text(formatTime(totalMinutes), fontSize = 42.sp, fontWeight = FontWeight.Bold)
            Text("общее экранное время")
            Text("по всем приложениям на устройстве", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun AppUsageCard(appName: String, usedMinutes: Int, limitMinutes: Int) {
    val exceeded = limitMinutes > 0 && usedMinutes > limitMinutes
    val progress = if (limitMinutes > 0) (usedMinutes.toFloat() / limitMinutes).coerceIn(0f, 1f) else 0f
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (exceeded) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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
                Text("Превышение: ${usedMinutes - limitMinutes} мин", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun formatTime(minutes: Int): String {
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return if (hours > 0) {
        if (remainingMinutes > 0) "$hours ч $remainingMinutes мин" else "$hours ч"
    } else "$remainingMinutes мин"
}

private fun formatDecimalHours(hours: Float): String =
    if (hours % 1f == 0f) "${hours.toInt()} ч" else "${String.format(java.util.Locale.US, "%.1f", hours)} ч"

private fun streakWord(days: Int): String = when {
    days % 10 == 1 && days % 100 != 11 -> "день"
    days % 10 in 2..4 && days % 100 !in 12..14 -> "дня"
    else -> "дней"
}