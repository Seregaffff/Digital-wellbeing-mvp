package com.example.myapplication.ui.statistics

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

@Composable
fun StatisticsScreen(viewModel: StatisticsViewModel) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadStatistics() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp)
    ) {
        item {
            Text("Статистика", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        item { Text("Экранное время выбранных приложений за последние 7 дней") }

        when {
            state.isLoading -> item {
                CircularProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp))
            }
            state.apps.isEmpty() -> item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                    Text(
                        text = "Добавьте приложения в Профиле, чтобы увидеть статистику.",
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
            else -> items(state.apps) { app -> WeeklyAppCard(app) }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun WeeklyAppCard(app: AppWeeklyUsageUi) {
    var showDetails by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { showDetails = !showDetails },
        shape = RoundedCornerShape(18.dp)
    ) {
        AnimatedContent(targetState = showDetails, label = "statistics_card") { detailsVisible ->
            if (!detailsVisible) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = app.app.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Нажмите на график для подробностей", style = MaterialTheme.typography.bodySmall)
                    WeeklyBarChart(app.days)
                }
            } else {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = app.app.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    DetailRow("Общее время", formatMinutes(app.totalMinutes))
                    DetailRow("Среднее в день", formatMinutes(app.averageMinutes))
                    DetailRow("Самый загруженный день", busiestDayText(app.days))
                    Text("Нажмите, чтобы вернуться к графику", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

private fun busiestDayText(days: List<WeeklyDayUi>): String {
    if (days.isEmpty()) return "Нет данных"
    val busiest = days.maxByOrNull { it.totalMinutes } ?: return "Нет данных"
    return "${formatDate(busiest.date)} — ${formatMinutes(busiest.totalMinutes)}"
}

@Composable
private fun WeeklyBarChart(days: List<WeeklyDayUi>) {
    val maxMinutes = (days.maxOfOrNull { it.totalMinutes } ?: 0).coerceAtLeast(1)

    Row(
        modifier = Modifier.fillMaxWidth().height(150.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEach { day ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val fraction = day.totalMinutes.toFloat() / maxMinutes
                val barColor = MaterialTheme.colorScheme.primary
                Canvas(modifier = Modifier.width(24.dp).height(110.dp)) {
                    val barHeight = size.height * fraction
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(0f, size.height - barHeight),
                        size = Size(size.width, barHeight),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(formatDate(day.date), fontSize = 11.sp)
                Text("${day.totalMinutes} мин", fontSize = 10.sp)
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val calendar = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
    return String.format(
        "%02d.%02d",
        calendar.get(java.util.Calendar.DAY_OF_MONTH),
        calendar.get(java.util.Calendar.MONTH) + 1
    )
}

private fun formatMinutes(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return when {
        hours > 0 && mins > 0 -> "$hours ч $mins мин"
        hours > 0 -> "$hours ч"
        else -> "$mins мин"
    }
}