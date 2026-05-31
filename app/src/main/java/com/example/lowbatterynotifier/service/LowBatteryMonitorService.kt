package com.example.lowbatterynotifier.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.lowbatterynotifier.alarm.AlarmPlayer
import com.example.lowbatterynotifier.battery.BatteryInfo
import com.example.lowbatterynotifier.battery.readBatteryInfo
import com.example.lowbatterynotifier.battery.repeatIntervalMs
import com.example.lowbatterynotifier.battery.resolveAlarmState
import com.example.lowbatterynotifier.battery.shouldAlarm
import com.example.lowbatterynotifier.battery.toBatteryInfo
import com.example.lowbatterynotifier.data.AlarmState
import com.example.lowbatterynotifier.data.AppPreferences
import com.example.lowbatterynotifier.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class LowBatteryMonitorService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var preferences: AppPreferences
    private lateinit var alarmPlayer: AlarmPlayer

    private var thresholdPercent = AppPreferences.DEFAULT_THRESHOLD
    private var ringtoneUri: Uri? = null
    private var alarmEnabled = true

    private var currentAlarmState = AlarmState.NORMAL
    private var userStoppedAlarm = false
    private var isAlarmCycleActive = false

    private val repeatRunnable = Runnable { onRepeatTick() }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            evaluateBattery(intent.toBatteryInfo())
        }
    }

    override fun onCreate() {
        super.onCreate()
        preferences = AppPreferences(this)
        alarmPlayer = AlarmPlayer(this)
        NotificationHelper.createChannels(this)

        scope.launch {
            preferences.settings.collect { settings ->
                thresholdPercent = settings.thresholdPercent
                alarmEnabled = settings.alarmEnabled
                ringtoneUri = settings.ringtoneUri?.let(Uri::parse)
                if (!alarmEnabled) {
                    stopAlarmCycle(resetUserStop = true)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_ALARM -> {
                userStoppedAlarm = true
                stopAlarmPlayback()
                handler.removeCallbacks(repeatRunnable)
                updateForegroundNotification(readBatteryInfo())
            }
            ACTION_TEST_ALARM -> {
                loadSettingsSync()
                val battery = readBatteryInfo()
                startForeground(
                    NotificationHelper.NOTIFICATION_MONITORING,
                    NotificationHelper.buildMonitoringNotification(
                        this,
                        battery.levelPercent,
                        battery.isCharging,
                    ),
                )
                alarmPlayer.play(ringtoneUri, loopOnComplete = false)
            }
            else -> {
                loadSettingsSync()
                val battery = readBatteryInfo()
                startForeground(
                    NotificationHelper.NOTIFICATION_MONITORING,
                    NotificationHelper.buildMonitoringNotification(
                        this,
                        battery.levelPercent,
                        battery.isCharging,
                    ),
                )
                registerBatteryReceiver()
                evaluateBattery(battery)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(repeatRunnable)
        unregisterBatteryReceiver()
        alarmPlayer.stop()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun loadSettingsSync() {
        runBlocking {
            val settings = preferences.settings.first()
            thresholdPercent = settings.thresholdPercent
            alarmEnabled = settings.alarmEnabled
            ringtoneUri = settings.ringtoneUri?.let(Uri::parse)
        }
    }

    private fun registerBatteryReceiver() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        ContextCompat.registerReceiver(
            this,
            batteryReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    private fun unregisterBatteryReceiver() {
        try {
            unregisterReceiver(batteryReceiver)
        } catch (_: IllegalArgumentException) {
        }
    }

    private fun evaluateBattery(battery: BatteryInfo) {
        val newState = battery.resolveAlarmState(thresholdPercent)

        if (battery.isCharging || (battery.levelPercent > thresholdPercent)) {
            userStoppedAlarm = false
        }

        if (newState != currentAlarmState) {
            currentAlarmState = newState
            scope.launch { preferences.setLastAlarmState(newState) }
            handler.removeCallbacks(repeatRunnable)
        }

        updateForegroundNotification(battery)

        if (!alarmEnabled || userStoppedAlarm) {
            if (isAlarmCycleActive) stopAlarmCycle(resetUserStop = false)
            return
        }

        if (newState.shouldAlarm(thresholdPercent, battery)) {
            if (!isAlarmCycleActive) {
                startAlarmCycle(battery)
            }
        } else {
            stopAlarmCycle(resetUserStop = false)
        }
    }

    private fun startAlarmCycle(battery: BatteryInfo) {
        isAlarmCycleActive = true
        playAlarmSound(loopWhileActive = true)
        scheduleNextRepeat()
        showAlarmNotification(battery)
    }

    private fun stopAlarmCycle(resetUserStop: Boolean) {
        if (resetUserStop) userStoppedAlarm = false
        if (!isAlarmCycleActive) return
        isAlarmCycleActive = false
        handler.removeCallbacks(repeatRunnable)
        stopAlarmPlayback()
        val battery = readBatteryInfo()
        startForeground(
            NotificationHelper.NOTIFICATION_MONITORING,
            NotificationHelper.buildMonitoringNotification(
                this,
                battery.levelPercent,
                battery.isCharging,
            ),
        )
    }

    private fun onRepeatTick() {
        val battery = readBatteryInfo()
        val state = battery.resolveAlarmState(thresholdPercent)
        currentAlarmState = state

        if (!alarmEnabled || userStoppedAlarm || !state.shouldAlarm(thresholdPercent, battery)) {
            stopAlarmCycle(resetUserStop = false)
            evaluateBattery(battery)
            return
        }

        if (!alarmPlayer.isPlaying) {
            playAlarmSound(loopWhileActive = true)
        }
        showAlarmNotification(battery)
        scheduleNextRepeat()
    }

    private fun scheduleNextRepeat() {
        handler.removeCallbacks(repeatRunnable)
        val interval = currentAlarmState.repeatIntervalMs()
        if (interval > 0) {
            handler.postDelayed(repeatRunnable, interval)
        }
    }

    private fun playAlarmSound(loopWhileActive: Boolean) {
        alarmPlayer.play(ringtoneUri, loopOnComplete = loopWhileActive)
    }

    private fun stopAlarmPlayback() {
        alarmPlayer.stop()
    }

    private fun showAlarmNotification(battery: BatteryInfo) {
        val notification = NotificationHelper.buildAlarmNotification(
            this,
            battery.levelPercent,
            thresholdPercent,
        )
        startForeground(NotificationHelper.NOTIFICATION_ALARM, notification)
    }

    private fun updateForegroundNotification(battery: BatteryInfo) {
        if (isAlarmCycleActive) {
            showAlarmNotification(battery)
        } else {
            startForeground(
                NotificationHelper.NOTIFICATION_MONITORING,
                NotificationHelper.buildMonitoringNotification(
                    this,
                    battery.levelPercent,
                    battery.isCharging,
                ),
            )
        }
    }

    companion object {
        const val ACTION_STOP_ALARM = NotificationHelper.ACTION_STOP_ALARM
        const val ACTION_TEST_ALARM = "com.example.lowbatterynotifier.TEST_ALARM"

        fun startMonitoring(context: Context) {
            val intent = Intent(context, LowBatteryMonitorService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopMonitoring(context: Context) {
            context.stopService(Intent(context, LowBatteryMonitorService::class.java))
        }

        fun testAlarm(context: Context) {
            val intent = Intent(context, LowBatteryMonitorService::class.java).apply {
                action = ACTION_TEST_ALARM
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopAlarm(context: Context) {
            val intent = Intent(context, LowBatteryMonitorService::class.java).apply {
                action = ACTION_STOP_ALARM
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
