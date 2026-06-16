package com.tripian.trpcore.ui.timeline.savedplans

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.domain.model.itinerary.SegmentFavoriteItem
import com.tripian.trpcore.domain.usecase.timeline.CreateReservedActivityFromFavoriteUseCase
import com.tripian.trpcore.domain.usecase.timeline.WaitForGenerationUseCase
import com.tripian.trpcore.repository.base.ErrorModel
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.Preferences
import com.tripian.trpcore.util.RemovedFavoritesStore
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
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
    private val waitForGenerationUseCase: WaitForGenerationUseCase,
    private val timelineRepository: com.tripian.trpcore.repository.TimelineRepository,
    private val preferences: Preferences
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

    // Emitted when segment creation fails so the screen can hide the in-sheet
    // loader (the sheet stays open for retry; the alert is shown by the VM).
    private val _segmentCreationFailed = MutableLiveData<Boolean?>()
    val segmentCreationFailed: LiveData<Boolean?> = _segmentCreationFailed

    private val _showTimeSelection = MutableLiveData<SegmentFavoriteItem?>()
    val showTimeSelection: LiveData<SegmentFavoriteItem?> = _showTimeSelection

    // Emitted after a favorite is removed so the screen can dismiss the sheet
    // and refresh. Carries true when the list became empty (so the screen can
    // close), false otherwise.
    private val _favoriteRemoved = MutableLiveData<Boolean?>()
    val favoriteRemoved: LiveData<Boolean?> = _favoriteRemoved

    // =====================
    // STATE
    // =====================

    private var favorites: List<SegmentFavoriteItem> = emptyList()
    private var tripHash: String = ""
    private var availableDays: List<Date> = emptyList()
    private var selectedDate: Date? = null
    private var pendingFavorite: SegmentFavoriteItem? = null

    // City name to ID mapping - maps cityName (lowercase) to our system's cityId
    private var cityNameToIdMap: Map<String, Int> = emptyMap()

    // =====================
    // INITIALIZATION
    // =====================

    /**
     * Initialize with pre-filtered favorites from ACTimeline
     * No need to fetch timeline - favorites are already filtered
     * @param cityNameToIdMap Mapping of cityName (lowercase) to our system's cityId
     */
    fun initialize(
        favorites: List<SegmentFavoriteItem>,
        tripHash: String,
        availableDays: List<Date>,
        cityNameToIdMap: Map<String, Int> = emptyMap()
    ) {
        this.favorites = favorites
        this.tripHash = tripHash
        this.availableDays = availableDays
        this.selectedDate = availableDays.firstOrNull()
        this.cityNameToIdMap = cityNameToIdMap

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
        startTime: String?,
        endTime: String?,
        isFlexible: Boolean,
        slotPrice: Double? = null
    ) {
        val favorite = pendingFavorite ?: return

        // The "adding to itinerary" loader is shown inline inside the open time
        // selection sheet (driven by the sheet's own VM); see ACSavedPlans.

        // Flexible favorite: window'u use case helper'ı belirliyor; lokal
        // calculateEndTime'ı atlıyoruz. Timed flow için bottom-sheet zaten
        // hesaplanmış endTime gönderiyor, gönderilmediyse duration'dan
        // türetiyoruz.
        val resolvedEndTime = if (isFlexible) {
            null
        } else {
            endTime ?: calculateEndTime(startTime, favorite.duration)
        }
        val resolvedStartTime = if (isFlexible) null else startTime

        // Get resolved cityId from mapping (our system's ID)
        val resolvedCityId = getResolvedCityId(favorite.cityName)

        viewModelScope.launch {
            runCatching {
                createReservedActivityFromFavoriteUseCase(
                    CreateReservedActivityFromFavoriteUseCase.Params(
                        tripHash = tripHash,
                        favorite = favorite,
                        selectedDate = selectedDate,
                        startTime = resolvedStartTime,
                        endTime = resolvedEndTime,
                        resolvedCityId = resolvedCityId,
                        isFlexible = isFlexible,
                        slotPrice = slotPrice
                    )
                )
            }
                .onSuccess { waitForSegmentGeneration() }
                .onFailure { t ->
                    val msg = (t as? ErrorModel)?.errorDesc ?: t.message
                    _segmentCreationFailed.value = true
                    showAlert(AlertType.ERROR, msg ?: getLanguageForKey(LanguageConst.COMMON_ERROR))
                }
        }
    }

    /**
     * Wait for segment generation to complete
     */
    private fun waitForSegmentGeneration() {
        viewModelScope.launch {
            // Whether generation polling succeeds or times out, the segment was
            // created — so drop the added favorite from the list either way and
            // signal success. The screen stays open and shows the empty state
            // once the list is exhausted.
            val timeline = runCatching {
                waitForGenerationUseCase(WaitForGenerationUseCase.Params(tripHash))
            }.getOrNull()
            // Cache the freshly-generated timeline so the timeline screen can
            // apply it on return without issuing a second GET (the wait above
            // already fetched it in the background).
            timeline?.let { timelineRepository.cacheGeneratedTimeline(tripHash, it) }
            dropAddedFavorite()
            // The inline loader is torn down when the host dismisses the sheet
            // on segmentCreated.
            _segmentCreated.value = true
        }
    }

    /** Removes the just-added favorite from the in-memory list and re-renders. */
    private fun dropAddedFavorite() {
        val added = pendingFavorite ?: return
        favorites = favorites.filterNot { it.activityId == added.activityId }
        processAndDisplayItems()
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

    /**
     * Remove a favorite from saved plans. Persists the removal locally (per
     * tripHash) so it stays removed across app restarts, notifies the host with
     * the base activityId, drops it from the list and emits [favoriteRemoved].
     */
    fun removeFavorite(favorite: SegmentFavoriteItem) {
        // Persist the removal so this favorite won't reappear for this timeline.
        RemovedFavoritesStore.addRemoved(preferences, tripHash, favorite.activityId)

        // Notify host with the base activityId (C_ prefix / suffixes stripped).
        RemovedFavoritesStore.baseActivityId(favorite.activityId)?.let { baseId ->
            TRPCore.notifyActivityRemovedFromSavedPlans(baseId)
        }

        // Drop it from the in-memory list and re-render.
        favorites = favorites.filterNot { it.activityId == favorite.activityId }
        pendingFavorite = null
        processAndDisplayItems()

        _favoriteRemoved.value = favorites.isEmpty()
    }

    /** Clears the one-shot [favoriteRemoved] event. */
    fun resetFavoriteRemoved() {
        _favoriteRemoved.value = null
    }

    /** Clears the one-shot [segmentCreationFailed] event. */
    fun resetSegmentCreationFailed() {
        _segmentCreationFailed.value = null
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

    /**
     * Returns the resolved cityId for a given cityName.
     * Uses the cityNameToIdMap passed from ACTimeline.
     * @param cityName The city name from host app data
     * @return Our system's cityId, or null if not found
     */
    fun getResolvedCityId(cityName: String?): Int? {
        if (cityName.isNullOrBlank()) return null
        return cityNameToIdMap[cityName.lowercase().trim()]
    }
}
