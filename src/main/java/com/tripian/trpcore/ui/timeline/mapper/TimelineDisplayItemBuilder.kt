package com.tripian.trpcore.ui.timeline.mapper

import com.tripian.one.api.cities.model.City
import com.tripian.one.api.timeline.model.SegmentType
import com.tripian.one.api.timeline.model.Timeline
import com.tripian.one.api.timeline.model.TimelineSegment
import com.tripian.trpcore.domain.model.timeline.TimelineDisplayItem
import com.tripian.trpcore.domain.model.timeline.toApiDateString
import com.tripian.trpcore.domain.model.timeline.toDate
import com.tripian.trpcore.repository.TripRepository
import com.tripian.trpcore.util.extensions.isFlexibleActivity
import java.util.Date
import javax.inject.Inject

/**
 * Builds the day's [TimelineDisplayItem] list from a [Timeline] in three phases:
 * segment iteration (per-day segment → display-item variant), conflict detection
 * (stamps `hasConflict` / `timeOverlap…`) and city grouping (section headers/footers,
 * flexible activities pinned to top, per-city sequential numbering, collapsed sections).
 */
class TimelineDisplayItemBuilder @Inject constructor(
    private val tripRepository: TripRepository
) {

    /**
     * @param timeline current timeline payload
     * @param date the day being rendered
     * @param cities cities snapshot used as a fallback when the cache can't resolve `segment.cityId`
     * @param collapsedSectionCityIds city ids collapsed in the section accordion —
     *                                content is dropped but the header stays
     * @param emptyStateMessage localized "no plans for this day" text
     * @param hiddenSegmentIndices segments queued for background deletion; excluded from render
     */
    fun build(
        timeline: Timeline,
        date: Date,
        cities: List<City>,
        collapsedSectionCityIds: Set<Int>,
        emptyStateMessage: String,
        hiddenSegmentIndices: Set<Int> = emptySet()
    ): List<TimelineDisplayItem> {
        val rawItems = generateForDay(timeline, date, cities, emptyStateMessage, hiddenSegmentIndices)
        val withConflicts = detectTimeConflicts(rawItems)
        return groupItemsByCity(withConflicts, collapsedSectionCityIds)
    }

    // ============================================================
    // Phase 1 — Segment → DisplayItem
    // ============================================================

    /**
     * Turns the day's segments into display items. "TimelineDate" control segments and
     * empty smart recommendations are skipped. Expired step ids and step times are
     * snapshotted into the Recommendations item so its data-class equality (and DiffUtil)
     * reacts to in-place step mutations.
     */
    private fun generateForDay(
        timeline: Timeline,
        date: Date,
        cities: List<City>,
        emptyStateMessage: String,
        hiddenSegmentIndices: Set<Int>
    ): List<TimelineDisplayItem> {
        val items = mutableListOf<TimelineDisplayItem>()
        val dateStr = date.toApiDateString()
        val segments = timeline.tripProfile?.segments ?: emptyList()

        segments.forEachIndexed { index, segment ->
            if (segment.startDate?.startsWith(dateStr) != true) return@forEachIndexed
            if (index in hiddenSegmentIndices) return@forEachIndexed

            val segmentType = segment.segmentType
            val plan = timeline.plans?.getOrNull(index)
            val planId = plan?.id

            when (segmentType) {
                SegmentType.BOOKED_ACTIVITY -> {
                    items.add(
                        TimelineDisplayItem.BookedActivity(
                            segment = segment,
                            isReserved = false,
                            segmentIndex = index,
                            city = getCityForSegment(segment, timeline, cities),
                            planId = planId,
                            startDateTimeSnapshot = segment.startDate
                                ?: segment.additionalData?.startDatetime,
                            endDateTimeSnapshot = segment.endDate
                                ?: segment.additionalData?.endDatetime
                        )
                    )
                }

                SegmentType.RESERVED_ACTIVITY -> {
                    if (segment.isFlexibleActivity) {
                        items.add(
                            TimelineDisplayItem.FlexibleActivity(
                                segment = segment,
                                segmentIndex = index,
                                city = getCityForSegment(segment, timeline, cities),
                                planId = planId,
                                isNoLocation = segment.additionalData?.isNoLocation == true,
                                isAvailabilityExpired =
                                    segment.additionalData?.isAvailabilityExpired == true
                            )
                        )
                    } else {
                        items.add(
                            TimelineDisplayItem.BookedActivity(
                                segment = segment,
                                isReserved = true,
                                segmentIndex = index,
                                city = getCityForSegment(segment, timeline, cities),
                                planId = planId,
                                isAvailabilityExpired =
                                    segment.additionalData?.isAvailabilityExpired == true,
                                startDateTimeSnapshot = segment.startDate
                                    ?: segment.additionalData?.startDatetime,
                                endDateTimeSnapshot = segment.endDate
                                    ?: segment.additionalData?.endDatetime
                            )
                        )
                    }
                }

                SegmentType.ITINERARY, SegmentType.GENERATED -> {
                    if (plan != null) {
                        val steps = plan.steps ?: emptyList()
                        if (segment.title == "TimelineDate") return@forEachIndexed
                        if (plan.generatedStatus == -2 && steps.isEmpty()) return@forEachIndexed

                        val cityId = segment.cityId
                        val recommendationIndex = items.count { item ->
                            item is TimelineDisplayItem.Recommendations && item.city?.id == cityId
                        } + 1

                        val expiredStepIds = steps
                            .filter { it.isAvailabilityExpired }
                            .map { it.id }
                            .toSet()
                        val stepFingerprint = steps.joinToString("|") {
                            "${it.id}:${it.startDateTimes}:${it.endDateTimes}"
                        }
                        items.add(
                            TimelineDisplayItem.Recommendations(
                                plan = plan,
                                steps = steps,
                                segment = segment,
                                segmentIndex = index,
                                cachedCity = getCityForSegment(segment, timeline, cities),
                                recommendationIndex = recommendationIndex,
                                expiredStepIds = expiredStepIds,
                                stepFingerprint = stepFingerprint
                            )
                        )
                    }
                }

                SegmentType.MANUAL_POI -> {
                    plan?.steps?.firstOrNull()?.let { step ->
                        items.add(
                            TimelineDisplayItem.ManualPoi(
                                step = step,
                                segment = segment,
                                segmentIndex = index,
                                city = getCityForSegment(segment, timeline, cities),
                                planId = planId
                            )
                        )
                    }
                }

                else -> Unit
            }
        }

        if (items.isEmpty()) {
            items.add(TimelineDisplayItem.EmptyState(message = emptyStateMessage))
        }

        return items
    }

    /**
     * Resolves the City for a segment. Priority:
     * 1. Cache (`tripRepository.getCachedCityById`) — full data incl. coordinates.
     * 2. The provided `cities` snapshot (VM's current `_cities.value`).
     * 3. Fallback to a plan-level city on the timeline.
     */
    private fun getCityForSegment(
        segment: TimelineSegment,
        timeline: Timeline,
        cities: List<City>
    ): City? {
        segment.cityId?.let { cityId ->
            tripRepository.getCachedCityById(cityId)?.let { return it }
            cities.find { it.id == cityId }?.let { return it }
            return timeline.plans?.find { it.city?.id == cityId }?.city
        }
        return timeline.city
    }

    // ============================================================
    // Phase 2 — Time conflict detection
    // ============================================================

    /** A flattened time range used during conflict analysis. */
    private data class TimeRange(
        val startTime: Date,
        val endTime: Date,
        val itemType: String,
        val itemIndex: Int,
        val stepId: Int? = null,
        val planId: String? = null,
        val segmentType: String? = null
    )

    private fun parsePlanId(planId: String?): Int {
        if (planId.isNullOrEmpty()) return 0
        val firstPart = planId.split("-").firstOrNull() ?: planId
        return firstPart.toIntOrNull() ?: 0
    }

    private fun determineOlderAndNewer(
        range1: TimeRange,
        range2: TimeRange
    ): Pair<TimeRange, TimeRange> {
        val planId1 = parsePlanId(range1.planId)
        val planId2 = parsePlanId(range2.planId)
        return if (planId1 < planId2) Pair(range1, range2) else Pair(range2, range1)
    }

    private fun collectTimeRanges(items: List<TimelineDisplayItem>): List<TimeRange> {
        val timeRanges = mutableListOf<TimeRange>()

        items.forEachIndexed { index, item ->
            when (item) {
                is TimelineDisplayItem.BookedActivity -> {
                    val startTime = item.startDateTime?.toDate()
                    val endTime = item.endDateTime?.toDate()
                    if (startTime != null && endTime != null) {
                        timeRanges.add(
                            TimeRange(
                                startTime = startTime,
                                endTime = endTime,
                                itemType = "booked",
                                itemIndex = index,
                                planId = item.planId,
                                segmentType = if (item.isReserved) "reserved_activity" else "booked_activity"
                            )
                        )
                    }
                }

                is TimelineDisplayItem.ManualPoi -> {
                    val startTime = item.startTime
                    val endTime = item.endTime
                    if (startTime != null && endTime != null) {
                        timeRanges.add(
                            TimeRange(
                                startTime = startTime,
                                endTime = endTime,
                                itemType = "manual",
                                itemIndex = index,
                                planId = item.planId,
                                segmentType = "manual_poi"
                            )
                        )
                    }
                }

                is TimelineDisplayItem.Recommendations -> {
                    item.steps.forEach { step ->
                        val startTime = step.startDateTimes?.toDate()
                        val endTime = step.endDateTimes?.toDate()
                        if (startTime != null && endTime != null && step.id != null) {
                            timeRanges.add(
                                TimeRange(
                                    startTime = startTime,
                                    endTime = endTime,
                                    itemType = "step",
                                    itemIndex = index,
                                    stepId = step.id,
                                    planId = item.planId,
                                    segmentType = "itinerary"
                                )
                            )
                        }
                    }
                }

                else -> Unit
            }
        }

        return timeRanges
    }

    /**
     * Transitive conflict groups via Union-Find:
     * A∩B and B∩C → {A,B,C}. Returns only groups with ≥ 3 items.
     */
    private fun buildConflictGroups(conflicts: List<Pair<Int, Int>>): List<Set<Int>> {
        val groups = mutableListOf<MutableSet<Int>>()

        conflicts.forEach { (i, j) ->
            val existing = groups.find { it.contains(i) || it.contains(j) }
            if (existing != null) {
                existing.add(i)
                existing.add(j)
            } else {
                groups.add(mutableSetOf(i, j))
            }
        }

        var merged = true
        while (merged) {
            merged = false
            for (i in groups.indices) {
                for (j in (i + 1) until groups.size) {
                    if (groups[i].any { it in groups[j] }) {
                        groups[i].addAll(groups[j])
                        groups.removeAt(j)
                        merged = true
                        break
                    }
                }
                if (merged) break
            }
        }

        return groups.filter { it.size >= 3 }
    }

    private fun applyThreePlusConflictRules(
        groupIndices: Set<Int>,
        timeRanges: List<TimeRange>,
        visualConflictIndices: MutableSet<Int>,
        visualConflictStepIds: MutableMap<Int, MutableSet<Int>>,
        timeOverlapIndices: MutableSet<Int>,
        timeOverlapStepIds: MutableMap<Int, MutableSet<Int>>
    ) {
        val groupRanges = groupIndices.map { timeRanges[it] }

        groupRanges.forEach { range ->
            visualConflictIndices.add(range.itemIndex)
            if (range.itemType == "step" && range.stepId != null) {
                visualConflictStepIds.getOrPut(range.itemIndex) { mutableSetOf() }.add(range.stepId)
            }
        }

        val nonBookedRanges = groupRanges.filter { it.segmentType != "booked_activity" }

        if (nonBookedRanges.isNotEmpty()) {
            val planIds = nonBookedRanges.mapNotNull { it.planId }.distinct()
            val minPlanId = planIds.minOfOrNull { parsePlanId(it) } ?: 0

            nonBookedRanges.forEach { range ->
                val rangePlanId = parsePlanId(range.planId)
                if (rangePlanId > minPlanId) {
                    if (range.itemType == "step" && range.stepId != null) {
                        timeOverlapStepIds.getOrPut(range.itemIndex) { mutableSetOf() }.add(range.stepId)
                    } else {
                        timeOverlapIndices.add(range.itemIndex)
                    }
                }
            }
        }
    }

    /**
     * Stamps `hasConflict` / `timeOverlap…` on each item based on time-range overlap.
     * 2-item conflicts: the newer plan shows "Time Overlap" (when booked-activity is the
     * newer one, the older shows it instead). 3+ groups: the oldest plan keeps only the
     * visual flag. Non-reserved BookedActivity only ever gets the visual style.
     */
    private fun detectTimeConflicts(items: List<TimelineDisplayItem>): List<TimelineDisplayItem> {
        val timeRanges = collectTimeRanges(items)

        val twoWayConflicts = mutableListOf<Pair<Int, Int>>()
        for (i in timeRanges.indices) {
            for (j in (i + 1) until timeRanges.size) {
                val r1 = timeRanges[i]
                val r2 = timeRanges[j]
                if (r1.startTime.before(r2.endTime) && r2.startTime.before(r1.endTime)) {
                    twoWayConflicts.add(Pair(i, j))
                }
            }
        }

        val visualConflictIndices = mutableSetOf<Int>()
        val visualConflictStepIds = mutableMapOf<Int, MutableSet<Int>>()
        val timeOverlapIndices = mutableSetOf<Int>()
        val timeOverlapStepIds = mutableMapOf<Int, MutableSet<Int>>()

        val conflictGroups = buildConflictGroups(twoWayConflicts)
        val handledByThreePlus = conflictGroups.flatten().toSet()

        conflictGroups.forEach { groupIndices ->
            applyThreePlusConflictRules(
                groupIndices,
                timeRanges,
                visualConflictIndices,
                visualConflictStepIds,
                timeOverlapIndices,
                timeOverlapStepIds
            )
        }

        twoWayConflicts.forEach { (i, j) ->
            if (i in handledByThreePlus || j in handledByThreePlus) return@forEach

            val r1 = timeRanges[i]
            val r2 = timeRanges[j]

            visualConflictIndices.add(r1.itemIndex)
            visualConflictIndices.add(r2.itemIndex)

            if (r1.itemType == "step" && r1.stepId != null) {
                visualConflictStepIds.getOrPut(r1.itemIndex) { mutableSetOf() }.add(r1.stepId)
            }
            if (r2.itemType == "step" && r2.stepId != null) {
                visualConflictStepIds.getOrPut(r2.itemIndex) { mutableSetOf() }.add(r2.stepId)
            }

            if (r1.planId == r2.planId) return@forEach

            val (older, newer) = determineOlderAndNewer(r1, r2)
            val target = when {
                newer.segmentType == "booked_activity" -> older
                older.segmentType == "booked_activity" -> newer
                else -> newer
            }
            if (target.itemType == "step" && target.stepId != null) {
                timeOverlapStepIds.getOrPut(target.itemIndex) { mutableSetOf() }.add(target.stepId)
            } else {
                timeOverlapIndices.add(target.itemIndex)
            }
        }

        return items.mapIndexed { index, item ->
            when (item) {
                is TimelineDisplayItem.BookedActivity -> {
                    val inConflict = index in visualConflictIndices
                    val showOverlap = if (item.isReserved) inConflict else false
                    item.copy(hasConflict = inConflict, showTimeOverlapText = showOverlap)
                }

                is TimelineDisplayItem.ManualPoi -> {
                    val inConflict = index in visualConflictIndices
                    item.copy(hasConflict = inConflict, showTimeOverlapText = inConflict)
                }

                is TimelineDisplayItem.Recommendations -> {
                    val conflictStepIds = visualConflictStepIds[index] ?: emptySet()
                    item.copy(
                        conflictingStepIds = conflictStepIds,
                        timeOverlapStepIds = conflictStepIds
                    )
                }

                else -> item
            }
        }
    }

    // ============================================================
    // Phase 3 — City grouping + ordering
    // ============================================================

    private fun groupItemsByCity(
        items: List<TimelineDisplayItem>,
        collapsedSectionCityIds: Set<Int>
    ): List<TimelineDisplayItem> {
        val contentItems = items.filterNot {
            it is TimelineDisplayItem.SectionHeader || it is TimelineDisplayItem.SectionFooter
        }
        if (contentItems.isEmpty()) return items

        val groupedByCity = linkedMapOf<Int?, MutableList<TimelineDisplayItem>>()
        contentItems.forEach { item ->
            val cityId = item.city?.id
            groupedByCity.getOrPut(cityId) { mutableListOf() }.add(item)
        }

        val result = mutableListOf<TimelineDisplayItem>()
        val totalCities = groupedByCity.size
        var currentOrder = 1

        groupedByCity.entries.forEachIndexed { cityIndex, (_, rawCityItems) ->
            val cityItems = rawCityItems.sortedWith(
                compareByDescending { it is TimelineDisplayItem.FlexibleActivity }
            )
            val city = cityItems.firstOrNull()?.city
            val isCollapsed = city != null && city.id in collapsedSectionCityIds

            if (city != null) {
                result.add(
                    TimelineDisplayItem.SectionHeader(
                        cityName = city.name ?: "",
                        city = city
                    )
                )
            }

            if (isCollapsed) {
                cityItems.forEach { item ->
                    if (item !is TimelineDisplayItem.FlexibleActivity) {
                        currentOrder += when (item) {
                            is TimelineDisplayItem.Recommendations -> item.steps.size.coerceAtLeast(1)
                            else -> 1
                        }
                    }
                }
                if (totalCities > 1 && cityIndex < totalCities - 1) {
                    result.add(TimelineDisplayItem.SectionFooter(city = city))
                }
                return@forEachIndexed
            }

            cityItems.forEach { item ->
                val itemWithOrder = when (item) {
                    is TimelineDisplayItem.BookedActivity -> {
                        val ordered = item.copy(order = currentOrder)
                        currentOrder += 1
                        ordered
                    }

                    is TimelineDisplayItem.ManualPoi -> {
                        val ordered = item.copy(order = currentOrder)
                        currentOrder += 1
                        ordered
                    }

                    is TimelineDisplayItem.FlexibleActivity -> item

                    is TimelineDisplayItem.Recommendations -> {
                        val stepCount = item.steps.size.coerceAtLeast(1)
                        val ordered = item.copy(
                            order = 0,
                            startingOrder = currentOrder
                        )
                        currentOrder += stepCount
                        ordered
                    }

                    else -> item
                }
                result.add(itemWithOrder)
            }

            if (totalCities > 1 && cityIndex < totalCities - 1) {
                result.add(TimelineDisplayItem.SectionFooter(city = city))
            }
        }

        return result
    }
}
