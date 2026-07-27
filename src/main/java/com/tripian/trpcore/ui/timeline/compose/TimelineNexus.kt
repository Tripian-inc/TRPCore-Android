package com.tripian.trpcore.ui.timeline.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.domain.model.itinerary.ItineraryWithActivities
import com.tripian.trpcore.ui.timeline.compose.core.LocalTimelineViewModelFactory
import com.tripian.trpcore.ui.timeline.compose.core.TimelineTheme

object TimelineRoutes {
    const val TIMELINE = "timeline"
    const val ADD_PLAN = "add_plan"
    const val STARTING_POINT = "starting_point"
    const val POI_SELECTION = "poi_selection"
    const val POI_LISTING = "poi_listing"
    const val POI_DETAIL = "poi_detail"
    const val ACTIVITY_LISTING = "activity_listing"
    const val SAVED_PLANS = "saved_plans"
}

/**
 * Compose twin of [TRPCore.startWithItinerary]: embeds the Timeline flow in
 * the host's own Compose hierarchy instead of launching SDK Activities.
 * Parameters mirror the Activity entry point and SDK events keep flowing
 * through [com.tripian.trpcore.sdk.TRPCoreSDKListener].
 *
 * @param onDismiss invoked when the flow requests to close (back from the root screen)
 */
@Composable
fun TimelineNexus(
    itinerary: ItineraryWithActivities,
    tripHash: String? = null,
    uniqueId: String? = null,
    appLanguage: String = "en",
    appCurrency: String = "EUR",
    onDismiss: () -> Unit = {},
    navController: NavHostController = rememberNavController()
) {
    check(TRPCore.isInitialized()) {
        "TRPCore is not initialized. Call TRPCore().init() before composing TimelineNexus."
    }
    require(itinerary.hasLocationData()) {
        "Either destinationItems or tripItems must contain at least one item with location data."
    }

    LaunchedEffect(appLanguage) {
        TRPCore.core.applyLanguageAndPrefetchCities(appLanguage)
    }

    TimelineTheme {
        CompositionLocalProvider(
            LocalTimelineViewModelFactory provides TRPCore.core.viewModelFactory
        ) {
            NavHost(navController = navController, startDestination = TimelineRoutes.TIMELINE) {
                composable(TimelineRoutes.TIMELINE) {
                    TimelineScreen(
                        itinerary = itinerary,
                        tripHash = tripHash ?: itinerary.tripianHash,
                        uniqueId = uniqueId ?: itinerary.uniqueId,
                        appLanguage = appLanguage,
                        appCurrency = appCurrency,
                        onDismiss = onDismiss
                    )
                }
            }
        }
    }
}

