package com.tripian.trpcore.ui.timeline.compose

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.domain.model.itinerary.ItineraryWithActivities
import com.tripian.trpcore.ui.timeline.compose.nav.TimelineRequest
import com.tripian.trpcore.ui.timeline.compose.nav.TimelineRoutes
import com.tripian.trpcore.ui.timeline.compose.nav.tripianTimeline

/**
 * Drop-in Compose twin of [TRPCore.startWithItinerary]: hosts the Timeline
 * graph in a NavHost of its own, for hosts that want a single composable
 * rather than SDK destinations in their graph. The host's back stack keeps
 * this composable's entry while host screens are pushed on top, so popping
 * them returns to the exact SDK screen. Hosts that want full control of
 * navigation add [tripianTimeline] to their own graph instead.
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

    NavHost(navController = navController, startDestination = TimelineRoutes.GRAPH) {
        tripianTimeline(
            navController = navController,
            request = { TimelineRequest(itinerary, tripHash, uniqueId, appLanguage, appCurrency) },
            onDismiss = onDismiss
        )
    }
}
