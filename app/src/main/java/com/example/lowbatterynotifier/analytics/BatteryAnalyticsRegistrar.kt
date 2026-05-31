package com.example.lowbatterynotifier.analytics

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.example.lowbatterynotifier.receiver.BatteryAnalyticsReceiver

/**
 * Registers a single dynamic receiver for battery/power events. No foreground service.
 */
object BatteryAnalyticsRegistrar {

    @Volatile
    private var registered = false

    fun ensureRegistered(context: Context) {
        if (registered) return
        synchronized(this) {
            if (registered) return
            val appContext = context.applicationContext
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
            }
            ContextCompat.registerReceiver(
                appContext,
                BatteryAnalyticsReceiver(),
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            registered = true
        }
    }
}
