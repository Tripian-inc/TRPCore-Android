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
import com.tripian.trpcore.domain.model.timeline.AddPlanMode
import com.tripian.trpcore.domain.model.timeline.AddPlanStep
import com.tripian.trpcore.domain.model.timeline.ManualCategory
import com.tripian.trpcore.domain.model.timeline.SmartCategory
import com.tripian.trpcore.util.LanguageConst
import java.util.Date
import javax.inject.Inject

/**
 * AddPlanContainerVM
 * Shared ViewModel for AddPlan flow container and child fragments
 * iOS Reference: AddPlanContainerVC.swift
 */
class AddPlanContainerVM @Inject constructor() : BaseViewModel() {

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

    // Flag to indicate we're navigating back (skip forward navigation in observer)
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

    // =====================
    // DATA SOURCES
    // =====================
    private val _availableDays = MutableLiveData<List<Date>>()
    val availableDays: LiveData<List<Date>> = _availableDays

    private val _cities = MutableLiveData<List<City>>()
    val cities: LiveData<List<City>> = _cities

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

    private val _selectedSmartCategories = MutableLiveData<List<SmartCategory>>(emptyList())
    val selectedSmartCategories: LiveData<List<SmartCategory>> = _selectedSmartCategories

    // Accommodation (from trip)
    private var accommodation: Accommodation? = null

    // Booked activities (for starting point selection)
    private var bookedActivities: List<TimelineSegment> = emptyList()

    // Starting point option ID (for bottom sheet selection state)
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
        val dayIndex = args.getInt(ARG_SELECTED_DAY_INDEX, 0)
        val city = args.getSerializable(ARG_SELECTED_CITY) as? City
        val tripHash = args.getString(ARG_TRIP_HASH)
        accommodation = args.getSerializable(ARG_ACCOMMODATION) as? Accommodation
        bookedActivities = args.getSerializable(ARG_BOOKED_ACTIVITIES) as? ArrayList<TimelineSegment> ?: arrayListOf()

        _availableDays.value = days
        _cities.value = citiesList
        _selectedDayIndex.value = dayIndex
        _selectedCity.value = city ?: citiesList.firstOrNull()

        // Initialize plan data
        planData.availableDays = days
        planData.cities = citiesList
        planData.tripHash = tripHash

        if (dayIndex < days.size) {
            planData.selectedDay = days[dayIndex]
            planData.selectedDayIndex = dayIndex
        }
        planData.selectedCity = city ?: citiesList.firstOrNull()

        // Don't set default times - user should select them
        // _startTime and _endTime remain null, showing "Select" in UI

        // Set default starting point (city center)
        val selectedCityObj = city ?: citiesList.firstOrNull()
        planData.startingPointLocation = selectedCityObj?.coordinate ?: Coordinate().apply {
            lat = 0.0
            lng = 0.0
        }
        _startingPointNameKey.value = LanguageConst.ADD_PLAN_CITY_CENTER

        updateUI()
    }

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
        // Update starting point if using city center
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

    // User location for "Near Me" feature
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

    fun clearStartingPoint() {
        _startingPointName.value = null
        _startingPointNameKey.value = LanguageConst.ADD_PLAN_CITY_CENTER
        planData.startingPointName = null
        planData.startingPointAccommodation = null
        // Use city center coordinate as default starting point
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

        // Use accommodation name, or let UI handle the resource string
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

    fun setEndTime(time: String) {
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
                        // Open full screen activity for manual selection
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
        // Set flag BEFORE updating currentStep to prevent forward navigation
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
        // Reset mode and categories
        _selectedMode.value = AddPlanMode.NONE
        _selectedManualCategory.value = null
        _selectedSmartCategories.value = emptyList()

        // Reset time selections
        _startTime.value = null
        _endTime.value = null
        planData.startTime = null
        planData.endTime = null

        // Reset travelers
        _travelers.value = 1
        planData.travelers = 1

        // Reset starting point to default (city center)
        clearStartingPoint()

        // Clear plan data selection
        planData.clearSelection()

        // Go back to first step
        _currentStep.value = AddPlanStep.SELECT_DAY_AND_CITY
        _resetToFirstStep.value = true

        updateUI()
    }

    fun clearResetToFirstStep() {
        _resetToFirstStep.value = false
    }

    // =====================
    // COMPLETION
    // =====================
    private fun completeSmartRecommendation() {
        if (planData.isValidForSmartMode()) {
            _onComplete.value = planData
            _dismissSheet.value = true
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

        // Update title based on step and mode
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

        // Update back button visibility
        _showBackButton.value = step != AddPlanStep.SELECT_DAY_AND_CITY

        // Update clear selection visibility - show on all steps when in Smart Recommendations mode
        val isSmartMode = planData.selectedMode == AddPlanMode.SMART_RECOMMENDATIONS ||
                planData.selectedMode == AddPlanMode.SMART
        _showClearSelection.value = when (step) {
            AddPlanStep.SELECT_DAY_AND_CITY -> planData.selectedMode != AddPlanMode.NONE
            AddPlanStep.TIME_AND_TRAVELERS,
            AddPlanStep.CATEGORY_SELECTION -> isSmartMode
        }

        // Update continue button text
        _continueButtonTextKey.value = when (step) {
            AddPlanStep.CATEGORY_SELECTION -> LanguageConst.ADD_PLAN_GENERATE
            else -> LanguageConst.ADD_PLAN_CONTINUE
        }

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
    }
}
