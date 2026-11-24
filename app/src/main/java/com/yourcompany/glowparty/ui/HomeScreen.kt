package com.yourcompany.glowparty.ui

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourcompany.glowparty.ads.HomeBanner
import com.yourcompany.glowparty.data.GlowSettings
import com.yourcompany.glowparty.data.Mode
import com.yourcompany.glowparty.ui.theme.Glow

@Composable
fun HomeScreen(
    settings: GlowSettings,
    onMode: (Mode) -> Unit,
    onLightShow: () -> Unit,
    onSettings: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Glow.Bg)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Glow Party", style = MaterialTheme.typography.headlineSmall, color = Glow.Ink)
                Text(
                    "Sound: ${settings.source.label.lowercase()}",
                    style = MaterialTheme.typography.bodyMedium, color = Glow.Dim,
                )
            }
            LightShowButton(onLightShow)
        }

        val tiles: List<Mode?> = Mode.entries + listOf(null)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(tiles) { mode ->
                ModePad(
                    mode = mode,
                    showLabel = mode == null || mode.ordinal >= Mode.RAVE.ordinal,
                    onClick = { if (mode == null) onSettings() else onMode(mode) },
                )
            }
        }
        HomeBanner(Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun ModePad(mode: Mode?, showLabel: Boolean, onClick: () -> Unit) {
    val color = NeonIcons.colorOf.getValue(mode)
    val icon = NeonIcons.of(mode)
    val name = mode?.title ?: "Settings"
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 104.dp)
            .clip(shape)
            .background(Brush.radialGradient(listOf(Glow.SurfaceHi, Glow.Surface)))
            .border(1.dp, Glow.Line, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = name }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Glow: a blurred copy under the sharp icon (blur needs Android 12+).
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Image(icon, contentDescription = null, modifier = Modifier.size(60.dp).blur(10.dp))
            }
            Image(icon, contentDescription = null, modifier = Modifier.size(60.dp))
        }
        if (showLabel) {
            Spacer(Modifier.height(4.dp))
            Text(
                name.uppercase(),
                style = TextStyle(
                    color = color,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    shadow = Shadow(color = color.copy(alpha = 0.8f), blurRadius = 18f),
                ),
            )
        }
    }
}

@Composable
private fun LightShowButton(onClick: () -> Unit) {
    val t = rememberInfiniteTransition(label = "sheen")
    val x by t.animateFloat(
        initialValue = 0f, targetValue = 600f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "x",
    )
    val brush = Brush.linearGradient(
        colors = listOf(Glow.Pink, Glow.Purple, Glow.Blue, Glow.Pink),
        start = androidx.compose.ui.geometry.Offset(-x, 0f),
        end = androidx.compose.ui.geometry.Offset(600f - x, 0f),
        tileMode = TileMode.Repeated,
    )
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier
            .shadow(16.dp, shape, ambientColor = Glow.Pink, spotColor = Glow.Pink)
            .clip(shape)
            .background(brush)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("▶", color = Color.White, fontSize = 13.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            "Light show",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            fontSize = 16.sp,
        )
    }
}
