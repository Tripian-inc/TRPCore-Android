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
 * Builds the day's [TimelineDisplayItem] list from a [Timeline]:
 *
 * 1. Segment iteration — picks segments whose `startDate` falls on the requested day
 *    and turns each into the matching display-item variant (booked, reserved,
 *    flexible, recommendations, manual POI).
 * 2. Conflict detection — flags every overlapping segment/step and stamps
 *    `hasConflict` / `timeOverlap…` on the items.
 * 3. City grouping — emits section headers/footers, pins flexible activities to the
 *    top of their city, applies per-city sequential numbering (1, 2, 3…) and
 *    respects the user's collapsed sections.
 *
 * The class is stateless apart from [tripRepository] (cache lookup) — the per-call
 * inputs cover everything else, so the same instance can serve every day-switch.
 */
class TimelineDisplayItemBuilder @Inject constructor(
    private val tripRepository: TripRepository
) {

    /**
     * @param timeline current timeline payload
     * @param date the day being rendered (already resolved by the VM)
     * @param cities cities snapshot used as a fallback when the cache miss path
     *               needs to resolve `segment.cityId`
     * @param collapsedSectionCityIds city ids the user has collapsed in the section
     *                                accordion — content is dropped but the header
     *                                stays so the user can toggle back
     * @param emptyStateMessage localized "no plans for this day" text — passed in
     *                          so the builder doesn't depend on a language provider
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
            // Hide segments that are queued for background deletion (city
            // removed from itinerary, day outside trip range). The actual
            // server-side delete happens after initial timeline shows.
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
                        android.util.Log.d(
                            "RECO_DEBUG",
                            "builder segment='${segment.title}' type=${segment.segmentType} " +
                                "planId=${plan.id} generatedStatus=${plan.generatedStatus} " +
                                "plan.steps=${plan.steps?.size ?: "null"} stepsUsed=${steps.size}"
                        )
                        // "TimelineDate" is a control segment, never render.
                        if (segment.title == "TimelineDate") return@forEachIndexed
                        // Smart recommendation that came back with no POIs — skip.
                        if (plan.generatedStatus == -2 && steps.isEmpty()) return@forEachIndexed

                        val cityId = segment.cityId
                        val recommendationIndex = items.count { item ->
                            item is TimelineDisplayItem.Recommendations && item.city?.id == cityId
                        } + 1

                        // Snapshot expired flags into a Set so the data class
                        // equality (and therefore the outer DiffUtil) reacts when
                        // the availability sweep mutates step.isAvailabilityExpired
                        // in-place. Without this, RecommendationsVH never re-binds
                        // after the sweep and the expired pill never reaches the UI.
                        val expiredStepIds = steps
                            .filter { it.isAvailabilityExpired }
                            .map { it.id }
                            .toSet()
                        // Capture step id + times as a single string so the data
                        // class equality reacts to local-mutation paths
                        // (delete-step, update-step-time) — the underlying steps
                        // list is reused across rebuilds and mutated in place, so
                        // without this fingerprint DiffUtil would skip the rebind.
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
     *
     * Rule recap:
     * - 2-item conflicts: newer plan shows "Time Overlap", booked-activity has
     *   special-case (older shows overlap when booked is newer).
     * - 3+ conflicts: oldest plan keeps only the visual flag, others get overlap.
     * - Final phase intentionally simplifies: every conflicting item shows the
     *   overlap text — except non-reserved BookedActivity, which keeps only the
     *   visual style.
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

        groupedByCity.entries.forEachIndexed { cityIndex, (_, rawCityItems) ->
            // Pin flexible-time items to the top of their city group (Theme 4).
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

            // Theme 12: collapsed sections keep their header (and trailing footer)
            // so the toggle remains usable, but content is dropped.
            if (isCollapsed) {
                if (totalCities > 1 && cityIndex < totalCities - 1) {
                    result.add(TimelineDisplayItem.SectionFooter(city = city))
                }
                return@forEachIndexed
            }

            // Sequential per-city numbering. Flexible activities skip the counter
            // — they render "−" instead.
            var currentOrder = 1
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
