package com.tripian.trpcore.ui.timeline.activity

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
     * Load schedule for a specific activity/tour on a given date
     * @param activityId The activity or tour ID
     * @param date The date to get schedule for
     * @param cityId The city ID (only for favorites mode, null for tour mode)
     */
    fun loadSchedule(activityId: String, date: Date, cityId: Int? = null) {
        _isLoading.value = true

        val formattedId = formatActivityIdForSchedule(activityId, cityId)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateString = dateFormat.format(date)

        getTourScheduleUseCase.on(
            params = GetTourScheduleUseCase.Params(
                productId = formattedId,
                date = dateString,
                currency = TRPCore.core.appConfig.appCurrency
            ),
            success = { response ->
                _isLoading.value = false
                val slots = response.data?.slots
                _scheduleSlots.value = groupSlotsByTime(slots)
            },
            error = {
                _isLoading.value = false
                _scheduleSlots.value = emptyList()
            }
        )
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
