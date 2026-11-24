package com.yourcompany.glowparty.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourcompany.glowparty.GlowApp
import com.yourcompany.glowparty.ads.AdsManager
import com.yourcompany.glowparty.ads.Rating
import com.yourcompany.glowparty.data.GlowSettings
import com.yourcompany.glowparty.data.LIGHT_SHOW_ORDER
import com.yourcompany.glowparty.data.Mode
import kotlinx.coroutines.launch

private sealed interface Route {
    data object Home : Route
    data class Light(val mode: Mode, val show: Boolean) : Route
    data object Settings : Route
    data object Policy : Route
    data object Permissions : Route
}

@Composable
fun GlowRoot() {
    val context = LocalContext.current
    val repo = (context.applicationContext as GlowApp).settings
    val settings by repo.flow.collectAsStateWithLifecycle(initialValue = GlowSettings())
    val scope = rememberCoroutineScope()
    val stack = remember { mutableStateListOf<Route>(Route.Home) }

    fun push(r: Route) { stack.add(r) }
    fun pop() { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    val update: ((GlowSettings) -> GlowSettings) -> Unit = { t -> scope.launch { repo.update(t) } }

    BackHandler(enabled = stack.size > 1) { pop() }

    fun leaveLight() {
        pop()
        val activity = context.findActivity() ?: return
        scope.launch {
            val now = repo.current()
            val sessions = now.sessions + 1
            val askReview = sessions >= 3 && !now.reviewAsked
            repo.update { it.copy(sessions = sessions, reviewAsked = it.reviewAsked || askReview) }
            // Never show an ad and the review card at the same moment.
            if (askReview) Rating.inAppReview(activity) else AdsManager.onLightShowExited(activity)
        }
    }

    fun rateUs() {
        val activity = context.findActivity() ?: return
        scope.launch {
            // Google may decide not to show the card (quota); then open the store page instead.
            if (!Rating.inAppReview(activity)) Rating.openStorePage(context)
        }
    }

    when (val route = stack.last()) {
        Route.Home -> HomeScreen(
            settings = settings,
            onMode = { push(Route.Light(it, show = false)) },
            onLightShow = { push(Route.Light(LIGHT_SHOW_ORDER.first(), show = true)) },
            onSettings = { push(Route.Settings) },
        )

        is Route.Light -> LightScreen(
            startMode = route.mode,
            lightShow = route.show,
            settings = settings,
            onExit = { leaveLight() },
            onOpenSettings = { push(Route.Settings) },
            onToggleSirenSound = { update { it.copy(sirenSound = !it.sirenSound) } },
        )

        Route.Settings -> SettingsScreen(
            settings = settings,
            onUpdate = update,
            onBack = { pop() },
            onPrivacyPolicy = { push(Route.Policy) },
            onPermissions = { push(Route.Permissions) },
            onRate = { rateUs() },
        )

        Route.Policy -> PolicyScreen(onBack = { pop() })
        Route.Permissions -> PermissionsScreen(onBack = { pop() })
    }
}
