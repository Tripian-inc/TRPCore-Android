package com.tripian.trpcore.domain.manager

import com.tripian.one.api.timeline.model.SegmentType
import com.tripian.one.api.timeline.model.Timeline
import com.tripian.one.api.timeline.model.TimelineSegment
import com.tripian.one.api.timeline.model.TimelineStep
import com.tripian.one.api.tour.model.TourScheduleAvailabilityItem
import com.tripian.trpcore.domain.model.timeline.toDate
import com.tripian.trpcore.repository.TourRepository
import com.tripian.trpcore.util.extensions.isFlexibleActivity
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Theme 17 — post-load availability sweep.
 *
 * After a timeline is fetched, this manager iterates every non-past day, batches
 * the activity IDs for that day, calls `POST /tour-api/schedule-bulk`,
 * then marks expired any reserved activity / activity step whose booked time no
 * longer appears in the response.
 *
 * Properties:
 *  - **One-shot per timeline**: [runInitialAvailabilityCheck] is a no-op if a sweep
 *    has already completed; call [reset] before reusing on a fresh timeline.
 *  - **Cancellation token**: [currentGeneration] increments on each `reset()` /
 *    `cancel()`. In-flight emissions check the generation and abort silently if
 *    a newer sweep has been started.
 *  - **Selected-day-first**: the user's currently-viewed day is requested before
 *    other days (when [selectedDate] is non-null and not in the past).
 *  - **Past-days skipped**: dates strictly before today are not queried.
 *  - **Sequential per-day**: one batched request per day, processed serially via
 *    `concatMap` (avoids slamming the backend).
 */
@Singleton
class AvailabilityCheckManager @Inject constructor(
    private val tourRepository: TourRepository
) {

    interface ItemUpdateListener {
        /**
         * Invoked on the main thread when a single (segmentIndex, stepId) target
         * resolves to an expiration verdict. [stepId] is non-null only for
         * itinerary steps inside a Recommendations plan.
         */
        fun onItemUpdated(segmentIndex: Int, stepId: Int?, isExpired: Boolean)
    }

    private val disposables = CompositeDisposable()
    private var hasRunInitialCheck: Boolean = false

    /**
     * Generation token. Each call to [reset] or [cancel] increments this; any
     * in-flight subscription captures the value at start time and bails if a
     * newer sweep has begun.
     */
    private var currentGeneration: Int = 0

    fun runInitialAvailabilityCheck(
        timeline: Timeline,
        selectedDate: Date?,
        currency: String?,
        lang: String?,
        providerId: Int = DEFAULT_PROVIDER_ID,
        listener: ItemUpdateListener,
        onCompleted: () -> Unit
    ) {
        if (hasRunInitialCheck) return
        hasRunInitialCheck = true
        currentGeneration += 1
        val gen = currentGeneration

        val days = collectNonPastDaysWithSelectedFirst(timeline, selectedDate)
        if (days.isEmpty()) {
            onCompleted()
            return
        }

        val sub = Observable.fromIterable(days)
            .concatMap { dayInfo ->
                if (gen != currentGeneration) {
                    return@concatMap Observable.empty<Pair<DayInfo, List<TourScheduleAvailabilityItem>>>()
                }
                val targets = collectTargetsForDay(timeline, dayInfo, providerId)
                if (targets.isEmpty()) return@concatMap Observable.empty()

                tourRepository.getTourScheduleAvailability(
                    items = targets.map { it.activityId },
                    date = dayInfo.dateString,
                    currency = currency,
                    lang = lang
                ).toObservable()
                    .map { response ->
                        val items = response.data?.schedules.orEmpty()
                        dayInfo to items
                    }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally {
                if (gen == currentGeneration) onCompleted()
            }
            .subscribe(
                { (dayInfo, response) ->
                    if (gen != currentGeneration) return@subscribe
                    val targets = collectTargetsForDay(timeline, dayInfo, providerId)
                    processResults(targets, response, listener)
                },
                { /* swallow — sweep is best-effort, surface nothing on the UI */ }
            )

        disposables.add(sub)
    }

    /** Cancel any in-flight sweep without resetting the one-shot guard. */
    fun cancel() {
        currentGeneration += 1
        disposables.clear()
    }

    /**
     * Reset for a fresh timeline. Clears in-flight subscriptions AND clears the
     * one-shot guard so the next [runInitialAvailabilityCheck] call actually
     * executes.
     */
    fun reset() {
        cancel()
        hasRunInitialCheck = false
    }

    // ------------------------------------------------------------------
    // Day / target collection
    // ------------------------------------------------------------------

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
                    val activityId = normalizeActivityId(rawId, providerId)
                    val expectedTime = if (segment.isFlexibleActivity) null
                    else segment.startDate?.substringAfter(' ', "")?.takeIf { it.isNotEmpty() }
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
                        targets += Target(
                            segmentIndex = index,
                            stepId = step.id,
                            activityId = normalizeActivityId(rawId, providerId),
                            expectedTime = step.startDateTimes?.substringAfter(' ', "")
                                ?.takeIf { it.isNotEmpty() }
                        )
                    }
                }
                else -> { /* booked_activity and manual_poi are not checked */ }
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
        listener: ItemUpdateListener
    ) {
        val byId = response.associateBy { it.id }
        targets.forEach { target ->
            val item = byId[target.activityId]
            val expired = when {
                // Missing from response → expired
                item == null -> true
                // Server returned null/empty schedule → sold out / not available
                item.schedule == null -> true
                else -> {
                    val slots = item.schedule!!.allSlots
                    if (target.expectedTime == null) {
                        // Flexible target — expects at least one slot (any time)
                        slots.isEmpty()
                    } else {
                        // Timed target — accept a flex slot OR a slot at the expected time
                        val hasFlex = slots.any { it.time == null }
                        val hasExact = slots.any { it.time == target.expectedTime }
                        !(hasFlex || hasExact)
                    }
                }
            }
            listener.onItemUpdated(target.segmentIndex, target.stepId, expired)
        }
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

    private fun normalizeActivityId(raw: String, providerId: Int): String =
        if (raw.startsWith("C_")) raw else "C_${raw}_${providerId}"

    private data class DayInfo(val dateString: String, val date: Date)
    private data class Target(
        val segmentIndex: Int,
        val stepId: Int?,
        val activityId: String,
        val expectedTime: String?
    )

    companion object {
        const val DEFAULT_PROVIDER_ID: Int = 15
    }
}
