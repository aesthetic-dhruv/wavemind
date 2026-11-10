package com.raj.wavemind

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat

/**
 * Foreground service so a binaural session keeps playing with the
 * screen off, in the background, and offline — exactly what a sleep
 * session needs. Auto-stops when the timer ends.
 *
 * Actions via intents: ACTION_PLAY (extras: mode id, minutes),
 * ACTION_PAUSE, ACTION_RESUME, ACTION_STOP.
 * The activity binds to the service for live timer/volume state.
 */
class BinauralService : Service() {

    companion object {
        const val CHANNEL_ID = "wavemind_playback"
        const val NOTIFICATION_ID = 1
        const val ACTION_PLAY = "com.raj.wavemind.PLAY"
        const val ACTION_PAUSE = "com.raj.wavemind.PAUSE"
        const val ACTION_RESUME = "com.raj.wavemind.RESUME"
        const val ACTION_STOP = "com.raj.wavemind.STOP"
        const val EXTRA_MODE_ID = "mode_id"
        const val EXTRA_MINUTES = "minutes"
        const val EXTRA_VOLUME = "volume"
    }

    private val binder = LocalBinder()
    private val engine = BinauralEngine()
    private var currentMode: Mode = Mode.SLEEP
    private var timer: CountDownTimer? = null
    private var remainingMs: Long = 0L
    private var totalMs: Long = 0L
    private var playing = false
    private var wakeLock: PowerManager.WakeLock? = null

    inner class LocalBinder : Binder() {
        fun getService(): BinauralService = this@BinauralService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WaveMind::playback")
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "WaveMind playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Shows the current binaural session" }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun notification(text: String): Notification {
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, BinauralService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("WaveMind — ${currentMode.title}")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_wave)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopIntent)
            .setOngoing(true)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val mode = Mode.byId(intent.getStringExtra(EXTRA_MODE_ID) ?: "sleep")
                val minutes = intent.getIntExtra(EXTRA_MINUTES, mode.defaultMinutes)
                val volume = intent.getFloatExtra(EXTRA_VOLUME, 0.45f)
                play(mode, minutes, volume)
            }
            ACTION_PAUSE -> pause()
            ACTION_RESUME -> resume()
            ACTION_STOP -> {
                stopSession()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    fun play(mode: Mode, minutes: Int, volume: Float) {
        currentMode = mode
        totalMs = minutes * 60L * 1000L
        remainingMs = totalMs
        engine.setVolume(volume)
        engine.start(mode)
        playing = true
        if (wakeLock?.isHeld == false) wakeLock?.acquire()
        startForeground(NOTIFICATION_ID, notification("Playing • ${mode.tag}"))
        timer?.cancel()
        timer = object : CountDownTimer(totalMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingMs = millisUntilFinished
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NOTIFICATION_ID, notification("Playing • ${mode.tag}"))
            }

            override fun onFinish() {
                remainingMs = 0L
                stopSession()
                stopSelf()
            }
        }.start()
    }

    fun pause() {
        if (!playing) return
        engine.stop()
        timer?.cancel()
        playing = false
        wakeLock?.let { if (it.isHeld) it.release() }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification("Paused • ${currentMode.tag}"))
    }

    fun resume() {
        if (playing) return
        val minutes = ((remainingMs + 59999) / 60000).toInt().coerceAtLeast(1)
        play(currentMode, minutes, engine.getVolume())
    }

    fun stopSession() {
        engine.stop()
        timer?.cancel()
        timer = null
        playing = false
        remainingMs = 0L
        wakeLock?.let { if (it.isHeld) it.release() }
        stopForeground(true)
    }

    fun setVolume(v: Float) = engine.setVolume(v)
    fun getVolume(): Float = engine.getVolume()
    fun isPlaying(): Boolean = playing
    fun getCurrentMode(): Mode = currentMode
    fun getRemainingMs(): Long = remainingMs
    fun getTotalMs(): Long = totalMs

    override fun onDestroy() {
        stopSession()
        super.onDestroy()
    }
}
