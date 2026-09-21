package com.tripian.trpcore.ui.timeline.compose.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.tripian.one.api.cities.model.City
import com.tripian.trpcore.ui.timeline.compose.core.TimelineOverlayState
import com.tripian.one.api.pois.model.Poi
import com.tripian.one.api.trip.model.Accommodation
import com.tripian.one.api.pois.model.Coordinate
import com.tripian.trpcore.domain.model.itinerary.SegmentFavoriteItem
import com.tripian.one.api.timeline.model.TimelineSegment
import com.tripian.trpcore.domain.model.timeline.AddPlanData
import com.tripian.trpcore.ui.timeline.poilisting.POIListingType
import java.io.Serializable
import java.util.Date

/** Route of the Tripian Timeline graph and of the destinations it contains. */
object TimelineRoutes {
    const val GRAPH = "tripian"
    const val TIMELINE = "tripian/timeline"
    const val POI_LISTING = "tripian/poi_listing"
    const val POI_DETAIL = "tripian/poi_detail"
    const val ACTIVITY_LISTING = "tripian/activity_listing"
    const val SAVED_PLANS = "tripian/saved_plans"
    const val STARTING_POINT = "tripian/starting_point"
}

/** Keys under which sub-screens hand their result back to the screen that opened them. */
object TimelineResults {
    const val SAVED_PLANS_CHANGED = "result_saved_plans_changed"
    const val SEGMENT_CREATED_DAY_INDEX = "result_segment_created_day_index"
    const val STARTING_POINT = "result_starting_point"
}

data class PoiDetailArgs(
    val poi: Poi,
    val tripStartDate: String?,
    val tripEndDate: String?
)

data class PoiListingArgs(
    val planData: AddPlanData,
    val tripHash: String,
    val listingType: POIListingType,
    val tripStartDate: String?,
    val tripEndDate: String?
)

data class ActivityListingArgs(
    val planData: AddPlanData,
    val tripHash: String,
    val plannedActivityIdsByDay: Map<String, List<String>>,
    val tripWideExcludedActivityIds: List<String>
)

data class SavedPlansArgs(
    val favorites: List<SegmentFavoriteItem>,
    val tripHash: String,
    val availableDays: List<Date>,
    val cityNameToIdMap: Map<String, Int>,
    val plannedActivityIdsByDay: Map<String, List<String>>
)

data class StartingPointArgs(
    val city: City?,
    val bookedActivities: List<TimelineSegment>,
    val favouriteItems: List<SegmentFavoriteItem>,
    val userLocation: Coordinate?
)

data class StartingPointResult(
    val name: String,
    val lat: Double,
    val lng: Double,
    val accommodation: Accommodation?
) : Serializable

/**
 * State shared by every destination of one Timeline graph: the in-memory
 * launch arguments of the sub-screens, keyed by route, and the overlay state.
 * Scoped to the graph's back stack entry, so it outlives the host pushing its
 * own screens on top of the SDK and coming back.
 */
class TimelineFlowScope : ViewModel() {
    private val args = mutableMapOf<String, Any>()

    val overlays = TimelineOverlayState()

    fun putArgs(route: String, value: Any) {
        args[route] = value
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> argsFor(route: String): T? = args[route] as? T
}

/**
 * Typed navigation for the Timeline flow. Sub-screens are destinations of the
 * flow's nested NavHost, so the host's own back stack keeps the SDK exactly
 * where the user left it when a host screen is pushed on top and popped again.
 * Results travel back through the opener's [NavBackStackEntry.savedStateHandle].
 */
class TimelineNavigator internal constructor(
    val navController: NavController,
    private val scope: TimelineFlowScope
) {

    fun <T> argsFor(route: String): T? = scope.argsFor(route)

    fun openPoiDetail(poi: Poi, tripStartDate: String?, tripEndDate: String?) =
        open(TimelineRoutes.POI_DETAIL, PoiDetailArgs(poi, tripStartDate, tripEndDate))

    fun openPoiListing(
        planData: AddPlanData,
        tripHash: String,
        listingType: POIListingType,
        tripStartDate: String?,
        tripEndDate: String?
    ) = open(
        TimelineRoutes.POI_LISTING,
        PoiListingArgs(planData, tripHash, listingType, tripStartDate, tripEndDate)
    )

    fun openActivityListing(
        planData: AddPlanData,
        tripHash: String,
        plannedActivityIdsByDay: Map<String, List<String>>,
        tripWideExcludedActivityIds: List<String>
    ) = open(
        TimelineRoutes.ACTIVITY_LISTING,
        ActivityListingArgs(planData, tripHash, plannedActivityIdsByDay, tripWideExcludedActivityIds)
    )

    fun openSavedPlans(
        favorites: List<SegmentFavoriteItem>,
        tripHash: String,
        availableDays: List<Date>,
        cityNameToIdMap: Map<String, Int>,
        plannedActivityIdsByDay: Map<String, List<String>>
    ) = open(
        TimelineRoutes.SAVED_PLANS,
        SavedPlansArgs(favorites, tripHash, availableDays, cityNameToIdMap, plannedActivityIdsByDay)
    )

    fun openStartingPoint(
        city: City?,
        bookedActivities: List<TimelineSegment>,
        favouriteItems: List<SegmentFavoriteItem>,
        userLocation: Coordinate?
    ) = open(
        TimelineRoutes.STARTING_POINT,
        StartingPointArgs(city, bookedActivities, favouriteItems, userLocation)
    )

    fun back() {
        navController.popBackStack()
    }

    /** Stores [value] for the screen that opened the current one and pops back to it. */
    fun finishWithResult(key: String, value: Any) {
        setResult(key, value)
        back()
    }

    /** Stores [value] for the opener without leaving the current screen. */
    fun setResult(key: String, value: Any) {
        navController.previousBackStackEntry?.savedStateHandle?.set(key, value)
    }

    private fun open(route: String, routeArgs: Any) {
        scope.putArgs(route, routeArgs)
        navController.navigate(route) { launchSingleTop = true }
    }
}

val LocalTimelineNavigator = staticCompositionLocalOf<TimelineNavigator> {
    error("LocalTimelineNavigator is not provided. Timeline screens must run inside TimelineNexus.")
}

@Composable
internal fun rememberTimelineNavigator(
    navController: NavController,
    scope: TimelineFlowScope
): TimelineNavigator = remember(navController, scope) { TimelineNavigator(navController, scope) }

/**
 * Delivers a result stored under [key] on [entry] once, then clears it so a
 * recomposition after the host pops back to the SDK does not replay it.
 */
@Composable
internal fun <T> ResultEffect(entry: NavBackStackEntry, key: String, onResult: (T) -> Unit) {
    val handle = entry.savedStateHandle
    val value by handle.getStateFlow<T?>(key, null).collectAsState()
    LaunchedEffect(value) {
        val result = value ?: return@LaunchedEffect
        handle.remove<T>(key)
        onResult(result)
    }
}
