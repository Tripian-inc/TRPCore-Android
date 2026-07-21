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
 * ViewModel for Saved Plans screen.
 * Groups favorite items by city and handles adding them to the timeline.
 * Receives pre-filtered favorites from ACTimeline (already excludes reserved activities).
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

    /** Emitted when segment creation fails so the screen can hide the in-sheet loader. */
    private val _segmentCreationFailed = MutableLiveData<Boolean?>()
    val segmentCreationFailed: LiveData<Boolean?> = _segmentCreationFailed

    private val _showTimeSelection = MutableLiveData<SegmentFavoriteItem?>()
    val showTimeSelection: LiveData<SegmentFavoriteItem?> = _showTimeSelection

    /** Emitted after a favorite is removed; carries true when the list became empty. */
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

    /** Maps cityName (lowercase) to our system's cityId. */
    private var cityNameToIdMap: Map<String, Int> = emptyMap()

    // =====================
    // INITIALIZATION
    // =====================

    /**
     * Initialize with pre-filtered favorites from ACTimeline
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

        val groupedByCity = favorites.groupBy { it.cityName }

        val items = mutableListOf<SavedPlansListItem>()

        groupedByCity.forEach { (cityName, cityFavorites) ->
            val cityId = cityFavorites.firstOrNull()?.cityId
            items.add(SavedPlansListItem.SectionHeader(cityName, cityId))

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
     * Creates a reserved activity segment when the user selects a time in the bottom sheet.
     * For flexible favorites the time window is resolved by the use case, so start/end are sent as null.
     */
    fun createReservedActivitySegment(
        selectedDate: Date,
        startTime: String?,
        endTime: String?,
        isFlexible: Boolean,
        slotPrice: Double? = null
    ) {
        val favorite = pendingFavorite ?: return

        val resolvedEndTime = if (isFlexible) {
            null
        } else {
            endTime ?: calculateEndTime(startTime, favorite.duration)
        }
        val resolvedStartTime = if (isFlexible) null else startTime

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
     * Waits for segment generation, caches the resulting timeline for the timeline screen,
     * then drops the added favorite and signals success. The segment is already created even
     * if polling times out, so the favorite is dropped either way.
     */
    private fun waitForSegmentGeneration() {
        viewModelScope.launch {
            val timeline = runCatching {
                waitForGenerationUseCase(WaitForGenerationUseCase.Params(tripHash))
            }.getOrNull()
            timeline?.let { timelineRepository.cacheGeneratedTimeline(tripHash, it) }
            dropAddedFavorite()
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
        RemovedFavoritesStore.addRemoved(preferences, tripHash, favorite.activityId)

        RemovedFavoritesStore.baseActivityId(favorite.activityId)?.let { baseId ->
            TRPCore.notifyActivityRemovedFromSavedPlans(baseId)
        }

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
     * @param cityName The city name from host app data
     * @return Our system's cityId, or null if not found
     */
    fun getResolvedCityId(cityName: String?): Int? {
        if (cityName.isNullOrBlank()) return null
        return cityNameToIdMap[cityName.lowercase().trim()]
    }
}
