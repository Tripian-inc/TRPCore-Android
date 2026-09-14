package com.tripian.trpcore.ui.timeline.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.domain.model.itinerary.ItineraryWithActivities
import com.tripian.trpcore.ui.timeline.compose.activity.ActivityListingScreen
import com.tripian.trpcore.ui.timeline.compose.addplan.StartingPointScreen
import com.tripian.trpcore.ui.timeline.compose.core.LocalTimelineOverlays
import com.tripian.trpcore.ui.timeline.compose.core.LocalTimelineViewModelFactory
import com.tripian.trpcore.ui.timeline.compose.core.TimelineOverlayState
import com.tripian.trpcore.ui.timeline.compose.core.TimelineWarningDialogHost
import com.tripian.trpcore.ui.timeline.compose.core.TimelineTheme
import com.tripian.trpcore.ui.timeline.compose.nav.ActivityListingArgs
import com.tripian.trpcore.ui.timeline.compose.nav.LocalTimelineNavigator
import com.tripian.trpcore.ui.timeline.compose.nav.PoiDetailArgs
import com.tripian.trpcore.ui.timeline.compose.nav.PoiListingArgs
import com.tripian.trpcore.ui.timeline.compose.nav.PoiSelectionArgs
import com.tripian.trpcore.ui.timeline.compose.nav.SavedPlansArgs
import com.tripian.trpcore.ui.timeline.compose.nav.StartingPointArgs
import com.tripian.trpcore.ui.timeline.compose.nav.TimelineNavigator
import com.tripian.trpcore.ui.timeline.compose.nav.TimelineRoutes
import com.tripian.trpcore.ui.timeline.compose.nav.rememberTimelineNavigator
import com.tripian.trpcore.ui.timeline.compose.poi.PoiSelectionScreen
import com.tripian.trpcore.ui.timeline.compose.poidetail.PoiDetailScreen
import com.tripian.trpcore.ui.timeline.compose.poilisting.PoiListingScreen
import com.tripian.trpcore.ui.timeline.compose.savedplans.SavedPlansScreen

/**
 * Compose twin of [TRPCore.startWithItinerary]: embeds the whole Timeline flow
 * in the host's own Compose hierarchy instead of launching SDK Activities.
 * Every SDK screen is a destination of the flow's nested NavHost, so pushing a
 * host screen on top and popping it returns to the exact SDK screen and state.
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

    val navigator = rememberTimelineNavigator(navController)
    val overlays = remember { TimelineOverlayState() }

    TimelineTheme {
        CompositionLocalProvider(
            LocalTimelineViewModelFactory provides TRPCore.core.timelineViewModelFactory,
            LocalTimelineNavigator provides navigator,
            LocalTimelineOverlays provides overlays
        ) {
            Box {
                NavHost(navController = navController, startDestination = TimelineRoutes.TIMELINE) {
                    composable(TimelineRoutes.TIMELINE) { entry ->
                        TimelineScreen(
                            itinerary = itinerary,
                            tripHash = tripHash ?: itinerary.tripianHash,
                            uniqueId = uniqueId ?: itinerary.uniqueId,
                            appLanguage = appLanguage,
                            appCurrency = appCurrency,
                            onDismiss = onDismiss,
                            navEntry = entry
                        )
                    }
                    composable(TimelineRoutes.POI_SELECTION) {
                        RouteScreen<PoiSelectionArgs>(navigator, TimelineRoutes.POI_SELECTION) {
                            PoiSelectionScreen(it)
                        }
                    }
                    composable(TimelineRoutes.POI_DETAIL) {
                        RouteScreen<PoiDetailArgs>(navigator, TimelineRoutes.POI_DETAIL) {
                            PoiDetailScreen(it)
                        }
                    }
                    composable(TimelineRoutes.POI_LISTING) {
                        RouteScreen<PoiListingArgs>(navigator, TimelineRoutes.POI_LISTING) {
                            PoiListingScreen(it)
                        }
                    }
                    composable(TimelineRoutes.ACTIVITY_LISTING) {
                        RouteScreen<ActivityListingArgs>(navigator, TimelineRoutes.ACTIVITY_LISTING) {
                            ActivityListingScreen(it)
                        }
                    }
                    composable(TimelineRoutes.SAVED_PLANS) {
                        RouteScreen<SavedPlansArgs>(navigator, TimelineRoutes.SAVED_PLANS) {
                            SavedPlansScreen(it)
                        }
                    }
                    composable(TimelineRoutes.STARTING_POINT) {
                        RouteScreen<StartingPointArgs>(navigator, TimelineRoutes.STARTING_POINT) {
                            StartingPointScreen(it)
                        }
                    }
                }
                TimelineWarningDialogHost(overlays)
            }
        }
    }
}

/**
 * Renders [content] with the launch arguments stored for [route]. Arguments
 * live in memory only, so after process death the destination has nothing to
 * show and pops itself.
 */
@Composable
private inline fun <reified A : Any> RouteScreen(
    navigator: TimelineNavigator,
    route: String,
    content: @Composable (A) -> Unit
) {
    val args = navigator.argsFor<A>(route)
    if (args == null) {
        LaunchedEffect(Unit) { navigator.back() }
        return
    }
    content(args)
}
