# Glow Party — music-reactive disco light (Android, Kotlin)

Jetpack Compose app, edge-to-edge, built on the latest toolchain:

| Piece | Version |
|---|---|
| compileSdk / targetSdk | 37 (Android 17) |
| minSdk | 26 (Android 8.0) |
| Android Gradle Plugin | 9.2.1 (built-in Kotlin, no kotlin-android plugin) |
| Gradle | 9.4.1 (wrapper included) |
| Kotlin + Compose compiler | 2.4.20 |
| Compose BOM | 2026.08.00 (Compose 1.12) |
| AdMob | Google Mobile Ads **Next-Gen SDK** 1.7.0 |
| Consent | User Messaging Platform (UMP) 4.0.0 |
| Rate us | Play In-App Review 2.0.2 |

## Open and run
1. Open the `GlowParty` folder in the latest Android Studio. Let it sync (it downloads Gradle and libraries).
2. If asked, install the **Android 17 (API 37)** SDK in SDK Manager.
3. Run on a real phone (flashlight, microphone and shake need real hardware).

Debug builds use Google's **test ad ids**, so ads are safe to tap.

## Before publishing
- `app/build.gradle.kts`: change `applicationId`, and put your real AdMob **app id**, **banner id** and **interstitial id** in the `release` block.
- `ui/Util.kt`: set `SUPPORT_EMAIL`.
- `ui/PolicyScreen.kt`: set company name and date, host the same text on a public web page, and add that link to your Play Store listing.
- In AdMob, create a **GDPR message** (and US-states message) under *Privacy & messaging*; the app shows it automatically through UMP.
- Play Console → **Data safety**: declare *Advertising ID*, *device or other IDs*, *app interactions* and *diagnostics* (collected by AdMob, shared for advertising). Audio: **not collected** (processed on device only).
- Play Console → **Foreground service** declaration: type *microphone*, used for "screen-off mode: flashlight follows the music while the phone is locked, user-started, with a Stop button in the notification". Attach a short video.

## Permissions (every one, and why)
| Permission | When asked | Why |
|---|---|---|
| RECORD_AUDIO | Only when you pick Microphone / Phone music, or turn on screen-off mode, after an explanation | Hear the beat. Analysed on the phone, never recorded or sent |
| MODIFY_AUDIO_SETTINGS | Automatic | Needed by Android's Visualizer for "Phone music" |
| POST_NOTIFICATIONS (13+) | Only when turning on screen-off mode | The "running" notification with Stop |
| FOREGROUND_SERVICE, FOREGROUND_SERVICE_MICROPHONE | Automatic | Screen-off mode |
| VIBRATE | Automatic | Beat buzz (off by default) |
| INTERNET, ACCESS_NETWORK_STATE, AD_ID | Automatic | AdMob ads |
| Flashlight | **No permission** | `CameraManager.setTorchMode`; the camera is never opened, no CAMERA permission |

The app also has an **App permissions** screen (Settings → Privacy) that shows each one, its status, and buttons to allow or manage it.

## Ads behaviour
- Consent form first (EEA/UK/CH/US states); the ads SDK starts only when consent allows it.
- Banner at the bottom of the home screen.
- Interstitial at most every 3rd time a light show is closed, and not more than once per 90 seconds. Never during a show.
- "Ad privacy choices" appears in Settings when the user's region requires it.

## Rate us
Uses Google's In-App Review card (Google controls when it appears). Asked once automatically after the 3rd light show, and from Settings → Rate us (falls back to the Play Store page). Following Google's policy, the app does not ask "do you like us?" first.

## Project layout
```
app/src/main/java/com/yourcompany/glowparty/
  MainActivity.kt          edge-to-edge, splash, consent
  GlowApp.kt               settings repository
  data/Settings.kt         modes, sources, DataStore settings
  engine/BeatEngine.kt     mic FFT, phone-music Visualizer, built-in beat, beat detection
  engine/Fft.kt            radix-2 FFT
  engine/Hardware.kt       flashlight, vibration, shake sensor, siren tone
  service/LightService.kt  screen-off mode (foreground service)
  ads/AdsManager.kt        UMP consent + Next-Gen SDK + interstitial
  ads/HomeBanner.kt        adaptive banner
  ads/Review.kt            in-app review / store page
  ui/…                     home, light modes, settings, permissions, privacy policy
```
