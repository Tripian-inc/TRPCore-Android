package com.tripian.trpcore.domain.manager

import com.tripian.one.api.timeline.model.SegmentType
import com.tripian.one.api.timeline.model.Timeline
import com.tripian.one.api.tour.model.TourScheduleAvailabilityItem
import com.tripian.trpcore.base.TRPCore
import com.tripian.one.api.tour.model.TourScheduleSlot
import com.tripian.trpcore.repository.TourRepository
import com.tripian.trpcore.util.ActivityIdFormat
import com.tripian.trpcore.util.extensions.isFlexibleActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Post-load availability sweep: after a timeline is fetched, iterates every
 * non-past day (selected day first), batches that day's activity IDs into one
 * `POST /tour-api/schedule-bulk` request per day (processed serially), then marks
 * expired any reserved activity / activity step whose booked time no longer
 * appears in the response. One-shot per timeline — call [reset] before reusing.
 */
@Singleton
class AvailabilityCheckManager @Inject constructor(
    private val tourRepository: TourRepository
) {

    interface ItemUpdateListener {
        /**
         * Invoked on the main thread when a single (segmentIndex, stepId)
         * target resolves to an expiration verdict. [stepId] is non-null
         * only for itinerary steps inside a Recommendations plan. [price] is
         * the booked slot's price in the requested currency, null when the
         * schedule no longer covers the booked time or carries no price.
         */
        fun onItemUpdated(segmentIndex: Int, stepId: Int?, isExpired: Boolean, price: Double?)

        /** Invoked on the main thread after each day's batch has been applied. */
        fun onDayCompleted()
    }

    /** Price the schedule reported for a booked slot, in the currency it was requested in. */
    data class RefreshedPrice(val price: Double, val currency: String?)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentJob: Job? = null
    private var hasRunInitialCheck: Boolean = false

    /**
     * Refreshed prices keyed by `{activityId}|{yyyy-MM-dd}|{HH:mm or flexible}`.
     * Survives a timeline re-fetch so a re-processed timeline does not fall back to
     * the price the server has stored on the segment.
     */
    private val refreshedPrices = mutableMapOf<String, RefreshedPrice>()

    fun runInitialAvailabilityCheck(
        timeline: Timeline,
        selectedDate: Date?,
        currency: String?,
        lang: String?,
        providerId: Int = TRPCore.provider.id,
        listener: ItemUpdateListener,
        onCompleted: () -> Unit
    ) {
        if (hasRunInitialCheck) return
        hasRunInitialCheck = true

        val days = collectNonPastDaysWithSelectedFirst(timeline, selectedDate)
        if (days.isEmpty()) {
            onCompleted()
            return
        }

        currentJob = scope.launch {
            try {
                for (dayInfo in days) {
                    val targets = collectTargetsForDay(timeline, dayInfo, providerId)
                    if (targets.isEmpty()) continue
                    val response = try {
                        tourRepository.getTourScheduleAvailabilityAsync(
                            items = targets.map { it.activityId },
                            date = dayInfo.dateString,
                            currency = currency,
                            lang = lang
                        )
                    } catch (_: Throwable) {
                        continue
                    }
                    val items = response.data?.schedules.orEmpty()
                    withContext(Dispatchers.Main) {
                        processResults(targets, items, dayInfo.dateString, currency, listener)
                        listener.onDayCompleted()
                    }
                }
            } finally {
                withContext(Dispatchers.Main) { onCompleted() }
            }
        }
    }

    /** Cancel any in-flight sweep without resetting the one-shot guard. */
    fun cancel() {
        currentJob?.cancel()
        currentJob = null
    }

    /**
     * Reset for a fresh timeline. Cancels in-flight work and clears the
     * one-shot guard so the next [runInitialAvailabilityCheck] runs.
     */
    fun reset() {
        cancel()
        hasRunInitialCheck = false
    }

    // ------------------------------------------------------------------
    // Day / target collection
    // ------------------------------------------------------------------

    /** Every day the timeline holds a segment on, past ones included. */
    private fun collectDays(timeline: Timeline): List<DayInfo> {
        return timeline.tripProfile?.segments
            ?.mapNotNull { it.startDate?.substringBefore(" ") }
            ?.distinct()
            ?.mapNotNull { dateStr ->
                runCatching {
                    SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr)
                }.getOrNull()?.let { DayInfo(dateStr, startOfDay(it)) }
            }
            ?: emptyList()
    }

    private fun collectNonPastDaysWithSelectedFirst(
        timeline: Timeline,
        selectedDate: Date?
    ): List<DayInfo> {
        val today = startOfDay(Date())
        val segmentDays = timeline.tripProfile?.segments
            ?.mapNotNull { it.startDate?.substringBefore(" ") }
            ?.distinct()
            ?.mapNotNull { dateStr ->
                runCatching {
                    SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr)
                }.getOrNull()?.let { DayInfo(dateStr, startOfDay(it)) }
            }
            ?.filter { !it.date.before(today) }
            ?.sortedBy { it.date }
            ?: return emptyList()

        val selectedDay = selectedDate?.let { startOfDay(it) }
        if (selectedDay != null && !selectedDay.before(today)) {
            val (selected, rest) = segmentDays.partition { it.date == selectedDay }
            return selected + rest
        }
        return segmentDays
    }

    /** Collects check targets for a day; booked_activity and manual_poi segments are not checked. */
    private fun collectTargetsForDay(
        timeline: Timeline,
        dayInfo: DayInfo,
        providerId: Int
    ): List<Target> {
        val segments = timeline.tripProfile?.segments ?: return emptyList()
        val targets = mutableListOf<Target>()

        segments.forEachIndexed { index, segment ->
            if (segment.startDate?.startsWith(dayInfo.dateString) != true) return@forEachIndexed
            when (segment.segmentType) {
                SegmentType.RESERVED_ACTIVITY -> {
                    val rawId = segment.additionalData?.activityId ?: return@forEachIndexed
                    val activityId = normalizeActivityId(rawId, providerId, segment.cityId ?: 0)
                    val expectedTime = if (segment.isFlexibleActivity) null
                    else extractHourMinute(segment.startDate)
                    targets += Target(
                        segmentIndex = index,
                        stepId = null,
                        activityId = activityId,
                        expectedTime = expectedTime
                    )
                }
                SegmentType.ITINERARY, SegmentType.GENERATED -> {
                    val plan = timeline.plans?.getOrNull(index) ?: return@forEachIndexed
                    plan.steps?.forEach { step ->
                        if (step.stepType != "activity") return@forEach
                        val rawId = step.poi?.id ?: return@forEach
                        val cityId = step.poi?.cityId ?: plan.city?.id ?: segment.cityId ?: 0
                        targets += Target(
                            segmentIndex = index,
                            stepId = step.id,
                            activityId = normalizeActivityId(rawId, providerId, cityId),
                            expectedTime = extractHourMinute(step.startDateTimes)
                        )
                    }
                }
                else -> {}
            }
        }
        return targets
    }

    // ------------------------------------------------------------------
    // Rules
    // ------------------------------------------------------------------

    private fun processResults(
        targets: List<Target>,
        response: List<TourScheduleAvailabilityItem>,
        dateString: String,
        currency: String?,
        listener: ItemUpdateListener
    ) {
        val byId = response.associateBy { it.id }
        targets.forEach { target ->
            val item = byId[target.activityId]
            val slots = item?.schedule?.allSlots.orEmpty()
            val expired = when {
                item == null -> true
                item.schedule == null -> true
                else -> {
                    if (target.expectedTime == null) {
                        slots.isEmpty()
                    } else {
                        val hasFlex = slots.any { it.time == null }
                        val hasExact = slots.any { it.time == target.expectedTime }
                        !(hasFlex || hasExact)
                    }
                }
            }
            val price = if (expired) null else resolveSlotPrice(slots, target.expectedTime)
            if (price != null) {
                refreshedPrices[cacheKey(target.activityId, dateString, target.expectedTime)] =
                    RefreshedPrice(price, currency)
            }
            listener.onItemUpdated(target.segmentIndex, target.stepId, expired, price)
        }
    }

    /**
     * Re-applies prices resolved by an earlier sweep. Called right after a timeline
     * is processed so a re-fetch does not surface the segment's stored price while
     * the next sweep is still running.
     */
    fun applyCachedPrices(
        timeline: Timeline,
        providerId: Int = TRPCore.provider.id,
        apply: (segmentIndex: Int, stepId: Int?, price: Double, currency: String?) -> Unit
    ) {
        if (refreshedPrices.isEmpty()) return

        collectDays(timeline).forEach { dayInfo ->
            collectTargetsForDay(timeline, dayInfo, providerId).forEach { target ->
                val cached = refreshedPrices[
                    cacheKey(target.activityId, dayInfo.dateString, target.expectedTime)
                ] ?: return@forEach
                apply(target.segmentIndex, target.stepId, cached.price, cached.currency)
            }
        }
    }

    /** Drops every cached entry of [activityId]; its slot moved or the item is gone. */
    fun invalidatePricesFor(activityId: String?) {
        val baseId = ActivityIdFormat.base(activityId) ?: return
        refreshedPrices.keys
            .filter { key -> ActivityIdFormat.base(key.substringBefore('|')) == baseId }
            .forEach { refreshedPrices.remove(it) }
    }

    private fun cacheKey(activityId: String, day: String, time: String?): String {
        return "$activityId|$day|${time ?: FLEXIBLE_SLOT_KEY}"
    }

    /**
     * Price of the slot the activity is booked into. A flexible target takes the
     * any-time slot and falls back to the day's cheapest; a timed target takes its
     * own hour and falls back to the any-time slot only. A non-positive price is
     * reported as absent so the caller keeps whatever the timeline already holds.
     */
    private fun resolveSlotPrice(
        slots: List<TourScheduleSlot>,
        expectedTime: String?
    ): Double? {
        val flexible = slots.filter { it.time == null }
        val candidates = if (expectedTime == null) {
            flexible.ifEmpty { slots }
        } else {
            slots.filter { it.time == expectedTime }.ifEmpty { flexible }
        }
        return candidates.mapNotNull { it.price }.minOrNull()?.takeIf { it > 0.0 }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private fun startOfDay(date: Date): Date {
        val cal = Calendar.getInstance()
        cal.time = date
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    /**
     * The `_{cityId}` suffix is what lets the bulk endpoint resolve availability,
     * so it is kept whenever the city is known.
     */
    private fun normalizeActivityId(raw: String, providerId: Int, cityId: Int): String {
        return ActivityIdFormat.make(raw, providerId, cityId.takeIf { it > 0 })
    }

    /**
     * Extracts the "HH:mm" portion of a datetime string. The timeline payload
     * mixes "yyyy-MM-dd HH:mm" (segments) and "yyyy-MM-dd HH:mm:ss" (steps);
     * slot.time on the bulk response is always "HH:mm", so trim to length 5.
     */
    private fun extractHourMinute(dateTime: String?): String? {
        val timePart = dateTime?.substringAfter(' ', "")?.takeIf { it.isNotEmpty() } ?: return null
        return if (timePart.length >= 5) timePart.substring(0, 5) else timePart
    }

    private data class DayInfo(val dateString: String, val date: Date)
    private data class Target(
        val segmentIndex: Int,
        val stepId: Int?,
        val activityId: String,
        val expectedTime: String?
    )

    companion object {
        const val DEFAULT_PROVIDER_ID: Int = ActivityIdFormat.DEFAULT_PROVIDER_ID
        private const val FLEXIBLE_SLOT_KEY = "flexible"
    }
}
