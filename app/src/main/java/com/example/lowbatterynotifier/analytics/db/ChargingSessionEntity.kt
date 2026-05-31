package com.example.lowbatterynotifier.analytics.db

data class ChargingSessionEntity(
    val id: Long = 0,
    val startTimeMillis: Long,
    val endTimeMillis: Long? = null,
    val startPercent: Int,
    val endPercent: Int? = null,
) {
    val isComplete: Boolean get() = endTimeMillis != null && endPercent != null

    val durationMillis: Long?
        get() {
            val end = endTimeMillis ?: return null
            return (end - startTimeMillis).coerceAtLeast(0)
        }
}
