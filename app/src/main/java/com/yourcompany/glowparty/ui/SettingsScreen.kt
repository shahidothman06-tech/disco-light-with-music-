package com.yourcompany.glowparty.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yourcompany.glowparty.BuildConfig
import com.yourcompany.glowparty.ads.AdsManager
import com.yourcompany.glowparty.data.GlowSettings
import com.yourcompany.glowparty.data.PALETTE_ARGB
import com.yourcompany.glowparty.data.SoundSource
import com.yourcompany.glowparty.engine.Torch
import com.yourcompany.glowparty.ui.theme.Glow
import kotlinx.coroutines.launch

private enum class Pending { NONE, MIC, PLAYER, SCREEN_OFF }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: GlowSettings,
    onUpdate: ((GlowSettings) -> GlowSettings) -> Unit,
    onBack: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onPermissions: () -> Unit,
    onRate: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val hasFlash = remember { Torch(context).available }
    val privacyRequired by AdsManager.privacyOptionsRequired.collectAsState()

    var explain by remember { mutableStateOf(Pending.NONE) }
    var pending by remember { mutableStateOf(Pending.NONE) }
    var confirmReset by remember { mutableStateOf(false) }

    fun denied() {
        scope.launch {
            val r = snackbar.showSnackbar(
                "Microphone is off, so the built-in beat is used.", actionLabel = "Settings",
            )
            if (r == SnackbarResult.ActionPerformed) context.openAppSettings()
        }
    }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val p = pending
        pending = Pending.NONE
        if (!granted) { denied(); return@rememberLauncherForActivityResult }
        when (p) {
            Pending.MIC -> onUpdate { it.copy(source = SoundSource.MIC) }
            Pending.PLAYER -> onUpdate { it.copy(source = SoundSource.PLAYER) }
            else -> Unit
        }
    }
    val screenOffLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        pending = Pending.NONE
        if (res[Manifest.permission.RECORD_AUDIO] == true || context.hasMic()) {
            onUpdate { it.copy(screenOff = true) }
        } else {
            denied()
        }
    }

    fun request(p: Pending) {
        pending = p
        if (p == Pending.SCREEN_OFF) {
            val perms = buildList {
                add(Manifest.permission.RECORD_AUDIO)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
            }
            screenOffLauncher.launch(perms.toTypedArray())
        } else {
            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun pickSource(src: SoundSource) {
        when {
            src == SoundSource.AUTO -> onUpdate { it.copy(source = src) }
            context.hasMic() -> onUpdate { it.copy(source = src) }
            else -> explain = if (src == SoundSource.MIC) Pending.MIC else Pending.PLAYER
        }
    }

    Box(Modifier.fillMaxSize().background(Glow.Bg)) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader("Settings", onBack)
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = WindowInsets.safeDrawing
                    .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal).asPaddingValues(),
            ) {
                item {
                    Column(Modifier.padding(horizontal = 14.dp).padding(bottom = 24.dp)) {
                        Section("Sound") {
                            Column(Modifier.padding(16.dp)) {
                                Text("Sound source", style = MaterialTheme.typography.titleMedium, color = Glow.Ink)
                                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                    SoundSource.entries.forEachIndexed { i, src ->
                                        SegmentedButton(
                                            selected = settings.source == src,
                                            onClick = { pickSource(src) },
                                            shape = SegmentedButtonDefaults.itemShape(i, SoundSource.entries.size),
                                        ) { Text(src.label, maxLines = 1) }
                                    }
                                }
                                Text(
                                    when (settings.source) {
                                        SoundSource.AUTO -> "A steady beat. Works anywhere, no permission needed."
                                        SoundSource.MIC -> "Listens to music around you, from a speaker or a party."
                                        SoundSource.PLAYER -> "Follows music playing on this phone (Spotify, YouTube Music…)."
                                    },
                                    style = MaterialTheme.typography.bodyMedium, color = Glow.Dim,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                            RowDivider()
                            SliderRow("Sensitivity", settings.sensitivity, 1..10, "${settings.sensitivity}") { v -> onUpdate { it.copy(sensitivity = v) } }
                            RowDivider()
                            SliderRow("Built-in beat tempo", settings.bpm, 70..180, "${settings.bpm} BPM") { v -> onUpdate { it.copy(bpm = v) } }
                        }

                        Section("Effects") {
                            SliderRow("Speed", settings.speed, 1..10, "${settings.speed}") { v -> onUpdate { it.copy(speed = v) } }
                            RowDivider()
                            Column(Modifier.padding(16.dp)) {
                                Text("Colours", style = MaterialTheme.typography.titleMedium, color = Glow.Ink)
                                Text("Pick at least two", style = MaterialTheme.typography.bodyMedium, color = Glow.Dim)
                                FlowRow(
                                    Modifier.padding(top = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    PALETTE_ARGB.forEachIndexed { i, argb ->
                                        val on = i in settings.colors
                                        Box(
                                            Modifier
                                                .size(44.dp)
                                                .alpha(if (on) 1f else 0.35f)
                                                .clip(CircleShape)
                                                .background(Color(argb))
                                                .border(3.dp, if (on) Color.White else Color.Transparent, CircleShape)
                                                .semantics { contentDescription = "Colour ${i + 1}"; selected = on }
                                                .clickable(role = Role.Checkbox) {
                                                    onUpdate { st ->
                                                        val list = st.colors.toMutableList()
                                                        if (on) { if (list.size > 2) list.remove(i) } else list.add(i)
                                                        st.copy(colors = list.sorted())
                                                    }
                                                },
                                        )
                                    }
                                }
                            }
                            RowDivider()
                            SwitchRow(
                                "Flashlight on the beat",
                                if (hasFlash) "Uses the camera flash as a light. The camera is never opened." else "This phone has no flashlight.",
                                settings.flashlight && hasFlash, enabled = hasFlash,
                            ) { v -> onUpdate { it.copy(flashlight = v) } }
                            RowDivider()
                            SwitchRow("Vibrate on the beat", null, settings.vibrate) { v -> onUpdate { it.copy(vibrate = v) } }
                            RowDivider()
                            SwitchRow("Keep screen awake", "Stops the screen from turning off during a show.", settings.keepAwake) { v ->
                                onUpdate { it.copy(keepAwake = v) }
                            }
                            RowDivider()
                            SwitchRow(
                                "Keep going when the screen is off",
                                "Flashlight keeps following the music after you lock the phone. Shows a notification.",
                                settings.screenOff,
                            ) { v ->
                                if (!v) onUpdate { it.copy(screenOff = false) }
                                else if (context.hasMic() && context.hasNotifications()) onUpdate { it.copy(screenOff = true) }
                                else explain = Pending.SCREEN_OFF
                            }
                            RowDivider()
                            SliderRow("Seconds per mode in Light Show", settings.showSeconds, 5..30, "${settings.showSeconds} s") { v ->
                                onUpdate { it.copy(showSeconds = v) }
                            }
                        }

                        Section("Support") {
                            LinkRow("Rate us", "Tell others what you think on Google Play", dot = Color(0xFFFFD60A), onClick = onRate)
                            RowDivider()
                            LinkRow("Share the app", dot = Color(0xFF19D27A), onClick = { context.shareApp() })
                            RowDivider()
                            LinkRow("Send feedback", SUPPORT_EMAIL, dot = Color(0xFF2F8CFF), onClick = {
                                if (!context.sendFeedback()) scope.launch { snackbar.showSnackbar("No email app found. Write to $SUPPORT_EMAIL") }
                            })
                        }

                        Section("Privacy") {
                            LinkRow("Privacy policy", dot = Color(0xFFC77DFF), onClick = onPrivacyPolicy)
                            RowDivider()
                            LinkRow("App permissions", "What each permission is for", dot = Color(0xFF1FE0E0), onClick = onPermissions)
                            if (privacyRequired) {
                                RowDivider()
                                LinkRow("Ad privacy choices", "Change your ad consent", dot = Glow.Amber, onClick = {
                                    context.findActivity()?.let { AdsManager.showPrivacyOptions(it) }
                                })
                            }
                        }

                        Section("About") {
                            LinkRow("Reset settings", dot = Color(0xFFFF3B3B), onClick = { confirmReset = true })
                            RowDivider()
                            LinkRow("Version", BuildConfig.VERSION_NAME, trailing = "", onClick = null)
                        }
                    }
                }
            }
        }
        SnackbarHost(
            snackbar,
            Modifier
                .align(Alignment.BottomCenter)
                .padding(WindowInsets.navigationBars.asPaddingValues())
                .padding(16.dp),
        )
    }

    if (explain != Pending.NONE) {
        val target = explain
        val screenOff = target == Pending.SCREEN_OFF
        AlertDialog(
            onDismissRequest = { explain = Pending.NONE },
            title = { Text(if (screenOff) "Allow microphone and notifications" else "Allow the microphone") },
            text = {
                Text(
                    if (screenOff) {
                        "Glow Party listens to the music to time the flashlight. Android shows a notification while it runs " +
                            "with the screen off, and you can stop it from there.\n\nSound is analysed on your phone. " +
                            "It is never recorded, saved or sent."
                    } else {
                        "Glow Party needs the microphone to hear the beat. Sound is analysed on your phone. " +
                            "It is never recorded, saved or sent."
                    },
                )
            },
            confirmButton = { TextButton(onClick = { explain = Pending.NONE; request(target) }) { Text("Continue") } },
            dismissButton = { TextButton(onClick = { explain = Pending.NONE }) { Text("Not now") } },
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset settings?") },
            text = { Text("Colours, speed, sensitivity and switches go back to the defaults.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    onUpdate { GlowSettings(sessions = it.sessions, reviewAsked = it.reviewAsked) }
                }) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }
}
