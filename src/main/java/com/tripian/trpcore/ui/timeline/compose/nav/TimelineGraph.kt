package com.tripian.trpcore.ui.timeline.compose.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.domain.model.itinerary.ItineraryWithActivities
import com.tripian.trpcore.ui.timeline.compose.TimelineScreen
import com.tripian.trpcore.ui.timeline.compose.activity.ActivityListingScreen
import com.tripian.trpcore.ui.timeline.compose.addplan.StartingPointScreen
import com.tripian.trpcore.ui.timeline.compose.core.LocalTimelineOverlays
import com.tripian.trpcore.ui.timeline.compose.core.LocalTimelineViewModelFactory
import com.tripian.trpcore.ui.timeline.compose.core.TimelineTheme
import com.tripian.trpcore.ui.timeline.compose.core.TimelineWarningDialogHost
import com.tripian.trpcore.ui.timeline.compose.poi.PoiSelectionScreen
import com.tripian.trpcore.ui.timeline.compose.poidetail.PoiDetailScreen
import com.tripian.trpcore.ui.timeline.compose.poilisting.PoiListingScreen
import com.tripian.trpcore.ui.timeline.compose.savedplans.SavedPlansScreen

/**
 * What the Timeline should open. Read through a provider when the Timeline
 * destination composes, so the host can add the graph once and fill the trip
 * in later. Parameters mirror [TRPCore.startWithItinerary].
 */
data class TimelineRequest(
    val itinerary: ItineraryWithActivities,
    val tripHash: String? = null,
    val uniqueId: String? = null,
    val appLanguage: String = "en",
    val appCurrency: String = "EUR"
)

/**
 * Adds the Tripian Timeline flow to the host's own Navigation Compose graph as
 * a nested graph of route [TimelineRoutes.GRAPH]. Every SDK screen becomes a
 * destination of the host's NavController, so the host keeps full control of
 * navigation: it opens the flow with `navigate(TimelineRoutes.GRAPH)`, pushes
 * its own screens on top from the SDK callbacks, and system back pops SDK
 * screens one by one before [onDismiss] fires from the Timeline root.
 *
 * @param request supplies the trip when the Timeline opens; a null result pops the flow
 * @param onDismiss invoked when the Timeline root asks to close; the host decides where to go
 */
fun NavGraphBuilder.tripianTimeline(
    navController: NavController,
    request: () -> TimelineRequest?,
    onDismiss: () -> Unit = {}
) {
    navigation(startDestination = TimelineRoutes.TIMELINE, route = TimelineRoutes.GRAPH) {
        composable(TimelineRoutes.TIMELINE) { entry ->
            TimelineRouteScope(navController, entry) {
                val launch = request()
                if (launch == null) {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                    return@TimelineRouteScope
                }
                LaunchedEffect(launch.appLanguage) {
                    TRPCore.core.applyLanguageAndPrefetchCities(launch.appLanguage)
                }
                BelowStatusBar {
                    TimelineScreen(
                        itinerary = launch.itinerary,
                        tripHash = launch.tripHash ?: launch.itinerary.tripianHash,
                        uniqueId = launch.uniqueId ?: launch.itinerary.uniqueId,
                        appLanguage = launch.appLanguage,
                        appCurrency = launch.appCurrency,
                        onDismiss = onDismiss,
                        navEntry = entry
                    )
                }
            }
        }
        composable(TimelineRoutes.POI_SELECTION) { entry ->
            TimelineRouteScope(navController, entry) {
                RouteScreen<PoiSelectionArgs>(TimelineRoutes.POI_SELECTION) {
                    BelowStatusBar { PoiSelectionScreen(it) }
                }
            }
        }
        composable(TimelineRoutes.POI_DETAIL) { entry ->
            TimelineRouteScope(navController, entry) {
                RouteScreen<PoiDetailArgs>(TimelineRoutes.POI_DETAIL) { PoiDetailScreen(it) }
            }
        }
        composable(TimelineRoutes.POI_LISTING) { entry ->
            TimelineRouteScope(navController, entry) {
                RouteScreen<PoiListingArgs>(TimelineRoutes.POI_LISTING) {
                    BelowStatusBar { PoiListingScreen(it) }
                }
            }
        }
        composable(TimelineRoutes.ACTIVITY_LISTING) { entry ->
            TimelineRouteScope(navController, entry) {
                RouteScreen<ActivityListingArgs>(TimelineRoutes.ACTIVITY_LISTING) {
                    BelowStatusBar { ActivityListingScreen(it) }
                }
            }
        }
        composable(TimelineRoutes.SAVED_PLANS) { entry ->
            TimelineRouteScope(navController, entry) {
                RouteScreen<SavedPlansArgs>(TimelineRoutes.SAVED_PLANS) {
                    BelowStatusBar { SavedPlansScreen(it) }
                }
            }
        }
        composable(TimelineRoutes.STARTING_POINT) { entry ->
            TimelineRouteScope(navController, entry) {
                RouteScreen<StartingPointArgs>(TimelineRoutes.STARTING_POINT) {
                    BelowStatusBar { StartingPointScreen(it) }
                }
            }
        }
    }
}

/**
 * Environment every Timeline destination runs in: the SDK theme, the flow
 * scope shared through the graph's back stack entry, the navigator and the
 * overlay host for ViewModel warning dialogs.
 */
@Composable
private fun TimelineRouteScope(
    navController: NavController,
    entry: NavBackStackEntry,
    content: @Composable () -> Unit
) {
    check(TRPCore.isInitialized()) {
        "TRPCore is not initialized. Call TRPCore().init() before navigating to the Timeline."
    }
    val graphEntry = remember(entry) { navController.getBackStackEntry(TimelineRoutes.GRAPH) }
    val scope: TimelineFlowScope = viewModel(graphEntry)
    val navigator = rememberTimelineNavigator(navController, scope)

    TimelineTheme {
        CompositionLocalProvider(
            LocalTimelineViewModelFactory provides TRPCore.core.timelineViewModelFactory,
            LocalTimelineNavigator provides navigator,
            LocalTimelineOverlays provides scope.overlays
        ) {
            Box(Modifier.fillMaxSize()) {
                content()
                TimelineWarningDialogHost(scope.overlays)
            }
        }
    }
}

/**
 * Keeps a screen below the status bar in an edge-to-edge host. A host that
 * already reserves that space (a Scaffold with its content padding consumed)
 * adds nothing here. POI detail draws under the status bar on purpose.
 */
@Composable
private fun BelowStatusBar(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .consumeWindowInsets(WindowInsets.statusBars)
    ) {
        content()
    }
}

/**
 * Renders [content] with the launch arguments stored for [route]. Arguments
 * live in memory only, so after process death the destination has nothing to
 * show and pops itself.
 */
@Composable
private inline fun <reified A : Any> RouteScreen(route: String, content: @Composable (A) -> Unit) {
    val navigator = LocalTimelineNavigator.current
    val args = navigator.argsFor<A>(route)
    if (args == null) {
        LaunchedEffect(Unit) { navigator.back() }
        return
    }
    content(args)
}
