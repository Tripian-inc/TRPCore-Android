package com.tripian.trpcore.domain.usecase.timeline.sync

import com.tripian.one.api.pois.model.Coordinate
import com.tripian.trpcore.base.SuspendUseCase
import com.tripian.trpcore.domain.model.itinerary.ItineraryCoordinate
import com.tripian.trpcore.domain.model.itinerary.SegmentActivityItem
import com.tripian.trpcore.domain.model.itinerary.SegmentFavoriteItem
import com.tripian.trpcore.repository.TimelineRepository
import com.tripian.trpcore.repository.TourRepository
import com.tripian.trpcore.util.ActivityIdFormat
import com.tripian.trpcore.util.extensions.cityNameKey
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

/**
 * ResolveCityIdsForActivitiesUseCase
 *
 * Resolves the cityId of every booked/favourite activity the host handed to the
 * SDK. A cityId that arrives on the item is never trusted — hosts don't know our
 * city ids — so each item is resolved from scratch in three tiers, every tier
 * only handling what the previous one left open:
 *
 *  1. `tour-api/product-lookup` by `providerId + productId`. The authoritative
 *     answer: it returns the product's own city, and it is the only tier that
 *     works for activities with no coordinate (online tours, audio guides).
 *  2. `cities/resolve` by the activity's coordinate.
 *  3. The cityName → cityId map built from the trip's destinations, which relies
 *     on the host's city naming.
 *
 * Whatever survives all three keeps the incoming value. Blocking operation — the
 * other sync operations wait on its result.
 *
 * The activity duration is normalized in the same pass: hosts express it in their
 * own unit, so the product lookup's value (minutes) replaces the incoming one and
 * every consumer — saved plans cards, end-time math, segment payloads — reads a
 * single unit.
 * iOS Reference: Guide Operation 1 (City Resolution)
 */
class ResolveCityIdsForActivitiesUseCase @Inject constructor(
    private val repository: TimelineRepository,
    private val tourRepository: TourRepository
) : SuspendUseCase<ResolveCityIdsForActivitiesUseCase.Result, ResolveCityIdsForActivitiesUseCase.Params>() {

    /**
     * @param knownActivityIds base ids the timeline already holds; those items are
     *   skipped by the lookup tier because their segment already carries a city.
     *   Empty when the timeline is about to be created.
     */
    data class Params(
        val tripItems: List<SegmentActivityItem>,
        val favouriteItems: List<SegmentFavoriteItem>,
        val existingCityMap: Map<String, Int>,
        val knownActivityIds: Set<String> = emptySet()
    )

    /**
     * @param tripItems / [favouriteItems] the input lists with every resolved
     *   cityId and product duration written back onto the item.
     * @param cityMap cityName → cityId, keyed by [cityNameKey].
     */
    data class Result(
        val tripItems: List<SegmentActivityItem>,
        val favouriteItems: List<SegmentFavoriteItem>,
        val cityMap: Map<String, Int>
    )

    override suspend fun execute(params: Params): Result {
        val cityMap = params.existingCityMap.toMutableMap()

        val tripCityIds = arrayOfNulls<Int>(params.tripItems.size)
        val favouriteCityIds = arrayOfNulls<Int>(params.favouriteItems.size)
        val tripDurations = arrayOfNulls<Double>(params.tripItems.size)
        val favouriteDurations = arrayOfNulls<Double>(params.favouriteItems.size)

        applyLookupTier(params, tripCityIds, favouriteCityIds, tripDurations, favouriteDurations)
        applyCoordinateTier(params, tripCityIds, favouriteCityIds)
        applyCityNameTier(params, tripCityIds, favouriteCityIds, cityMap)

        val tripItems = params.tripItems.mapIndexed { index, item ->
            item.copy(
                cityId = tripCityIds[index] ?: item.cityId,
                duration = tripDurations[index] ?: item.duration
            )
        }
        val favouriteItems = params.favouriteItems.mapIndexed { index, item ->
            item.copy(
                cityId = favouriteCityIds[index] ?: item.cityId,
                duration = favouriteDurations[index] ?: item.duration
            )
        }

        tripItems.forEach { item -> rememberCity(item.cityName, item.cityId, cityMap) }
        favouriteItems.forEach { item -> rememberCity(item.cityName, item.cityId, cityMap) }

        return Result(tripItems, favouriteItems, cityMap)
    }

    /**
     * Tier 1. Every distinct activity id the timeline doesn't already hold is looked
     * up once, concurrently; a failed or city-less lookup leaves the item to the
     * next tier. The product's duration is taken from the same response.
     */
    private suspend fun applyLookupTier(
        params: Params,
        tripCityIds: Array<Int?>,
        favouriteCityIds: Array<Int?>,
        tripDurations: Array<Double?>,
        favouriteDurations: Array<Double?>
    ) {
        val activityIds = (
            params.tripItems.map { it.activityId } + params.favouriteItems.map { it.activityId }
            )
            .mapNotNull { id -> lookupKey(id) }
            .filterNot { id -> id in params.knownActivityIds }
            .distinct()

        if (activityIds.isEmpty()) return

        val resolved = coroutineScope {
            activityIds
                .map { activityId -> async { activityId to lookupProduct(activityId) } }
                .map { deferred -> deferred.await() }
                .mapNotNull { (activityId, product) -> product?.let { activityId to it } }
                .toMap()
        }

        params.tripItems.forEachIndexed { index, item ->
            val product = resolved[lookupKey(item.activityId)]
            tripCityIds[index] = product?.cityId
            tripDurations[index] = product?.durationMinutes
        }
        params.favouriteItems.forEachIndexed { index, item ->
            val product = resolved[lookupKey(item.activityId)]
            favouriteCityIds[index] = product?.cityId
            favouriteDurations[index] = product?.durationMinutes
        }
    }

    /**
     * Host activity ids arrive bare ("2373"), so the provider is never read off the
     * id — the SDK's own [ActivityIdFormat.DEFAULT_PROVIDER_ID] is authoritative.
     */
    private suspend fun lookupProduct(productId: String): ProductInfo? = runCatching {
        val product = tourRepository.lookupTourProductAsync(
            providerId = ActivityIdFormat.DEFAULT_PROVIDER_ID,
            productId = productId
        ).data ?: return@runCatching null

        ProductInfo(
            cityId = product.cityId.takeIf { it > 0 },
            durationMinutes = product.duration?.takeIf { it > 0 }
        )
    }.getOrNull()

    /**
     * Tier 2. Single bulk call for the items tier 1 left open that do carry a
     * usable coordinate.
     */
    private suspend fun applyCoordinateTier(
        params: Params,
        tripCityIds: Array<Int?>,
        favouriteCityIds: Array<Int?>
    ) {
        val targets = mutableListOf<Pair<Array<Int?>, Int>>()
        val coordinates = mutableListOf<Coordinate>()

        params.tripItems.forEachIndexed { index, item ->
            if (tripCityIds[index] != null) return@forEachIndexed
            val coordinate = usableCoordinate(item.coordinate) ?: return@forEachIndexed
            targets += tripCityIds to index
            coordinates += coordinate
        }
        params.favouriteItems.forEachIndexed { index, item ->
            if (favouriteCityIds[index] != null) return@forEachIndexed
            val coordinate = usableCoordinate(item.coordinate) ?: return@forEachIndexed
            targets += favouriteCityIds to index
            coordinates += coordinate
        }

        if (coordinates.isEmpty()) return

        val resolved = try {
            repository.resolveCitiesAsync(coordinates)
        } catch (error: Throwable) {
            android.util.Log.e("SYNC", "City resolution failed: ${error.message}")
            return
        }

        resolved.forEachIndexed { index, data ->
            val cityId = data.cityId ?: 0
            if (cityId <= 0) return@forEachIndexed
            val (holder, position) = targets.getOrNull(index) ?: return@forEachIndexed
            holder[position] = cityId
        }
    }

    /** Tier 3. Falls back to the destination-derived cityName map. */
    private fun applyCityNameTier(
        params: Params,
        tripCityIds: Array<Int?>,
        favouriteCityIds: Array<Int?>,
        cityMap: Map<String, Int>
    ) {
        params.tripItems.forEachIndexed { index, item ->
            if (tripCityIds[index] == null) tripCityIds[index] = mappedCityId(item.cityName, cityMap)
        }
        params.favouriteItems.forEachIndexed { index, item ->
            if (favouriteCityIds[index] == null) {
                favouriteCityIds[index] = mappedCityId(item.cityName, cityMap)
            }
        }
    }

    private fun usableCoordinate(coordinate: ItineraryCoordinate): Coordinate? {
        if (coordinate.lat == 0.0 && coordinate.lng == 0.0) return null
        return Coordinate().apply {
            lat = coordinate.lat
            lng = coordinate.lng
        }
    }

    /** Bare product id — the form both the lookup request and [Params.knownActivityIds] use. */
    private fun lookupKey(activityId: String?): String? = ActivityIdFormat.base(activityId)

    private fun mappedCityId(cityName: String?, cityMap: Map<String, Int>): Int? {
        val name = cityName?.takeIf { it.isNotBlank() } ?: return null
        return cityMap[name.cityNameKey()]?.takeIf { it > 0 }
    }

    private fun rememberCity(cityName: String?, cityId: Int?, cityMap: MutableMap<String, Int>) {
        val name = cityName?.takeIf { it.isNotBlank() } ?: return
        val resolved = cityId?.takeIf { it > 0 } ?: return
        cityMap[name.cityNameKey()] = resolved
    }

    /** @param durationMinutes the product's own duration, in minutes. */
    private data class ProductInfo(val cityId: Int?, val durationMinutes: Double?)
}
