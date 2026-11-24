package com.yourcompany.glowparty

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.yourcompany.glowparty.ads.AdsManager
import com.yourcompany.glowparty.ui.GlowRoot
import com.yourcompany.glowparty.ui.theme.GlowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        // Edge-to-edge: draw behind transparent status and navigation bars (light icons on dark UI).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        // GDPR / US-state consent first; the ads SDK starts only after it.
        AdsManager.gatherConsent(this)

        setContent {
            GlowTheme { GlowRoot() }
        }
    }
}
