package com.example.lowbatterynotifier.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.example.lowbatterynotifier.data.AlarmState
import com.example.lowbatterynotifier.data.AppPreferences

data class BatteryInfo(
    val levelPercent: Int,
    val isCharging: Boolean,
)

fun Intent.toBatteryInfo(): BatteryInfo {
    val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
    val percent = ((level * 100f) / scale).toInt().coerceIn(0, 100)
    val status = getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    val isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING) ||
        (status == BatteryManager.BATTERY_STATUS_FULL)
    return BatteryInfo(percent, isCharging)
}

fun Context.readBatteryInfo(): BatteryInfo {
    val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    val intent = registerReceiver(null, filter)
    return intent?.toBatteryInfo() ?: BatteryInfo(levelPercent = 0, isCharging = false)
}

fun BatteryInfo.resolveAlarmState(thresholdPercent: Int): AlarmState {
    if (isCharging || (levelPercent > thresholdPercent)) {
        return AlarmState.NORMAL
    }
    return if (levelPercent <= AppPreferences.CRITICAL_BATTERY_LEVEL) {
        AlarmState.CRITICAL
    } else {
        AlarmState.WARNING
    }
}

fun AlarmState.repeatIntervalMs(): Long = when (this) {
    AlarmState.CRITICAL -> AppPreferences.CRITICAL_REPEAT_MS
    AlarmState.WARNING -> AppPreferences.WARNING_REPEAT_MS
    AlarmState.NORMAL -> 0L
}

fun AlarmState.shouldAlarm(thresholdPercent: Int, battery: BatteryInfo): Boolean =
    this != AlarmState.NORMAL && !battery.isCharging && battery.levelPercent <= thresholdPercent
