package com.example.myapplication.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ProgressScreen(viewModel: ProgressViewModel) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 32.dp)
    ) {
        item {
            Text("Прогресс", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🔥 Streak", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        if (state.currentStreak == 0) "Начни серию сегодня" else "${state.currentStreak} ${streakWord(state.currentStreak)} подряд",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Рекорд: ${state.longestStreak} ${streakWord(state.longestStreak)}")
                    Text("День засчитывается, если ни один лимит не превышен.")
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🛡️ Щиты", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Всего: ${state.totalShields}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Недельный: ${state.weeklyShields} · за стрики: ${state.streakShields}")
                    Text("Недельный щит не суммируется. Щиты за достигнутые этапы Streak сохраняются.")
                }
            }
        }

        state.statusMessage?.let { message ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(message, modifier = Modifier.padding(16.dp), fontWeight = FontWeight.SemiBold)
                }
            }
        }

        item {
            Text("Этапы Streak", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        items(state.milestones) { milestone ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (milestone.unlocked) "🛡️" else "🔒")
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${milestone.days} ${streakWord(milestone.days)} — ${milestone.title}", fontWeight = FontWeight.Bold)
                        Text(if (milestone.unlocked) "Достигнуто · щит получен" else "Продолжай серию без превышений")
                        LinearProgressIndicator(
                            progress = { (state.currentStreak.toFloat() / milestone.days).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🏆 Новые достижения", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (state.achievements.isEmpty()) {
                        Text("Новые достижения появятся здесь после достижения целей.")
                    } else {
                        state.achievements.forEach { achievement ->
                            Text("🏆 $achievement", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("⏱️ Возвращённое время", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(formatTime(state.totalSavedMinutes), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Всего накоплено в копилке")
                }
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

private fun streakWord(days: Int): String = when {
    days % 100 in 11..14 -> "дней"
    days % 10 == 1 -> "день"
    days % 10 in 2..4 -> "дня"
    else -> "дней"
}
