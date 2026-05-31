package com.example.lowbatterynotifier.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.lowbatterynotifier.MainActivity
import com.example.lowbatterynotifier.R
import com.example.lowbatterynotifier.service.LowBatteryMonitorService

object NotificationHelper {

    const val CHANNEL_MONITORING = "battery_monitoring"
    const val CHANNEL_ALARM = "low_battery_alarm"

    const val NOTIFICATION_MONITORING = 1001
    const val NOTIFICATION_ALARM = 1002

    const val ACTION_STOP_ALARM = "com.example.lowbatterynotifier.STOP_ALARM"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return

            val monitoringChannel = NotificationChannel(
                CHANNEL_MONITORING,
                context.getString(R.string.channel_monitoring_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.channel_monitoring_desc)
            }

            val alarmChannel = NotificationChannel(
                CHANNEL_ALARM,
                context.getString(R.string.channel_alarm_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.channel_alarm_desc)
                enableVibration(true)
            }

            manager.createNotificationChannels(listOf(monitoringChannel, alarmChannel))
        }
    }

    fun buildMonitoringNotification(context: Context, batteryPercent: Int, isCharging: Boolean): Notification {
        val content = context.getString(
            R.string.notification_monitoring_text,
            batteryPercent,
            chargingLabel(context, isCharging),
        )
        return NotificationCompat.Builder(context, CHANNEL_MONITORING)
            .setSmallIcon(R.drawable.ic_battery_alert)
            .setContentTitle(context.getString(R.string.notification_monitoring_title))
            .setContentText(content)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openAppPendingIntent(context))
            .build()
    }

    fun buildAlarmNotification(
        context: Context,
        batteryPercent: Int,
        thresholdPercent: Int,
    ): Notification {
        val content = context.getString(
            R.string.notification_alarm_text,
            batteryPercent,
            thresholdPercent,
        )
        return NotificationCompat.Builder(context, CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_battery_alert)
            .setContentTitle(context.getString(R.string.notification_alarm_title))
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(openAppPendingIntent(context))
            .addAction(
                R.drawable.ic_battery_alert,
                context.getString(R.string.action_stop_alarm),
                stopAlarmPendingIntent(context),
            )
            .addAction(
                R.drawable.ic_battery_alert,
                context.getString(R.string.action_open_app),
                openAppPendingIntent(context),
            )
            .build()
    }

    private fun openAppPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun stopAlarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, LowBatteryMonitorService::class.java).apply {
            action = ACTION_STOP_ALARM
        }
        return PendingIntent.getService(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun chargingLabel(context: Context, isCharging: Boolean): String =
        if (isCharging) {
            context.getString(R.string.charging_yes)
        } else {
            context.getString(R.string.charging_no)
        }
}
