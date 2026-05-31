package com.example.lowbatterynotifier

import android.app.Application
import com.example.lowbatterynotifier.analytics.BatteryAnalyticsRegistrar

class LowBatteryNotifierApp : Application() {

    override fun onCreate() {
        super.onCreate()
        BatteryAnalyticsRegistrar.ensureRegistered(this)
    }
}
