package com.yourcompany.glowparty.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import com.yourcompany.glowparty.data.GlowSettings
import com.yourcompany.glowparty.data.Mode
import com.yourcompany.glowparty.engine.BeatEngine
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random
import kotlin.math.sin

/** Draws one frame of the current light mode. Keeps per-mode animation state. */
class LightRenderer {
    private class Ring(var r: Float, val c: Color)

    private var mode: Mode? = null
    private var ci = 0
    private var f = 0f
    private var ph = 0f
    private var rot = 0f
    private var strobeStep = -1L
    private var beats = 0
    private val rings = ArrayList<Ring>()
    private var shakeSeen = -1L
    private var lastBeat = 0L
    private var lastNanos = 0L
    private var tileA = FloatArray(0)
    private var tileC = IntArray(0)
    private val peaks = FloatArray(64)

    private fun reset(m: Mode) {
        mode = m
        ci = 0; f = 0f; ph = 0f; rot = 0f; strobeStep = -1L; beats = 0
        rings.clear(); shakeSeen = -1L
        tileA = FloatArray(0); tileC = IntArray(0); peaks.fill(0f)
        lastBeat = BeatEngine.beatCount
    }

    fun DrawScope.render(m: Mode, pal: List<Color>, s: GlowSettings, nanos: Long, shakeHits: Long) {
        if (m != mode) reset(m)
        val dt = if (lastNanos == 0L) 0f else ((nanos - lastNanos) / 1e9f).coerceIn(0f, 0.05f)
        lastNanos = nanos
        val bc = BeatEngine.beatCount
        val beat = bc != lastBeat
        lastBeat = bc
        val bass = BeatEngine.bass
        val vol = BeatEngine.volume
        val n = pal.size.coerceAtLeast(1)
        val w = size.width
        val h = size.height
        val nowMs = nanos / 1_000_000L

        when (m) {
            Mode.DISCO -> {
                if (beat) ci = (ci + 1) % n
                drawRect(pal[ci % n])
            }

            Mode.SMOOTH -> {
                ph += dt * (0.15f + vol * 1.6f) * (s.speed / 5f)
                val i = ph.toInt() % n
                drawRect(lerp(pal[i], pal[(i + 1) % n], ph - floor(ph)))
                if (beat) f = 0.35f
                f = max(0f, f - dt * 2f)
                if (f > 0f) drawRect(Color.White, alpha = f)
            }

            Mode.FLARE -> {
                if (beat) { f = 1f; ci = (ci + 1) % n }
                f = max(0f, f - dt * (0.6f + s.speed * 0.35f))
                drawRect(Color.Black)
                drawRect(pal[ci % n], alpha = f)
            }

            Mode.STROBE -> {
                val per = (320 - s.speed * 29).toLong()
                val step = nowMs / per
                if (step != strobeStep) {
                    strobeStep = step
                    if (step % 2 == 0L) ci = (ci + 1) % n
                }
                drawRect(if (step % 2 == 0L || beat) pal[ci % n] else Color.Black)
            }

            Mode.RAVE -> rave(pal, n, s, dt, beat, bass, vol)

            Mode.SPECTRUM -> spectrum(pal, n, dt)

            Mode.GRADIENT -> {
                val cx = w / 2; val cy = h / 2
                val radius = hypot(w, h) / 2
                ph += dt * (0.2f + bass * 2f) * (s.speed / 5f)
                rotate(degrees = ph * 57.2958f, pivot = Offset(cx, cy)) {
                    drawCircle(Brush.sweepGradient(pal + pal.first(), Offset(cx, cy)), radius, Offset(cx, cy))
                }
                val clear = (0.15f + bass * 0.5f).coerceAtMost(0.9f)
                drawRect(
                    Brush.radialGradient(
                        0f to Color.Transparent, clear to Color.Transparent, 1f to Color.Black.copy(alpha = 0.9f),
                        center = Offset(cx, cy), radius = radius,
                    ),
                )
                if (beat) { ci = (ci + 1) % n; rings.add(Ring(0f, pal[ci])) }
                rings.removeAll { it.r > radius }
                rings.forEach { ring ->
                    ring.r += dt * radius * 1.3f
                    drawCircle(
                        ring.c, ring.r.coerceAtLeast(1f), Offset(cx, cy),
                        alpha = (1f - ring.r / radius).coerceIn(0f, 1f), style = Stroke(width = 7f * density),
                    )
                }
            }

            Mode.TILES -> {
                drawRect(Color.Black)
                val cols = if (w > h) 7 else 4
                val ts = w / cols
                val rows = ceil(h / ts).toInt()
                val count = cols * rows
                if (tileA.size != count) { tileA = FloatArray(count); tileC = IntArray(count) }
                if (beat) {
                    beats++
                    val pattern = (beats / 8) % 4
                    val c = beats % n
                    for (i in 0 until count) {
                        val r = i / cols; val k = i % cols
                        val on = when (pattern) {
                            0 -> Random.nextFloat() < 0.22f
                            1 -> r == beats % rows
                            2 -> k == beats % cols
                            else -> (r + k + beats) % 2 == 0
                        }
                        if (on) { tileA[i] = 1f; tileC[i] = c }
                    }
                }
                val gap = 3f * density
                for (i in 0 until count) {
                    tileA[i] = max(0f, tileA[i] - dt * (1f + s.speed * 0.3f))
                    val col = if (tileA[i] > 0f) pal[tileC[i] % n] else Color(0xFF333333)
                    drawRoundRect(
                        col, Offset((i % cols) * ts + gap, (i / cols) * ts + gap), Size(ts - 2 * gap, ts - 2 * gap),
                        CornerRadius(8f * density), alpha = 0.06f + tileA[i] * 0.94f,
                    )
                }
            }

            Mode.SHAKE -> {
                if (shakeSeen < 0) shakeSeen = shakeHits
                if (shakeHits != shakeSeen) { shakeSeen = shakeHits; f = 1f; ci = (ci + 1) % n }
                f = max(0f, f - dt * 2.5f)
                drawRect(Color.Black)
                drawRect(pal[ci % n], alpha = f)
            }

            Mode.SYNC -> {
                // Uses the phone clock, so every phone with the same colours and speed matches.
                val per = (2200 - s.speed * 200).toLong()
                val now = System.currentTimeMillis()
                val i = now / per
                val frac = (now % per) / per.toFloat()
                val e = if (frac < 0.75f) 0f else (frac - 0.75f) / 0.25f
                drawRect(lerp(pal[(i % n).toInt()], pal[((i + 1) % n).toInt()], e * e * (3 - 2 * e)))
            }

            Mode.SIREN -> {
                val step = ((nowMs / 90) % 12).toInt()
                val left = step < 6
                val on = step % 6 == 0 || step % 6 == 2 || step % 6 == 4
                val c = if (left) Color(0xFFFF1A1A) else Color(0xFF1A4DFF)
                drawRect(Color.Black)
                if (on) drawRect(c, topLeft = Offset(if (left) 0f else w / 2, 0f), size = Size(w / 2, h))
                val gx = if (left) w * 0.25f else w * 0.75f
                drawRect(
                    Brush.radialGradient(
                        listOf(c.copy(alpha = 0.35f), Color.Transparent), center = Offset(gx, h / 2), radius = w * 0.6f,
                    ),
                )
            }
        }
    }

    private fun DrawScope.rave(pal: List<Color>, n: Int, s: GlowSettings, dt: Float, beat: Boolean, bass: Float, vol: Float) {
        val w = size.width
        val h = size.height
        drawRect(Color.Black)
        rot += dt * (0.35f + vol * 1.8f) * (s.speed / 5f)
        if (beat) ci = (ci + 1) % n
        val cx = w / 2
        val cy = h * 0.3f
        val r = min(w, h) * 0.15f

        // Lasers
        for (i in 0 until 7) {
            val ang = PI.toFloat() / 2 + sin(rot * 1.4f + i * 0.9f) * 1.05f
            drawLine(
                pal[(ci + i) % n], Offset(cx, cy), Offset(cx + cos(ang) * h * 1.6f, cy + sin(ang) * h * 1.6f),
                strokeWidth = (1.5f + bass * 5f) * density, alpha = (0.35f + bass * 0.6f).coerceAtMost(1f),
                blendMode = BlendMode.Plus,
            )
        }
        // Light spots
        for (i in 0 until 34) {
            val ang = i * 2.399f + rot * (if (i % 2 == 1) 0.6f else -0.4f)
            val rr = (0.25f + (i % 7) / 9f) * max(w, h) * 0.6f
            val p = Offset(cx + cos(ang) * rr, cy + h * 0.2f + sin(ang) * rr * 0.8f)
            val sr = (8f + vol * 22f) * density
            drawCircle(
                Brush.radialGradient(listOf(pal[i % n], Color.Transparent), center = p, radius = sr),
                sr, p, alpha = (0.5f + vol * 0.5f).coerceAtMost(1f), blendMode = BlendMode.Plus,
            )
        }
        // Disco ball
        drawLine(Color(0xFF666666), Offset(cx, 0f), Offset(cx, cy - r), strokeWidth = 1.5f * density)
        val ball = Path().apply { addOval(Rect(center = Offset(cx, cy), radius = r)) }
        clipPath(ball) {
            drawRect(
                Brush.radialGradient(
                    listOf(Color(0xFFBBBBBB), Color(0xFF222222)), center = Offset(cx - r * 0.3f, cy - r * 0.3f), radius = r * 1.3f,
                ),
                topLeft = Offset(cx - r, cy - r), size = Size(2 * r, 2 * r),
            )
            val sq = r / 5
            val off = (rot * r) % sq
            for (row in -5..5) for (col in -7..7) {
                val br = max(0f, sin(col * 0.8f + row * 1.3f + rot * 4f))
                drawRect(
                    Color.White.copy(alpha = (0.08f + br * 0.8f * (0.4f + vol)).coerceIn(0f, 1f)),
                    topLeft = Offset(cx + col * sq + off + 1f, cy + row * sq + 1f), size = Size(sq - 2f, sq - 2f),
                )
            }
        }
    }

    private fun DrawScope.spectrum(pal: List<Color>, n: Int, dt: Float) {
        val w = size.width
        val h = size.height
        drawRect(Color.Black)
        val bars = (w / (12f * density)).toInt().coerceIn(16, 64)
        val bw = w / bars
        val base = h * 0.72f
        val spec = BeatEngine.spectrum
        for (i in 0 until bars) {
            val lo = 1.075.pow(i * 64.0 / bars).toInt().coerceIn(0, spec.size - 1)
            val hi = (1.075.pow((i + 1) * 64.0 / bars).toInt() + 1).coerceIn(lo, spec.size - 1)
            var v = 0f
            for (j in lo..hi) v = max(v, spec[j])
            v = v.pow(1.4f)
            peaks[i] = max(v, peaks[i] - dt * 0.6f)
            val bh = v * h * 0.62f
            val c = pal[(i * n / bars) % n]
            val x = i * bw + 1.5f
            drawRect(c, Offset(x, base - bh), Size(bw - 3f, bh))
            drawRect(c, Offset(x, base - peaks[i] * h * 0.62f - 6f), Size(bw - 3f, 3f * density))
            drawRect(c, Offset(x, base + 4f), Size(bw - 3f, bh * 0.4f), alpha = 0.25f)
        }
    }
}
