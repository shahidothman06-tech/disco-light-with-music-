package com.yourcompany.glowparty.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Where the beat comes from. */
enum class SoundSource(val label: String) {
    AUTO("Built-in beat"),
    MIC("Microphone"),
    PLAYER("Phone music"),
}

/** Every light mode on the home grid. */
enum class Mode(val title: String, val hint: String, val reactive: Boolean) {
    DISCO("Disco", "New colour on every beat", true),
    SMOOTH("Smooth", "Colours flow faster when the music is louder", true),
    FLARE("Flare", "Flashes on the beat, then fades", true),
    STROBE("Strobe", "Speed sets the strobe rate", true),
    RAVE("Rave", "Disco ball, lasers and spots", true),
    SPECTRUM("Spectrum", "Live frequency bars", true),
    GRADIENT("Gradient", "Rotating colour wheel, rings burst on the beat", true),
    TILES("Tiles", "Tiles light up in patterns", true),
    SHAKE("Shake", "Shake the phone (or tap) to flash", false),
    SYNC("Sync", "Same colours and speed on every phone change together", false),
    SIREN("Siren", "Police lights with siren sound", false),
}

/** Order used by the automatic Light Show. */
val LIGHT_SHOW_ORDER = listOf(
    Mode.RAVE, Mode.SPECTRUM, Mode.GRADIENT, Mode.TILES,
    Mode.FLARE, Mode.SMOOTH, Mode.DISCO, Mode.STROBE,
)

/** Selectable colours (ARGB). */
val PALETTE_ARGB = longArrayOf(
    0xFFFF1F5A, 0xFFFF8A00, 0xFFFFE600, 0xFF3DFF5C, 0xFF00E5FF,
    0xFF2F6BFF, 0xFF9B30FF, 0xFFFF2DD4, 0xFFFFFFFF, 0xFFFF4D4D,
)

data class GlowSettings(
    val source: SoundSource = SoundSource.AUTO,
    val sensitivity: Int = 5,          // 1..10
    val speed: Int = 5,                // 1..10
    val bpm: Int = 120,                // built-in beat tempo
    val colors: List<Int> = listOf(0, 1, 2, 3, 4, 5, 6, 7),
    val vibrate: Boolean = false,
    val flashlight: Boolean = false,
    val keepAwake: Boolean = true,
    val screenOff: Boolean = false,    // keep flashing with the screen locked
    val showSeconds: Int = 10,
    val sirenSound: Boolean = true,
    val sessions: Int = 0,
    val reviewAsked: Boolean = false,
)

private val Context.store: DataStore<Preferences> by preferencesDataStore(name = "glow_settings")

class SettingsRepository(private val context: Context) {

    private object K {
        val source = stringPreferencesKey("source")
        val sensitivity = intPreferencesKey("sensitivity")
        val speed = intPreferencesKey("speed")
        val bpm = intPreferencesKey("bpm")
        val colors = stringPreferencesKey("colors")
        val vibrate = booleanPreferencesKey("vibrate")
        val flashlight = booleanPreferencesKey("flashlight")
        val keepAwake = booleanPreferencesKey("keep_awake")
        val screenOff = booleanPreferencesKey("screen_off")
        val showSeconds = intPreferencesKey("show_seconds")
        val sirenSound = booleanPreferencesKey("siren_sound")
        val sessions = intPreferencesKey("sessions")
        val reviewAsked = booleanPreferencesKey("review_asked")
    }

    val flow: Flow<GlowSettings> = context.store.data.map { p -> p.toSettings() }

    suspend fun current(): GlowSettings = flow.first()

    suspend fun update(transform: (GlowSettings) -> GlowSettings) {
        context.store.edit { p ->
            val n = transform(p.toSettings())
            p[K.source] = n.source.name
            p[K.sensitivity] = n.sensitivity
            p[K.speed] = n.speed
            p[K.bpm] = n.bpm
            p[K.colors] = n.colors.joinToString(",")
            p[K.vibrate] = n.vibrate
            p[K.flashlight] = n.flashlight
            p[K.keepAwake] = n.keepAwake
            p[K.screenOff] = n.screenOff
            p[K.showSeconds] = n.showSeconds
            p[K.sirenSound] = n.sirenSound
            p[K.sessions] = n.sessions
            p[K.reviewAsked] = n.reviewAsked
        }
    }

    private fun Preferences.toSettings(): GlowSettings {
        val d = GlowSettings()
        val colors = this[K.colors]
            ?.split(",")
            ?.mapNotNull { it.toIntOrNull() }
            ?.filter { it in PALETTE_ARGB.indices }
            ?.takeIf { it.size >= 2 }
            ?: d.colors
        return GlowSettings(
            source = this[K.source]?.let { runCatching { SoundSource.valueOf(it) }.getOrNull() } ?: d.source,
            sensitivity = this[K.sensitivity] ?: d.sensitivity,
            speed = this[K.speed] ?: d.speed,
            bpm = this[K.bpm] ?: d.bpm,
            colors = colors,
            vibrate = this[K.vibrate] ?: d.vibrate,
            flashlight = this[K.flashlight] ?: d.flashlight,
            keepAwake = this[K.keepAwake] ?: d.keepAwake,
            screenOff = this[K.screenOff] ?: d.screenOff,
            showSeconds = this[K.showSeconds] ?: d.showSeconds,
            sirenSound = this[K.sirenSound] ?: d.sirenSound,
            sessions = this[K.sessions] ?: d.sessions,
            reviewAsked = this[K.reviewAsked] ?: d.reviewAsked,
        )
    }
}
