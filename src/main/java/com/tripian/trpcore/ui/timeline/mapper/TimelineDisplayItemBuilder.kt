package com.tripian.trpcore.ui.timeline.mapper

import com.tripian.one.api.cities.model.City
import com.tripian.one.api.pois.model.Coordinate
import com.tripian.one.api.timeline.model.SegmentType
import com.tripian.one.api.timeline.model.Timeline
import com.tripian.one.api.timeline.model.TimelinePlan
import com.tripian.one.api.timeline.model.TimelineSegment
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.domain.model.timeline.FlatRouteChain
import com.tripian.trpcore.domain.model.timeline.StepRouteInfo
import com.tripian.trpcore.domain.model.timeline.TimelineDisplayItem
import com.tripian.trpcore.domain.model.timeline.hasNoLocation
import com.tripian.trpcore.domain.model.timeline.isGenerating
import com.tripian.trpcore.domain.model.timeline.isMissingOrZero
import com.tripian.trpcore.domain.model.timeline.planFor
import com.tripian.trpcore.domain.model.timeline.routeWaypoint
import com.tripian.trpcore.domain.model.timeline.toApiDateString
import com.tripian.trpcore.domain.model.timeline.toDate
import com.tripian.trpcore.repository.TripRepository
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.isFlexibleActivity
import java.util.Date
import javax.inject.Inject

/**
 * Builds the day's [TimelineDisplayItem] list from a [Timeline] in three phases:
 * segment iteration (per-day segment → display-item variant), conflict detection
 * (stamps `hasConflict` / `timeOverlap…`) and city grouping (section headers/footers,
 * flexible activities pinned to top, per-city sequential numbering, collapsed sections).
 * Hosts with [com.tripian.trpcore.base.host.HostStrategy.usesFlatTimeline] get plan
 * steps as top-level rows ordered by start time with route separators in between.
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
     * @param flatRoutes route legs per [FlatRouteChain.key]; interleaved as separators in flat mode
     */
    fun build(
        timeline: Timeline,
        date: Date,
        cities: List<City>,
        collapsedSectionCityIds: Set<Int>,
        emptyStateMessage: String,
        hiddenSegmentIndices: Set<Int> = emptySet(),
        flatRoutes: Map<String, List<StepRouteInfo>> = emptyMap()
    ): List<TimelineDisplayItem> {
        val rawItems = generateForDay(timeline, date, cities, emptyStateMessage, hiddenSegmentIndices)
        val withConflicts = detectTimeConflicts(rawItems)
        return if (usesFlatTimeline) {
            groupItemsByCityFlat(withConflicts, collapsedSectionCityIds, flatRoutes)
        } else {
            groupItemsByCity(withConflicts, collapsedSectionCityIds)
        }
    }

    private val usesFlatTimeline: Boolean
        get() = TRPCore.host.usesFlatTimeline()

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
            val plan = timeline.planFor(segment)
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
                                isNoLocation = segment.hasNoLocation(),
                                isAvailabilityExpired =
                                    segment.additionalData?.isAvailabilityExpired == true,
                                priceSnapshot = segment.additionalData?.price
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
                                    ?: segment.additionalData?.endDatetime,
                                priceSnapshot = segment.additionalData?.price
                            )
                        )
                    }
                }

                SegmentType.ITINERARY, SegmentType.GENERATED -> {
                    if (plan != null) {
                        val steps = plan.steps ?: emptyList()
                        if (segment.title == "TimelineDate") return@forEachIndexed
                        if (plan.generatedStatus == -2 && steps.isEmpty()) return@forEachIndexed
                        if (usesFlatTimeline) {
                            items.addAll(flatItemsForPlan(plan, segment, index, timeline, cities))
                            return@forEachIndexed
                        }

                        val cityId = segment.cityId
                        val recommendationIndex = items.count { item ->
                            item is TimelineDisplayItem.Recommendations && item.city?.id == cityId
                        } + 1

                        val expiredStepIds = steps
                            .filter { it.isAvailabilityExpired }
                            .map { it.id }
                            .toSet()
                        val stepFingerprint = steps.joinToString("|") {
                            "${it.id}:${it.startDateTimes}:${it.endDateTimes}:" +
                                "${it.poi?.additionalData?.price}"
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
     * Flat-mode rows for an itinerary plan: a starting point row followed by one
     * [TimelineDisplayItem.PlanStep] per step. A plan still generating or generated
     * without places yields no rows; the screen loader and the alert cover those.
     */
    private fun flatItemsForPlan(
        plan: TimelinePlan,
        segment: TimelineSegment,
        segmentIndex: Int,
        timeline: Timeline,
        cities: List<City>
    ): List<TimelineDisplayItem> {
        val steps = plan.steps.orEmpty()
        if (plan.isGenerating || steps.isEmpty()) return emptyList()

        val city = getCityForSegment(segment, timeline, cities)
        val startingPoint = TimelineDisplayItem.StartingPoint(
            name = startingPointName(segment, city, plan),
            coordinate = startingPointCoordinate(segment, city, plan),
            city = city,
            segmentIndex = segmentIndex,
            planId = plan.id
        )
        val stepRows = steps.map { step ->
            TimelineDisplayItem.PlanStep(
                step = step,
                segment = segment,
                segmentIndex = segmentIndex,
                city = city,
                planId = plan.id,
                isAvailabilityExpired = step.isAvailabilityExpired,
                startDateTimeSnapshot = step.startDateTimes,
                endDateTimeSnapshot = step.endDateTimes,
                priceSnapshot = step.poi?.additionalData?.price
            )
        }
        return listOf(startingPoint) + stepRows
    }

    private fun startingPointName(segment: TimelineSegment, city: City?, plan: TimelinePlan): String {
        segment.accommodation?.name?.let { return it }
        val cityCentre = TRPCore.core.miscRepository.getLanguageValueForKey(LanguageConst.ADD_PLAN_CITY_CENTER)
        val cityName = city?.name ?: plan.city?.name ?: return cityCentre
        return "$cityName | $cityCentre"
    }

    private fun startingPointCoordinate(segment: TimelineSegment, city: City?, plan: TimelinePlan): Coordinate? =
        listOf(segment.coordinate, segment.accommodation?.coordinate, city?.coordinate, plan.city?.coordinate)
            .firstOrNull { !it.isMissingOrZero() }

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

    /**
     * A 24-hour-or-longer span is a validity window (24/48h passes), not a busy
     * block, so it is kept out of conflict analysis — otherwise it collides with
     * everything else on the day.
     */
    private fun spansFullDay(startTime: Date, endTime: Date): Boolean {
        return endTime.time - startTime.time >= FULL_DAY_IN_MILLIS
    }

    private fun collectTimeRanges(items: List<TimelineDisplayItem>): List<TimeRange> {
        val timeRanges = mutableListOf<TimeRange>()

        items.forEachIndexed { index, item ->
            when (item) {
                is TimelineDisplayItem.BookedActivity -> {
                    val startTime = item.startDateTime?.toDate()
                    val endTime = item.endDateTime?.toDate()
                    if (startTime != null && endTime != null && !spansFullDay(startTime, endTime)) {
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
                    if (startTime != null && endTime != null && !spansFullDay(startTime, endTime)) {
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

                is TimelineDisplayItem.PlanStep -> {
                    val startTime = item.startTime
                    val endTime = item.endTime
                    if (startTime != null && endTime != null && !spansFullDay(startTime, endTime)) {
                        timeRanges.add(
                            TimeRange(
                                startTime = startTime,
                                endTime = endTime,
                                itemType = "step",
                                itemIndex = index,
                                stepId = item.step.id,
                                planId = item.planId,
                                segmentType = "itinerary"
                            )
                        )
                    }
                }

                is TimelineDisplayItem.Recommendations -> {
                    item.steps.forEach { step ->
                        val startTime = step.startDateTimes?.toDate()
                        val endTime = step.endDateTimes?.toDate()
                        if (startTime != null && endTime != null && step.id != null &&
                            !spansFullDay(startTime, endTime)
                        ) {
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

                is TimelineDisplayItem.PlanStep -> {
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

    // ============================================================
    // Phase 3 (flat) — City grouping + chronological ordering
    // ============================================================

    /**
     * Flat layout of each city group: flexible activities pinned first, then the
     * starting point row, then every timed row by start time (ties: booked →
     * reserved → POI; rows without a time last), numbered sequentially across cities.
     * Legs already known for the group's [FlatRouteChain] are interleaved as
     * separators above the row each leg arrives at.
     */
    private fun groupItemsByCityFlat(
        items: List<TimelineDisplayItem>,
        collapsedSectionCityIds: Set<Int>,
        flatRoutes: Map<String, List<StepRouteInfo>>
    ): List<TimelineDisplayItem> {
        val contentItems = items.filterNot {
            it is TimelineDisplayItem.SectionHeader || it is TimelineDisplayItem.SectionFooter
        }
        if (contentItems.isEmpty()) return items

        val groupedByCity = linkedMapOf<Int?, MutableList<TimelineDisplayItem>>()
        contentItems.forEach { item ->
            groupedByCity.getOrPut(item.city?.id) { mutableListOf() }.add(item)
        }

        val result = mutableListOf<TimelineDisplayItem>()
        val totalCities = groupedByCity.size
        var currentOrder = 1

        groupedByCity.entries.forEachIndexed { cityIndex, (_, cityItems) ->
            val city = cityItems.firstOrNull()?.city
            val isCollapsed = city != null && city.id in collapsedSectionCityIds
            val timedRows = cityItems
                .filter { it.isFlatTimedRow }
                .sortedWith(
                    compareBy<TimelineDisplayItem, Long?>(nullsLast()) { it.flatSortTime?.time }
                        .thenBy { it.flatTypeRank }
                )

            if (city != null) {
                result.add(
                    TimelineDisplayItem.SectionHeader(
                        cityName = city.name ?: "",
                        city = city
                    )
                )
            }

            if (isCollapsed) {
                currentOrder += timedRows.size
            } else {
                val rows = mutableListOf<TimelineDisplayItem>()
                rows.addAll(cityItems.filterIsInstance<TimelineDisplayItem.FlexibleActivity>())
                cityItems.firstOrNull { it is TimelineDisplayItem.StartingPoint }?.let { rows.add(it) }
                timedRows.forEach { row ->
                    rows.add(row.withOrder(currentOrder))
                    currentOrder += 1
                }
                rows.addAll(cityItems.filterIsInstance<TimelineDisplayItem.EmptyState>())
                result.addAll(interleaveRouteSeparators(rows, city, flatRoutes))
            }

            if (totalCities > 1 && cityIndex < totalCities - 1) {
                result.add(TimelineDisplayItem.SectionFooter(city = city))
            }
        }

        return result
    }

    private fun interleaveRouteSeparators(
        rows: List<TimelineDisplayItem>,
        city: City?,
        flatRoutes: Map<String, List<StepRouteInfo>>
    ): List<TimelineDisplayItem> {
        val chain = FlatRouteChain.collect(rows).firstOrNull() ?: return rows
        val legs = flatRoutes[chain.key] ?: return rows
        val legByDestination = legs.associateBy { it.toStepId }
        return rows.flatMap { row ->
            val leg = row.routeWaypoint()?.id?.let { legByDestination[it] }
            listOfNotNull(leg?.let { TimelineDisplayItem.RouteSeparator(it, city) }, row)
        }
    }

    private val TimelineDisplayItem.isFlatTimedRow: Boolean
        get() = this is TimelineDisplayItem.BookedActivity ||
                this is TimelineDisplayItem.ManualPoi ||
                this is TimelineDisplayItem.PlanStep

    private val TimelineDisplayItem.flatSortTime: Date?
        get() = when (this) {
            is TimelineDisplayItem.BookedActivity -> startDateTime.toDate()
            else -> startTime
        }

    private val TimelineDisplayItem.flatTypeRank: Int
        get() = when (this) {
            is TimelineDisplayItem.BookedActivity -> if (isReserved) 1 else 0
            is TimelineDisplayItem.PlanStep -> if (isActivity) 1 else 2
            else -> 2
        }

    private fun TimelineDisplayItem.withOrder(order: Int): TimelineDisplayItem = when (this) {
        is TimelineDisplayItem.BookedActivity -> copy(order = order)
        is TimelineDisplayItem.ManualPoi -> copy(order = order)
        is TimelineDisplayItem.PlanStep -> copy(order = order)
        else -> this
    }

    private companion object {
        const val FULL_DAY_IN_MILLIS = 24L * 60L * 60L * 1000L
    }
}
