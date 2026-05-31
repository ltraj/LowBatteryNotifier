package com.example.lowbatterynotifier.analytics

import com.example.lowbatterynotifier.analytics.db.ChargingSessionEntity
import com.example.lowbatterynotifier.analytics.db.DrainSampleEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class InsightsCalculatorTest {

    @Test
    fun averageDrainPerHour_returnsNullWithSingleSample() {
        val samples = listOf(
            DrainSampleEntity(timestampMillis = 0, batteryPercent = 80, isCharging = false),
        )
        assertNull(InsightsCalculator.averageDrainPerHour(samples))
    }

    @Test
    fun averageDrainPerHour_computesRollingSegmentAverage() {
        val now = System.currentTimeMillis()
        val samples = listOf(
            DrainSampleEntity(timestampMillis = now - 3_600_000, batteryPercent = 60, isCharging = false),
            DrainSampleEntity(timestampMillis = now - 1_800_000, batteryPercent = 50, isCharging = false),
            DrainSampleEntity(timestampMillis = now, batteryPercent = 40, isCharging = false),
        )
        val rate = InsightsCalculator.averageDrainPerHour(samples)
        assertNotNull(rate)
        assertEquals(20.0, rate!!, 0.5)
    }

    @Test
    fun compute_excludesChargingSamplesFromDrain() {
        val now = System.currentTimeMillis()
        val samples = listOf(
            DrainSampleEntity(timestampMillis = now - 3_600_000, batteryPercent = 60, isCharging = false),
            DrainSampleEntity(timestampMillis = now - 2_700_000, batteryPercent = 70, isCharging = true),
            DrainSampleEntity(timestampMillis = now - 1_800_000, batteryPercent = 50, isCharging = false),
            DrainSampleEntity(timestampMillis = now, batteryPercent = 40, isCharging = false),
        )
        val rate = InsightsCalculator.averageDrainPerHour(samples)
        assertNotNull(rate)
        assertEquals(20.0, rate!!, 1.0)
    }

    @Test
    fun compute_chargingSessionAverages() {
        val sessions = listOf(
            ChargingSessionEntity(
                id = 1,
                startTimeMillis = 0,
                endTimeMillis = 3_600_000,
                startPercent = 20,
                endPercent = 80,
            ),
            ChargingSessionEntity(
                id = 2,
                startTimeMillis = 10_000,
                endTimeMillis = 7_600_000,
                startPercent = 30,
                endPercent = 90,
            ),
        )
        val insights = InsightsCalculator.compute(
            currentBatteryPercent = 50,
            drainSamples = emptyList(),
            chargingSessions = sessions,
            formatDuration = { "${it / 60_000} min" },
            formatDateTime = { "t" },
            notEnoughData = "N/A",
        )
        assertEquals(2, insights.totalChargingSessions)
        assertEquals("25%", insights.averagePlugInPercentText)
        assertEquals("85%", insights.averageUnplugPercentText)
    }
}
