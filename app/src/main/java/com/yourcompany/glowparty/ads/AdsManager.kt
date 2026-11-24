package com.yourcompany.glowparty.ads

import android.app.Activity
import android.os.SystemClock
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdPreloader
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.yourcompany.glowparty.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * AdMob (Google Mobile Ads Next-Gen SDK) with Google's consent form (UMP).
 * Order: ask for consent first, only then start the ads SDK.
 * Placements: a banner on the home screen and, at most, one interstitial
 * every 3rd time the user leaves a light show (never while lights are running).
 */
object AdsManager {
    private const val TAG = "AdsManager"
    private const val INTERSTITIAL_EVERY = 3
    private const val INTERSTITIAL_MIN_GAP_MS = 90_000L

    private val started = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var consent: ConsentInformation? = null
    private var exits = 0
    private var lastShown = 0L

    private val _ready = MutableStateFlow(false)
    /** True once ads may be requested and the SDK has started. */
    val ready: StateFlow<Boolean> = _ready

    private val _privacyOptionsRequired = MutableStateFlow(false)
    /** True when the user must be offered "Ad privacy choices" in Settings (for example in the EU). */
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired

    /** Call from MainActivity.onCreate on every launch. */
    fun gatherConsent(activity: Activity) {
        val info = UserMessagingPlatform.getConsentInformation(activity)
        consent = info
        val params = ConsentRequestParameters.Builder().build()
        info.requestConsentInfoUpdate(
            activity, params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) Log.w(TAG, "Consent form: ${formError.message}")
                    _privacyOptionsRequired.value =
                        info.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
                    if (info.canRequestAds()) startSdk(activity)
                }
            },
            { error -> Log.w(TAG, "Consent update failed: ${error.message}") },
        )
        // Consent from a previous session lets ads start right away.
        if (info.canRequestAds()) startSdk(activity)
    }

    /** Opens Google's privacy options form (Settings > Ad privacy choices). */
    fun showPrivacyOptions(activity: Activity, done: () -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) Log.w(TAG, "Privacy options: ${formError.message}")
            done()
        }
    }

    private fun startSdk(activity: Activity) {
        if (!started.compareAndSet(false, true)) return
        val app = activity.applicationContext
        scope.launch {
            // Initialize on a background thread, as Google recommends.
            MobileAds.initialize(app, InitializationConfig.Builder(BuildConfig.ADMOB_APP_ID).build()) {
                _ready.value = true
                startInterstitialPreload()
            }
        }
    }

    private fun startInterstitialPreload() {
        val id = BuildConfig.INTERSTITIAL_ID
        val config = PreloadConfiguration(AdRequest.Builder(id).build())
        InterstitialAdPreloader.start(id, config, object : PreloadCallback {
            override fun onAdPreloaded(preloadId: String, responseInfo: ResponseInfo) {
                Log.i(TAG, "Interstitial ready")
            }

            override fun onAdFailedToPreload(preloadId: String, adError: LoadAdError) {
                Log.w(TAG, "Interstitial preload failed: ${adError.message}")
            }

            override fun onAdsExhausted(preloadId: String) = Unit
        })
    }

    /** Call when the user leaves a light screen. Shows an interstitial only now and then. */
    fun onLightShowExited(activity: Activity) {
        if (!_ready.value) return
        exits++
        val now = SystemClock.elapsedRealtime()
        if (exits % INTERSTITIAL_EVERY != 0) return
        if (lastShown != 0L && now - lastShown < INTERSTITIAL_MIN_GAP_MS) return
        val ad = InterstitialAdPreloader.pollAd(BuildConfig.INTERSTITIAL_ID) ?: return
        ad.adEventCallback = object : InterstitialAdEventCallback {
            override fun onAdShowedFullScreenContent() {
                lastShown = SystemClock.elapsedRealtime()
            }

            override fun onAdDismissedFullScreenContent() = Unit

            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                Log.w(TAG, "Interstitial failed to show: ${fullScreenContentError.message}")
            }

            override fun onAdImpression() = Unit

            override fun onAdClicked() = Unit
        }
        ad.show(activity)
    }
}
