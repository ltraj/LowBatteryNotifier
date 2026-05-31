package com.example.lowbatterynotifier.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object PreferenceKeys {
    val THRESHOLD = intPreferencesKey("threshold_percent")
    val ALARM_ENABLED = booleanPreferencesKey("alarm_enabled")
    val RINGTONE_URI = stringPreferencesKey("ringtone_uri")
    val LAST_ALARM_STATE = stringPreferencesKey("last_alarm_state")
}

data class UserSettings(
    val thresholdPercent: Int = AppPreferences.DEFAULT_THRESHOLD,
    val alarmEnabled: Boolean = true,
    val ringtoneUri: String? = null,
    val lastAlarmState: String = AlarmState.NORMAL.name,
)

enum class AlarmState {
    NORMAL,
    WARNING,
    CRITICAL,
}

class AppPreferences(private val context: Context) {

    val settings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            thresholdPercent = prefs[PreferenceKeys.THRESHOLD] ?: DEFAULT_THRESHOLD,
            alarmEnabled = prefs[PreferenceKeys.ALARM_ENABLED] ?: true,
            ringtoneUri = prefs[PreferenceKeys.RINGTONE_URI],
            lastAlarmState = prefs[PreferenceKeys.LAST_ALARM_STATE] ?: AlarmState.NORMAL.name,
        )
    }

    suspend fun setThreshold(percent: Int) {
        context.dataStore.edit { it[PreferenceKeys.THRESHOLD] = percent.coerceIn(MIN_THRESHOLD, MAX_THRESHOLD) }
    }

    suspend fun setAlarmEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferenceKeys.ALARM_ENABLED] = enabled }
    }

    suspend fun setRingtoneUri(uri: String?) {
        context.dataStore.edit {
            if (uri == null) {
                it.remove(PreferenceKeys.RINGTONE_URI)
            } else {
                it[PreferenceKeys.RINGTONE_URI] = uri
            }
        }
    }

    suspend fun setLastAlarmState(state: AlarmState) {
        context.dataStore.edit { it[PreferenceKeys.LAST_ALARM_STATE] = state.name }
    }

    companion object {
        const val DEFAULT_THRESHOLD = 5
        const val MIN_THRESHOLD = 1
        const val MAX_THRESHOLD = 50
        const val CRITICAL_BATTERY_LEVEL = 2
        const val WARNING_REPEAT_MS = 60_000L
        const val CRITICAL_REPEAT_MS = 20_000L
    }
}
