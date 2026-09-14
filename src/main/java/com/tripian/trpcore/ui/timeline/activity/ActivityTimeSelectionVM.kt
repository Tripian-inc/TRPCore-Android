package com.tripian.trpcore.ui.timeline.activity

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tripian.one.api.tour.model.TourSchedule
import com.tripian.one.api.tour.model.TourScheduleSlot
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.domain.usecase.timeline.GetTourScheduleUseCase
import com.tripian.trpcore.util.ActivityIdFormat
import com.tripian.trpcore.util.LanguageConst
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
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
 * Shape of the selected day's schedule:
 *  - [TIMED]: timed slots exist (or none at all); render the time grid, or the
 *    "not available" warning when empty.
 *  - [FLEXIBLE_ONLY]: only flexible slots (`time == null`); show the flexible
 *    info card and create the segment with 00:00/23:59 + duration -1.
 */
enum class TimeSelectionMode { TIMED, FLEXIBLE_ONLY }

/**
 * Resolved per-day schedule the bottom sheet renders from: grouped timed slots
 * plus the flexible slot price (if any) used by the flexible info card.
 */
data class ResolvedSchedule(
    val mode: TimeSelectionMode,
    val timedSlots: List<GroupedTimeSlot>,
    val flexiblePrice: Double?
) {
    val isEmpty: Boolean get() = timedSlots.isEmpty() && mode == TimeSelectionMode.TIMED
}

/**
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
     * price. The bottom sheet observes this to decide between the time grid and
     * the flexible info card.
     */
    private val _resolvedSchedule = MutableLiveData<ResolvedSchedule?>()
    val resolvedSchedule: LiveData<ResolvedSchedule?> = _resolvedSchedule

    /**
     * Subset of trip days (in "yyyy-MM-dd" form) that actually have schedule
     * slots; days outside this set are rendered disabled in the day filter.
     * Null while loading — every day shows as selectable until the response is in.
     */
    private val _availableDateStrings = MutableLiveData<Set<String>?>(null)
    val availableDateStrings: LiveData<Set<String>?> = _availableDateStrings

    /**
     * True when the schedule response is in but no day in the trip range has
     * any slot. The sheet uses this to swap the day filter / slot grid for a
     * trip-wide "not available" warning card.
     */
    private val _isUnavailableForTrip = MutableLiveData(false)
    val isUnavailableForTrip: LiveData<Boolean> = _isUnavailableForTrip

    /**
     * Cached range response. Populated once by [loadSchedule] for the trip's full
     * date range; subsequent day switches go through [selectDate] which filters
     * this cache client-side instead of hitting the API again.
     */
    private var cachedSchedule: TourSchedule? = null

    /**
     * Days ("yyyy-MM-dd") that already hold this activity. They stay unselectable
     * regardless of what the schedule offers, so the same activity can't be added
     * twice to one day. Empty in edit flows.
     */
    private var blockedDateStrings: Set<String> = emptySet()

    /** Trip days in "yyyy-MM-dd" form, captured on [loadSchedule]. */
    private var tripDayStrings: List<String> = emptyList()

    /**
     * Day whose slots are currently on screen. The sheet can move off the initially
     * requested day while the schedule request is still in flight (a blocked day is
     * skipped immediately), so the response renders this day rather than the one
     * [loadSchedule] was called with.
     */
    private var displayedDate: Date? = null

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Show-more collapsing for long slot lists: when a schedule has at least
     * [collapsedSlotThreshold] slots, only the first [collapsedSlotCount] are
     * shown with a final "Show more" cell that expands the full list.
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
     * Marks the days that already hold this activity as unselectable. Must be called
     * before [loadSchedule]. Edit flows pass an empty map so the activity's own day
     * stays selectable and its update doesn't exclude itself.
     *
     * @param plannedIdsByDay "yyyy-MM-dd" → ids planned that day, in any id format.
     * @param activityId the activity the sheet is opened for.
     */
    fun applyPlannedActivities(plannedIdsByDay: Map<String, List<String>>, activityId: String?) {
        val targetId = ActivityIdFormat.base(activityId)
        blockedDateStrings = if (targetId == null) {
            emptySet()
        } else {
            plannedIdsByDay
                .filterValues { ids -> ids.any { ActivityIdFormat.base(it) == targetId } }
                .keys
                .toSet()
        }
    }

    /**
     * Language key for the "no day can take this" card: distinguishes an activity
     * that is sold out for the trip from one already planned on every single day.
     */
    fun unavailableBannerKey(): String {
        val addedEveryDay = blockedDateStrings.isNotEmpty() &&
            tripDayStrings.isNotEmpty() &&
            tripDayStrings.all { it in blockedDateStrings }

        return if (addedEveryDay) {
            LanguageConst.ADD_PLAN_ACTIVITY_ALREADY_ADDED_EVERY_DAY
        } else {
            LanguageConst.ADD_PLAN_ACTIVITY_NOT_AVAILABLE_TRIP_DAYS
        }
    }

    /**
     * Load schedule for an activity/tour across the trip's full date range and
     * display slots for [selectedDate]. The full range is cached in
     * [cachedSchedule]; subsequent day switches must go through [selectDate]
     * (no extra network call).
     *
     * @param availableDays Trip date range (first = from, last = to)
     * @param cityId The city ID (only for favorites mode, null for tour mode)
     */
    fun loadSchedule(
        activityId: String,
        availableDays: List<Date>,
        selectedDate: Date,
        cityId: Int? = null
    ) {
        if (availableDays.isEmpty()) return

        showInSheetLoader(LanguageConst.LOADING_TEXT_LOADING_TIME_SLOTS, "Loading available times")
        resetExpansionState()
        cachedSchedule = null
        displayedDate = selectedDate
        tripDayStrings = availableDays.map { dateFormatter.format(it) }
        val selectableDays = tripDayStrings.filterNot { it in blockedDateStrings }.toSet()
        _availableDateStrings.value = if (blockedDateStrings.isEmpty()) null else selectableDays
        _isUnavailableForTrip.value = blockedDateStrings.isNotEmpty() && selectableDays.isEmpty()

        val formattedId = formatActivityIdForSchedule(activityId, cityId)
        val fromString = dateFormatter.format(availableDays.first())
        val toString = dateFormatter.format(availableDays.last())
        val toParam = if (toString == fromString) null else toString

        viewModelScope.launch {
            runCatching {
                getTourScheduleUseCase(
                    GetTourScheduleUseCase.Params(
                        productId = formattedId,
                        date = fromString,
                        to = toParam,
                        currency = TRPCore.core.appConfig.appCurrency
                    )
                )
            }.onSuccess { response ->
                hideLottieLoading()
                cachedSchedule = response.data
                val available = computeAvailableDateStrings(response.data) - blockedDateStrings
                _isUnavailableForTrip.value = available.isEmpty()
                publishSlotsFor(displayedDate ?: selectedDate)
                _availableDateStrings.value = available
            }.onFailure {
                hideLottieLoading()
                cachedSchedule = null
                _availableDateStrings.value = emptySet()
                _isUnavailableForTrip.value = true
                _scheduleSlots.value = emptyList()
                _resolvedSchedule.value = ResolvedSchedule(
                    mode = TimeSelectionMode.TIMED,
                    timedSlots = emptyList(),
                    flexiblePrice = null
                )
            }
        }
    }

    /**
     * Switch the displayed day without hitting the API — re-filters [cachedSchedule].
     * If the cache is empty (e.g. previous load failed), emits an empty slot list.
     */
    fun selectDate(date: Date) {
        resetExpansionState()
        displayedDate = date
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
     * Bucket raw slots into a [TimeSelectionMode]. Timed slots are grouped by
     * time (lowest price wins). Any timed slot forces [TIMED]; only a schedule
     * with exclusively flexible slots becomes [FLEXIBLE_ONLY]. An empty
     * schedule stays [TIMED] (empty grid → "not available" warning).
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
            timedGrouped.isNotEmpty() -> TimeSelectionMode.TIMED
            flexible.isNotEmpty() -> TimeSelectionMode.FLEXIBLE_ONLY
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
     * Group slots by time, keeping the minimum price for each time.
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
        displayedDate = null
        _scheduleSlots.value = null
        _resolvedSchedule.value = null
        _availableDateStrings.value = null
        _isUnavailableForTrip.value = false
    }

    /**
     * Format activityId for the schedule API.
     * For favorites the format is C_{rawId}_15_{cityId}; for tours the
     * activityId is used as-is.
     *
     * @param cityId The city ID (null for tour mode)
     */
    private fun formatActivityIdForSchedule(activityId: String, cityId: Int?): String {
        if (cityId == null) return activityId
        return ActivityIdFormat.make(activityId, cityId = cityId)
    }
}
