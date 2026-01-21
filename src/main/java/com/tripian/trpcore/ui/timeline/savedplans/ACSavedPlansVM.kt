package com.tripian.trpcore.ui.timeline.savedplans

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.model.itinerary.SegmentFavoriteItem
import com.tripian.trpcore.domain.usecase.timeline.CreateReservedActivityFromFavoriteUseCase
import com.tripian.trpcore.domain.usecase.timeline.WaitForGenerationUseCase
import com.tripian.trpcore.util.AlertType
import java.util.Date
import javax.inject.Inject

/**
 * ACSavedPlansVM
 * ViewModel for Saved Plans screen
 * Groups favorite items by city and handles adding them to timeline
 *
 * Note: Receives pre-filtered favorites from ACTimeline (already excludes reserved activities)
 */
class ACSavedPlansVM @Inject constructor(
    private val createReservedActivityFromFavoriteUseCase: CreateReservedActivityFromFavoriteUseCase,
    private val waitForGenerationUseCase: WaitForGenerationUseCase
) : BaseViewModel() {

    // =====================
    // LIVEDATA
    // =====================

    private val _listItems = MutableLiveData<List<SavedPlansListItem>>()
    val listItems: LiveData<List<SavedPlansListItem>> = _listItems

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _segmentCreated = MutableLiveData(false)
    val segmentCreated: LiveData<Boolean> = _segmentCreated

    private val _isCreatingSegment = MutableLiveData(false)
    val isCreatingSegment: LiveData<Boolean> = _isCreatingSegment

    private val _showTimeSelection = MutableLiveData<SegmentFavoriteItem?>()
    val showTimeSelection: LiveData<SegmentFavoriteItem?> = _showTimeSelection

    // =====================
    // STATE
    // =====================

    private var favorites: List<SegmentFavoriteItem> = emptyList()
    private var tripHash: String = ""
    private var availableDays: List<Date> = emptyList()
    private var selectedDate: Date? = null
    private var pendingFavorite: SegmentFavoriteItem? = null

    // =====================
    // INITIALIZATION
    // =====================

    /**
     * Initialize with pre-filtered favorites from ACTimeline
     * No need to fetch timeline - favorites are already filtered
     */
    fun initialize(
        favorites: List<SegmentFavoriteItem>,
        tripHash: String,
        availableDays: List<Date>
    ) {
        this.favorites = favorites
        this.tripHash = tripHash
        this.availableDays = availableDays
        this.selectedDate = availableDays.firstOrNull()

        // Process and display items directly
        processAndDisplayItems()
    }

    /**
     * Process favorites and create grouped list items
     */
    private fun processAndDisplayItems() {
        if (favorites.isEmpty()) {
            _listItems.value = emptyList()
            return
        }

        // Group favorites by city
        val groupedByCity = favorites.groupBy { it.cityName }

        // Create list items with section headers
        val items = mutableListOf<SavedPlansListItem>()

        groupedByCity.forEach { (cityName, cityFavorites) ->
            // Add section header
            val cityId = cityFavorites.firstOrNull()?.cityId
            items.add(SavedPlansListItem.SectionHeader(cityName, cityId))

            // Add activity items
            cityFavorites.forEach { favorite ->
                items.add(SavedPlansListItem.ActivityItem(favorite))
            }
        }

        _listItems.value = items
    }

    // =====================
    // USER ACTIONS
    // =====================

    /**
     * Called when user clicks "+" button on an activity
     */
    fun onActivityAddClicked(favorite: SegmentFavoriteItem) {
        pendingFavorite = favorite
        _showTimeSelection.value = favorite
    }

    /**
     * Clear time selection trigger
     */
    fun clearTimeSelectionTrigger() {
        _showTimeSelection.value = null
    }

    /**
     * Create reserved activity segment
     * Called from Activity when user selects time in bottom sheet
     */
    fun createReservedActivitySegment(
        selectedDate: Date,
        startTime: String?
    ) {
        val favorite = pendingFavorite ?: return

        _isCreatingSegment.value = true

        // Calculate end time from duration
        val endTime = calculateEndTime(startTime, favorite.duration)

        createReservedActivityFromFavoriteUseCase.on(
            params = CreateReservedActivityFromFavoriteUseCase.Params(
                tripHash = tripHash,
                favorite = favorite,
                selectedDate = selectedDate,
                startTime = startTime,
                endTime = endTime
            ),
            success = {
                // Wait for generation to complete
                waitForSegmentGeneration()
            },
            error = { errorModel ->
                _isCreatingSegment.value = false
                showAlert(AlertType.ERROR, errorModel.errorDesc ?: "Failed to add activity")
            }
        )
    }

    /**
     * Wait for segment generation to complete
     */
    private fun waitForSegmentGeneration() {
        waitForGenerationUseCase.on(
            params = WaitForGenerationUseCase.Params(tripHash),
            success = {
                _isCreatingSegment.value = false
                _segmentCreated.value = true
            },
            error = {
                _isCreatingSegment.value = false
                _segmentCreated.value = true // Still return success to refresh timeline
            }
        )
    }

    /**
     * Calculate end time from start time and duration
     */
    private fun calculateEndTime(startTime: String?, duration: Double?): String? {
        if (startTime == null || duration == null || duration <= 0) return null

        try {
            val parts = startTime.split(":")
            val startHour = parts[0].toInt()
            val startMinute = parts[1].toInt()

            val totalMinutes = startHour * 60 + startMinute + duration.toInt()
            val endHour = (totalMinutes / 60) % 24
            val endMinute = totalMinutes % 60

            return String.format("%02d:%02d", endHour, endMinute)
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Reset segment created flag
     */
    fun resetSegmentCreated() {
        _segmentCreated.value = false
        pendingFavorite = null
    }

    // =====================
    // GETTERS
    // =====================

    /**
     * Get available days for time selection
     */
    fun getAvailableDays(): List<Date> = availableDays

    /**
     * Get selected date
     */
    fun getSelectedDate(): Date? = selectedDate
}
