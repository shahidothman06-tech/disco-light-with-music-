package com.yourcompany.glowparty.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.yourcompany.glowparty.engine.Torch
import com.yourcompany.glowparty.ui.theme.Glow

/** Plain-language list of every permission, why it's used, and its current state. */
@Composable
fun PermissionsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }

    // Re-read states whenever `refresh` changes.
    val mic = remember(refresh) { context.hasMic() }
    val notif = remember(refresh) { context.hasNotifications() }
    val flash = remember { Torch(context).available }

    Column(Modifier.fillMaxSize().background(Glow.Bg)) {
        ScreenHeader("App permissions", onBack)
        LazyColumn(
            contentPadding = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal).asPaddingValues(),
        ) {
            item {
                Column(Modifier.padding(horizontal = 14.dp).padding(bottom = 24.dp)) {
                    Text(
                        "Glow Party asks only for what a feature needs, and only when you turn that feature on.",
                        style = MaterialTheme.typography.bodyLarge, color = Glow.Dim,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                    )

                    Section("Asked when you turn on a feature") {
                        PermRow(
                            "Microphone",
                            "Hears the music so the lights follow the beat (Microphone and Phone music modes, and " +
                                "screen-off mode). Sound is analysed on your phone. It is never recorded, saved or sent.",
                            granted = mic,
                            action = if (mic) "Manage" else "Allow",
                            onAction = { if (mic) context.openAppSettings() else micLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            RowDivider()
                            PermRow(
                                "Notifications",
                                "Only used for the 'Glow Party is running' notice in screen-off mode, with a Stop button. " +
                                    "No marketing notifications.",
                                granted = notif,
                                action = if (notif) "Manage" else "Allow",
                                onAction = {
                                    if (notif) context.openAppSettings() else notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                },
                            )
                        }
                    }

                    Section("Granted automatically by Android") {
                        PermRow(
                            "Flashlight",
                            if (flash) "No permission needed. The flash is used as a light only; the camera is never opened."
                            else "This phone has no flashlight.",
                            granted = null,
                        )
                        RowDivider()
                        PermRow("Vibration", "Short buzz on the beat, only if you switch it on.", granted = null)
                        RowDivider()
                        PermRow(
                            "Run in the background",
                            "Lets screen-off mode keep working while the phone is locked. Off unless you switch it on.",
                            granted = null,
                        )
                        RowDivider()
                        PermRow(
                            "Internet and advertising ID",
                            "Used by Google AdMob to show ads that keep the app free. You can reset or delete your " +
                                "advertising ID in Android Settings › Privacy › Ads.",
                            granted = null,
                        )
                    }

                    OutlinedButton(
                        onClick = { context.openAppSettings() },
                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                    ) { Text("Open Android app settings") }
                }
            }
        }
    }
}

@Composable
private fun PermRow(
    title: String,
    why: String,
    granted: Boolean?,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Column(Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Glow.Ink, modifier = Modifier.weight(1f))
            when (granted) {
                true -> Text("Allowed", color = Color(0xFF19D27A), style = MaterialTheme.typography.labelMedium)
                false -> Text("Not allowed", color = Glow.Amber, style = MaterialTheme.typography.labelMedium)
                null -> Text("Automatic", color = Glow.Dim, style = MaterialTheme.typography.labelMedium)
            }
        }
        Text(why, style = MaterialTheme.typography.bodyMedium, color = Glow.Dim, modifier = Modifier.padding(top = 4.dp))
        if (action != null) {
            OutlinedButton(onClick = onAction, modifier = Modifier.padding(top = 8.dp)) { Text(action) }
        }
    }
}
