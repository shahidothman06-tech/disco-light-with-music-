package com.yourcompany.glowparty.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yourcompany.glowparty.ui.theme.Glow

// TODO: replace the company name, email and date, have it reviewed, and host the same text
// at a public URL — Google Play asks for a privacy policy link on the store listing.
private const val COMPANY = "Your Company"
private const val EFFECTIVE = "25 November 2026"

private val POLICY = listOf(
    "Summary" to
        "Glow Party turns your phone into a light show. It listens to sound only to find the beat, and that " +
        "sound never leaves your phone. The app shows ads from Google AdMob to stay free.",
    "Microphone" to
        "When you choose Microphone or Phone music, the app reads sound levels and frequencies in real time to " +
        "time the lights. Audio is processed on your device, is not recorded, is not saved, and is not sent to " +
        "us or anyone else. You can turn off microphone access at any time in Android Settings.",
    "Flashlight, vibration and motion" to
        "The camera flash is used only as a light; the camera is never opened and no photos are taken. " +
        "Vibration is used only for beat effects. Motion sensors are used only in Shake mode. None of this " +
        "data is stored or shared.",
    "Screen-off mode" to
        "If you turn it on, the app keeps listening while the phone is locked so the flashlight can follow the " +
        "music. A notification shows while it runs and you can stop it at any time.",
    "Advertising (Google AdMob)" to
        "We use Google AdMob to show ads. AdMob may collect and use your device's advertising ID, IP address, " +
        "approximate location derived from it, device information and ad interactions to deliver, measure and " +
        "(where you allow it) personalise ads, and to prevent fraud. In the EEA, UK, Switzerland and US states " +
        "with privacy laws we ask for your choice using Google's consent message, and you can change it any " +
        "time in Settings › Ad privacy choices. Learn more: https://policies.google.com/technologies/ads",
    "Information stored on your phone" to
        "Your settings (colours, speed, sensitivity and switches) are saved on your phone so they're there next " +
        "time. Uninstalling the app deletes them. We don't run our own servers and don't collect personal data.",
    "Ratings and feedback" to
        "Rate us opens Google Play's review card, handled by Google. Send feedback opens your email app; we see " +
        "only what you choose to send.",
    "Children" to
        "Glow Party is not directed at children under 13 and we don't knowingly collect their personal information.",
    "Changes" to
        "If this policy changes, we'll update the date above and show the new version in the app.",
    "Contact" to "$COMPANY · $SUPPORT_EMAIL",
)

@Composable
fun PolicyScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Glow.Bg)) {
        ScreenHeader("Privacy policy", onBack)
        LazyColumn(
            contentPadding = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal).asPaddingValues(),
        ) {
            item {
                Text(
                    "Template: replace the company name and contact, and have it checked before publishing.",
                    color = Glow.Amber, style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF2A2410))
                        .padding(12.dp),
                )
                Text(
                    "Glow Party · Effective $EFFECTIVE",
                    style = MaterialTheme.typography.titleMedium, color = Glow.Ink,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            items(POLICY) { (title, body) ->
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text(body, style = MaterialTheme.typography.bodyLarge, color = Color(0xFFD6D6DE), modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}
