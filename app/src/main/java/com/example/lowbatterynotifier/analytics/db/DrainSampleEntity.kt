package com.example.lowbatterynotifier.analytics.db

data class DrainSampleEntity(
    val id: Long = 0,
    val timestampMillis: Long,
    val batteryPercent: Int,
    val isCharging: Boolean,
)
