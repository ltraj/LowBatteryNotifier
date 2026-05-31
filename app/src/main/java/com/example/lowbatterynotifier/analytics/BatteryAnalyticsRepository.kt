package com.example.lowbatterynotifier.analytics

import android.content.Context
import com.example.lowbatterynotifier.analytics.db.BatteryAnalyticsStore
import com.example.lowbatterynotifier.analytics.db.ChargingSessionEntity
import com.example.lowbatterynotifier.analytics.db.DrainSampleEntity
import com.example.lowbatterynotifier.battery.readBatteryInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BatteryAnalyticsRepository(context: Context) {

    private val store = BatteryAnalyticsStore.getInstance(context)
    private val appContext = context.applicationContext

    suspend fun loadInsightsData(): InsightsData = withContext(Dispatchers.IO) {
        pruneOldData()
        val now = System.currentTimeMillis()
        val sevenDaysAgo = now - RETENTION_MS
        InsightsData(
            currentBatteryPercent = appContext.readBatteryInfo().levelPercent,
            drainSamples = store.getDrainSamplesSince(sevenDaysAgo),
            chargingSessions = store.getRecentChargingSessions(limit = 50),
        )
    }

    private fun pruneOldData() {
        val cutoff = System.currentTimeMillis() - RETENTION_MS
        store.deleteDrainSamplesOlderThan(cutoff)
        store.deleteCompletedChargingSessionsOlderThan(cutoff)
    }

    data class InsightsData(
        val currentBatteryPercent: Int,
        val drainSamples: List<DrainSampleEntity>,
        val chargingSessions: List<ChargingSessionEntity>,
    )

    companion object {
        private const val RETENTION_MS = 30L * 24 * 60 * 60 * 1000
    }
}
