package com.example.lowbatterynotifier.analytics

import android.content.Context
import android.content.Intent
import com.example.lowbatterynotifier.analytics.db.BatteryAnalyticsStore
import com.example.lowbatterynotifier.analytics.db.ChargingSessionEntity
import com.example.lowbatterynotifier.analytics.db.DrainSampleEntity
import com.example.lowbatterynotifier.battery.BatteryInfo
import com.example.lowbatterynotifier.battery.readBatteryInfo
import com.example.lowbatterynotifier.battery.toBatteryInfo

/**
 * Records battery changes from broadcasts only. Writes when level changes by ≥1%
 * or charging state changes. Separate from alarm logic.
 */
class BatteryAnalyticsTracker private constructor(context: Context) {

    private val store = BatteryAnalyticsStore.getInstance(context)
    private val appContext = context.applicationContext
    private val lock = Any()

    fun onBatteryEvent(intent: Intent?) {
        val info = intent?.toBatteryInfo() ?: appContext.readBatteryInfo()
        processBatteryInfo(info, System.currentTimeMillis())
    }

    fun processBatteryInfo(info: BatteryInfo, timestampMillis: Long) {
        synchronized(lock) {
            val latest = store.getLatestDrainSample()
            if ((latest != null) && (!isMeaningfulChange(latest, info))) {
                return
            }

            store.insertDrainSample(
                DrainSampleEntity(
                    timestampMillis = timestampMillis,
                    batteryPercent = info.levelPercent,
                    isCharging = info.isCharging,
                ),
            )

            syncChargingSession(info, timestampMillis)
        }
    }

    private fun syncChargingSession(info: BatteryInfo, timestampMillis: Long) {
        val openSession = store.getOpenChargingSession()
        if (info.isCharging) {
            if (openSession == null) {
                store.insertChargingSession(
                    ChargingSessionEntity(
                        startTimeMillis = timestampMillis,
                        startPercent = info.levelPercent,
                    ),
                )
            }
        } else {
            openSession?.let { session ->
                store.closeChargingSession(
                    sessionId = session.id,
                    endTimeMillis = timestampMillis,
                    endPercent = info.levelPercent,
                )
            }
        }
    }

    private fun isMeaningfulChange(latest: DrainSampleEntity, info: BatteryInfo): Boolean {
        if (latest.isCharging != info.isCharging) return true
        return kotlin.math.abs(latest.batteryPercent - info.levelPercent) >= MIN_LEVEL_DELTA_PERCENT
    }

    companion object {
        private const val MIN_LEVEL_DELTA_PERCENT = 1

        @Volatile
        private var instance: BatteryAnalyticsTracker? = null

        fun getInstance(context: Context): BatteryAnalyticsTracker =
            instance ?: synchronized(this) {
                instance ?: BatteryAnalyticsTracker(context.applicationContext).also { instance = it }
            }
    }
}
