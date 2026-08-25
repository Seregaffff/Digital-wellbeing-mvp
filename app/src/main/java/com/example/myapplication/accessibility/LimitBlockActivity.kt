package com.example.myapplication.accessibility

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
        val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val protectionPreferences = ProtectionPreferences(applicationContext)
        val graceAlreadyUsed = protectionPreferences.hasSoftGrace(packageName, dateKey)

        setContent {
            MyApplicationTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 28.dp, vertical = 32.dp),
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
                        when {
                            hardBlock -> "Доступ к приложению заблокирован до завтра."
                            graceAlreadyUsed -> "Дополнительные 5 минут уже использованы сегодня."
                            else -> "Ты достиг дневного лимита. Можно сделать короткое исключение на 5 минут."
                        },
                        modifier = Modifier.padding(top = 12.dp, bottom = 28.dp)
                    )

                    OutlinedButton(
                        onClick = { closeBlockedApp() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Закрыть приложение")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { openMyApplication() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Открыть My Application")
                    }

                    if (!hardBlock && !graceAlreadyUsed) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                protectionPreferences.setSoftGraceUntil(
                                    packageName = packageName,
                                    dateKey = dateKey,
                                    untilMillis = System.currentTimeMillis() + SOFT_GRACE_MILLIS
                                )
                                finishAndRemoveTask()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Продолжить ещё 5 минут")
                        }
                    }
                }
            }
        }
    }

    private fun closeBlockedApp() {
        // Android does not allow a regular app to force-stop another app.
        // AccessibilityService can reliably leave the blocked app by navigating Home.
        LimitAccessibilityService.performHomeAction()
        finishAndRemoveTask()
    }

    private fun openMyApplication() {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            ?: Intent(this, com.example.myapplication.MainActivity::class.java)

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(launchIntent)
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
        private const val SOFT_GRACE_MILLIS = 5 * 60 * 1000L
    }
}
