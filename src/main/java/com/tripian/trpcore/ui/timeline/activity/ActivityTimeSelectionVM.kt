package com.tripian.trpcore.ui.timeline.activity

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tripian.one.api.tour.model.TourSchedule
import com.tripian.one.api.tour.model.TourScheduleSlot
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.domain.usecase.timeline.GetTourScheduleUseCase
import com.tripian.trpcore.util.LanguageConst
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Grouped time slot data class
 * Groups slots with same time and keeps minimum price
 */
data class GroupedTimeSlot(
    val time: String,
    val minPrice: Double?,
    val fullRefund: Boolean?
)

/**
 * Shape of the selected day's schedule, derived from how the backend returns
 * slots:
 *  - [TIMED]: every slot has a concrete `time`; render the grid as usual.
 *  - [FLEXIBLE_ONLY]: every slot has `time == null` (any-time ticket); hide
 *    the grid and show the flexible info card. Continue is enabled with no
 *    chip selection — the segment is created with 00:00/23:59 + duration -1.
 *  - [MIXED]: some slots are timed, some are flexible; render the grid PLUS
 *    an extra "Any time" chip that selects the flexible slot.
 */
enum class TimeSelectionMode { TIMED, FLEXIBLE_ONLY, MIXED }

/**
 * Resolved per-day schedule the bottom sheet renders from. Holds the timed
 * slots already grouped (existing behavior) plus the flexible slot price (if
 * any) so the sheet can show "Any time" or fall back to the info card.
 */
data class ResolvedSchedule(
    val mode: TimeSelectionMode,
    val timedSlots: List<GroupedTimeSlot>,
    val flexiblePrice: Double?
) {
    val isEmpty: Boolean get() = timedSlots.isEmpty() && mode == TimeSelectionMode.TIMED
}

/**
 * ActivityTimeSelectionVM
 * ViewModel for ActivityTimeSelectionBottomSheet
 * Handles schedule loading for both tours and favorites
 */
class ActivityTimeSelectionVM @Inject constructor(
    private val getTourScheduleUseCase: GetTourScheduleUseCase
) : BaseViewModel() {

    private val _scheduleSlots = MutableLiveData<List<GroupedTimeSlot>?>()
    val scheduleSlots: LiveData<List<GroupedTimeSlot>?> = _scheduleSlots

    /**
     * Full per-day schedule including [TimeSelectionMode] + flexible slot
     * price. The bottom sheet observes this to decide between the time grid,
     * the flexible info card, or the mixed "Any time" augmentation.
     */
    private val _resolvedSchedule = MutableLiveData<ResolvedSchedule?>()
    val resolvedSchedule: LiveData<ResolvedSchedule?> = _resolvedSchedule

    /**
     * Subset of trip days (in "yyyy-MM-dd" form) that actually have schedule
     * slots. Days outside this set are rendered disabled in the day filter so
     * users can't pick a day with no availability. Null while loading — the
     * filter shows every day as selectable until the response is in.
     */
    private val _availableDateStrings = MutableLiveData<Set<String>?>(null)
    val availableDateStrings: LiveData<Set<String>?> = _availableDateStrings

    /**
     * Cached range response. Populated once by [loadSchedule] for the trip's full
     * date range; subsequent day switches go through [selectDate] which filters
     * this cache client-side instead of hitting the API again.
     */
    private var cachedSchedule: TourSchedule? = null

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Theme 9: show-more collapsing for long slot lists. When a schedule has at
     * least [collapsedSlotThreshold] slots, only the first [collapsedSlotCount]
     * are shown with a final "Show more" cell. Tapping it sets
     * [isTimeSlotsExpanded] = true and the full list paints.
     */
    val collapsedSlotThreshold: Int = 8
    val collapsedSlotCount: Int = 7
    var isTimeSlotsExpanded: Boolean = false
        private set

    /** Reset on every fresh schedule load. */
    fun resetExpansionState() {
        isTimeSlotsExpanded = false
    }

    fun expandTimeSlots() {
        isTimeSlotsExpanded = true
    }

    /**
     * Returns the slot list that should currently be rendered as chips.
     * In the collapsed state, only the first [collapsedSlotCount] slots are
     * returned; otherwise the full list is returned.
     */
    fun getDisplayedSlots(): List<GroupedTimeSlot> {
        val all = _scheduleSlots.value.orEmpty()
        return if (all.size >= collapsedSlotThreshold && !isTimeSlotsExpanded) {
            all.take(collapsedSlotCount)
        } else {
            all
        }
    }

    /** True when the "Show more times" cell should be appended after the chips. */
    val shouldShowMoreCell: Boolean
        get() {
            val all = _scheduleSlots.value.orEmpty()
            return all.size >= collapsedSlotThreshold && !isTimeSlotsExpanded
        }

    /**
     * Load schedule for an activity/tour across the trip's full date range and
     * display slots for [selectedDate]. The full range is cached in
     * [cachedSchedule]; subsequent day switches must go through [selectDate]
     * (no extra network call).
     *
     * @param activityId The activity or tour ID
     * @param availableDays Trip date range (first = from, last = to)
     * @param selectedDate Day whose slots should be shown after load
     * @param cityId The city ID (only for favorites mode, null for tour mode)
     */
    fun loadSchedule(
        activityId: String,
        availableDays: List<Date>,
        selectedDate: Date,
        cityId: Int? = null
    ) {
        if (availableDays.isEmpty()) return

        // Bottom-sheet Lottie loader, surfaced over the time-selection sheet via
        // BaseBottomDialogFragment's lottie observer.
        showBottomSheetLoader(LanguageConst.LOADING_TEXT_LOADING_TIME_SLOTS, "Loading available times")
        // Fresh load → collapse again so the user sees the trimmed first 7 chips.
        resetExpansionState()
        cachedSchedule = null
        _availableDateStrings.value = null

        val formattedId = formatActivityIdForSchedule(activityId, cityId)
        val fromString = dateFormatter.format(availableDays.first())
        val toString = dateFormatter.format(availableDays.last())
        // `to == from` is fine — backend still returns the per-day bucket shape.
        val toParam = if (toString == fromString) null else toString

        getTourScheduleUseCase.on(
            params = GetTourScheduleUseCase.Params(
                productId = formattedId,
                date = fromString,
                to = toParam,
                currency = TRPCore.core.appConfig.appCurrency
            ),
            success = { response ->
                hideLottieLoading()
                cachedSchedule = response.data
                _availableDateStrings.value = computeAvailableDateStrings(response.data)
                publishSlotsFor(selectedDate)
            },
            error = {
                hideLottieLoading()
                cachedSchedule = null
                _availableDateStrings.value = emptySet()
                _scheduleSlots.value = emptyList()
                _resolvedSchedule.value = ResolvedSchedule(
                    mode = TimeSelectionMode.TIMED,
                    timedSlots = emptyList(),
                    flexiblePrice = null
                )
            }
        )
    }

    /**
     * Switch the displayed day without hitting the API — re-filters [cachedSchedule].
     * If the cache is empty (e.g. previous load failed), emits an empty slot list.
     */
    fun selectDate(date: Date) {
        resetExpansionState()
        publishSlotsFor(date)
    }

    private fun publishSlotsFor(date: Date) {
        val schedule = cachedSchedule
        if (schedule == null) {
            _scheduleSlots.value = emptyList()
            _resolvedSchedule.value = ResolvedSchedule(
                mode = TimeSelectionMode.TIMED,
                timedSlots = emptyList(),
                flexiblePrice = null
            )
            return
        }
        val dateString = dateFormatter.format(date)
        val rawSlots = extractSlotsForDate(schedule, dateString)
        val resolved = resolveSchedule(rawSlots)
        _scheduleSlots.value = resolved.timedSlots
        _resolvedSchedule.value = resolved
    }

    /**
     * Bucket raw slots into the three [TimeSelectionMode] shapes. Timed slots
     * are grouped by time (lowest price wins, matching legacy behavior); a
     * single flexible price is picked as the minimum across `time == null`
     * slots so the "Any time" chip / segment can show the cheapest available.
     */
    private fun resolveSchedule(rawSlots: List<TourScheduleSlot>): ResolvedSchedule {
        if (rawSlots.isEmpty()) {
            return ResolvedSchedule(
                mode = TimeSelectionMode.TIMED,
                timedSlots = emptyList(),
                flexiblePrice = null
            )
        }

        val (flexible, timed) = rawSlots.partition { it.time.isNullOrEmpty() }
        val timedGrouped = groupSlotsByTime(timed)
        val flexiblePrice = flexible.mapNotNull { it.price }.minOrNull()

        val mode = when {
            timedGrouped.isEmpty() && flexible.isNotEmpty() -> TimeSelectionMode.FLEXIBLE_ONLY
            timedGrouped.isNotEmpty() && flexible.isNotEmpty() -> TimeSelectionMode.MIXED
            else -> TimeSelectionMode.TIMED
        }

        return ResolvedSchedule(
            mode = mode,
            timedSlots = timedGrouped,
            flexiblePrice = flexiblePrice
        )
    }

    /**
     * Walks the same two schedule shapes [extractSlotsForDate] handles and
     * returns every date string that has at least one slot. Used to disable
     * empty days in the day filter.
     */
    private fun computeAvailableDateStrings(schedule: TourSchedule?): Set<String> {
        if (schedule == null) return emptySet()
        val result = mutableSetOf<String>()
        schedule.dates?.forEach { entry ->
            val date = entry.date
            if (!date.isNullOrEmpty() && !entry.slots.isNullOrEmpty()) {
                result.add(date)
            }
        }
        val topLevelDate = schedule.date
        if (!topLevelDate.isNullOrEmpty() && !schedule.slots.isNullOrEmpty()) {
            result.add(topLevelDate)
        }
        return result
    }

    /**
     * Pulls slots for [dateString] out of a [TourSchedule], handling both
     * shapes the backend may return:
     *  - range: `dates` carries per-day buckets
     *  - single-day fallback: top-level `slots` belong to top-level `date`
     */
    private fun extractSlotsForDate(
        schedule: TourSchedule,
        dateString: String
    ): List<TourScheduleSlot> {
        schedule.dates
            ?.firstOrNull { it.date == dateString }
            ?.slots
            ?.let { return it }
        if (schedule.date == dateString) return schedule.slots.orEmpty()
        return emptyList()
    }

    /**
     * Group slots by time and keep minimum price for each time
     * If API returns: time:10:00,price:27.0 / time:10:00,price:29.0 / time:11:00,price:27.0
     * Result will be: time:10:00,minPrice:27.0 / time:11:00,minPrice:27.0
     */
    private fun groupSlotsByTime(slots: List<TourScheduleSlot>?): List<GroupedTimeSlot> {
        if (slots.isNullOrEmpty()) return emptyList()

        return slots
            .filter { !it.time.isNullOrEmpty() }
            .groupBy { it.time!! }
            .map { (time, slotsForTime) ->
                GroupedTimeSlot(
                    time = time,
                    minPrice = slotsForTime.mapNotNull { it.price }.minOrNull(),
                    fullRefund = slotsForTime.any { it.fullRefund == true }
                )
            }
            .sortedBy { it.time }
    }

    /**
     * Clear schedule data
     */
    fun clearSchedule() {
        cachedSchedule = null
        _scheduleSlots.value = null
        _resolvedSchedule.value = null
        _availableDateStrings.value = null
    }

    /**
     * Format activityId for schedule API
     * For favorites: Format is C_{rawId}_15_{cityId}
     * For tours: activityId is used as-is
     *
     * @param activityId The original activity ID
     * @param cityId The city ID (null for tour mode)
     * @return Formatted activity ID for schedule API
     */
    private fun formatActivityIdForSchedule(activityId: String, cityId: Int?): String {
        // Tour mode - use activityId as is
        if (cityId == null) return activityId

        // Extract raw ID if starts with C_
        val rawId = if (activityId.startsWith("C_")) {
            // Format: C_15423_15 → extract 15423
            activityId.removePrefix("C_").split("_").firstOrNull() ?: activityId
        } else {
            activityId
        }

        return "C_${rawId}_15_$cityId"
    }
}
