package com.example.myapplication.accessibility

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.preferences.ProtectionPreferences
import com.example.myapplication.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LimitBlockActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
        val appName = intent.getStringExtra(EXTRA_APP_NAME).orEmpty()
        val hardBlock = intent.getBooleanExtra(EXTRA_HARD_BLOCK, false)
        val usedMinutes = intent.getIntExtra(EXTRA_USED_MINUTES, 0)
        val limitMinutes = intent.getIntExtra(EXTRA_LIMIT_MINUTES, 0)

        setContent {
            MyApplicationTheme {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (hardBlock) "Лимит достигнут" else "Время вышло",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        appName,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        "Использовано ${formatMinutes(usedMinutes)} из ${formatMinutes(limitMinutes)}",
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        if (hardBlock) {
                            "Доступ к приложению заблокирован до завтра."
                        } else {
                            "Ты достиг дневного лимита. Можно сделать короткое исключение на 5 минут."
                        },
                        modifier = Modifier.padding(top = 12.dp, bottom = 24.dp)
                    )

                    if (!hardBlock) {
                        Button(onClick = {
                            val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                            ProtectionPreferences(applicationContext).setSoftGraceUntil(
                                packageName = packageName,
                                dateKey = dateKey,
                                untilMillis = System.currentTimeMillis() + SOFT_GRACE_MILLIS
                            )
                            finish()
                        }) {
                            Text("Продолжить ещё 5 минут")
                        }
                    }

                    OutlinedButton(onClick = { moveToHome() }) {
                        Text("Закрыть приложение")
                    }
                }
            }
        }
    }

    private fun moveToHome() {
        sendBroadcast(android.content.Intent(ACTION_REQUEST_HOME).setPackage(packageName))
        finishAndRemoveTask()
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

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_HARD_BLOCK = "extra_hard_block"
        const val EXTRA_USED_MINUTES = "extra_used_minutes"
        const val EXTRA_LIMIT_MINUTES = "extra_limit_minutes"
        const val ACTION_REQUEST_HOME = "com.example.myapplication.ACTION_REQUEST_HOME"
        private const val SOFT_GRACE_MILLIS = 5 * 60 * 1000L
    }
}