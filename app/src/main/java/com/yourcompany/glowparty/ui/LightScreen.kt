package com.yourcompany.glowparty.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourcompany.glowparty.data.GlowSettings
import com.yourcompany.glowparty.data.LIGHT_SHOW_ORDER
import com.yourcompany.glowparty.data.Mode
import com.yourcompany.glowparty.data.PALETTE_ARGB
import com.yourcompany.glowparty.engine.BeatEngine
import com.yourcompany.glowparty.engine.Haptics
import com.yourcompany.glowparty.engine.ShakeDetector
import com.yourcompany.glowparty.engine.Siren
import com.yourcompany.glowparty.engine.Torch
import com.yourcompany.glowparty.service.LightService
import kotlinx.coroutines.delay

@Composable
fun LightScreen(
    startMode: Mode,
    lightShow: Boolean,
    settings: GlowSettings,
    onExit: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleSirenSound: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val s by rememberUpdatedState(settings)
    var mode by remember { mutableStateOf(startMode) }
    val palette = remember(settings.colors) { settings.colors.map { Color(PALETTE_ARGB[it]) } }

    BackHandler(onBack = onExit)

    // Full screen: hide status and navigation bars (swipe from the edge to see them).
    DisposableEffect(Unit) {
        val window = context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            view.keepScreenOn = false
        }
    }
    LaunchedEffect(settings.keepAwake) { view.keepScreenOn = settings.keepAwake }

    // Sound analysis runs while this screen is open.
    DisposableEffect(settings.source) {
        BeatEngine.acquire(context, settings.source)
        onDispose { BeatEngine.release() }
    }
    LaunchedEffect(settings.sensitivity, settings.bpm) {
        BeatEngine.sensitivity = settings.sensitivity
        BeatEngine.bpm = settings.bpm
    }

    // Screen-off mode: a foreground service keeps the flashlight going when the phone locks.
    DisposableEffect(settings.screenOff) {
        if (settings.screenOff && context.hasMic()) LightService.start(context)
        onDispose { LightService.stop(context) }
    }

    val torch = remember { Torch(context) }
    val haptics = remember { Haptics(context) }
    DisposableEffect(Unit) { onDispose { torch.set(false) } }

    LaunchedEffect(Unit) {
        BeatEngine.beats.collect {
            // When the service is running it handles the flashlight, so we don't double-flash.
            if (!LightService.running.value && mode.reactive) {
                if (s.flashlight) torch.pulse(scope)
                if (s.vibrate) haptics.tick()
            }
        }
    }

    var shakeHits by remember { mutableLongStateOf(0L) }
    val shake = remember {
        ShakeDetector(context) {
            shakeHits++
            if (s.flashlight) torch.pulse(scope)
            if (s.vibrate) haptics.tick()
        }
    }
    DisposableEffect(mode) {
        if (mode == Mode.SHAKE) shake.start()
        onDispose { shake.stop() }
    }

    val siren = remember { Siren() }
    DisposableEffect(mode, settings.sirenSound) {
        if (mode == Mode.SIREN && settings.sirenSound) siren.start(scope)
        onDispose { siren.stop() }
    }
    LaunchedEffect(mode, settings.flashlight) {
        if (mode == Mode.SIREN && settings.flashlight) {
            try {
                while (true) {
                    repeat(3) { torch.set(true); delay(90); torch.set(false); delay(90) }
                    delay(540)
                }
            } finally {
                torch.set(false)
            }
        }
    }

    // Light Show: step through the modes automatically.
    var showTitle by remember { mutableStateOf(false) }
    LaunchedEffect(lightShow, settings.showSeconds) {
        if (!lightShow) return@LaunchedEffect
        var idx = LIGHT_SHOW_ORDER.indexOf(mode).coerceAtLeast(0)
        while (true) {
            delay(settings.showSeconds * 1000L)
            idx = (idx + 1) % LIGHT_SHOW_ORDER.size
            mode = LIGHT_SHOW_ORDER[idx]
        }
    }
    LaunchedEffect(mode) {
        if (lightShow) {
            showTitle = true
            delay(1800)
            showTitle = false
        }
    }

    // Frame clock: redraw every display frame.
    var frame by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) { while (true) withFrameNanos { frame = it } }
    val renderer = remember { LightRenderer() }

    var hudTick by remember { mutableIntStateOf(0) }
    var hud by remember { mutableStateOf(true) }
    LaunchedEffect(hudTick) {
        hud = true
        delay(3500)
        hud = false
    }

    val status by BeatEngine.status.collectAsStateWithLifecycle()

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    hudTick++
                    if (mode == Mode.SHAKE) {
                        shakeHits++
                        if (s.flashlight) torch.pulse(scope)
                    }
                })
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val nanos = frame
            with(renderer) { render(mode, palette, s, nanos, shakeHits) }
        }

        AnimatedVisibility(
            visible = showTitle && lightShow,
            enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    mode.title,
                    style = TextStyle(
                        color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic,
                        shadow = Shadow(Color.Black, blurRadius = 24f),
                    ),
                )
                Text(
                    mode.hint,
                    style = TextStyle(
                        color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold, shadow = Shadow(Color.Black, blurRadius = 12f),
                    ),
                )
            }
        }

        AnimatedVisibility(hud, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.displayCutout)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)))
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HudButton("←", onExit, Modifier.size(48.dp), "Back")
                    Text(
                        (if (lightShow) "Light show · " else "") + mode.title,
                        modifier = Modifier.weight(1f),
                        style = TextStyle(
                            color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic,
                            shadow = Shadow(Color.Black, blurRadius = 8f),
                        ),
                    )
                    if (mode == Mode.SIREN) HudButton(if (settings.sirenSound) "Sound on" else "Sound off", onToggleSirenSound)
                    HudButton(settings.source.label, onOpenSettings)
                }
                Column(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    status?.let {
                        Text(
                            it, color = Color(0xFFFFCF4D), fontSize = 14.sp, textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }
                    Text(
                        mode.hint, color = Color.White.copy(alpha = 0.85f), fontSize = 15.sp,
                        textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 10.dp),
                    )
                    // Live sound level
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .drawBehind {
                                frame.let { }
                                drawRect(Color.White, size = Size(size.width * BeatEngine.volume, size.height))
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun HudButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, label: String? = null) {
    Box(
        modifier
            .heightIn(min = 48.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.6f))
            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontSize = 15.sp)
    }
}
