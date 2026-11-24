package com.yourcompany.glowparty.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

fun Context.hasMic(): Boolean = hasPermission(Manifest.permission.RECORD_AUDIO)

fun Context.hasNotifications(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || hasPermission(Manifest.permission.POST_NOTIFICATIONS)

/** Opens this app's page in Android Settings, where every permission can be changed. */
fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

fun Context.shareApp() {
    val link = "https://play.google.com/store/apps/details?id=$packageName"
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, "Turn your phone into a disco light: $link")
    startActivity(Intent.createChooser(send, "Share Glow Party").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

const val SUPPORT_EMAIL = "support@yourcompany.com" // TODO: your support address

fun Context.sendFeedback(): Boolean = try {
    val i = Intent(Intent.ACTION_SENDTO, "mailto:$SUPPORT_EMAIL".toUri())
        .putExtra(Intent.EXTRA_SUBJECT, "Glow Party feedback")
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(i)
    true
} catch (e: Exception) {
    false
}
