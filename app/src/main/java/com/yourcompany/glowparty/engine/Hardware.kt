package com.yourcompany.glowparty.engine

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/** Camera flash used as a light. Needs no permission and never opens the camera. */
class Torch(context: Context) {
    private val cm = context.getSystemService(CameraManager::class.java)
    private val id: String? = runCatching {
        val m = cm ?: return@runCatching null
        m.cameraIdList.firstOrNull { m.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true }
    }.getOrNull()
    private var pulseJob: Job? = null

    val available: Boolean get() = id != null

    fun set(on: Boolean) {
        val cam = id ?: return
        runCatching { cm?.setTorchMode(cam, on) }
    }

    fun pulse(scope: CoroutineScope, ms: Long = 70) {
        if (id == null) return
        pulseJob?.cancel()
        pulseJob = scope.launch {
            set(true)
            delay(ms)
            set(false)
        }
    }
}

/** Short buzz on the beat. */
class Haptics(context: Context) {
    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }

    fun tick() {
        runCatching { vibrator?.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE)) }
    }
}

/** Calls [onShake] when the phone is shaken hard. */
class ShakeDetector(context: Context, private val onShake: () -> Unit) : SensorEventListener {
    private val sm = context.getSystemService(SensorManager::class.java)
    private val linear = sm?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    private val sensor = linear ?: sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var last = 0L

    val available: Boolean get() = sensor != null

    fun start() { sensor?.let { sm?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) } }
    fun stop() { sm?.unregisterListener(this) }

    override fun onSensorChanged(e: SensorEvent) {
        val (x, y, z) = Triple(e.values[0], e.values[1], e.values[2])
        var m = sqrt(x * x + y * y + z * z)
        if (linear == null) m = abs(m - SensorManager.GRAVITY_EARTH)
        val now = SystemClock.elapsedRealtime()
        if (m > 12f && now - last > 220) {
            last = now
            onShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

/** Generates a police-style siren tone in real time. */
class Siren {
    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        if (job?.isActive == true) return
        job = scope.launch(Dispatchers.Default) {
            val sr = 44_100
            val minBuf = AudioTrack.getMinBufferSize(sr, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sr)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(max(minBuf, 8192))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            val buf = ShortArray(1024)
            var phase = 0.0
            var t = 0.0
            try {
                track.play()
                while (isActive) {
                    for (i in buf.indices) {
                        val f = 950.0 + 380.0 * sin(2 * PI * 0.6 * t)
                        phase += f / sr
                        phase -= floor(phase)
                        val saw = 2 * phase - 1
                        buf[i] = (saw * 0.18 * Short.MAX_VALUE).toInt().toShort()
                        t += 1.0 / sr
                    }
                    track.write(buf, 0, buf.size)
                }
            } finally {
                runCatching { track.stop() }
                track.release()
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
