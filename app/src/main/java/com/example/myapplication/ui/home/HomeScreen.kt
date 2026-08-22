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
            if (sleepBlocks > 0) {
                Text("Твои ${formatTime(totalSavedMinutes)} — это до ${"%.1f".format(java.util.Locale.US, weeklySleepHours)} ч дополнительного сна в неделю.")
            }
        }
    }
}

private fun formatTime(minutes: Int): String {
    val hours = minutes / 60
    val remaining = minutes % 60
    return when {
        hours > 0 && remaining > 0 -> "$hours ч $remaining мин"
        hours > 0 -> "$hours ч"
        else -> "$remaining мин"
    }
}

@Composable
private fun AppUsageCard(name: String, usedMinutes: Int, limitMinutes: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(name, fontWeight = FontWeight.Bold)
            Text("${formatTime(usedMinutes)} из ${formatTime(limitMinutes)}")
            LinearProgressIndicator(
                progress = { (usedMinutes / limitMinutes.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ScreenTimeCard(totalMinutes: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Общее экранное время", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(formatTime(totalMinutes), fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text("по всем приложениям на устройстве")
        }
    }
}
