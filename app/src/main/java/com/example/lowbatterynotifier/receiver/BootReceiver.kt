package com.example.lowbatterynotifier.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.lowbatterynotifier.analytics.BatteryAnalyticsRegistrar
import com.example.lowbatterynotifier.data.AppPreferences
import com.example.lowbatterynotifier.service.LowBatteryMonitorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if ((intent?.action != Intent.ACTION_BOOT_COMPLETED) &&
            (intent?.action != Intent.ACTION_LOCKED_BOOT_COMPLETED)
        ) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                BatteryAnalyticsRegistrar.ensureRegistered(context)
                val settings = AppPreferences(context).settings.first()
                if (settings.alarmEnabled) {
                    LowBatteryMonitorService.startMonitoring(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
