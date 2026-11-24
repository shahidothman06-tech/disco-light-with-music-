package com.yourcompany.glowparty

import android.app.Application
import com.yourcompany.glowparty.data.SettingsRepository

class GlowApp : Application() {
    val settings: SettingsRepository by lazy { SettingsRepository(this) }
}
