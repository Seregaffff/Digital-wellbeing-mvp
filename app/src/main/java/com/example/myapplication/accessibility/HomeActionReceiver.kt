package com.example.myapplication.accessibility

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class HomeActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == LimitBlockActivity.ACTION_REQUEST_HOME) {
            LimitAccessibilityService.performHomeAction()
        }
    }
}