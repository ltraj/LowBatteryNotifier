package com.example.lowbatterynotifier.ui

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lowbatterynotifier.battery.BatteryInfo
import com.example.lowbatterynotifier.battery.readBatteryInfo
import com.example.lowbatterynotifier.battery.toBatteryInfo
import com.example.lowbatterynotifier.data.AppPreferences
import com.example.lowbatterynotifier.data.UserSettings
import com.example.lowbatterynotifier.service.LowBatteryMonitorService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val batteryPercent: Int = 0,
    val isCharging: Boolean = false,
    val settings: UserSettings = UserSettings(),
    val ringtoneDisplayName: String = "",
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = AppPreferences(application)
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val settings = preferences.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        UserSettings(),
    )

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            updateBattery(intent.toBatteryInfo())
        }
    }

    private var receiverRegistered = false

    init {
        refreshBattery()
        viewModelScope.launch {
            settings.collect { prefs ->
                _uiState.update { it.copy(settings = prefs) }
                if (prefs.alarmEnabled) {
                    LowBatteryMonitorService.startMonitoring(getApplication())
                } else {
                    LowBatteryMonitorService.stopMonitoring(getApplication())
                }
            }
        }
    }

    fun onStart() {
        registerBatteryReceiver()
        refreshBattery()
    }

    fun onStop() {
        unregisterBatteryReceiver()
    }

    fun setThreshold(percent: Int) {
        viewModelScope.launch {
            preferences.setThreshold(percent)
        }
    }

    fun setAlarmEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setAlarmEnabled(enabled)
        }
    }

    fun setRingtone(uri: String?, displayName: String) {
        viewModelScope.launch {
            preferences.setRingtoneUri(uri)
            _uiState.update { it.copy(ringtoneDisplayName = displayName) }
        }
    }

    fun testAlarm() {
        LowBatteryMonitorService.testAlarm(getApplication())
    }

    fun stopAlarm() {
        LowBatteryMonitorService.stopAlarm(getApplication())
    }

    private fun refreshBattery() {
        updateBattery(getApplication<Application>().readBatteryInfo())
    }

    private fun updateBattery(info: BatteryInfo) {
        _uiState.update {
            it.copy(
                batteryPercent = info.levelPercent,
                isCharging = info.isCharging,
            )
        }
    }

    private fun registerBatteryReceiver() {
        if (receiverRegistered) return
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        ContextCompat.registerReceiver(
            getApplication(),
            batteryReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        receiverRegistered = true
    }

    private fun unregisterBatteryReceiver() {
        if (!receiverRegistered) return
        try {
            getApplication<Application>().unregisterReceiver(batteryReceiver)
        } catch (_: IllegalArgumentException) {
        }
        receiverRegistered = false
    }
}
