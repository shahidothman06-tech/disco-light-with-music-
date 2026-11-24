package com.yourcompany.glowparty.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import com.yourcompany.glowparty.data.Mode

/** Original neon line icons, drawn on a 64x64 grid. */
object NeonIcons {

    private class P(
        val d: String,
        val fill: Brush? = null,
        val stroke: Brush? = null,
        val width: Float = 2.5f,
        val alpha: Float = 1f,
    )

    private fun circle(cx: Float, cy: Float, r: Float) =
        "M${cx - r},$cy a$r,$r 0 1,0 ${2 * r},0 a$r,$r 0 1,0 ${-2 * r},0 Z"

    private fun rrect(x: Float, y: Float, w: Float, h: Float, r: Float) =
        "M${x + r},$y h${w - 2 * r} a$r,$r 0 0 1 $r,$r v${h - 2 * r} a$r,$r 0 0 1 ${-r},$r " +
            "h${-(w - 2 * r)} a$r,$r 0 0 1 ${-r},${-r} v${-(h - 2 * r)} a$r,$r 0 0 1 $r,${-r} Z"

    private fun build(name: String, vararg paths: P): ImageVector =
        ImageVector.Builder(name, 64.dp, 64.dp, 64f, 64f).apply {
            paths.forEach { p ->
                addPath(
                    pathData = PathParser().parsePathString(p.d).toNodes(),
                    fill = p.fill,
                    fillAlpha = p.alpha,
                    stroke = p.stroke,
                    strokeAlpha = p.alpha,
                    strokeLineWidth = p.width,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    private fun s(c: Long) = SolidColor(Color(c))

    val colorOf: Map<Mode?, Color> = mapOf(
        Mode.DISCO to Color(0xFFFF2D95),
        Mode.SMOOTH to Color(0xFFC77DFF),
        Mode.FLARE to Color(0xFFFFA21F),
        Mode.STROBE to Color(0xFFE8E8F0),
        Mode.RAVE to Color(0xFF1FE0E0),
        Mode.SPECTRUM to Color(0xFFFFD60A),
        Mode.GRADIENT to Color(0xFFB44DFF),
        Mode.TILES to Color(0xFFA6F20A),
        Mode.SHAKE to Color(0xFF2F8CFF),
        Mode.SYNC to Color(0xFF19D27A),
        Mode.SIREN to Color(0xFFFF3B3B),
        null to Color(0xFF9A9AA6), // Settings
    )

    val disco = build("disco", P(circle(32f, 32f, 22f), fill = s(0xFFFF2D95)))

    val smooth = build(
        "smooth",
        P(
            circle(32f, 32f, 22f),
            fill = Brush.verticalGradient(
                0f to Color(0xFFFF4FA3), 0.4f to Color(0xFFFFB347),
                0.6f to Color(0xFF5EC8FF), 1f to Color(0xFFA66BFF),
                startY = 10f, endY = 54f,
            ),
        ),
    )

    val flare = build(
        "flare",
        P(circle(32f, 32f, 9f), fill = s(0xFFFFA21F)),
        P(circle(32f, 32f, 17f), stroke = s(0xFFFFA21F), width = 3f, alpha = 0.7f),
        P(circle(32f, 32f, 25f), stroke = s(0xFFFFA21F), width = 3f, alpha = 0.35f),
    )

    val strobe = build("strobe", P("M36 6 L16 36 H29 L25 58 L47 26 H33 Z", fill = s(0xFFF4F4FA)))

    val rave = build(
        "rave",
        P(circle(32f, 34f, 14f), stroke = s(0xFF1FE0E0), width = 2f),
        P(
            "M18 34 H46 M20 27 H44 M20 41 H44 M32 20 V48 M25 21.5 C22 29.5 22 38.5 25 46.5 " +
                "M39 21.5 C42 29.5 42 38.5 39 46.5 M32 4 V14 M10 14 L17 21 M54 14 L47 21 M6 34 H12 M52 34 H58",
            stroke = s(0xFF1FE0E0), width = 2f,
        ),
    )

    val spectrum = build(
        "spectrum",
        P("M8 54 V34 H16 V54 M20 54 V22 H28 V54 M32 54 V12 H40 V54 M44 54 V28 H52 V54 M6 56 H58", stroke = s(0xFFFFD60A)),
    )

    val gradient = build(
        "gradient",
        P(
            "M32 32 A3 3 0 0 1 38 32 A8 8 0 0 1 24 36 A12 12 0 0 1 44 26 A17 17 0 0 1 36 49 A22 22 0 0 1 10 23",
            stroke = s(0xFFB44DFF),
        ),
    )

    val tiles: ImageVector = run {
        val paths = mutableListOf<P>()
        for (r in 0..2) for (c in 0..2) {
            val filled = r == c
            val d = rrect(10f + c * 16f, 10f + r * 16f, 12f, 12f, 2f)
            paths += if (filled) P(d, fill = s(0xFFA6F20A), stroke = s(0xFFA6F20A)) else P(d, stroke = s(0xFFA6F20A))
        }
        build("tiles", *paths.toTypedArray())
    }

    val shake = build(
        "shake",
        P(rrect(22f, 10f, 20f, 40f, 4f), stroke = s(0xFF2F8CFF)),
        P(
            "M12 20 C9 26 9 34 12 40 M6 16 C1 25 1 35 6 44 M52 20 C55 26 55 34 52 40 M58 16 C63 25 63 35 58 44 M22 58 H42",
            stroke = s(0xFF2F8CFF),
        ),
    )

    val sync = build(
        "sync",
        P(rrect(4f, 16f, 14f, 30f, 3f), stroke = s(0xFF19D27A)),
        P(rrect(46f, 16f, 14f, 30f, 3f), stroke = s(0xFF19D27A)),
        P("M18 31 H23 L25 26 L28 36 L31 26 L34 36 L37 26 L40 36 L42 31 H46", stroke = s(0xFF19D27A)),
    )

    val siren = build(
        "siren",
        P("M18 48 V32 A14 14 0 0 1 46 32 V48 M12 48 H52 V54 H12 Z M32 6 V12 M12 14 L16 18 M52 14 L48 18", stroke = s(0xFFFF3B3B)),
        P("M26 32 A6 6 0 0 1 32 26", stroke = s(0xFFFF3B3B), alpha = 0.7f),
    )

    val settings = build(
        "settings",
        P("M10 18 H54 M10 32 H54 M10 46 H54", stroke = s(0xFFB8B8C2)),
        P(circle(22f, 18f, 5f), fill = s(0xFF17171B), stroke = s(0xFFB8B8C2)),
        P(circle(42f, 32f, 5f), fill = s(0xFF17171B), stroke = s(0xFFB8B8C2)),
        P(circle(28f, 46f, 5f), fill = s(0xFF17171B), stroke = s(0xFFB8B8C2)),
    )

    fun of(mode: Mode?): ImageVector = when (mode) {
        Mode.DISCO -> disco
        Mode.SMOOTH -> smooth
        Mode.FLARE -> flare
        Mode.STROBE -> strobe
        Mode.RAVE -> rave
        Mode.SPECTRUM -> spectrum
        Mode.GRADIENT -> gradient
        Mode.TILES -> tiles
        Mode.SHAKE -> shake
        Mode.SYNC -> sync
        Mode.SIREN -> siren
        null -> settings
    }
}
