package com.tripian.trpcore.ui.timeline.activity

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tripian.one.api.tour.model.TourSchedule
import com.tripian.one.api.tour.model.TourScheduleSlot
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.domain.usecase.timeline.GetTourScheduleUseCase
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
 * ActivityTimeSelectionVM
 * ViewModel for ActivityTimeSelectionBottomSheet
 * Handles schedule loading for both tours and favorites
 */
class ActivityTimeSelectionVM @Inject constructor(
    private val getTourScheduleUseCase: GetTourScheduleUseCase
) : BaseViewModel() {

    private val _scheduleSlots = MutableLiveData<List<GroupedTimeSlot>?>()
    val scheduleSlots: LiveData<List<GroupedTimeSlot>?> = _scheduleSlots

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    /**
     * Cached range response. Populated once by [loadSchedule] for the trip's full
     * date range; subsequent day switches go through [selectDate] which filters
     * this cache client-side instead of hitting the API again.
     */
    private var cachedSchedule: TourSchedule? = null

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

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

        _isLoading.value = true
        // Fresh load → collapse again so the user sees the trimmed first 7 chips.
        resetExpansionState()
        cachedSchedule = null

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
                _isLoading.value = false
                cachedSchedule = response.data
                publishSlotsFor(selectedDate)
            },
            error = {
                _isLoading.value = false
                cachedSchedule = null
                _scheduleSlots.value = emptyList()
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
            return
        }
        val dateString = dateFormatter.format(date)
        val slots = extractSlotsForDate(schedule, dateString)
        _scheduleSlots.value = groupSlotsByTime(slots)
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
