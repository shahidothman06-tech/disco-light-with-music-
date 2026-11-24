package com.yourcompany.glowparty.ads

import android.util.Log
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.yourcompany.glowparty.BuildConfig
import com.yourcompany.glowparty.ui.findActivity

/** Anchored adaptive banner. Shows nothing until consent is done and the SDK is ready. */
@Composable
fun HomeBanner(modifier: Modifier = Modifier) {
    val ready by AdsManager.ready.collectAsStateWithLifecycle()
    if (!ready) return
    val activity = LocalContext.current.findActivity() ?: return

    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val widthDp = maxWidth.value.toInt()
        AndroidView(
            factory = {
                AdView(activity).also { view ->
                    val size = AdSize.getLargeAnchoredAdaptiveBannerAdSize(activity, widthDp)
                    view.loadAd(
                        BannerAdRequest.Builder(BuildConfig.BANNER_ID, size).build(),
                        object : AdLoadCallback<BannerAd> {
                            override fun onAdLoaded(ad: BannerAd) = Unit
                            override fun onAdFailedToLoad(adError: LoadAdError) {
                                Log.w("BannerAd", "Banner failed: ${adError.message}")
                            }
                        },
                    )
                }
            },
            onRelease = { it.destroy() },
        )
    }
}
