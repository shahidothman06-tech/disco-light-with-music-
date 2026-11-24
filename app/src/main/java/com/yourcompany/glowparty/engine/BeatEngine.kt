package com.yourcompany.glowparty.engine

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.Visualizer
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.yourcompany.glowparty.data.SoundSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Listens to the chosen sound source, turns it into a 256-bin spectrum (about 43 Hz per bin)
 * and detects beats. Shared by the light screens and the screen-off service.
 * All audio is analysed in memory and thrown away. Nothing is recorded or stored.
 */
object BeatEngine {
    const val BINS = 256

    /** 0..1 per bin. Read by the UI every frame. */
    val spectrum = FloatArray(BINS)

    @Volatile var bass = 0f; private set
    @Volatile var mid = 0f; private set
    @Volatile var high = 0f; private set
    @Volatile var volume = 0f; private set
    @Volatile var beatCount = 0L; private set

    @Volatile var sensitivity = 5
    @Volatile var bpm = 120

    private val _beats = MutableSharedFlow<Long>(extraBufferCapacity = 16)
    val beats: SharedFlow<Long> = _beats

    /** Problems the UI should show (for example "microphone permission missing"). */
    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private var users = 0
    private var source = SoundSource.AUTO
    private lateinit var appContext: Context

    private var avg = 0.2f
    private var lastBeatMs = 0L

    /** Start (or keep) the engine running for one more user. */
    @Synchronized
    fun acquire(context: Context, src: SoundSource) {
        appContext = context.applicationContext
        users++
        if (users == 1 || src != source || job?.isActive != true) restart(src)
    }

    /** Release one user. The engine stops when nobody is using it. */
    @Synchronized
    fun release() {
        users = max(0, users - 1)
        if (users == 0) {
            job?.cancel()
            job = null
            clear()
        }
    }

    /** Switch source while running. */
    @Synchronized
    fun setSource(context: Context, src: SoundSource) {
        appContext = context.applicationContext
        if (users > 0 && src != source) restart(src)
        source = src
    }

    private fun restart(src: SoundSource) {
        source = src
        job?.cancel()
        _status.value = null
        avg = 0.2f
        job = scope.launch {
            when (src) {
                SoundSource.AUTO -> runBuiltIn()
                SoundSource.MIC -> if (hasMic()) runMic() else fallBack("Microphone permission is off. Using the built-in beat.")
                SoundSource.PLAYER -> if (hasMic()) runPlayer() else fallBack("Microphone permission is off. Using the built-in beat.")
            }
        }
    }

    private suspend fun fallBack(msg: String) {
        _status.value = msg
        runBuiltIn()
    }

    private fun hasMic() = ContextCompat.checkSelfPermission(
        appContext, Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED

    private fun clear() {
        spectrum.fill(0f)
        bass = 0f; mid = 0f; high = 0f; volume = 0f
    }

    // ---------------------------------------------------------------- sources

    /** A steady synthetic beat, so every mode works with no sound at all. */
    private suspend fun runBuiltIn() {
        while (currentCoroutineContextActive()) {
            val t = SystemClock.elapsedRealtime()
            val per = 60_000f / bpm
            val ph = (t % per.toLong()) / per
            val b = max(0f, 1f - ph * 2.6f) * 0.85f + 0.08f
            val m = 0.3f + 0.15f * sin(t / 310f)
            val h = 0.22f + 0.12f * sin(t / 170f)
            for (i in 0 until BINS) {
                val level = when {
                    i < 6 -> b
                    i < 60 -> m
                    else -> h
                }
                spectrum[i] = (level * (1f - i / 350f) * (0.7f + 0.3f * sin(t / 190f + i * 0.35f))).coerceIn(0f, 1f)
            }
            analyse(b, m, h, t)
            delay(16)
        }
    }

    @SuppressLint("MissingPermission") // checked in hasMic()
    private suspend fun runMic() {
        val rate = 44_100
        val n = 1024
        val minBuf = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val record = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC, rate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, max(minBuf, n * 4),
            )
        } catch (e: Exception) {
            fallBack("Couldn't open the microphone. Using the built-in beat.")
            return
        }
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            fallBack("Couldn't open the microphone. Using the built-in beat.")
            return
        }
        val pcm = ShortArray(n)
        val re = FloatArray(n)
        val im = FloatArray(n)
        val window = Fft.hann(n)
        try {
            record.startRecording()
            while (currentCoroutineContextActive()) {
                val read = record.read(pcm, 0, n)
                if (read <= 0) { delay(5); continue }
                for (i in 0 until n) {
                    re[i] = if (i < read) pcm[i] / 32768f * window[i] else 0f
                    im[i] = 0f
                }
                Fft.transform(re, im)
                for (k in 0 until BINS) {
                    val mag = hypot(re[k], im[k]) / (n / 4f)
                    spectrum[k] = toLevel(mag, floorDb = -70f)
                }
                pushBands(SystemClock.elapsedRealtime())
            }
        } finally {
            runCatching { record.stop() }
            record.release()
        }
    }

    /** Uses Android's Visualizer on the global output mix (music from any player app). */
    private suspend fun runPlayer() {
        val viz = try {
            Visualizer(0).apply {
                enabled = false
                captureSize = Visualizer.getCaptureSizeRange()[1]
            }
        } catch (e: Exception) {
            fallBack("This phone doesn't allow reading phone music. Try Microphone instead.")
            return
        }
        try {
            viz.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, samplingRate: Int) = Unit
                override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                    if (fft == null) return
                    val bins = min(BINS, fft.size / 2)
                    for (k in 1 until bins) {
                        val mag = hypot(fft[2 * k].toFloat(), fft[2 * k + 1].toFloat()) / 128f
                        spectrum[k] = toLevel(mag, floorDb = -60f)
                    }
                    spectrum[0] = spectrum[1]
                    pushBands(SystemClock.elapsedRealtime())
                }
            }, Visualizer.getMaxCaptureRate(), false, true)
            viz.enabled = true
            awaitCancellation()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            fallBack("Couldn't read phone music. Using the built-in beat.")
        } finally {
            runCatching { viz.enabled = false }
            viz.release()
        }
    }

    // ---------------------------------------------------------------- analysis

    private fun toLevel(mag: Float, floorDb: Float): Float {
        val db = 20f * log10(mag + 1e-9f)
        return ((db - floorDb) / -floorDb).coerceIn(0f, 1f)
    }

    private fun band(from: Int, to: Int): Float {
        var s = 0f
        for (i in from..to) s += spectrum[i]
        return s / (to - from + 1)
    }

    private fun pushBands(now: Long) {
        analyse(band(1, 6), band(7, 60), band(61, 230), now)
    }

    private fun analyse(b: Float, m: Float, h: Float, now: Long) {
        bass = b; mid = m; high = h
        volume = ((b * 2 + m + h) / 3f).coerceIn(0f, 1f)
        val k = 1.6f - sensitivity * 0.07f
        if (b > avg * k && b > 0.16f && now - lastBeatMs > 200) {
            lastBeatMs = now
            beatCount++
            _beats.tryEmit(beatCount)
        }
        avg = avg * 0.93f + b * 0.07f
    }

    private suspend fun currentCoroutineContextActive(): Boolean =
        kotlinx.coroutines.currentCoroutineContext().isActive
}
