package com.yourcompany.glowparty.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object Glow {
    val Bg = Color(0xFF0B0B0E)
    val Surface = Color(0xFF17171B)
    val SurfaceHi = Color(0xFF1D1D22)
    val Line = Color(0xFF26262C)
    val Ink = Color(0xFFECECF1)
    val Dim = Color(0xFF8C8C96)
    val Pink = Color(0xFFFF2D95)
    val Purple = Color(0xFF9B30FF)
    val Blue = Color(0xFF00B8FF)
    val Amber = Color(0xFFFFCF4D)
}

private val scheme = darkColorScheme(
    primary = Glow.Pink,
    onPrimary = Color.White,
    secondary = Glow.Purple,
    background = Glow.Bg,
    onBackground = Glow.Ink,
    surface = Glow.Surface,
    onSurface = Glow.Ink,
    surfaceVariant = Glow.SurfaceHi,
    onSurfaceVariant = Glow.Dim,
    outline = Glow.Line,
    surfaceContainerHigh = Glow.SurfaceHi,
)

private val type = Typography(
    headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic),
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
)

@Composable
fun GlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = type, content = content)
}
