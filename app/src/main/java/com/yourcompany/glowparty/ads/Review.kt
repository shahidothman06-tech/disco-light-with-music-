package com.yourcompany.glowparty.ads

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * "Rate us" using Google Play's In-App Review card. Google decides whether the card
 * is shown (there is a quota), so for the Settings button we fall back to the Play
 * Store page. Google's policy: don't ask "do you like the app?" before showing it.
 */
object Rating {
    suspend fun inAppReview(activity: Activity): Boolean = try {
        val manager = ReviewManagerFactory.create(activity)
        val info = manager.requestReview()
        manager.launchReview(activity, info)
        true
    } catch (e: Exception) {
        false
    }

    fun openStorePage(context: Context) {
        val pkg = context.packageName
        val market = Intent(Intent.ACTION_VIEW, "market://details?id=$pkg".toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(market)
        } catch (e: ActivityNotFoundException) {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$pkg".toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}
