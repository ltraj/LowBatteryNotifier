package com.example.lowbatterynotifier.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log

class AlarmPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var shouldLoopOnComplete = false

    fun play(uri: Uri?, loopOnComplete: Boolean) {
        stop()
        shouldLoopOnComplete = loopOnComplete
        setMediaVolumeMax()

        val soundUri = uri ?: defaultAlarmUri()
        val player = try {
            MediaPlayer.create(context, soundUri)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to play selected sound, using default alarm", e)
            MediaPlayer.create(context, defaultAlarmUri())
        }

        if (player == null) {
            Log.e(TAG, "MediaPlayer could not be created")
            return
        }

        player.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        player.setOnCompletionListener {
            if (shouldLoopOnComplete) {
                it.seekTo(0)
                it.start()
            } else {
                stop()
            }
        }
        player.start()
        mediaPlayer = player
    }

    fun stop() {
        shouldLoopOnComplete = false
        mediaPlayer?.run {
            try {
                if (isPlaying) stop()
            } catch (_: IllegalStateException) {
            }
            release()
        }
        mediaPlayer = null
    }

    val isPlaying: Boolean
        get() = mediaPlayer?.isPlaying == true

    private fun setMediaVolumeMax() {
        val audioManager = context.getSystemService(AudioManager::class.java) ?: return
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)
    }

    private fun defaultAlarmUri(): Uri =
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    companion object {
        private const val TAG = "AlarmPlayer"
    }
}
