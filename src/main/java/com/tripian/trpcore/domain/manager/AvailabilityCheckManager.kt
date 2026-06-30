package com.tripian.trpcore.domain.manager

import com.tripian.one.api.timeline.model.SegmentType
import com.tripian.one.api.timeline.model.Timeline
import com.tripian.one.api.tour.model.TourScheduleAvailabilityItem
import com.tripian.trpcore.repository.TourRepository
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
 * Theme 17 — post-load availability sweep.
 *
 * After a timeline is fetched, this manager iterates every non-past day,
 * batches the activity IDs for that day, calls
 * `POST /tour-api/schedule-bulk`, then marks expired any reserved activity
 * / activity step whose booked time no longer appears in the response.
 *
 * Properties:
 *  - **One-shot per timeline**: [runInitialAvailabilityCheck] is a no-op if
 *    a sweep has already completed; call [reset] before reusing.
 *  - **Cancellation**: [reset] / [cancel] cancel the running coroutine job;
 *    a fresh call starts a new one.
 *  - **Selected-day-first**: the user's currently-viewed day is requested
 *    before other days.
 *  - **Past-days skipped**: dates strictly before today are not queried.
 *  - **Sequential per-day**: one batched request per day, processed serially
 *    (avoids slamming the backend).
 */
@Singleton
class AvailabilityCheckManager @Inject constructor(
    private val tourRepository: TourRepository
) {

    interface ItemUpdateListener {
        /**
         * Invoked on the main thread when a single (segmentIndex, stepId)
         * target resolves to an expiration verdict. [stepId] is non-null
         * only for itinerary steps inside a Recommendations plan.
         */
        fun onItemUpdated(segmentIndex: Int, stepId: Int?, isExpired: Boolean)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentJob: Job? = null
    private var hasRunInitialCheck: Boolean = false

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
                        // Best-effort sweep — skip the day, continue to the next.
                        continue
                    }
                    val items = response.data?.schedules.orEmpty()
                    withContext(Dispatchers.Main) {
                        processResults(targets, items, listener)
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
                item == null -> true
                item.schedule == null -> true
                else -> {
                    val slots = item.schedule!!.allSlots
                    if (target.expectedTime == null) {
                        slots.isEmpty()
                    } else {
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

    /**
     * Builds the schedule-bulk product id as `C_{baseId}_{providerId}_{cityId}`,
     * matching the rest of the SDK. The `_{cityId}` suffix is required for the
     * bulk endpoint to resolve availability; it is dropped only when the city is
     * unknown.
     */
    private fun normalizeActivityId(raw: String, providerId: Int, cityId: Int): String {
        val baseId = if (raw.startsWith("C_")) {
            raw.removePrefix("C_").split("_").firstOrNull() ?: raw
        } else {
            raw
        }
        return if (cityId > 0) "C_${baseId}_${providerId}_$cityId" else "C_${baseId}_$providerId"
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
        const val DEFAULT_PROVIDER_ID: Int = 15
    }
}
