package com.tripian.trpcore.ui.timeline.addplan

import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tripian.one.api.cities.model.City
import com.tripian.one.api.pois.model.Coordinate
import com.tripian.one.api.timeline.model.TimelineSegment
import com.tripian.one.api.trip.model.Accommodation
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.model.timeline.AddPlanData
import com.tripian.trpcore.domain.model.timeline.toApiDateString
import com.tripian.trpcore.domain.model.timeline.AddPlanMode
import com.tripian.trpcore.domain.model.timeline.AddPlanStep
import com.tripian.trpcore.domain.model.timeline.ManualCategory
import com.tripian.trpcore.domain.model.timeline.SmartCategory
import com.tripian.trpcore.repository.TripRepository
import com.tripian.trpcore.ui.timeline.addplan.MaterialTimePickerHelper
import com.tripian.trpcore.util.extensions.asIdsByDay
import com.tripian.trpcore.util.extensions.isPastDay
import com.tripian.trpcore.util.extensions.isTodayDate
import com.tripian.trpcore.util.CityTimeZones
import com.tripian.trpcore.util.LanguageConst
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

/**
 * AddPlanContainerVM
 * Shared ViewModel for AddPlan flow container and child fragments
 * iOS Reference: AddPlanContainerVC.swift
 */
class AddPlanContainerVM @Inject constructor(
    private val tripRepository: TripRepository
) : BaseViewModel() {

    // =====================
    // PLAN DATA (Shared state)
    // =====================
    val planData = AddPlanData()

    // =====================
    // NAVIGATION
    // =====================
    private val _currentStep = MutableLiveData(AddPlanStep.SELECT_DAY_AND_CITY)
    val currentStep: LiveData<AddPlanStep> = _currentStep

    private val _navigateBack = MutableLiveData<Boolean>()
    val navigateBack: LiveData<Boolean> = _navigateBack

    /** True while navigating back so the step observer skips forward navigation. */
    private var _isNavigatingBack = false
    val isNavigatingBack: Boolean get() = _isNavigatingBack

    private val _dismissSheet = MutableLiveData<Boolean>()
    val dismissSheet: LiveData<Boolean> = _dismissSheet

    private val _openManualListing = MutableLiveData<ManualCategory?>()
    val openManualListing: LiveData<ManualCategory?> = _openManualListing

    // =====================
    // UI STATE
    // =====================
    private val _titleKey = MutableLiveData<String>()
    val titleKey: LiveData<String> = _titleKey

    private val _showBackButton = MutableLiveData(false)
    val showBackButton: LiveData<Boolean> = _showBackButton

    private val _continueButtonEnabled = MutableLiveData(false)
    val continueButtonEnabled: LiveData<Boolean> = _continueButtonEnabled

    private val _continueButtonTextKey = MutableLiveData<String>()
    val continueButtonTextKey: LiveData<String> = _continueButtonTextKey

    private val _showClearSelection = MutableLiveData(false)
    val showClearSelection: LiveData<Boolean> = _showClearSelection

    private val _expandBottomSheet = MutableLiveData(false)
    val expandBottomSheet: LiveData<Boolean> = _expandBottomSheet

    fun setExpandBottomSheet(expand: Boolean) {
        _expandBottomSheet.value = expand
    }

    // =====================
    // DATA SOURCES
    // =====================
    private val _availableDays = MutableLiveData<List<Date>>()
    val availableDays: LiveData<List<Date>> = _availableDays

    private val _cities = MutableLiveData<List<City>>()
    val cities: LiveData<List<City>> = _cities

    private val _isLoadingCities = MutableLiveData(false)
    val isLoadingCities: LiveData<Boolean> = _isLoadingCities

    private var isUsingAllCities = false

    private val _selectedDayIndex = MutableLiveData(0)
    val selectedDayIndex: LiveData<Int> = _selectedDayIndex

    private val _selectedCity = MutableLiveData<City?>()
    val selectedCity: LiveData<City?> = _selectedCity

    private val _selectedMode = MutableLiveData(AddPlanMode.NONE)
    val selectedMode: LiveData<AddPlanMode> = _selectedMode

    private val _selectedManualCategory = MutableLiveData<ManualCategory?>()
    val selectedManualCategory: LiveData<ManualCategory?> = _selectedManualCategory

    private val _startingPointName = MutableLiveData<String?>(null)
    val startingPointName: LiveData<String?> = _startingPointName

    private val _startingPointNameKey = MutableLiveData<String?>(LanguageConst.ADD_PLAN_CITY_CENTER)
    val startingPointNameKey: LiveData<String?> = _startingPointNameKey

    private val _startTime = MutableLiveData<String?>(null)
    val startTime: LiveData<String?> = _startTime

    private val _endTime = MutableLiveData<String?>(null)
    val endTime: LiveData<String?> = _endTime

    private val _travelers = MutableLiveData(1)
    val travelers: LiveData<Int> = _travelers

    /** Party size the flow starts with and falls back to when the step is cleared. */
    private var defaultTravelers: Int = 1

    private val _selectedSmartCategories = MutableLiveData<List<SmartCategory>>(emptyList())
    val selectedSmartCategories: LiveData<List<SmartCategory>> = _selectedSmartCategories

    private var accommodation: Accommodation? = null

    private var bookedActivities: List<TimelineSegment> = emptyList()

    private var plannedActivityIdsByDay: Map<String, List<String>> = emptyMap()
    private var plannedPoiIdsByDay: Map<String, List<String>> = emptyMap()

    private var tripWideExcludedActivityIds: List<String> = emptyList()

    var selectedStartingPointOptionId: Int = StartingPointOption.CITY_CENTER
        private set

    // =====================
    // COMPLETION CALLBACK
    // =====================
    private val _onComplete = MutableLiveData<AddPlanData?>()
    val onComplete: LiveData<AddPlanData?> = _onComplete

    // =====================
    // INITIALIZATION
    // =====================
    @Suppress("UNCHECKED_CAST")
    fun initializeFromArgs(args: Bundle) {
        val days = args.getSerializable(ARG_AVAILABLE_DAYS) as? ArrayList<Date> ?: arrayListOf()
        val citiesList = args.getSerializable(ARG_CITIES) as? ArrayList<City> ?: arrayListOf()
        var dayIndex = args.getInt(ARG_SELECTED_DAY_INDEX, 0)
        val city = args.getSerializable(ARG_SELECTED_CITY) as? City
        val tripHash = args.getString(ARG_TRIP_HASH)
        accommodation = args.getSerializable(ARG_ACCOMMODATION) as? Accommodation
        bookedActivities = args.getSerializable(ARG_BOOKED_ACTIVITIES) as? ArrayList<TimelineSegment> ?: arrayListOf()
        plannedActivityIdsByDay = args.getSerializable(ARG_PLANNED_ACTIVITY_IDS).asIdsByDay()
        plannedPoiIdsByDay = args.getSerializable(ARG_PLANNED_POI_IDS).asIdsByDay()
        tripWideExcludedActivityIds =
            args.getStringArrayList(ARG_TRIP_WIDE_EXCLUDED_IDS).orEmpty()
        defaultTravelers = args.getInt(ARG_DEFAULT_TRAVELERS, 1).coerceAtLeast(1)
        _travelers.value = defaultTravelers
        planData.travelers = defaultTravelers

        if (dayIndex in days.indices && days[dayIndex].isPastDay()) {
            val todayIndex = days.indexOfFirst { it.isTodayDate() }
            val firstFutureIndex = days.indexOfFirst { !it.isPastDay() }
            dayIndex = when {
                todayIndex >= 0 -> todayIndex
                firstFutureIndex >= 0 -> firstFutureIndex
                else -> (days.size - 1).coerceAtLeast(0)
            }
        }

        _availableDays.value = days
        _selectedDayIndex.value = dayIndex

        planData.availableDays = days
        planData.tripHash = tripHash

        if (dayIndex < days.size) {
            planData.selectedDay = days[dayIndex]
            planData.selectedDayIndex = dayIndex
        }

        if (citiesList.isEmpty()) {
            isUsingAllCities = true
            _selectedCity.value = null
            planData.selectedCity = null
            fetchAllCities()
        } else {
            isUsingAllCities = false
            _cities.value = citiesList
            CityTimeZones.register(citiesList)
            _selectedCity.value = city ?: citiesList.firstOrNull()
            planData.cities = citiesList
            planData.selectedCity = city ?: citiesList.firstOrNull()

            val selectedCityObj = city ?: citiesList.firstOrNull()
            planData.startingPointLocation = selectedCityObj?.coordinate ?: Coordinate().apply {
                lat = 0.0
                lng = 0.0
            }
            _startingPointNameKey.value = LanguageConst.ADD_PLAN_CITY_CENTER
        }

        updateUI()
    }

    /**
     * Fetch all available cities when timeline has no destination cities
     */
    private fun fetchAllCities() {
        _isLoadingCities.value = true

        val cachedCities = tripRepository.getCachedCities()
        if (cachedCities.isNotEmpty()) {
            _cities.value = cachedCities
            CityTimeZones.register(cachedCities)
            planData.cities = cachedCities
            _isLoadingCities.value = false
            updateContinueButtonState()
        } else {
            viewModelScope.launch {
                runCatching { tripRepository.prefetchCitiesAsync() }
                val cities = tripRepository.getCachedCities()
                _cities.value = cities
                CityTimeZones.register(cities)
                planData.cities = cities
                _isLoadingCities.value = false
                updateContinueButtonState()
            }
        }
    }

    /**
     * Check if city selection should always be shown (when using all cities fallback)
     */
    fun shouldAlwaysShowCitySelection(): Boolean = isUsingAllCities

    /**
     * Earliest selectable "HH:mm" for the selected day's start/end time pickers
     * (see [CityTimeZones.minSelectableTimeRounded]).
     */
    fun minSelectableTimeForSelectedDay(): String? {
        val day = _availableDays.value?.getOrNull(_selectedDayIndex.value ?: 0) ?: return null
        return CityTimeZones.minSelectableTimeRounded(day, _selectedCity.value)
    }

    /**
     * Suggested "HH:mm" to prefill the start-time picker with when nothing has
     * been chosen yet (see [CityTimeZones.defaultStartTime]).
     */
    fun defaultStartTimeForSelectedDay(): String? {
        val day = _availableDays.value?.getOrNull(_selectedDayIndex.value ?: 0) ?: return null
        return CityTimeZones.defaultStartTime(day, _selectedCity.value)
    }

    /** Selected city's IANA timezone (e.g. "Europe/Madrid"), or null. */
    fun selectedCityTimeZone(): String? = _selectedCity.value?.timezone

    // =====================
    // DAY SELECTION
    // =====================
    fun selectDay(index: Int) {
        val days = _availableDays.value ?: return
        if (index >= 0 && index < days.size) {
            _selectedDayIndex.value = index
            planData.selectedDay = days[index]
            planData.selectedDayIndex = index
        }
    }

    // =====================
    // CITY SELECTION
    // =====================
    fun selectCity(city: City) {
        _selectedCity.value = city
        planData.selectedCity = city
        if (selectedStartingPointOptionId == StartingPointOption.CITY_CENTER) {
            planData.startingPointLocation = city.coordinate ?: Coordinate().apply {
                lat = 0.0
                lng = 0.0
            }
        }
        updateContinueButtonState()
    }

    fun hasSingleCity(): Boolean = (_cities.value?.size ?: 0) <= 1

    // =====================
    // MODE SELECTION
    // =====================
    fun selectMode(mode: AddPlanMode) {
        _selectedMode.value = mode
        planData.selectedMode = mode

        if (mode == AddPlanMode.SMART_RECOMMENDATIONS) {
            _selectedManualCategory.value = null
            planData.selectedManualCategory = null
        }

        updateContinueButtonState()
    }

    // =====================
    // MANUAL CATEGORY SELECTION
    // =====================
    fun selectManualCategory(category: ManualCategory) {
        _selectedManualCategory.value = category
        planData.selectedManualCategory = category
        updateContinueButtonState()
    }

    /** User location for the "Near Me" option. */
    private var userLocation: Coordinate? = null

    // =====================
    // STARTING POINT
    // =====================
    fun setStartingPoint(name: String, coordinate: Coordinate, selectedAccommodation: Accommodation? = null) {
        _startingPointName.value = name
        _startingPointNameKey.value = null
        planData.startingPointName = name
        planData.startingPointLocation = coordinate
        planData.startingPointAccommodation = selectedAccommodation
        selectedStartingPointOptionId = StartingPointOption.SEARCH_LOCATION
        updateContinueButtonState()
    }

    fun getUserLocation(): Coordinate? = userLocation

    fun setUserLocation(location: Coordinate?) {
        userLocation = location
    }

    fun getBookedActivities(): List<TimelineSegment> = bookedActivities

    /** "yyyy-MM-dd" → activity ids that day already holds, as handed over by the timeline. */
    fun getPlannedActivityIdsByDay(): Map<String, List<String>> = plannedActivityIdsByDay

    fun getPlannedPoiIdsByDay(): Map<String, List<String>> = plannedPoiIdsByDay

    /** Activity ids excluded on every day of the trip (bookings + removed favorites). */
    fun getTripWideExcludedActivityIds(): List<String> = tripWideExcludedActivityIds

    /** Trip window as "yyyy-MM-dd"; POI detail scopes its product query to it. */
    fun tripStartDate(): String? = _availableDays.value?.firstOrNull()?.toApiDateString()

    fun tripEndDate(): String? = _availableDays.value?.lastOrNull()?.toApiDateString()

    fun clearStartingPoint() {
        _startingPointName.value = null
        _startingPointNameKey.value = LanguageConst.ADD_PLAN_CITY_CENTER
        planData.startingPointName = null
        planData.startingPointAccommodation = null
        planData.startingPointLocation = _selectedCity.value?.coordinate ?: Coordinate().apply {
            lat = 0.0
            lng = 0.0
        }
        selectedStartingPointOptionId = StartingPointOption.CITY_CENTER
        updateContinueButtonState()
    }

    fun isUsingDefaultStartingPoint(): Boolean {
        return _startingPointName.value == null
    }

    /**
     * Check if trip has accommodation
     */
    fun hasAccommodation(): Boolean {
        return accommodation?.coordinate != null
    }

    /**
     * Select accommodation as starting point
     */
    fun selectAccommodationAsStartingPoint() {
        val acc = accommodation ?: return
        val coordinate = acc.coordinate ?: return

        val accommodationName = acc.name ?: getLanguageForKey(LanguageConst.ACCOMMODATION_POINT)
        _startingPointName.value = accommodationName
        _startingPointNameKey.value = null
        planData.startingPointName = accommodationName
        planData.startingPointLocation = coordinate
        selectedStartingPointOptionId = StartingPointOption.MY_ACCOMMODATION
        updateContinueButtonState()
    }

    // =====================
    // TIME SELECTION
    // =====================
    fun setStartTime(time: String) {
        _startTime.value = time
        planData.startTime = time
        updateContinueButtonState()
    }

    fun setEndTime(time: String?) {
        if (time != null) {
            val startTime = _startTime.value
            if (startTime != null && !MaterialTimePickerHelper.isEndTimeAfterStartTime(startTime, time)) {
                return
            }
        }

        _endTime.value = time
        planData.endTime = time
        updateContinueButtonState()
    }

    // =====================
    // TRAVELERS
    // =====================
    fun incrementTravelers() {
        val current = _travelers.value ?: 1
        _travelers.value = current + 1
        planData.travelers = current + 1
    }

    fun decrementTravelers() {
        val current = _travelers.value ?: 1
        if (current > 1) {
            _travelers.value = current - 1
            planData.travelers = current - 1
        }
    }

    // =====================
    // SMART CATEGORY SELECTION
    // =====================
    fun toggleSmartCategory(category: SmartCategory) {
        val current = _selectedSmartCategories.value?.toMutableList() ?: mutableListOf()
        if (current.contains(category)) {
            current.remove(category)
        } else {
            current.add(category)
        }
        _selectedSmartCategories.value = current
        planData.selectedSmartCategories = current.toMutableList()
        updateContinueButtonState()
    }

    fun isCategorySelected(category: SmartCategory): Boolean {
        return _selectedSmartCategories.value?.contains(category) == true
    }

    // =====================
    // NAVIGATION
    // =====================
    fun goToNextStep() {
        when (_currentStep.value) {
            AddPlanStep.SELECT_DAY_AND_CITY -> {
                when (planData.selectedMode) {
                    AddPlanMode.SMART_RECOMMENDATIONS, AddPlanMode.SMART -> {
                        _currentStep.value = AddPlanStep.TIME_AND_TRAVELERS
                        updateUI()
                    }
                    AddPlanMode.MANUAL -> {
                        _openManualListing.value = planData.selectedManualCategory
                    }
                    AddPlanMode.NONE -> { /* Do nothing */ }
                }
            }
            AddPlanStep.TIME_AND_TRAVELERS -> {
                _currentStep.value = AddPlanStep.CATEGORY_SELECTION
                updateUI()
            }
            AddPlanStep.CATEGORY_SELECTION -> {
                completeSmartRecommendation()
            }
            else -> {}
        }
    }

    fun goToPreviousStep() {
        _isNavigatingBack = true

        when (_currentStep.value) {
            AddPlanStep.TIME_AND_TRAVELERS -> {
                _currentStep.value = AddPlanStep.SELECT_DAY_AND_CITY
                updateUI()
            }
            AddPlanStep.CATEGORY_SELECTION -> {
                _currentStep.value = AddPlanStep.TIME_AND_TRAVELERS
                updateUI()
            }
            else -> { /* First step - do nothing */ }
        }
        _navigateBack.value = true
    }

    fun clearNavigateBack() {
        _navigateBack.value = false
        _isNavigatingBack = false
    }

    fun clearOpenManualListing() {
        _openManualListing.value = null
    }

    // =====================
    // CLEAR SELECTION
    // =====================
    private val _resetToFirstStep = MutableLiveData<Boolean>()
    val resetToFirstStep: LiveData<Boolean> = _resetToFirstStep

    fun clearSelection() {
        when (_currentStep.value) {
            AddPlanStep.SELECT_DAY_AND_CITY -> clearSelectDayStep()
            AddPlanStep.TIME_AND_TRAVELERS -> clearTimeAndTravelersStep()
            AddPlanStep.CATEGORY_SELECTION -> clearCategorySelectionStep()
            else -> clearSelectDayStep()
        }
        updateUI()
    }

    private fun clearSelectDayStep() {
        _selectedMode.value = AddPlanMode.NONE
        _selectedManualCategory.value = null
        planData.selectedMode = AddPlanMode.NONE
        planData.selectedManualCategory = null
    }

    private fun clearTimeAndTravelersStep() {
        _startTime.value = null
        _endTime.value = null
        planData.startTime = null
        planData.endTime = null

        _travelers.value = defaultTravelers
        planData.travelers = defaultTravelers

        clearStartingPoint()
    }

    private fun clearCategorySelectionStep() {
        _selectedSmartCategories.value = emptyList()
        planData.selectedSmartCategories.clear()
    }

    fun clearResetToFirstStep() {
        _resetToFirstStep.value = false
    }

    // =====================
    // COMPLETION
    // =====================
    /**
     * Emits [onComplete] without dismissing the sheet; the host dismisses it
     * only after the segment create succeeds.
     */
    private fun completeSmartRecommendation() {
        if (planData.isValidForSmartMode()) {
            _onComplete.value = planData
        }
    }

    /**
     * Get the current plan data (used for passing to listing activities)
     */
    fun getValidPlanData(): AddPlanData? = if (planData.selectedCity != null) planData else null

    /**
     * Get the trip hash (used for API calls in listing activities)
     */
    fun getTripHash(): String? = planData.tripHash

    // =====================
    // UI STATE UPDATES
    // =====================
    private fun updateUI() {
        val step = _currentStep.value ?: AddPlanStep.SELECT_DAY_AND_CITY

        _titleKey.value = when (step) {
            AddPlanStep.SELECT_DAY_AND_CITY -> LanguageConst.ADD_PLAN_ADD_ACTIVITY
            AddPlanStep.TIME_AND_TRAVELERS,
            AddPlanStep.CATEGORY_SELECTION -> {
                if (planData.selectedMode == AddPlanMode.SMART_RECOMMENDATIONS ||
                    planData.selectedMode == AddPlanMode.SMART) {
                    LanguageConst.ADD_PLAN_SMART_RECOMMENDATIONS
                } else {
                    LanguageConst.ADD_PLAN_ADD_ACTIVITY
                }
            }
        }

        _showBackButton.value = step != AddPlanStep.SELECT_DAY_AND_CITY

        val isSmartMode = planData.selectedMode == AddPlanMode.SMART_RECOMMENDATIONS ||
                planData.selectedMode == AddPlanMode.SMART
        _showClearSelection.value = when (step) {
            AddPlanStep.SELECT_DAY_AND_CITY -> false
            AddPlanStep.TIME_AND_TRAVELERS,
            AddPlanStep.CATEGORY_SELECTION -> isSmartMode
        }

        _continueButtonTextKey.value = LanguageConst.ADD_PLAN_CONTINUE

        updateContinueButtonState()
    }

    private fun updateContinueButtonState() {
        val step = _currentStep.value ?: AddPlanStep.SELECT_DAY_AND_CITY

        _continueButtonEnabled.value = when (step) {
            AddPlanStep.SELECT_DAY_AND_CITY -> planData.canContinueFromSelectDay()
            AddPlanStep.TIME_AND_TRAVELERS -> planData.canContinueFromTimeAndTravelers()
            AddPlanStep.CATEGORY_SELECTION -> planData.canContinueFromCategorySelection()
        }
    }

    companion object {
        const val ARG_AVAILABLE_DAYS = "availableDays"
        const val ARG_CITIES = "cities"
        const val ARG_SELECTED_DAY_INDEX = "selectedDayIndex"
        const val ARG_SELECTED_CITY = "selectedCity"
        const val ARG_TRIP_HASH = "tripHash"
        const val ARG_ACCOMMODATION = "accommodation"
        const val ARG_BOOKED_ACTIVITIES = "bookedActivities"
        const val ARG_PLANNED_ACTIVITY_IDS = "plannedActivityIdsByDay"
        const val ARG_PLANNED_POI_IDS = "plannedPoiIdsByDay"
        const val ARG_TRIP_WIDE_EXCLUDED_IDS = "tripWideExcludedActivityIds"
        const val ARG_DEFAULT_TRAVELERS = "defaultTravelers"
    }
}
