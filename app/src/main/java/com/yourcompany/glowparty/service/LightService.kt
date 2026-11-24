package com.yourcompany.glowparty.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.yourcompany.glowparty.GlowApp
import com.yourcompany.glowparty.MainActivity
import com.yourcompany.glowparty.R
import com.yourcompany.glowparty.engine.BeatEngine
import com.yourcompany.glowparty.engine.Haptics
import com.yourcompany.glowparty.engine.Torch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Optional "screen-off mode". Keeps listening to the music and flashes the flashlight
 * (and vibrates, if enabled) while the phone is locked. Android requires a visible
 * notification for this, with a Stop button.
 */
class LightService : LifecycleService() {

    companion object {
        private const val CHANNEL = "glow_screen_off"
        private const val NOTIF_ID = 42
        private const val ACTION_STOP = "com.yourcompany.glowparty.STOP"

        private val _running = MutableStateFlow(false)
        val running: StateFlow<Boolean> = _running

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, LightService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LightService::class.java))
        }
    }

    private lateinit var torch: Torch
    private lateinit var haptics: Haptics
    @Volatile private var useTorch = false
    @Volatile private var useVibe = false
    private var engineHeld = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (_running.value) return START_NOT_STICKY

        createChannel()
        try {
            ServiceCompat.startForeground(
                this, NOTIF_ID, buildNotification(),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0,
            )
        } catch (e: Exception) {
            // Missing microphone permission or app not in the foreground: don't crash.
            stopSelf()
            return START_NOT_STICKY
        }

        _running.value = true
        torch = Torch(this)
        haptics = Haptics(this)
        val repo = (application as GlowApp).settings

        lifecycleScope.launch {
            val s = repo.current()
            BeatEngine.sensitivity = s.sensitivity
            BeatEngine.bpm = s.bpm
            BeatEngine.acquire(this@LightService, s.source)
            engineHeld = true
        }
        lifecycleScope.launch {
            repo.flow.collect { s ->
                useTorch = s.flashlight
                useVibe = s.vibrate
                BeatEngine.sensitivity = s.sensitivity
                BeatEngine.bpm = s.bpm
            }
        }
        lifecycleScope.launch {
            BeatEngine.beats.collect {
                if (useTorch) torch.pulse(lifecycleScope)
                if (useVibe) haptics.tick()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        _running.value = false
        if (engineHeld) BeatEngine.release()
        engineHeld = false
        if (::torch.isInitialized) torch.set(false)
        super.onDestroy()
    }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        val ch = NotificationChannel(CHANNEL, getString(R.string.notif_channel), NotificationManager.IMPORTANCE_LOW)
        nm?.createNotificationChannel(ch)
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(getString(R.string.notif_title))
        .setContentText(getString(R.string.notif_text))
        .setOngoing(true)
        .setSilent(true)
        .setContentIntent(
            PendingIntent.getActivity(
                this, 0, Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            ),
        )
        .addAction(
            0, getString(R.string.notif_stop),
            PendingIntent.getService(
                this, 1, Intent(this, LightService::class.java).setAction(ACTION_STOP),
                PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        .build()
}
