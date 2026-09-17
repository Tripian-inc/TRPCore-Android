package com.tripian.trpcore.ui.timeline.savedplans

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.domain.model.itinerary.SegmentFavoriteItem
import com.tripian.trpcore.domain.usecase.timeline.CreateReservedActivityFromFavoriteUseCase
import com.tripian.trpcore.domain.usecase.timeline.WaitForGenerationUseCase
import com.tripian.trpcore.repository.base.ErrorModel
import com.tripian.trpcore.util.ActivityIdFormat
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.Preferences
import com.tripian.trpcore.util.RemovedFavoritesStore
import com.tripian.trpcore.util.extensions.cityNameKey
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel for Saved Plans screen.
 * Groups favorite items by their SDK-resolved city and handles adding them to the
 * timeline. Receives pre-filtered favorites from the timeline (already excludes
 * booked activities and favourites without a resolved trip city).
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
    private var pendingAddDate: Date? = null
    private val dayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /** Maps cityName (lowercase) to our system's cityId. */
    private var cityNameToIdMap: Map<String, Int> = emptyMap()

    private val plannedActivityIdsByDay = mutableMapOf<String, MutableList<String>>()

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
        cityNameToIdMap: Map<String, Int> = emptyMap(),
        plannedActivityIdsByDay: Map<String, List<String>> = emptyMap()
    ) {
        this.favorites = favorites
        this.tripHash = tripHash
        this.availableDays = availableDays
        this.selectedDate = availableDays.firstOrNull()
        this.cityNameToIdMap = cityNameToIdMap
        this.plannedActivityIdsByDay.clear()
        plannedActivityIdsByDay.forEach { (day, ids) ->
            this.plannedActivityIdsByDay[day] = ids.toMutableList()
        }

        processAndDisplayItems()
    }

    /** Days that already hold a given activity; blocks them in the time selection sheet. */
    fun getPlannedActivityIdsByDay(): Map<String, List<String>> =
        plannedActivityIdsByDay.mapValues { it.value.toList() }

    /**
     * Groups the favourites by their resolved city, in first-seen order. A favourite
     * without a resolved city is not shown.
     */
    private fun processAndDisplayItems() {
        val groupedByCity = favorites
            .filter { resolvedCityIdOf(it) != null }
            .groupBy { resolvedCityIdOf(it)!! }

        val items = mutableListOf<SavedPlansListItem>()

        groupedByCity.forEach { (cityId, cityFavorites) ->
            items.add(SavedPlansListItem.SectionHeader(cityNameFor(cityId, cityFavorites), cityId))

            cityFavorites.forEach { favorite ->
                items.add(SavedPlansListItem.ActivityItem(favorite))
            }
        }

        _listItems.value = items
    }

    private fun resolvedCityIdOf(favorite: SegmentFavoriteItem): Int? =
        favorite.cityId?.takeIf { it > 0 }

    private fun cityNameFor(cityId: Int, cityFavorites: List<SegmentFavoriteItem>): String =
        TRPCore.core.getCachedCityById(cityId)?.name?.takeIf { it.isNotBlank() }
            ?: cityFavorites.first().cityName

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
     * A favourite without a resolved city is refused with an error instead of being booked anywhere.
     */
    fun createReservedActivitySegment(
        selectedDate: Date,
        startTime: String?,
        endTime: String?,
        isFlexible: Boolean,
        slotPrice: Double? = null
    ) {
        val favorite = pendingFavorite ?: return

        val resolvedCityId = resolvedCityIdOf(favorite)
        if (resolvedCityId == null) {
            _segmentCreationFailed.value = true
            showAlert(AlertType.ERROR, getLanguageForKey(LanguageConst.COMMON_ERROR))
            return
        }

        val resolvedEndTime = if (isFlexible) {
            null
        } else {
            endTime ?: calculateEndTime(startTime, favorite.duration)
        }
        val resolvedStartTime = if (isFlexible) null else startTime
        pendingAddDate = selectedDate

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
            markActivityAdded()
            dropAddedFavorite()
            _segmentCreated.value = true
        }
    }

    /** Records the day just taken by the added favourite so re-opening the sheet blocks it right away. */
    private fun markActivityAdded() {
        val added = pendingFavorite ?: return
        val day = pendingAddDate ?: return
        val id = ActivityIdFormat.make(
            activityId = added.activityId,
            cityId = resolvedCityIdOf(added)
        )
        if (id.isEmpty()) return

        val dayIds = plannedActivityIdsByDay.getOrPut(dayKeyFormat.format(day)) { mutableListOf() }
        if (id !in dayIds) dayIds += id
    }

    /** Removes the just-added favorite from the in-memory list and re-renders. */
    private fun dropAddedFavorite() {
        val added = pendingFavorite ?: return
        favorites = favorites.filterNot { sameActivity(it, added) }
        processAndDisplayItems()
    }

    private fun sameActivity(a: SegmentFavoriteItem, b: SegmentFavoriteItem): Boolean {
        val baseA = ActivityIdFormat.base(a.activityId) ?: return a.activityId == b.activityId
        return baseA == ActivityIdFormat.base(b.activityId)
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
        pendingAddDate = null
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

        favorites = favorites.filterNot { sameActivity(it, favorite) }
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
        return cityNameToIdMap[cityName.cityNameKey()]
    }
}
