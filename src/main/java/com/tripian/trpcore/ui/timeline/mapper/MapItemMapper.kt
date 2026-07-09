package com.tripian.trpcore.ui.timeline.mapper

import com.tripian.one.api.cities.model.City
import com.tripian.one.api.pois.model.Coordinate
import com.tripian.trpcore.domain.model.MapStep
import com.tripian.trpcore.domain.model.timeline.TimelineDisplayItem
import com.tripian.trpcore.domain.model.timeline.toDate
import com.tripian.trpcore.ui.timeline.adapter.MapBottomItem
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

/**
 * Output of [MapItemMapper.buildMapSteps].
 *
 * @property mapSteps the marker list with the very first marker pre-selected.
 * @property hasMultipleCities `true` when the day spans more than one city.
 * @property firstStepIdOfFirstCity poiId of the first marker in city 0; consumed by
 *                                  the VM as the default selected step id when none
 *                                  has been picked yet.
 */
data class MapStepsResult(
    val mapSteps: List<MapStep>,
    val hasMultipleCities: Boolean,
    val firstStepIdOfFirstCity: String?
)

/**
 * Pure domain → UI mappers for the map view layer. No state, no side effects —
 * inputs in, models out.
 */
class MapItemMapper @Inject constructor() {

    /**
     * Builds the per-step map markers for the given day's display items.
     * A global counter (1, 2, 3…) numbers non-flexible items only — flexible markers
     * render a "−" chip and do not advance it, keeping the sequence in lockstep with
     * [buildMapBottomItems]. Unlocated items still consume a position (no marker drawn)
     * so subsequent markers stay numbered correctly. Only the very first marker overall
     * is pre-selected.
     */
    fun buildMapSteps(items: List<TimelineDisplayItem>): MapStepsResult {
        val mapSteps = mutableListOf<MapStep>()
        val cityOrder = mutableMapOf<Int, Int>()
        var nextCityIndex = 0
        var globalPosition = 0

        fun nextPosition(): Int {
            globalPosition++
            return globalPosition
        }

        items.forEach { item ->
            val cityId = item.city?.id ?: 0
            val currentCityIndex = if (cityId != 0) {
                cityOrder.getOrPut(cityId) { nextCityIndex++ }
            } else {
                0
            }

            when (item) {
                is TimelineDisplayItem.Recommendations -> {
                    item.steps.forEach { step ->
                        val pos = nextPosition()
                        val poi = step.poi
                        val coord = poi?.coordinate
                        if (poi != null && coord != null && coord.lat != 0.0 && coord.lng != 0.0) {
                            mapSteps.add(
                                MapStep().apply {
                                    group = "step"
                                    poiId = poi.id ?: ""
                                    name = poi.name ?: ""
                                    coordinate = Coordinate().apply {
                                        lat = coord.lat
                                        lng = coord.lng
                                    }
                                    markerIcon = -1
                                    this.position = pos
                                    isOffer = false
                                    this.cityIndex = currentCityIndex; this.cityId = item.city?.id
                                }
                            )
                        }
                    }
                }

                is TimelineDisplayItem.BookedActivity -> {
                    val pos = nextPosition()
                    if (!item.isNoLocation) {
                        val coord = item.segment.additionalData?.coordinate ?: item.segment.coordinate
                        coord?.let {
                            if (it.lat != 0.0 && it.lng != 0.0) {
                                mapSteps.add(
                                    MapStep().apply {
                                        group = "booked"
                                        poiId = item.segment.additionalData?.activityId
                                            ?: "booked_${item.segmentIndex}"
                                        name = item.segment.additionalData?.title ?: item.segment.title ?: ""
                                        coordinate = Coordinate().apply {
                                            lat = it.lat
                                            lng = it.lng
                                        }
                                        markerIcon = -1
                                        this.position = pos
                                        isOffer = false
                                        this.cityIndex = currentCityIndex; this.cityId = item.city?.id
                                    }
                                )
                            }
                        }
                    }
                }

                is TimelineDisplayItem.FlexibleActivity -> {
                    if (!item.isNoLocation) {
                        val coord = item.segment.additionalData?.coordinate ?: item.segment.coordinate
                        coord?.let {
                            if (it.lat != 0.0 && it.lng != 0.0) {
                                mapSteps.add(
                                    MapStep().apply {
                                        group = "flexible"
                                        poiId = item.segment.additionalData?.activityId
                                            ?: "flexible_${item.segmentIndex}"
                                        name = item.title
                                        coordinate = Coordinate().apply {
                                            lat = it.lat
                                            lng = it.lng
                                        }
                                        markerIcon = -1
                                        isFlexible = true
                                        isOffer = false
                                        this.cityIndex = currentCityIndex; this.cityId = item.city?.id
                                    }
                                )
                            }
                        }
                    }
                }

                is TimelineDisplayItem.ManualPoi -> {
                    val pos = nextPosition()
                    val poi = item.step.poi
                    val coord = poi?.coordinate
                    if (poi != null && coord != null && coord.lat != 0.0 && coord.lng != 0.0) {
                        mapSteps.add(
                            MapStep().apply {
                                group = "manual"
                                poiId = poi.id ?: ""
                                name = poi.name ?: ""
                                coordinate = Coordinate().apply {
                                    lat = coord.lat
                                    lng = coord.lng
                                }
                                markerIcon = -1
                                this.position = pos
                                isOffer = false
                                this.cityIndex = currentCityIndex; this.cityId = item.city?.id
                            }
                        )
                    }
                }

                else -> Unit
            }
        }

        val firstStepIdOfFirstCity: String? =
            mapSteps.firstOrNull()?.also { it.isSelected = true }?.poiId

        return MapStepsResult(
            mapSteps = mapSteps,
            hasMultipleCities = cityOrder.size > 1,
            firstStepIdOfFirstCity = firstStepIdOfFirstCity
        )
    }

    /**
     * Builds the horizontal bottom-list items. Auto-selects the very first item
     * in the list — only one item can be selected across the whole bottom list,
     * regardless of city.
     */
    fun buildMapBottomItems(
        items: List<TimelineDisplayItem>,
        markerIds: Set<String>
    ): List<MapBottomItem> {
        val bottomItems = mutableListOf<MapBottomItem>()
        val cityOrder = mutableMapOf<Int, Int>()
        var nextCityIndex = 0
        var globalPosition = 0

        fun nextPosition(): Int {
            globalPosition++
            return globalPosition
        }

        val outputTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        items.forEach { item ->
            val cityId = item.city?.id ?: 0
            val currentCityIndex = if (cityId != 0) {
                cityOrder.getOrPut(cityId) { nextCityIndex++ }
            } else {
                0
            }

            when (item) {
                is TimelineDisplayItem.Recommendations -> {
                    item.steps.forEach { step ->
                        val dateTime = step.startDateTimes.toDate()
                        val stepCoord = step.poi?.coordinate
                        val stepHasLocation = stepCoord != null && stepCoord.lat != 0.0 && stepCoord.lng != 0.0

                        bottomItems.add(
                            MapBottomItem(
                                id = step.poi?.id ?: "step_${step.id}",
                                order = nextPosition(),
                                title = step.poi?.name ?: "",
                                imageUrl = step.poi?.image?.url,
                                time = dateTime?.let { outputTimeFormat.format(it) },
                                type = "step",
                                stepType = step.stepType,
                                cityIndex = currentCityIndex,
                                cityId = item.city?.id,
                                cityName = item.city?.name,
                                isNoLocation = !stepHasLocation
                            )
                        )
                    }
                }

                is TimelineDisplayItem.BookedActivity -> {
                    val data = item.segment.additionalData
                    val dateTime = data?.startDatetime.toDate()

                    bottomItems.add(
                        MapBottomItem(
                            id = data?.activityId ?: "booked_${item.segmentIndex}",
                            order = nextPosition(),
                            title = data?.title ?: item.segment.title ?: "",
                            imageUrl = data?.imageUrl,
                            time = dateTime?.let { outputTimeFormat.format(it) },
                            type = if (item.isReserved) "reserved" else "booked",
                            cityIndex = currentCityIndex,
                            cityId = item.city?.id,
                            cityName = item.city?.name,
                            isNoLocation = item.isNoLocation
                        )
                    )
                }

                is TimelineDisplayItem.ManualPoi -> {
                    val step = item.step
                    val dateTime = step.startDateTimes.toDate()

                    bottomItems.add(
                        MapBottomItem(
                            id = step.poi?.id ?: "manual_${step.id}",
                            order = nextPosition(),
                            title = step.poi?.name ?: "",
                            imageUrl = step.poi?.image?.url,
                            time = dateTime?.let { outputTimeFormat.format(it) },
                            type = "manual",
                            cityIndex = currentCityIndex,
                            cityId = item.city?.id,
                            cityName = item.city?.name,
                            isNoLocation = item.isNoLocation
                        )
                    )
                }

                is TimelineDisplayItem.FlexibleActivity -> {
                    val data = item.segment.additionalData

                    bottomItems.add(
                        MapBottomItem(
                            id = data?.activityId ?: "flexible_${item.segmentIndex}",
                            order = 0,
                            title = item.title,
                            imageUrl = item.imageUrl,
                            time = null,
                            type = "flexible",
                            cityIndex = currentCityIndex,
                            isFlexible = true,
                            cityId = item.city?.id,
                            cityName = item.city?.name,
                            isNoLocation = item.isNoLocation
                        )
                    )
                }

                else -> Unit
            }
        }

        return bottomItems.mapIndexed { index, item ->
            if (index == 0) item.copy(isSelected = true) else item
        }
    }

    /**
     * Builds one MapStep per unique city for the day. Used in multi-city overview
     * mode (CITY_MARKERS) before the user drills into a specific city.
     */
    fun buildCityMarkers(items: List<TimelineDisplayItem>): List<MapStep> {
        val cities = linkedMapOf<Int, City>()

        items.forEach { item ->
            val city = item.city ?: return@forEach
            val id = city.id ?: return@forEach
            if (city.coordinate == null) return@forEach
            cities.putIfAbsent(id, city)
        }

        return cities.values.mapNotNull { city ->
            city.coordinate?.let { coord ->
                MapStep().apply {
                    poiId = "city_${city.id}"
                    coordinate = Coordinate().apply {
                        lat = coord.lat
                        lng = coord.lng
                    }
                    isCityMarker = true
                    cityId = city.id
                    group = "city"
                    position = -1
                    markerIcon = -1
                    isSelected = false
                }
            }
        }
    }
}
