package com.tripian.trpcore.ui.timeline

import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tripian.one.api.cities.model.City
import com.tripian.one.api.timeline.model.SegmentType
import com.tripian.one.api.timeline.model.Timeline
import com.tripian.one.api.timeline.model.TimelinePlan
import com.tripian.one.api.timeline.model.TimelineSegment
import com.tripian.one.api.timeline.model.isGenerated
import com.tripian.trpcore.R
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.domain.DoLightLogin
import com.tripian.trpcore.domain.model.MapStep
import com.tripian.trpcore.domain.model.itinerary.ItineraryWithActivities
import com.tripian.trpcore.domain.model.itinerary.SegmentFavoriteItem
import com.tripian.trpcore.domain.model.timeline.AddPlanData
import com.tripian.trpcore.domain.model.timeline.AddPlanMode
import com.tripian.trpcore.domain.model.timeline.StepRouteInfo
import com.tripian.trpcore.domain.model.timeline.TimelineDisplayItem
import com.tripian.trpcore.domain.model.timeline.generateDateRange
import com.tripian.trpcore.domain.model.timeline.toApiDateString
import com.tripian.trpcore.domain.model.timeline.toDate
import com.tripian.trpcore.domain.usecase.timeline.CreateSegmentUseCase
import com.tripian.trpcore.domain.usecase.timeline.CreateTimelineUseCase
import com.tripian.trpcore.domain.usecase.timeline.DeleteSegmentUseCase
import com.tripian.trpcore.domain.usecase.timeline.DeleteStepUseCase
import com.tripian.trpcore.domain.usecase.timeline.FetchTimelineUseCase
import com.tripian.trpcore.domain.usecase.timeline.GetTimelineStepRoutesUseCase
import com.tripian.trpcore.domain.usecase.timeline.UpdateStepTimeUseCase
import com.tripian.trpcore.domain.usecase.timeline.WaitForGenerationUseCase
import com.tripian.trpcore.ui.timeline.adapter.MapBottomItem
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.appLanguage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * ACTimelineVM
 * ViewModel for the Timeline screen
 */
class ACTimelineVM @Inject constructor(
    private val doLightLogin: DoLightLogin,
    private val fetchTimelineUseCase: FetchTimelineUseCase,
    private val createTimelineUseCase: CreateTimelineUseCase,
    private val createSegmentUseCase: CreateSegmentUseCase,
    private val waitForGenerationUseCase: WaitForGenerationUseCase,
    private val deleteSegmentUseCase: DeleteSegmentUseCase,
    private val deleteStepUseCase: DeleteStepUseCase,
    private val updateStepTimeUseCase: UpdateStepTimeUseCase,
    private val getTimelineStepRoutesUseCase: GetTimelineStepRoutesUseCase,
    private val tripRepository: com.tripian.trpcore.repository.TripRepository
) : BaseViewModel() {

    // =====================
    // LIVEDATA
    // =====================

    private val _timeline = MutableLiveData<Timeline>()
    val timeline: LiveData<Timeline> = _timeline

    private val _displayItems = MutableLiveData<List<TimelineDisplayItem>>()
    val displayItems: LiveData<List<TimelineDisplayItem>> = _displayItems

    private val _availableDays = MutableLiveData<List<Date>>()
    val availableDays: LiveData<List<Date>> = _availableDays

    private val _selectedDayIndex = MutableLiveData(0)
    val selectedDayIndex: LiveData<Int> = _selectedDayIndex

    private val _cities = MutableLiveData<List<City>>()
    val cities: LiveData<List<City>> = _cities

    private val _isMapMode = MutableLiveData(false)
    val isMapMode: LiveData<Boolean> = _isMapMode

    private val _showAddPlanSheet = MutableLiveData<Boolean>()
    val showAddPlanSheet: LiveData<Boolean> = _showAddPlanSheet

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _mapSteps = MutableLiveData<List<MapStep>>()
    val mapSteps: LiveData<List<MapStep>> = _mapSteps

    private val _mapBottomItems = MutableLiveData<List<MapBottomItem>>()
    val mapBottomItems: LiveData<List<MapBottomItem>> = _mapBottomItems

    private val _launchPoiSelection = MutableLiveData<AddPlanData?>()
    val launchPoiSelection: LiveData<AddPlanData?> = _launchPoiSelection

    private val _showNearMeButton = MutableLiveData(false)
    val showNearMeButton: LiveData<Boolean> = _showNearMeButton

    private val _savedPlansCount = MutableLiveData(0)
    val savedPlansCount: LiveData<Int> = _savedPlansCount

    private val _selectedCity = MutableLiveData<City?>()

    // Route info cache - maps segmentIndex to route info list
    private val _routeInfoCache = mutableMapOf<Int, List<StepRouteInfo>>()

    // LiveData to notify UI when route info is updated for a segment
    private val _routeInfoUpdated = MutableLiveData<Int?>()
    val routeInfoUpdated: LiveData<Int?> = _routeInfoUpdated

    // =====================
    // STATE
    // =====================

    private var _tripHash: String = ""
    val tripHash: String get() = _tripHash
    private var itinerary: ItineraryWithActivities? = null
    private var uniqueId: String? = null
    private var isLoggedIn: Boolean = false

    // =====================
    // LIFECYCLE
    // =====================

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

        // Get trip hash, itinerary and uniqueId from arguments
        _tripHash = arguments?.getString(TRPCore.EXTRA_TRIP_HASH) ?: ""
        itinerary = arguments?.getParcelable(TRPCore.EXTRA_ITINERARY)
        uniqueId = arguments?.getString(TRPCore.EXTRA_UNIQUE_ID)

        // Set app language from intent (important for localization)
        val language = arguments?.getString(TRPCore.EXTRA_APP_LANGUAGE)
        if (!language.isNullOrEmpty()) {
            appLanguage = language
            // Also update MiscRepository to use correct language for translations
            miscRepository.changeLanguage(language)
        }

        // Also check legacy ARG_TRIP_HASH
        if (_tripHash.isEmpty()) {
            _tripHash = arguments?.getString(ARG_TRIP_HASH) ?: ""
        }

        // First, perform light login before any timeline operations
        performLightLogin()
    }

    /**
     * Performs light login.
     * Proceeds with timeline operations on success.
     */
    private fun performLightLogin() {
        _isLoading.value = true
        _error.value = null

        doLightLogin.on(
            params = DoLightLogin.Params(
                uniqueId = uniqueId
            ),
            success = { response ->
                isLoggedIn = true
                // Login successful, now proceed with timeline operations
                proceedWithTimelineOperations()
            },
            error = { errorModel ->
                _isLoading.value = false
                _error.value = errorModel.errorDesc ?: "Login failed"
                TRPCore.notifyError(errorModel.errorDesc ?: "Login failed")
            }
        )
    }

    /**
     * Continues with timeline operations after login.
     * Fetches if tripHash exists, creates from itinerary otherwise.
     */
    private fun proceedWithTimelineOperations() {
        // Update saved plans count from itinerary favourites
        updateSavedPlansCount()

        when {
            _tripHash.isNotEmpty() -> {
                fetchTimeline()
            }

            itinerary != null -> {
                createTimelineFromItinerary()
            }

            else -> {
                _isLoading.value = false
                _error.value = "No trip hash or itinerary provided"
            }
        }
    }

    /**
     * Updates the saved plans count from itinerary favouriteItems
     * Filters out items that are already added as reserved_activity in the timeline
     */
    private fun updateSavedPlansCount(timeline: Timeline? = null) {
        val favourites = itinerary?.favouriteItems ?: emptyList()

        // Get activityIds of reserved_activity segments from timeline
        val reservedActivityIds = timeline?.tripProfile?.segments
            ?.filter { it.segmentType == SegmentType.RESERVED_ACTIVITY }
            ?.mapNotNull { it.additionalData?.activityId }
            ?.toSet() ?: emptySet()

        // Filter out favourites that are already in timeline as reserved_activity
        val filteredCount = favourites.count { favourite ->
            favourite.activityId !in reservedActivityIds
        }

        _savedPlansCount.value = filteredCount
    }

    // =====================
    // TIMELINE CREATION FROM ITINERARY
    // =====================

    /**
     * Creates timeline from ItineraryWithActivities
     * Flow:
     * 1. Call createTimeline API
     * 2. Store returned hash
     * 3. Poll with waitForGeneration
     * 4. Display timeline
     */
    private fun createTimelineFromItinerary() {
        val itineraryData = itinerary ?: return

        _isLoading.value = true
        _error.value = null

        createTimelineUseCase.on(
            params = CreateTimelineUseCase.Params(itineraryData),
            success = { timeline ->
                // Timeline created, store hash
                _tripHash = timeline.tripHash ?: ""

                if (_tripHash.isNotEmpty()) {
                    // Notify host app that timeline was created
                    TRPCore.notifyTimelineCreated(_tripHash)

                    // Wait for generation to complete
                    waitForTimelineGeneration()
                } else {
                    processTimeline(timeline)
                    _isLoading.value = false
                }
            },
            error = { errorModel ->
                _error.value = errorModel.errorDesc
                TRPCore.notifyError(errorModel.errorDesc ?: "Timeline creation failed")
                _isLoading.value = false
            }
        )
    }

    /**
     * Waits until timeline generation is complete
     */
    private fun waitForTimelineGeneration() {
        waitForGenerationUseCase.on(
            params = WaitForGenerationUseCase.Params(_tripHash),
            success = { timeline ->
                processTimeline(timeline)
                _isLoading.value = false
            },
            error = {
                _isLoading.value = false
                // Show current timeline even if generation is not complete
                fetchTimeline()
            }
        )
    }

    // =====================
    // FETCH & REFRESH
    // =====================

    fun fetchTimeline() {
        _isLoading.value = true
        _error.value = null

        fetchTimelineUseCase.on(
            params = FetchTimelineUseCase.Params(_tripHash),
            success = { timeline ->
                processTimeline(timeline)
                _isLoading.value = false
            },
            error = { errorModel ->
                _error.value = errorModel.errorDesc
                TRPCore.notifyError(errorModel.errorDesc ?: "Timeline fetch failed")
                _isLoading.value = false
            }
        )
    }

    fun refreshTimeline() {
        _isLoading.value = true
        // Clear route info cache to ensure fresh calculations
        clearRouteInfoCache()

        fetchTimelineUseCase.on(
            params = FetchTimelineUseCase.Params(_tripHash),
            success = { timeline ->
                processTimeline(timeline)
                _isLoading.value = false
            },
            error = {
                _isLoading.value = false
            }
        )
    }

    // =====================
    // DATA PROCESSING
    // =====================

    private fun processTimeline(timeline: Timeline) {
        _timeline.value = timeline

        // Update saved plans count (filter out already reserved activities)
        updateSavedPlansCount(timeline)

        // Notify host app that timeline is loaded
        timeline.tripHash.let { hash ->
            if (hash.isNotEmpty()) {
                TRPCore.notifyTimelineLoaded(hash)
            }
        }

        // Extract cities
        val uniqueCities = extractCities(timeline)
        _cities.value = uniqueCities

        // Calculate available days from segments
        val days = calculateAvailableDays(timeline)
        _availableDays.value = days

        // Ensure selected index is valid
        val currentIndex = _selectedDayIndex.value ?: 0
        if (currentIndex >= days.size && days.isNotEmpty()) {
            _selectedDayIndex.value = 0
        }

        // Generate display items for selected day
        updateDisplayItems()
    }

    /**
     * Extract cities from timeline, using cached cities for full City model data.
     * This ensures we have complete City information (including coordinates)
     * by matching cityIds with pre-fetched cities from getCities API.
     */
    private fun extractCities(timeline: Timeline): List<City> {
        val cityIds = mutableSetOf<Int>()

        // Collect all unique cityIds from timeline
        timeline.plans?.forEach { plan ->
            plan.city?.id?.let { cityIds.add(it) }
        }

        timeline.city?.id?.let { cityIds.add(it) }

        // Also get cityIds from segments
        timeline.tripProfile?.segments?.forEach { segment ->
            segment.cityId?.let { cityIds.add(it) }
        }

        // Map cityIds to full City models from cache
        val cities = mutableListOf<City>()
        cityIds.forEach { cityId ->
            // First try to get from cached cities (has full data including coordinates)
            val cachedCity = tripRepository.getCachedCityById(cityId)
            if (cachedCity != null) {
                cities.add(cachedCity)
            } else {
                // Fallback to timeline city data if not in cache
                val timelineCity = timeline.plans?.find { it.city?.id == cityId }?.city
                    ?: if (timeline.city?.id == cityId) timeline.city else null
                timelineCity?.let { cities.add(it) }
            }
        }

        return cities
    }

    private fun calculateAvailableDays(timeline: Timeline): List<Date> {
        val segments = timeline.tripProfile?.segments ?: return emptyList()

        var minDate: Date? = null
        var maxDate: Date? = null

        segments.forEach { segment ->
            segment.startDate?.toDate()?.let { date ->
                if (minDate == null || date.before(minDate)) minDate = date
            }
            segment.endDate?.toDate()?.let { date ->
                if (maxDate == null || date.after(maxDate)) maxDate = date
            }
        }

        return generateDateRange(minDate, maxDate)
    }

    // =====================
    // DAY FILTERING
    // =====================

    fun selectDay(index: Int) {
        _selectedDayIndex.value = index
        updateDisplayItems()
    }

    private fun updateDisplayItems() {
        val timeline = _timeline.value ?: return
        val days = _availableDays.value ?: return
        val selectedIndex = _selectedDayIndex.value ?: 0

        if (selectedIndex >= days.size) return

        val selectedDate = days[selectedIndex]
        val items = generateDisplayItemsForDay(timeline, selectedDate)

        _displayItems.value = items

        // Always update map steps so they're ready when user switches to map mode
        updateMapSteps()
    }

    /**
     * Generates display items for a specific day.
     *
     * IMPORTANT: Only uses tripProfile.segments as the single source of truth.
     * - For booked_activity/reserved_activity: uses segment's additionalData
     * - For itinerary: finds matching plan to get steps/POIs
     *
     * Plans list is NOT iterated separately to avoid duplicates.
     */
    private fun generateDisplayItemsForDay(
        timeline: Timeline,
        date: Date
    ): List<TimelineDisplayItem> {
        val items = mutableListOf<TimelineDisplayItem>()
        val dateStr = date.toApiDateString()
        val segments = timeline.tripProfile?.segments ?: emptyList()

        // Process ONLY segments - single source of truth
        segments.forEachIndexed { index, segment ->
            if (segment.startDate?.startsWith(dateStr) == true) {
                val segmentType = segment.segmentType
                val plan = timeline.plans?.get(index) //findPlanForSegment(segment, timeline.plans, dateStr)
                when (segmentType) {
                    // Booked Activity (not reserved)
                    SegmentType.BOOKED_ACTIVITY -> {
                        items.add(
                            TimelineDisplayItem.BookedActivity(
                                segment = segment,
                                isReserved = false,
                                segmentIndex = index,
                                city = getCityForSegment(segment, timeline)
                            )
                        )
                    }
                    // Reserved Activity
                    SegmentType.RESERVED_ACTIVITY -> {
                        items.add(
                            TimelineDisplayItem.BookedActivity(
                                segment = segment,
                                isReserved = true,
                                segmentIndex = index,
                                city = getCityForSegment(segment, timeline)
                            )
                        )
                    }
                    // Itinerary / Generated (Smart Recommendations)
                    SegmentType.ITINERARY, SegmentType.GENERATED -> {
                        // Find matching plan to get steps/POIs
                        if (plan != null) {
                            val steps = plan.steps ?: emptyList()

                            // Skip empty itinerary segments (title "Empty" with no steps)
                            if ((segment.title?.equals(
                                    "Empty",
                                    ignoreCase = true
                                ) == true || plan.generatedStatus == -2) && steps.isEmpty()
                            ) {
                                return@forEachIndexed // Skip empty recommendations
                            }

                            items.add(
                                TimelineDisplayItem.Recommendations(
                                    plan = plan,
                                    steps = steps,
                                    segment = segment,
                                    segmentIndex = index,
                                    cachedCity = getCityForSegment(segment, timeline)
                                )
                            )
                        }
                    }
                    // Manual POI
                    SegmentType.MANUAL_POI -> {
                        // Find matching plan for manual POI steps
            //                        val plan = findPlanForSegment(segment, timeline.plans, dateStr)
                        plan?.steps?.firstOrNull()?.let { step ->
                            items.add(
                                TimelineDisplayItem.ManualPoi(
                                    step = step,
                                    segment = segment,
                                    segmentIndex = index,
                                    city = getCityForSegment(segment, timeline)
                                )
                            )
                        }
                    }
                }
            }
        }

        // Empty day check
        if (items.isEmpty()) {
            items.add(
                TimelineDisplayItem.EmptyState(
                    message = getLanguageForKey(LanguageConst.NO_PLANS_FOR_DAY)
                )
            )
        }

        // Group items by city
        return groupItemsByCity(items)
    }

    /**
     * Get City for a segment, prioritizing cached cities for full data.
     */
    private fun getCityForSegment(segment: TimelineSegment, timeline: Timeline): City? {
        segment.cityId?.let { cityId ->
            // First try cached cities (has full data including coordinates)
            tripRepository.getCachedCityById(cityId)?.let { return it }

            // Then try from _cities LiveData
            _cities.value?.find { it.id == cityId }?.let { return it }

            // Finally fallback to timeline data
            return timeline.plans?.find { it.city?.id == cityId }?.city
        }
        return timeline.city
    }

    /**
     * Finds the matching plan for a segment.
     * Uses multiple matching strategies for robustness:
     * 1. Match by dayIds - most reliable (segment.dayIds contains plan.id)
     * 2. Match by exact startDate (includes time)
     * 3. Match by date AND cityId
     * 4. Match by date only (fallback)
     */
    private fun findPlanForSegment(
        segment: TimelineSegment,
        plans: List<TimelinePlan>?,
        dateStr: String
    ): TimelinePlan? {
        if (plans.isNullOrEmpty()) return null

        // Strategy 1: Match by dayIds - most reliable
        // segment.dayIds contains the plan IDs that belong to this segment
        val dayIds = segment.dayIds
        if (!dayIds.isNullOrEmpty()) {
            plans.find { plan ->
                val planId = plan.id?.toIntOrNull()
                planId != null && dayIds.contains(planId)
            }?.let { return it }
        }

        val segmentStartDate = segment.startDate

        // Strategy 2: Match by exact startDate (includes time)
        if (!segmentStartDate.isNullOrEmpty()) {
            plans.find { plan ->
                plan.startDate == segmentStartDate
            }?.let { return it }
        }

        // Strategy 3: Match by date and cityId
        if (segment.cityId != null) {
            plans.find { plan ->
                plan.startDate.startsWith(dateStr) &&
                        plan.city?.id == segment.cityId
            }?.let { return it }
        }

        // Strategy 4: Match by date only (fallback)
        return plans.find { plan ->
            plan.startDate.startsWith(dateStr)
        }
    }

    /**
     * Groups timeline items by city, assigns sequential order numbers, and adds section headers/footers.
     *
     * Key behaviors:
     * - Preserves API segment order (no alphabetical sorting)
     * - Sequential order numbering across ALL items AND steps within each city
     * - Recommendations header does NOT show order badge (order = 0), only steps show order
     * - Each step within Recommendations gets its own sequential order number
     * - Order resets to 1 for each city
     * - Section headers are added only when multiple cities exist
     * - Section footers (separators) are added between city groups (not after last)
     */
    private fun groupItemsByCity(items: List<TimelineDisplayItem>): List<TimelineDisplayItem> {
        // Filter out headers and footers, keep only content items
        val contentItems = items.filterNot {
            it is TimelineDisplayItem.SectionHeader || it is TimelineDisplayItem.SectionFooter
        }

        if (contentItems.isEmpty()) return items

        // Use LinkedHashMap to preserve API segment order (insertion order)
        val groupedByCity = linkedMapOf<Int?, MutableList<TimelineDisplayItem>>()
        contentItems.forEach { item ->
            val cityId = item.city?.id
            groupedByCity.getOrPut(cityId) { mutableListOf() }.add(item)
        }

        // Build result list preserving API segment order
        val result = mutableListOf<TimelineDisplayItem>()
        val totalCities = groupedByCity.size

        groupedByCity.entries.forEachIndexed { cityIndex, (_, cityItems) ->
            val city = cityItems.firstOrNull()?.city

            // Always add section header (city name)
            if (city != null) {
                result.add(
                    TimelineDisplayItem.SectionHeader(
                        cityName = city.name ?: "",
                        city = city
                    )
                )
            }

            // Sequential order numbering - starts at 1 for each city
            var currentOrder = 1

            cityItems.forEach { item ->
                val itemWithOrder = when (item) {
                    is TimelineDisplayItem.BookedActivity -> {
                        val ordered = item.copy(order = currentOrder)
                        currentOrder += 1  // Single item = +1
                        ordered
                    }

                    is TimelineDisplayItem.ManualPoi -> {
                        val ordered = item.copy(order = currentOrder)
                        currentOrder += 1  // Single item = +1
                        ordered
                    }

                    is TimelineDisplayItem.Recommendations -> {
                        val stepCount = item.steps.size.coerceAtLeast(1)
                        val ordered = item.copy(
                            order = 0,  // Header does NOT show order badge
                            startingOrder = currentOrder  // Steps start from this order
                        )
                        currentOrder += stepCount  // Increment by step count
                        ordered
                    }

                    else -> item
                }
                result.add(itemWithOrder)
            }

            // Add section footer between city groups (not after last)
            if (totalCities > 1 && cityIndex < totalCities - 1) {
                result.add(TimelineDisplayItem.SectionFooter(city = city))
            }
        }

        return result
    }

    // =====================
    // SMART RECOMMENDATIONS
    // =====================

    fun createSmartRecommendationSegment(data: AddPlanData) {
        val city = data.selectedCity
        val selectedDate = data.selectedDate

        if (city == null || selectedDate == null) {
            return
        }

        // Set loading immediately (not postValue) since we're on main thread
        _isLoading.value = true

        // Generate unique title ("Recommendations", "Recommendations 2", etc.)
        val title = generateSegmentTitle(city, selectedDate)

        // Combine selected date with time strings (HH:mm)
        val dateStr = selectedDate.toApiDateString()
        val startDateTimeStr = if (data.startTime != null) {
            "$dateStr ${data.startTime}"
        } else {
            "$dateStr 10:00"  // Default start time
        }
        val endDateTimeStr = if (data.endTime != null) {
            "$dateStr ${data.endTime}"
        } else {
            "$dateStr 18:00"  // Default end time
        }

        // Get favorite tour IDs if available
        val tourActivityIds = data.activityIds.toMutableList()
        // TODO: Add favorite tour IDs from favorites if needed

        createSegmentUseCase.on(
            params = CreateSegmentUseCase.Params(
                tripHash = _tripHash,
                title = title,
                cityId = city.id,
                startDate = startDateTimeStr,
                endDate = endDateTimeStr,
                // coordinate is null - don't send when cityId is present
                adults = data.travelers,
                children = 0,
                activityFreeText = data.smartCategoriesAsString,  // Use smart categories
                activityIds = tourActivityIds,
                smartRecommendation = true,
                accommodation = data.startingPointAccommodation  // Starting point (Google Place)
            ),
            success = {
                waitForSegmentGeneration()
            },
            error = { errorModel ->
                _isLoading.value = false
                _error.value = errorModel.errorDesc
            }
        )
    }

    private fun waitForSegmentGeneration() {
        waitForGenerationUseCase.on(
            params = WaitForGenerationUseCase.Params(_tripHash),
            success = { timeline ->
                processTimeline(timeline)
                _isLoading.value = false
            },
            error = {
                _isLoading.value = false
                // Still refresh to show partial results
                refreshTimeline()
            }
        )
    }

    private fun generateSegmentTitle(city: City, date: Date): String {
        val baseTitle = getLanguageForKey(LanguageConst.RECOMMENDATIONS)
        val dateStr = date.toApiDateString()

        val existingCount = _timeline.value?.tripProfile?.segments
            ?.filter { segment ->
                (segment.segmentType == SegmentType.ITINERARY || segment.segmentType == "itinerary") &&
                        segment.startDate?.startsWith(dateStr) == true &&
                        segment.cityId == city.id &&
                        segment.title?.startsWith(baseTitle) == true
            }
            ?.size ?: 0

        return if (existingCount == 0) {
            baseTitle
        } else {
            "$baseTitle ${existingCount + 1}"
        }
    }

    // =====================
    // DELETE OPERATIONS
    // =====================

    fun deleteSegment(segmentIndex: Int) {
        _isLoading.value = true

        deleteSegmentUseCase.on(
            params = DeleteSegmentUseCase.Params(_tripHash, segmentIndex),
            success = {
                refreshTimeline()
                _isLoading.value = false
            },
            error = { errorModel ->
                _error.value = errorModel.errorDesc
                _isLoading.value = false
            }
        )
    }

    fun deleteStep(stepId: Int) {
        _isLoading.value = true

        deleteStepUseCase.on(
            params = DeleteStepUseCase.Params(stepId),
            success = {
                refreshTimeline()
                _isLoading.value = false
            },
            error = { errorModel ->
                _error.value = errorModel.errorDesc
                _isLoading.value = false
            }
        )
    }

    /**
     * Delete a step using the TimelineStep object
     */
    fun deleteStep(step: com.tripian.one.api.timeline.model.TimelineStep) {
        deleteStep(step.id)
    }

    /**
     * Update step time
     * @param stepId The step ID to update
     * @param startTime New start time in HH:mm format
     * @param endTime New end time in HH:mm format
     */
    fun updateStepTime(stepId: Int, startTime: String?, endTime: String?) {
        if (startTime == null && endTime == null) return

        _isLoading.value = true

        // API expects time only in HH:mm format (not full datetime)
        updateStepTimeUseCase.on(
            params = UpdateStepTimeUseCase.Params(
                stepId = stepId,
                startTime = startTime,
                endTime = endTime
            ),
            success = {
                // Step updated successfully, refresh timeline
                refreshTimeline()
            },
            error = { errorModel ->
                _isLoading.value = false
                _error.value = errorModel.errorDesc
            }
        )
    }

    /**
     * Show change time picker for a step
     * This will be handled by the activity to show TimePickerBottomSheet
     */
    private val _showChangeTimePickerStep =
        MutableLiveData<com.tripian.one.api.timeline.model.TimelineStep?>()
    val showChangeTimePickerStep: LiveData<com.tripian.one.api.timeline.model.TimelineStep?> =
        _showChangeTimePickerStep

    fun showStepChangeTimePicker(step: com.tripian.one.api.timeline.model.TimelineStep) {
        _showChangeTimePickerStep.value = step
    }

    fun clearChangeTimePickerStep() {
        _showChangeTimePickerStep.value = null
    }

    // =====================
    // UI ACTIONS
    // =====================

    fun toggleMapMode() {
        val newMapMode = !(_isMapMode.value ?: false)
        _isMapMode.value = newMapMode

        // Generate map steps when entering map mode
        if (newMapMode) {
            updateMapSteps()
        }
    }

    fun showAddPlan() {
        _showAddPlanSheet.value = true
    }

    fun hideAddPlan() {
        _showAddPlanSheet.value = false
    }

    fun onAddPlanComplete(data: AddPlanData) {
        _showAddPlanSheet.value = false
        when (data.mode) {
            AddPlanMode.SMART, AddPlanMode.SMART_RECOMMENDATIONS -> createSmartRecommendationSegment(
                data
            )

            AddPlanMode.MANUAL -> {
                if (data.selectedPoi != null) {
                    createManualPoiStep(data)
                } else {
                    // Need to select POI first
                    _launchPoiSelection.value = data
                }
            }

            AddPlanMode.NONE -> {
                // No mode selected, ignore
            }
        }
    }

    fun selectCity(city: City) {
        _selectedCity.value = city
        updateDisplayItems()
    }

    fun showNearMePois() {
        // TODO: Implement Near Me POI fetching based on user location
        // This would typically:
        // 1. Get user's current location
        // 2. Check if within 50km of selected city
        // 3. Fetch POIs near user's location
    }

    fun clearPoiSelectionTrigger() {
        _launchPoiSelection.value = null
    }

    // =====================
    // SDK CALLBACKS - Host App Communication
    // =====================

    /**
     * Called when user taps on an activity card.
     * Forwards activity detail request to host app.
     *
     * @param activityId ID of the tapped activity
     */
    fun onActivityDetailRequested(activityId: String) {
        TRPCore.notifyActivityDetailRequested(activityId)
    }

    /**
     * Called when user taps "Reserve" or "Book" button.
     * Forwards reservation request to host app.
     *
     * @param activityId ID of the activity to be reserved
     */
    fun onActivityReservationRequested(activityId: String) {
        TRPCore.notifyActivityReservationRequested(activityId)
    }

    /**
     * Called when SDK is dismissed (back pressed).
     * Forwards dismissed notification to host app.
     */
    fun onSDKDismissed() {
        TRPCore.notifySDKDismissed()
    }

    // =====================
    // MAP INTEGRATION
    // =====================

    private fun updateMapSteps() {
        val items = _displayItems.value ?: return
        val mapSteps = mutableListOf<MapStep>()
        var position = 1

        items.forEach { item ->
            when (item) {
                is TimelineDisplayItem.Recommendations -> {
                    item.steps.forEach { step ->
                        step.poi?.let { poi ->
                            poi.coordinate?.let { coord ->
                                mapSteps.add(
                                    MapStep().apply {
                                        group = "step"
                                        poiId = poi.id ?: ""
                                        name = poi.name ?: ""
                                        coordinate =
                                            com.tripian.one.api.pois.model.Coordinate().apply {
                                                lat = coord.lat
                                                lng = coord.lng
                                            }
                                        // No icon, only show order label
                                        markerIcon = -1
                                        this.position = position++
                                        isOffer = false
                                    }
                                )
                            }
                        }
                    }
                }

                is TimelineDisplayItem.BookedActivity -> {
                    // Get coordinate from additionalData for booked/reserved activities
                    item.segment.additionalData?.coordinate?.let { coord ->
                        mapSteps.add(
                            MapStep().apply {
                                group = "booked"
                                poiId = item.segment.additionalData?.activityId ?: "booked_${item.segmentIndex}"
                                name = item.segment.additionalData?.title ?: item.segment.title ?: ""
                                coordinate = com.tripian.one.api.pois.model.Coordinate().apply {
                                    lat = coord.lat
                                    lng = coord.lng
                                }
                                // No icon, only show order label
                                markerIcon = -1
                                this.position = position++
                                isOffer = false
                            }
                        )
                    }
                }

                is TimelineDisplayItem.ManualPoi -> {
                    item.step.poi?.let { poi ->
                        poi.coordinate?.let { coord ->
                            mapSteps.add(
                                MapStep().apply {
                                    group = "manual"
                                    poiId = poi.id ?: ""
                                    name = poi.name ?: ""
                                    coordinate = com.tripian.one.api.pois.model.Coordinate().apply {
                                        lat = coord.lat
                                        lng = coord.lng
                                    }
                                    // No icon, only show order label
                                    markerIcon = -1
                                    this.position = position++
                                    isOffer = false
                                }
                            )
                        }
                    }
                }

                else -> {}
            }
        }

        _mapSteps.value = mapSteps

        // Also update map bottom items
        updateMapBottomItems()
    }

    /**
     * Updates the map bottom items for horizontal list display.
     * Converts TimelineDisplayItems to MapBottomItem format.
     */
    private fun updateMapBottomItems() {
        val items = _displayItems.value ?: return
        val bottomItems = mutableListOf<MapBottomItem>()
        var order = 1

        // Date formatters for display
        val inputDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val outputTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        items.forEach { item ->
            when (item) {
                is TimelineDisplayItem.Recommendations -> {
                    item.steps.forEach { step ->
                        val dateTime = step.startDateTimes?.let {
                            try {
                                inputDateFormat.parse(it)
                            } catch (e: Exception) {
                                null
                            }
                        }

                        bottomItems.add(
                            MapBottomItem(
                                id = step.poi?.id ?: "step_${step.id}",
                                order = order++,
                                title = step.poi?.name ?: "",
                                imageUrl = step.poi?.image?.url,
                                date = dateTime?.let { outputDateFormat.format(it) },
                                time = dateTime?.let { outputTimeFormat.format(it) },
                                type = "step",
                                stepType = step.stepType  // "poi" or "activity"
                            )
                        )
                    }
                }

                is TimelineDisplayItem.BookedActivity -> {
                    val data = item.segment.additionalData
                    val dateTime = data?.startDatetime?.let {
                        try {
                            inputDateFormat.parse(it)
                        } catch (e: Exception) {
                            null
                        }
                    }

                    bottomItems.add(
                        MapBottomItem(
                            id = data?.activityId ?: "booked_${item.segmentIndex}",
                            order = order++,
                            title = data?.title ?: item.segment.title ?: "",
                            imageUrl = data?.imageUrl,
                            date = dateTime?.let { outputDateFormat.format(it) },
                            time = dateTime?.let { outputTimeFormat.format(it) },
                            type = if (item.isReserved) "reserved" else "booked"
                        )
                    )
                }

                is TimelineDisplayItem.ManualPoi -> {
                    val step = item.step
                    val dateTime = step.startDateTimes?.let {
                        try {
                            inputDateFormat.parse(it)
                        } catch (e: Exception) {
                            null
                        }
                    }

                    bottomItems.add(
                        MapBottomItem(
                            id = step.poi?.id ?: "manual_${step.id}",
                            order = order++,
                            title = step.poi?.name ?: "",
                            imageUrl = step.poi?.image?.url,
                            date = dateTime?.let { outputDateFormat.format(it) },
                            time = dateTime?.let { outputTimeFormat.format(it) },
                            type = "manual"
                        )
                    )
                }

                else -> {
                    // Skip headers, footers, empty states
                }
            }
        }

        _mapBottomItems.value = bottomItems
    }

    // =====================
    // MANUAL POI
    // =====================

    private fun createManualPoiStep(data: AddPlanData) {
        // TODO: Implement manual POI step creation
        // This would call an API to add a manual step to the timeline
        _isLoading.value = true

        // For now, just refresh the timeline
        refreshTimeline()
        _isLoading.value = false
    }

    // =====================
    // HELPERS
    // =====================

    fun hasSingleCity(): Boolean = (_cities.value?.size ?: 0) <= 1

    fun getSelectedCity(): City? = _cities.value?.firstOrNull()

    fun getItinerary(): ItineraryWithActivities? = itinerary

    /**
     * Returns favorites that haven't been added as reserved_activity yet
     * Used when opening SavedPlans screen
     */
    fun getFilteredFavorites(): List<SegmentFavoriteItem> {
        val favourites = itinerary?.favouriteItems ?: return emptyList()
        val timeline = _timeline.value

        // Get activityIds of reserved_activity segments from timeline
        val reservedActivityIds = timeline?.tripProfile?.segments
            ?.filter { it.segmentType == SegmentType.RESERVED_ACTIVITY }
            ?.mapNotNull { it.additionalData?.activityId }
            ?.toSet() ?: emptySet()

        // Return only favourites that are NOT in timeline as reserved_activity
        return favourites.filter { favourite ->
            favourite.activityId !in reservedActivityIds
        }
    }

    fun getSelectedDate(): Date? {
        val days = _availableDays.value ?: return null
        val index = _selectedDayIndex.value ?: 0
        return if (index < days.size) days[index] else null
    }

    fun getBookedActivities(): List<TimelineSegment> {
        return _timeline.value?.tripProfile?.segments
            ?.filter {
                it.segmentType == SegmentType.BOOKED_ACTIVITY ||
                        it.segmentType == SegmentType.RESERVED_ACTIVITY
            } ?: emptyList()
    }

    fun isTimelineGenerated(): Boolean {
        return _timeline.value.isGenerated()
    }

    // =====================
    // ROUTE CALCULATION
    // =====================

    /**
     * Calculate routes for a Recommendations segment.
     * Routes are cached by segmentIndex to avoid redundant API calls.
     *
     * @param recommendations The Recommendations item to calculate routes for
     */
    fun calculateRoutesForRecommendations(recommendations: TimelineDisplayItem.Recommendations) {
        val segmentIndex = recommendations.segmentIndex ?: return

        // Skip if already cached
        if (_routeInfoCache.containsKey(segmentIndex)) {
            updateRecommendationsWithRouteInfo(segmentIndex)
            return
        }

        // Skip if no steps to calculate routes between
        if (recommendations.steps.isEmpty()) return

        getTimelineStepRoutesUseCase.on(
            params = GetTimelineStepRoutesUseCase.Params(
                startingPointCoordinate = recommendations.startingPointCoordinate,
                steps = recommendations.steps
            ),
            success = { routeInfoList ->
                // Cache the results
                _routeInfoCache[segmentIndex] = routeInfoList

                // Update display items with route info
                updateRecommendationsWithRouteInfo(segmentIndex)
            },
            error = {
                // Silently fail - steps will still be displayed without route info
            }
        )
    }

    /**
     * Updates the display items with route info from cache.
     * Finds the Recommendations item with matching segmentIndex and updates its routeInfoList.
     */
    private fun updateRecommendationsWithRouteInfo(segmentIndex: Int) {
        val routeInfoList = _routeInfoCache[segmentIndex] ?: return
        val currentItems = _displayItems.value?.toMutableList() ?: return

        // Find and update the Recommendations item
        val updatedItems = currentItems.map { item ->
            if (item is TimelineDisplayItem.Recommendations && item.segmentIndex == segmentIndex) {
                item.copy(routeInfoList = routeInfoList)
            } else {
                item
            }
        }

        _displayItems.value = updatedItems

        // Notify UI that route info was updated
        _routeInfoUpdated.value = segmentIndex
    }

    /**
     * Clears the route info update notification.
     * Should be called after UI has processed the update.
     */
    fun clearRouteInfoUpdate() {
        _routeInfoUpdated.value = null
    }

    /**
     * Clears the route info cache.
     * Called when timeline is refreshed to ensure fresh data.
     */
    private fun clearRouteInfoCache() {
        _routeInfoCache.clear()
    }

    companion object {
        const val ARG_TRIP_HASH = "tripHash"
    }
}
