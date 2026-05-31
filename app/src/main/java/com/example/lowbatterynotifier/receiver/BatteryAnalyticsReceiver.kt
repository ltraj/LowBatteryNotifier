package com.example.lowbatterynotifier.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.lowbatterynotifier.analytics.BatteryAnalyticsTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BatteryAnalyticsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                BatteryAnalyticsTracker.getInstance(context).onBatteryEvent(intent)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
