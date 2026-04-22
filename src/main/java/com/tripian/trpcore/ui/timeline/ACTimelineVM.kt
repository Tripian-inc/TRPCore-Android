package com.tripian.trpcore.ui.timeline

import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.mapbox.geojson.Point
import com.tripian.one.api.cities.model.City
import com.tripian.one.api.cities.model.CityResolveData
import com.tripian.one.api.pois.model.Coordinate
import com.tripian.one.api.timeline.model.SegmentType
import com.tripian.one.api.timeline.model.Timeline
import com.tripian.one.api.timeline.model.TimelinePlan
import com.tripian.one.api.timeline.model.TimelineSegment
import com.tripian.one.api.timeline.model.isGenerated
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.domain.DoLightLogin
import com.tripian.trpcore.domain.model.MapStep
import com.tripian.trpcore.domain.model.itinerary.ItineraryWithActivities
import com.tripian.trpcore.domain.model.itinerary.SegmentDestinationItem
import com.tripian.trpcore.domain.model.itinerary.SegmentFavoriteItem
import com.tripian.trpcore.domain.model.timeline.AddPlanData
import com.tripian.trpcore.domain.model.timeline.AddPlanMode
import com.tripian.trpcore.domain.model.timeline.MapMarkersMode
import com.tripian.trpcore.domain.model.timeline.StepRouteInfo
import com.tripian.trpcore.domain.model.timeline.TimelineDisplayItem
import com.tripian.trpcore.domain.model.timeline.TransitionInfo
import com.tripian.trpcore.domain.model.timeline.generateDateRange
import com.tripian.trpcore.domain.model.timeline.toApiDateString
import com.tripian.trpcore.domain.model.timeline.toDate
import com.tripian.trpcore.domain.usecase.timeline.CreateSegmentUseCase
import com.tripian.trpcore.domain.usecase.timeline.CreateTimelineUseCase
import com.tripian.trpcore.domain.usecase.timeline.DeleteSegmentUseCase
import com.tripian.trpcore.domain.usecase.timeline.DeleteStepUseCase
import com.tripian.trpcore.domain.usecase.timeline.FetchTimelineUseCase
import com.tripian.trpcore.domain.usecase.timeline.GetTimelineStepRoutesUseCase
import com.tripian.trpcore.domain.usecase.timeline.ResolveCitiesUseCase
import com.tripian.trpcore.domain.usecase.timeline.UpdateStepTimeUseCase
import com.tripian.trpcore.domain.usecase.timeline.WaitForGenerationUseCase
import com.tripian.trpcore.domain.usecase.timeline.sync.AddMissingBookedActivitiesUseCase
import com.tripian.trpcore.domain.usecase.timeline.sync.DetectReservedToBookedTransitionUseCase
import com.tripian.trpcore.domain.usecase.timeline.sync.RemoveSegmentsForDeletedCitiesUseCase
import com.tripian.trpcore.domain.usecase.timeline.sync.ResolveCityIdsForActivitiesUseCase
import com.tripian.trpcore.domain.usecase.timeline.sync.SyncReservedToBookedUseCase
import com.tripian.trpcore.domain.usecase.timeline.sync.UpdateDateRangeUseCase
import com.tripian.trpcore.repository.CityResolveResult
import com.tripian.trpcore.ui.timeline.adapter.MapBottomItem
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.Preferences
import com.tripian.trpcore.util.extensions.hideLoading
import com.tripian.trpcore.util.extensions.showLoading
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
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
    private val resolveCitiesUseCase: ResolveCitiesUseCase,
    private val tripRepository: com.tripian.trpcore.repository.TripRepository,
    private val preferences: Preferences,
    // Timeline Sync UseCases (iOS Guide Implementation)
    private val resolveCityIdsForActivitiesUseCase: ResolveCityIdsForActivitiesUseCase,
    private val detectReservedToBookedTransitionUseCase: DetectReservedToBookedTransitionUseCase,
    private val syncReservedToBookedUseCase: SyncReservedToBookedUseCase,
    private val addMissingBookedActivitiesUseCase: AddMissingBookedActivitiesUseCase,
    private val updateDateRangeUseCase: UpdateDateRangeUseCase,
    private val removeSegmentsForDeletedCitiesUseCase: RemoveSegmentsForDeletedCitiesUseCase
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

    // Main View button visibility (for multi-city map mode)
    private val _showMainViewButton = MutableLiveData(false)
    val showMainViewButton: LiveData<Boolean> = _showMainViewButton

    // City markers for multi-city overview mode
    private val _cityMarkers = MutableLiveData<List<MapStep>>()
    val cityMarkers: LiveData<List<MapStep>> = _cityMarkers

    // Map markers mode (city markers vs step markers)
    private val _mapMarkersMode = MutableLiveData(MapMarkersMode.STEP_MARKERS)
    val mapMarkersMode: LiveData<MapMarkersMode> = _mapMarkersMode

    // Route info cache - maps segmentIndex to route info list
    private val _routeInfoCache = mutableMapOf<Int, List<StepRouteInfo>>()

    // LiveData to notify UI when route info is updated for a segment
    private val _routeInfoUpdated = MutableLiveData<Int?>()
    val routeInfoUpdated: LiveData<Int?> = _routeInfoUpdated

    // No cities available state - shown when all destinations have invalid cityId
    private val _noCitiesAvailable = MutableLiveData<Boolean>()
    val noCitiesAvailable: LiveData<Boolean> = _noCitiesAvailable

    // Partial unavailable alert event - contains list of invalid city names
    private val _showPartialUnavailableAlert = MutableLiveData<List<String>?>()
    val showPartialUnavailableAlert: LiveData<List<String>?> = _showPartialUnavailableAlert

    // Onboarding
    private val _showOnboarding = MutableLiveData<Boolean>()
    val showOnboarding: LiveData<Boolean> = _showOnboarding
    private var onboardingCompleted = false

    // Scroll to new segment event - contains plan.id to scroll to
    private val _scrollToNewSegmentPlanId = MutableLiveData<String?>()
    val scrollToNewSegmentPlanId: LiveData<String?> = _scrollToNewSegmentPlanId

    // Track existing plan IDs before creating new segment
    private var existingPlanIds: Set<String> = emptySet()

    // =====================
    // STATE
    // =====================

    private var _tripHash: String = ""
    val tripHash: String get() = _tripHash
    private var itinerary: ItineraryWithActivities? = null
    private var uniqueId: String? = null
    private var isLoggedIn: Boolean = false
    private var isLoginInProgress: Boolean = false
    private var hasMultipleCitiesInSelectedDay: Boolean = false

    // City name to ID mapping - maps resolved city names (lowercase) to our cityIds
    // Used to convert host app cityIds to our system's cityIds
    private val cityNameToIdMap = mutableMapOf<String, Int>()

    // Sync operations flag - ensures sync only runs once after initial fetch
    private var syncOperationsCompleted = false

    // Multi-city map mode state
    private var isShowingStepMarkersInMultiCity: Boolean = false
    private var selectedStepId: String? = null

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
            TRPCore.core.appConfig.appLanguage = language
        }

        // Set app currency - Priority: Intent > Preferences > Default (EUR)
        // Supports both ISO 4217 codes (USD, EUR) and locale format (es-MX, en-US)
        val currencyFromIntent = arguments?.getString(TRPCore.EXTRA_APP_CURRENCY)
        val currencyFromPrefs = TRPCore.core.miscRepository.getSavedCurrency()

        val currencyInput = when {
            !currencyFromIntent.isNullOrEmpty() -> currencyFromIntent
            currencyFromPrefs.isNotEmpty() -> currencyFromPrefs
            else -> "EUR"
        }

        val currencyCode = com.tripian.trpcore.util.CurrencyUtil.resolveCurrencyCode(currencyInput)
        TRPCore.core.appConfig.appCurrency = currencyCode
        // Also update TRPOne to use correct currency for API calls
        TRPCore.core.trpRest.setCurrency(currencyCode)

        // Also check legacy ARG_TRIP_HASH
        if (_tripHash.isEmpty()) {
            _tripHash = arguments?.getString(ARG_TRIP_HASH) ?: ""
        }

        // Start LightLogin immediately in background
        // This runs in parallel with language loading and onboarding
        performLightLoginInBackground()

        // Wait for languages to be loaded before proceeding with timeline operations
        // This ensures all UI texts are available
        ensureLanguagesLoadedThenProceed()
    }

    /**
     * Ensures languages are loaded before proceeding with timeline operations.
     * Shows loading indicator while waiting.
     */
    private fun ensureLanguagesLoadedThenProceed() {
        showLoading()

        miscRepository.waitForLanguagesLoaded()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { _ ->
                    // Languages loaded, now proceed with timeline operations
                    proceedAfterLanguagesLoaded()
                },
                { error ->
                    // Even if language fetch fails, proceed (will use cached/fallback)
                    proceedAfterLanguagesLoaded()
                }
            )
    }

    /**
     * Called after languages are loaded.
     * Sets language, checks onboarding, then resolves cities and starts login flow.
     */
    private fun proceedAfterLanguagesLoaded() {
        // Apply language change after languages are loaded
        val language = arguments?.getString(TRPCore.EXTRA_APP_LANGUAGE)
        if (!language.isNullOrEmpty()) {
            miscRepository.changeLanguage(language)
        }

        // Hide loading before showing onboarding
        hideLoading()

        // Check and show onboarding if needed
        // onOnboardingComplete() will be called to continue with city resolution
        checkAndShowOnboarding()
    }

    /**
     * Resolves destination cities and proceeds with timeline operations.
     * Called after login is complete (login runs in background when ACTimeline opens).
     * If city resolution fails (cityId=0), shows error and closes SDK.
     */
    private fun resolveDestinationCitiesAndProceed() {
        val destinationItems = itinerary?.destinationItems

        // If no destination items, proceed directly (will use tripHash)
        if (destinationItems.isNullOrEmpty()) {
            proceedWithTimelineOperations()
            return
        }

        showLoading()

        // Step 1: Try to find cities from cache
        val resolvedCities = mutableListOf<City>()
        val unresolvedCoordinates = mutableListOf<Coordinate>()
        val unresolvedCityNames = mutableListOf<String>()

        destinationItems.forEach { item ->
            // NOTE: Do NOT use item.cityId - host app sends garbage/invalid cityIds
            // Only use coordinates and city name for resolution
            val city = item.getCoordinateObject()?.let { coord ->
                tripRepository.findCityByCoordinate(coord.lat, coord.lng)
            } ?: tripRepository.findCityByName(item.title, item.countryName)

            if (city != null) {
                resolvedCities.add(city)
            } else {
                // Not found in cache, need API resolution
                item.getCoordinateObject()?.let { coord ->
                    unresolvedCoordinates.add(Coordinate().apply {
                        lat = coord.lat
                        lng = coord.lng
                    })
                }
                item.title?.let { unresolvedCityNames.add(it) }
            }
        }

        // Step 2: If all cities resolved from cache, proceed
        if (unresolvedCoordinates.isEmpty()) {
            val uniqueCities = resolvedCities.distinctBy { it.id }
            if (uniqueCities.isNotEmpty()) {
                _cities.value = uniqueCities
                // Update itinerary.destinationItems with resolved cityIds
                updateItineraryWithResolvedCities(resolvedCities)
            }
            proceedWithTimelineOperations()
            return
        }

        // Step 3: Resolve remaining cities via API (blocking before timeline creation)
        tripRepository.resolveCitiesByCoordinates(unresolvedCoordinates)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result ->
                    handleCityResolveResult(result, resolvedCities, unresolvedCityNames)
                },
                { error ->
                    // API failed
                    if (resolvedCities.isNotEmpty()) {
                        // Use cached cities and continue
                        _cities.value = resolvedCities.distinctBy { it.id }
                        // Update itinerary.destinationItems with cached cityIds
                        updateItineraryWithResolvedCities(resolvedCities)
                        proceedWithTimelineOperations()
                    } else if (_tripHash.isNotEmpty()) {
                        // EXISTING TRIP: API failed but we have tripHash - continue anyway
                        // Timeline will be fetched, city data comes from API response
                        val warningMsg = getLanguageForKey(LanguageConst.CITY_NOT_SUPPORTED)
                            .replace("%s", unresolvedCityNames.joinToString(", "))
                        showAlert(AlertType.WARNING, warningMsg)
                        proceedWithTimelineOperations()
                    } else {
                        // NEW TRIP: No cities at all - fatal error, close SDK
                        hideLoading()
                        val errorMsg = getLanguageForKey(LanguageConst.CITY_NOT_SUPPORTED)
                            .replace("%s", unresolvedCityNames.joinToString(", "))
                        TRPCore.notifyError(errorMsg)
                        TRPCore.closeSDK()
                    }
                }
            )
    }

    /**
     * Handles city resolve result and decides whether to proceed or show error.
     */
    private fun handleCityResolveResult(
        result: CityResolveResult,
        cachedCities: MutableList<City>,
        fallbackCityNames: List<String>
    ) {
        when (result) {
            is CityResolveResult.Success -> {
                cachedCities.addAll(result.cities)
                val uniqueCities = cachedCities.distinctBy { it.id }
                _cities.value = uniqueCities
                // Update itinerary.destinationItems with resolved cityIds
                updateItineraryWithResolvedCities(cachedCities)
                proceedWithTimelineOperations()
            }
            is CityResolveResult.PartialSuccess -> {
                // Some cities resolved, some not found - show warning and continue
                cachedCities.addAll(result.cities)
                val uniqueCities = cachedCities.distinctBy { it.id }
                _cities.value = uniqueCities
                // Update itinerary.destinationItems with resolved cityIds
                updateItineraryWithResolvedCities(cachedCities)

                // Show warning for unsupported cities
                val warningMsg = getLanguageForKey(LanguageConst.CITY_NOT_SUPPORTED)
                    .replace("%s", result.unresolvedCityNames.joinToString(", "))
                showAlert(AlertType.WARNING, warningMsg)

                proceedWithTimelineOperations()
            }
            is CityResolveResult.AllFailed -> {
                // No cities could be resolved from API
                if (cachedCities.isNotEmpty()) {
                    // Use cached cities and continue
                    _cities.value = cachedCities.distinctBy { it.id }
                    // Update itinerary.destinationItems with resolved cityIds
                    updateItineraryWithResolvedCities(cachedCities)
                    proceedWithTimelineOperations()
                } else if (_tripHash.isNotEmpty()) {
                    // EXISTING TRIP: Show warning but continue with fetchTimeline
                    // User added new destination that's not supported - warn but proceed
                    val warningMsg = getLanguageForKey(LanguageConst.CITY_NOT_SUPPORTED)
                        .replace("%s", fallbackCityNames.joinToString(", "))
                    showAlert(AlertType.WARNING, warningMsg)
                    proceedWithTimelineOperations()
                } else {
                    // NEW TRIP: No cities at all - show NoCityView (blocking)
                    hideLoading()
                    _noCitiesAvailable.value = true
                }
            }
        }
    }

    /**
     * Updates itinerary.destinationItems with resolved cityIds.
     * This ensures that when resolveCitiesAndCreateTimeline() is called later,
     * it will find valid cityIds instead of null/0.
     */
    private fun updateItineraryWithResolvedCities(resolvedCities: List<City>) {
        val currentItinerary = itinerary ?: return

        val updatedDestinations = currentItinerary.destinationItems.map { item ->
            // Find matching city by coordinate (within 0.01 degree tolerance)
            val matchingCity = item.getCoordinateObject()?.let { coord ->
                resolvedCities.find { city ->
                    city.coordinate?.let { c ->
                        kotlin.math.abs(c.lat - coord.lat) < 0.01 &&
                        kotlin.math.abs(c.lng - coord.lng) < 0.01
                    } ?: false
                }
            } ?: resolvedCities.find { it.name == item.title }

            if (matchingCity != null) {
                item.copy(cityId = matchingCity.id)
            } else {
                item
            }
        }

        itinerary = currentItinerary.copy(destinationItems = updatedDestinations)

        // Build cityName -> cityId mapping for favorites/tripItems city resolution
        // This allows us to convert host app cityIds to our system's cityIds
        resolvedCities.forEach { city ->
            city.name?.lowercase()?.trim()?.let { name ->
                cityNameToIdMap[name] = city.id
            }
        }

        android.util.Log.d("TIMELINE_DEBUG", "Updated itinerary with ${resolvedCities.size} resolved cityIds, cityNameToIdMap size: ${cityNameToIdMap.size}")
    }

    /**
     * Performs light login in background immediately when ACTimeline opens.
     * This runs in parallel with language loading and onboarding.
     * Does NOT proceed with timeline operations - that's done after onboarding completes.
     */
    private fun performLightLoginInBackground() {
        if (isLoginInProgress || isLoggedIn) return

        isLoginInProgress = true
        _error.value = null

        doLightLogin.on(
            params = DoLightLogin.Params(
                uniqueId = uniqueId
            ),
            success = { response ->
                isLoggedIn = true
                isLoginInProgress = false
                android.util.Log.d("TIMELINE_DEBUG", "LightLogin completed successfully")
            },
            error = { errorModel ->
                isLoginInProgress = false
                _error.value = errorModel.errorDesc ?: "Login failed"
                TRPCore.notifyError(errorModel.errorDesc ?: "Login failed")
                android.util.Log.e("TIMELINE_DEBUG", "LightLogin failed: ${errorModel.errorDesc}")
            }
        )
    }

    /**
     * Waits for light login to complete, then executes the callback.
     * If already logged in, executes immediately.
     * If login failed, still executes (error already shown).
     */
    private fun waitForLoginThenProceed(onLoginComplete: () -> Unit) {
        if (isLoggedIn) {
            onLoginComplete()
            return
        }

        if (!isLoginInProgress) {
            // Login not started or failed, try again
            performLightLoginInBackground()
        }

        // Poll for login completion
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (isLoggedIn || !isLoginInProgress) {
                onLoginComplete()
            } else {
                waitForLoginThenProceed(onLoginComplete)
            }
        }, 100)
    }

    /**
     * Continues with timeline operations after login.
     * Fetches if tripHash exists, resolves cities and creates from itinerary otherwise.
     */
    private fun proceedWithTimelineOperations() {
        // Update saved plans count from itinerary favourites
        updateSavedPlansCount()

        when {
            _tripHash.isNotEmpty() -> {
                // Existing trip - just fetch it
                fetchTimeline()
            }

            itinerary != null -> {
                // New timeline - FIRST resolve cityIds from coordinates
                resolveCitiesAndCreateTimeline()
            }

            else -> {
                hideLoading()
                _error.value = "No trip hash or itinerary provided"
            }
        }
    }

    /**
     * Resolves city IDs from destination coordinates, then validates and creates timeline.
     * Flow:
     * 1. Extract coordinates from destinations
     * 2. Call cities/resolve API
     * 3. Update destinations with resolved cityIds
     * 4. Validate and proceed with timeline creation
     */
    private fun resolveCitiesAndCreateTimeline() {
        // IMPORTANT: Clear cityIds from host app - they may be invalid/garbage
        // We will resolve fresh cityIds from coordinates via cities/resolve API
        val destinations = itinerary!!.destinationItems.map { it.copy(cityId = null) }

        // Extract coordinates from destinations
        val coordinates = destinations.mapNotNull { destination ->
            destination.getCoordinateObject()?.let { coord ->
                Coordinate().apply {
                    lat = coord.lat
                    lng = coord.lng
                }
            }
        }

        if (coordinates.isEmpty()) {
            hideLoading()
            _error.value = "No valid coordinates found"
            return
        }

        showLoading()

        // Call cities/resolve API
        resolveCitiesUseCase.on(
            params = ResolveCitiesUseCase.Params(coordinates),
            success = { resolvedCities ->
                // Update destinations with resolved cityIds
                val updatedDestinations = updateDestinationsWithCityIds(destinations, resolvedCities)

                // Update itinerary with resolved cityIds
                itinerary = itinerary!!.copy(destinationItems = updatedDestinations)

                // Now validate and proceed
                validateAndCreateTimeline(updatedDestinations)
            },
            error = { errorModel ->
                hideLoading()
                _error.value = errorModel.errorDesc ?: "City resolve failed"
                TRPCore.notifyError(errorModel.errorDesc ?: "City resolve failed")
            }
        )
    }

    /**
     * Match resolved cityIds back to destinations.
     * API returns cities in same order as request coordinates.
     */
    private fun updateDestinationsWithCityIds(
        destinations: List<SegmentDestinationItem>,
        resolvedCities: List<CityResolveData>
    ): List<SegmentDestinationItem> {
        return destinations.mapIndexed { index, destination ->
            // API returns cities in same order as request coordinates
            val resolvedCity = resolvedCities.getOrNull(index)
            destination.copy(cityId = resolvedCity?.cityId)
        }
    }

    /**
     * Validates destinations and creates timeline with valid ones.
     */
    private fun validateAndCreateTimeline(destinations: List<SegmentDestinationItem>) {
        val (validDestinations, invalidDestinations) = validateDestinations(destinations)

        when {
            // Case 1: ALL cities invalid - show NoCityView
            validDestinations.isEmpty() -> {
                hideLoading()
                _noCitiesAvailable.value = true
            }

            // Case 2: SOME cities invalid - show alert and continue with valid
            invalidDestinations.isNotEmpty() -> {
                val invalidCityNames = invalidDestinations.map { it.title }
                _showPartialUnavailableAlert.value = invalidCityNames
                createTimelineWithValidDestinations(validDestinations)
            }

            // Case 3: ALL cities valid - normal flow
            else -> {
                createTimelineFromItinerary()
            }
        }
    }

    /**
     * Validates destination cityIds.
     * Returns Pair(validDestinations, invalidDestinations)
     */
    private fun validateDestinations(
        destinations: List<SegmentDestinationItem>
    ): Pair<List<SegmentDestinationItem>, List<SegmentDestinationItem>> {
        val valid = destinations.filter { it.cityId != null && it.cityId > 0 }
        val invalid = destinations.filter { it.cityId == null || it.cityId <= 0 }
        return Pair(valid, invalid)
    }

    /**
     * Creates timeline with only valid destinations.
     * Called when some destinations have invalid cityId.
     */
    private fun createTimelineWithValidDestinations(validDestinations: List<SegmentDestinationItem>) {
        val modifiedItinerary = itinerary!!.copy(destinationItems = validDestinations)

        showLoading()
        _error.value = null

        createTimelineUseCase.on(
            params = CreateTimelineUseCase.Params(modifiedItinerary),
            success = { timeline ->
                _tripHash = timeline.tripHash ?: ""
                if (_tripHash.isNotEmpty()) {
                    TRPCore.notifyTimelineCreated(_tripHash)
                    waitForTimelineGeneration()
                } else {
                    processTimeline(timeline)
                    hideLoading()
                }
            },
            error = { errorModel ->
                _error.value = errorModel.errorDesc
                TRPCore.notifyError(errorModel.errorDesc ?: "Timeline creation failed")
                hideLoading()
            }
        )
    }

    /**
     * Clears the partial unavailable alert event.
     * Should be called after the alert is shown to prevent re-showing on configuration change.
     */
    fun clearPartialUnavailableAlert() {
        _showPartialUnavailableAlert.value = null
    }

    /**
     * Clear scroll to new segment event after scrolling is done.
     */
    fun clearScrollToNewSegment() {
        _scrollToNewSegmentPlanId.value = null
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

        showLoading()
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
                    hideLoading()
                }
            },
            error = { errorModel ->
                _error.value = errorModel.errorDesc
                TRPCore.notifyError(errorModel.errorDesc ?: "Timeline creation failed")
                hideLoading()
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
                hideLoading()
            },
            error = { errorModel ->
                hideLoading()
                _error.value = errorModel.errorDesc ?: "Timeline generation failed"
                TRPCore.notifyError(errorModel.errorDesc ?: "Timeline generation failed")
            }
        )
    }

    // =====================
    // FETCH & REFRESH
    // =====================

    fun fetchTimeline() {
        showLoading()
        _error.value = null

        fetchTimelineUseCase.on(
            params = FetchTimelineUseCase.Params(_tripHash),
            success = { timeline ->
                processTimeline(timeline)
                hideLoading()
            },
            error = { errorModel ->
                _error.value = errorModel.errorDesc
                TRPCore.notifyError(errorModel.errorDesc ?: "Timeline fetch failed")
                hideLoading()
            }
        )
    }

    fun refreshTimeline() {
        showLoading()
        // Clear route info cache to ensure fresh calculations
        clearRouteInfoCache()

        fetchTimelineUseCase.on(
            params = FetchTimelineUseCase.Params(_tripHash),
            success = { timeline ->
                processTimeline(timeline)
                hideLoading()
            },
            error = {
                hideLoading()
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

        // iOS Guide: Perform sync operations after initial timeline fetch
        if (!syncOperationsCompleted && itinerary != null) {
            syncOperationsCompleted = true
            performSyncOperations(timeline)
        }
    }

    /**
     * Extract cities from timeline, using cached cities for full City model data.
     * This ensures we have complete City information (including coordinates)
     * by matching cityIds with pre-fetched cities from getCities API.
     *
     * Priority:
     * 1. First, add cities from destinationItems (SDK input - always included)
     * 2. Then, add cities from Timeline (API response)
     * 3. Deduplicate by city ID
     */
    private fun extractCities(timeline: Timeline): List<City> {
        val cities = mutableListOf<City>()
        val addedCityIds = mutableSetOf<Int>()

        // PRIORITY 1: Always include cities from destinationItems (SDK input)
        // NOTE: Do NOT use item.cityId - host app sends garbage/invalid cityIds
        // Only use coordinates and city name for resolution
        itinerary?.destinationItems?.forEach { item ->
            val city = item.getCoordinateObject()?.let { coord ->
                tripRepository.findCityByCoordinate(coord.lat, coord.lng)
            } ?: tripRepository.findCityByName(item.title, item.countryName)

            if (city != null && !addedCityIds.contains(city.id)) {
                // Use destination title as city name (localized by host app)
                city.name = item.title
                cities.add(city)
                addedCityIds.add(city.id)
            }
        }

        // PRIORITY 2: Add cities from Timeline
        val cityIds = mutableSetOf<Int>()
        val cityNames = mutableSetOf<String>()

        // Collect all unique cityIds and names from timeline
        timeline.plans?.forEach { plan ->
            plan.city?.id?.let { if (it != 0) cityIds.add(it) }
            plan.city?.name?.let { cityNames.add(it) }
        }

        timeline.city?.id?.let { if (it != 0) cityIds.add(it) }
        timeline.city?.name?.let { cityNames.add(it) }

        // Also get cityIds from segments
        timeline.tripProfile?.segments?.forEach { segment ->
            segment.cityId?.let { if (it != 0) cityIds.add(it) }
        }

        // Add timeline cities by ID (skip already added)
        cityIds.forEach { cityId ->
            if (!addedCityIds.contains(cityId)) {
                val cachedCity = tripRepository.getCachedCityById(cityId)
                if (cachedCity != null) {
                    cities.add(cachedCity)
                    addedCityIds.add(cachedCity.id)
                } else {
                    // Fallback to timeline city data if not in cache
                    val timelineCity = timeline.plans?.find { it.city?.id == cityId }?.city
                        ?: if (timeline.city?.id == cityId) timeline.city else null
                    timelineCity?.let {
                        cities.add(it)
                        addedCityIds.add(it.id)
                    }
                }
            }
        }

        // Add timeline cities by name (if not already added by ID)
        if (cities.isEmpty() || cities.all { it.id == 0 }) {
            cityNames.forEach { name ->
                val cachedCity = tripRepository.findCityByName(name)
                if (cachedCity != null && !addedCityIds.contains(cachedCity.id)) {
                    cities.add(cachedCity)
                    addedCityIds.add(cachedCity.id)
                }
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
        // Reset selected step when day changes
        selectedStepId = null
        updateDisplayItems()
    }

    private fun updateDisplayItems() {
        val timeline = _timeline.value ?: return
        val days = _availableDays.value ?: return
        val selectedIndex = _selectedDayIndex.value ?: 0

        if (selectedIndex >= days.size) return

        // Preserve existing collapse states (plan.id -> isExpanded)
        val existingExpandStates = _displayItems.value
            ?.filterIsInstance<TimelineDisplayItem.Recommendations>()
            ?.associate { it.plan.id to it.isExpanded }
            ?: emptyMap()

        val selectedDate = days[selectedIndex]
        val items = generateDisplayItemsForDay(timeline, selectedDate)

        // Apply preserved collapse states to new items
        val itemsWithPreservedState = items.map { item ->
            if (item is TimelineDisplayItem.Recommendations) {
                existingExpandStates[item.plan.id]?.let { savedExpanded ->
                    item.copy(isExpanded = savedExpanded)
                } ?: item
            } else {
                item
            }
        }

        _displayItems.value = itemsWithPreservedState

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
                val planId = plan?.id  // Get planId for conflict detection
                when (segmentType) {
                    // Booked Activity (not reserved)
                    SegmentType.BOOKED_ACTIVITY -> {
                        items.add(
                            TimelineDisplayItem.BookedActivity(
                                segment = segment,
                                isReserved = false,
                                segmentIndex = index,
                                city = getCityForSegment(segment, timeline),
                                planId = planId
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
                                city = getCityForSegment(segment, timeline),
                                planId = planId
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

                            // Calculate recommendation index for same city
                            val cityId = segment.cityId
                            val recommendationIndex = items.count { item ->
                                item is TimelineDisplayItem.Recommendations && item.city?.id == cityId
                            } + 1

                            items.add(
                                TimelineDisplayItem.Recommendations(
                                    plan = plan,
                                    steps = steps,
                                    segment = segment,
                                    segmentIndex = index,
                                    cachedCity = getCityForSegment(segment, timeline),
                                    recommendationIndex = recommendationIndex
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
                                    city = getCityForSegment(segment, timeline),
                                    planId = planId
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

        // Detect time conflicts before grouping by city
        val itemsWithConflicts = detectTimeConflicts(items)

        // Group items by city
        return groupItemsByCity(itemsWithConflicts)
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
    // TIME CONFLICT DETECTION
    // =====================

    /**
     * Data class representing a time range for conflict detection.
     */
    private data class TimeRange(
        val startTime: Date,
        val endTime: Date,
        val itemType: String,  // "booked", "manual", "step"
        val itemIndex: Int,    // Index in items list
        val stepId: Int? = null, // Step ID for Recommendations steps
        val planId: String? = null,  // Plan ID for determining older/newer segments
        val segmentType: String? = null  // Segment type for booked_activity special handling
    )

    /**
     * Parses planId from string format.
     * Format can be "12345" or "20123-20124" (takes first part after split)
     */
    private fun parsePlanId(planId: String?): Int {
        if (planId.isNullOrEmpty()) return 0
        val firstPart = planId.split("-").firstOrNull() ?: planId
        return firstPart.toIntOrNull() ?: 0
    }

    /**
     * Determines which segment is older by comparing planIds.
     * Lower planId = older segment
     */
    private fun determineOlderAndNewer(
        range1: TimeRange,
        range2: TimeRange
    ): Pair<TimeRange, TimeRange> {
        val planId1 = parsePlanId(range1.planId)
        val planId2 = parsePlanId(range2.planId)

        return if (planId1 < planId2) {
            Pair(range1, range2)  // range1 is older
        } else {
            Pair(range2, range1)  // range2 is older
        }
    }

    /**
     * Collects all time ranges from display items
     */
    private fun collectTimeRanges(items: List<TimelineDisplayItem>): List<TimeRange> {
        val timeRanges = mutableListOf<TimeRange>()

        items.forEachIndexed { index, item ->
            when (item) {
                is TimelineDisplayItem.BookedActivity -> {
                    val startTime = item.startDateTime?.toDate()
                    val endTime = item.endDateTime?.toDate()
                    if (startTime != null && endTime != null) {
                        timeRanges.add(
                            TimeRange(
                                startTime = startTime,
                                endTime = endTime,
                                itemType = "booked",
                                itemIndex = index,
                                planId = item.planId,
                                segmentType = if (item.isReserved) "reserved_activity" else "booked_activity"
                            )
                        )
                    }
                }

                is TimelineDisplayItem.ManualPoi -> {
                    val startTime = item.startTime
                    val endTime = item.endTime
                    if (startTime != null && endTime != null) {
                        timeRanges.add(
                            TimeRange(
                                startTime = startTime,
                                endTime = endTime,
                                itemType = "manual",
                                itemIndex = index,
                                planId = item.planId,
                                segmentType = "manual_poi"
                            )
                        )
                    }
                }

                is TimelineDisplayItem.Recommendations -> {
                    item.steps.forEach { step ->
                        val startTime = step.startDateTimes?.toDate()
                        val endTime = step.endDateTimes?.toDate()
                        if (startTime != null && endTime != null && step.id != null) {
                            timeRanges.add(
                                TimeRange(
                                    startTime = startTime,
                                    endTime = endTime,
                                    itemType = "step",
                                    itemIndex = index,
                                    stepId = step.id,
                                    planId = item.planId,
                                    segmentType = "itinerary"
                                )
                            )
                        }
                    }
                }

                else -> { /* Ignore */ }
            }
        }

        return timeRanges
    }

    /**
     * Builds transitive conflict groups (Union-Find approach).
     * Example: A-B overlap, B-C overlap → A,B,C in same group
     */
    private fun buildConflictGroups(conflicts: List<Pair<Int, Int>>): List<Set<Int>> {
        val groups = mutableListOf<MutableSet<Int>>()

        conflicts.forEach { (index1, index2) ->
            val existingGroup = groups.find { it.contains(index1) || it.contains(index2) }

            if (existingGroup != null) {
                existingGroup.add(index1)
                existingGroup.add(index2)
            } else {
                groups.add(mutableSetOf(index1, index2))
            }
        }

        // Merge overlapping groups (transitive closure)
        var merged = true
        while (merged) {
            merged = false
            for (i in groups.indices) {
                for (j in (i + 1) until groups.size) {
                    if (groups[i].any { it in groups[j] }) {
                        groups[i].addAll(groups[j])
                        groups.removeAt(j)
                        merged = true
                        break
                    }
                }
                if (merged) break
            }
        }

        return groups.filter { it.size >= 3 }  // Only return groups with 3+ items
    }

    /**
     * Applies 3+ conflict rule with plan-based logic:
     * - ALL conflicting items get visual conflict (red border)
     * - Time Overlap text only for items from DIFFERENT/NEWER plans
     * - Booked activities NEVER show Time Overlap text
     */
    private fun applyThreePlusConflictRules(
        groupIndices: Set<Int>,
        timeRanges: List<TimeRange>,
        visualConflictIndices: MutableSet<Int>,
        visualConflictStepIds: MutableMap<Int, MutableSet<Int>>,
        timeOverlapIndices: MutableSet<Int>,
        timeOverlapStepIds: MutableMap<Int, MutableSet<Int>>
    ) {
        val groupRanges = groupIndices.map { timeRanges[it] }

        // Step 1: Apply visual conflict to ALL items in the group
        groupRanges.forEach { range ->
            visualConflictIndices.add(range.itemIndex)
            // For Recommendations steps, also add to visualConflictStepIds
            if (range.itemType == "step" && range.stepId != null) {
                visualConflictStepIds.getOrPut(range.itemIndex) { mutableSetOf() }.add(range.stepId)
            }
        }

        // Step 2: Determine Time Overlap for non-booked items
        val nonBookedRanges = groupRanges.filter { it.segmentType != "booked_activity" }

        if (nonBookedRanges.isNotEmpty()) {
            // Find the minimum planId (oldest plan)
            val planIds = nonBookedRanges.mapNotNull { it.planId }.distinct()
            val minPlanId = planIds.minOfOrNull { parsePlanId(it) } ?: 0

            // Time Overlap only for items from plans NEWER than the oldest plan
            nonBookedRanges.forEach { range ->
                val rangePlanId = parsePlanId(range.planId)
                if (rangePlanId > minPlanId) {
                    // This item is from a newer plan → Time Overlap
                    if (range.itemType == "step" && range.stepId != null) {
                        timeOverlapStepIds.getOrPut(range.itemIndex) { mutableSetOf() }.add(range.stepId)
                    } else {
                        timeOverlapIndices.add(range.itemIndex)
                    }
                }
                // Items from the oldest plan do NOT get Time Overlap (but already have visual conflict)
            }
        }

        // Booked activities already have visual conflict from Step 1
        // They never get Time Overlap text (handled by NOT being in nonBookedRanges)
    }

    /**
     * Detects time conflicts based on planId-based rules.
     *
     * Rules:
     * 1. 2-item conflicts: Newer segment (higher planId) shows "Time Overlap"
     * 2. Booked activity special: If booked is newer, older segment shows "Time Overlap"
     * 3. 3+ conflicts: Oldest (min planId) gets only visual, others get Time Overlap
     */
    private fun detectTimeConflicts(items: List<TimelineDisplayItem>): List<TimelineDisplayItem> {
        // Phase 1: Collect all time ranges
        val timeRanges = collectTimeRanges(items)

        // Phase 2: Detect all 2-way conflicts
        val twoWayConflicts = mutableListOf<Pair<Int, Int>>()

        for (i in timeRanges.indices) {
            for (j in (i + 1) until timeRanges.size) {
                val range1 = timeRanges[i]
                val range2 = timeRanges[j]

                // Check overlap: start1 < end2 AND start2 < end1
                if (range1.startTime.before(range2.endTime) && range2.startTime.before(range1.endTime)) {
                    twoWayConflicts.add(Pair(i, j))
                }
            }
        }

        // Storage for marking
        val visualConflictIndices = mutableSetOf<Int>()
        val visualConflictStepIds = mutableMapOf<Int, MutableSet<Int>>()  // For Recommendations: ALL conflicting steps
        val timeOverlapIndices = mutableSetOf<Int>()
        val timeOverlapStepIds = mutableMapOf<Int, MutableSet<Int>>()  // For Recommendations: steps with Time Overlap text

        // Phase 3: Build transitive conflict groups (3+ items)
        val conflictGroups = buildConflictGroups(twoWayConflicts)
        val handledByThreePlus = conflictGroups.flatten().toSet()

        // Phase 4: Apply 3+ conflict rules (overrides 2-item rules)
        conflictGroups.forEach { groupIndices ->
            applyThreePlusConflictRules(
                groupIndices,
                timeRanges,
                visualConflictIndices,
                visualConflictStepIds,
                timeOverlapIndices,
                timeOverlapStepIds
            )
        }

        // Phase 5: Handle 2-item conflicts (NOT in 3+ groups)
        twoWayConflicts.forEach { (i, j) ->
            // Skip if already handled by 3+ rule
            if (i in handledByThreePlus || j in handledByThreePlus) return@forEach

            val range1 = timeRanges[i]
            val range2 = timeRanges[j]

            // ALWAYS apply visual conflict to both (regardless of plan)
            visualConflictIndices.add(range1.itemIndex)
            visualConflictIndices.add(range2.itemIndex)

            // For Recommendations steps, also add to visualConflictStepIds
            if (range1.itemType == "step" && range1.stepId != null) {
                visualConflictStepIds.getOrPut(range1.itemIndex) { mutableSetOf() }.add(range1.stepId)
            }
            if (range2.itemType == "step" && range2.stepId != null) {
                visualConflictStepIds.getOrPut(range2.itemIndex) { mutableSetOf() }.add(range2.stepId)
            }

            // Check if both items are from the SAME plan
            if (range1.planId == range2.planId) {
                // Same plan: NO Time Overlap text (only visual conflict already added above)
                return@forEach
            }

            // Different plans: apply Time Overlap rules
            val (olderRange, newerRange) = determineOlderAndNewer(range1, range2)

            // Booked activity special rule
            if (newerRange.segmentType == "booked_activity") {
                // Booked is newer → older segment shows Time Overlap
                if (olderRange.itemType == "step" && olderRange.stepId != null) {
                    timeOverlapStepIds.getOrPut(olderRange.itemIndex) { mutableSetOf() }.add(olderRange.stepId)
                } else {
                    timeOverlapIndices.add(olderRange.itemIndex)
                }
            } else if (olderRange.segmentType == "booked_activity") {
                // Booked is older → newer segment shows Time Overlap (normal rule)
                if (newerRange.itemType == "step" && newerRange.stepId != null) {
                    timeOverlapStepIds.getOrPut(newerRange.itemIndex) { mutableSetOf() }.add(newerRange.stepId)
                } else {
                    timeOverlapIndices.add(newerRange.itemIndex)
                }
            } else {
                // Normal rule: newer segment shows Time Overlap
                if (newerRange.itemType == "step" && newerRange.stepId != null) {
                    timeOverlapStepIds.getOrPut(newerRange.itemIndex) { mutableSetOf() }.add(newerRange.stepId)
                } else {
                    timeOverlapIndices.add(newerRange.itemIndex)
                }
            }
        }

        // Phase 6: Update items with final flags
        return items.mapIndexed { index, item ->
            when (item) {
                is TimelineDisplayItem.BookedActivity -> {
                    // Booked activity: NEVER shows Time Overlap (booked_activity)
                    // Reserved activity: CAN show Time Overlap (reserved_activity, normal segment)
                    val showOverlap = if (item.isReserved) {
                        index in timeOverlapIndices
                    } else {
                        false  // Booked activity never shows Time Overlap
                    }
                    item.copy(
                        hasConflict = index in visualConflictIndices,
                        showTimeOverlapText = showOverlap
                    )
                }

                is TimelineDisplayItem.ManualPoi -> {
                    item.copy(
                        hasConflict = index in visualConflictIndices,
                        showTimeOverlapText = index in timeOverlapIndices
                    )
                }

                is TimelineDisplayItem.Recommendations -> {
                    val conflictStepIds = visualConflictStepIds[index] ?: emptySet()
                    val overlapStepIds = timeOverlapStepIds[index] ?: emptySet()
                    item.copy(
                        conflictingStepIds = conflictStepIds,  // ALL conflicting steps (visual conflict)
                        timeOverlapStepIds = overlapStepIds    // Steps with Time Overlap text
                    )
                }

                else -> item
            }
        }
    }

    // =====================
    // SMART RECOMMENDATIONS
    // =====================

    /**
     * Format activityId for Smart Recommendations API
     * Standardizes format to C_{rawId}_15_{cityId}
     * Provider ID 15 = Civitatis (hardcoded as per iOS implementation)
     *
     * @param activityId The original activity ID (can be plain "12345", partial "C_12345_15", or full "C_12345_15_28")
     * @param cityId The target city ID
     * @return Formatted activity ID: "C_{rawId}_15_{cityId}"
     */
    private fun formatActivityId(activityId: String?, cityId: Int): String {
        if (activityId.isNullOrBlank()) return ""

        // Extract raw ID if starts with C_
        val rawId = if (activityId.startsWith("C_")) {
            // Format: "C_12345_15" or "C_12345_15_28" → extract "12345"
            activityId.removePrefix("C_").split("_").firstOrNull() ?: activityId
        } else {
            // Plain ID: "12345"
            activityId
        }

        // Standardize to C_{rawId}_15_{cityId}
        // Provider ID 15 = Civitatis (hardcoded)
        return "C_${rawId}_15_$cityId"
    }

    fun createSmartRecommendationSegment(data: AddPlanData) {
        val city = data.selectedCity
        val selectedDate = data.selectedDate

        if (city == null || selectedDate == null) {
            return
        }

        // If city.id is 0, try to find city from cache by name
        val validCity = if (city.id == 0 && city.name != null) {
            tripRepository.findCityByName(city.name!!) ?: city
        } else {
            city
        }

        // Final validation - if still no valid cityId, return error
        if (validCity.id == 0) {
            _error.value = getLanguageForKey(com.tripian.trpcore.util.LanguageConst.COMMON_ERROR)
            return
        }

        // Track existing plan IDs before creating new segment (for scroll after creation)
        existingPlanIds = _timeline.value?.plans
            ?.map { it.id }
            ?.filter { it.isNotEmpty() }
            ?.toSet() ?: emptySet()

        // Set loading immediately (not postValue) since we're on main thread
        showLoading()

        // Generate unique title ("Recommendations", "Recommendations 2", etc.)
        val title = generateSegmentTitle(validCity, selectedDate)

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

        // Get favorite tour IDs and apply format conversion
        val filteredFavorites = getFilteredFavorites()
        val favoriteActivityIds = filteredFavorites
            .filter { it.activityId != null }
            .map { formatActivityId(it.activityId, validCity.id) }

        // Combine data.activityIds (from SavedPlans) with favorites, format all
        val combinedActivityIds = (data.activityIds + favoriteActivityIds)
            .distinct()
            .map { formatActivityId(it, validCity.id) }

        // Build excludedActivityIds: Booked + Reserved activities
        val timeline = _timeline.value
        val bookedAndReservedIds = timeline?.tripProfile?.segments
            ?.filter {
                it.segmentType == SegmentType.BOOKED_ACTIVITY ||
                it.segmentType == SegmentType.RESERVED_ACTIVITY
            }
            ?.mapNotNull { it.additionalData?.activityId }
            ?.map { formatActivityId(it, validCity.id) }
            ?: emptyList()

        createSegmentUseCase.on(
            params = CreateSegmentUseCase.Params(
                tripHash = _tripHash,
                title = title,
                cityId = validCity.id,
                startDate = startDateTimeStr,
                endDate = endDateTimeStr,
                adults = data.travelers,
                children = 0,
                activityFreeText = data.smartCategoriesAsString,
                activityIds = combinedActivityIds,  // Formatted favorites + saved
                excludedActivityIds = bookedAndReservedIds,  // Formatted booked + reserved
                smartRecommendation = true,
                accommodation = data.startingPointAccommodation
            ),
            success = {
                waitForSegmentGeneration()
            },
            error = { errorModel ->
                hideLoading()
                _error.value = errorModel.errorDesc
            }
        )
    }

    private fun waitForSegmentGeneration() {
        waitForGenerationUseCase.on(
            params = WaitForGenerationUseCase.Params(_tripHash),
            success = { timeline ->
                // Find newly added plan ID (not in existingPlanIds)
                val newPlanId = try {
                    timeline.plans?.find { plan ->
                        plan.id.isNotEmpty() && plan.id !in existingPlanIds
                    }?.id
                } catch (e: Exception) {
                    null
                }

                processTimeline(timeline)

                // Trigger scroll to new segment after list is updated
                if (!newPlanId.isNullOrEmpty()) {
                    _scrollToNewSegmentPlanId.value = newPlanId
                }

                hideLoading()
            },
            error = {
                hideLoading()
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
        showLoading()

        deleteSegmentUseCase.on(
            params = DeleteSegmentUseCase.Params(_tripHash, segmentIndex),
            success = {
                refreshTimeline()
                hideLoading()
            },
            error = { errorModel ->
                _error.value = errorModel.errorDesc
                hideLoading()
            }
        )
    }

    fun deleteStep(stepId: Int) {
        showLoading()

        deleteStepUseCase.on(
            params = DeleteStepUseCase.Params(stepId),
            success = {
                refreshTimeline()
                hideLoading()
            },
            error = { errorModel ->
                _error.value = errorModel.errorDesc
                hideLoading()
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

        showLoading()

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
                hideLoading()
                _error.value = errorModel.errorDesc
            }
        )
    }

    /**
     * Show change time picker for a step
     * This will be handled by the activity to show TimeSelectionBottomSheet
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

    /**
     * Called when a marker is focused (user taps on bottom list item or marker).
     * Shows Main View button if there are multiple cities in the selected day.
     */
    fun onMarkerFocused() {
        if (hasMultipleCitiesInSelectedDay) {
            _showMainViewButton.value = true
        }
    }

    /**
     * Called when Main View button is clicked.
     * Hides the button and resets to city markers mode (camera will be reset by the Activity).
     */
    fun onMainViewClicked() {
        _showMainViewButton.value = false
        resetToOverviewMode()
    }

    /**
     * Called when zoom level changes on the map.
     * Automatically switches between city markers and step markers mode based on zoom threshold.
     *
     * @param zoomLevel Current zoom level from the map
     */
    fun onZoomLevelChanged(zoomLevel: Double) {
        // Only handle zoom-based switching in multi-city mode
        if (!hasMultipleCitiesInSelectedDay) return

        val shouldShowStepMarkers = zoomLevel > MULTI_CITY_ZOOM_THRESHOLD

        // Only update if state changed
        if (shouldShowStepMarkers == isShowingStepMarkersInMultiCity) return

        isShowingStepMarkersInMultiCity = shouldShowStepMarkers

        if (shouldShowStepMarkers) {
            // Switch to step markers mode
            _mapMarkersMode.value = MapMarkersMode.STEP_MARKERS
            _showMainViewButton.value = true
        } else {
            // Switch to city markers mode
            _mapMarkersMode.value = MapMarkersMode.CITY_MARKERS
            _showMainViewButton.value = false
        }
    }

    /**
     * Called when a city marker is clicked on the map.
     * Selects the first step of the clicked city.
     *
     * @param cityId ID of the clicked city
     */
    fun onCityMarkerClicked(cityId: Int?) {
        if (cityId == null) return

        val items = _displayItems.value ?: return

        // Find first step of this city
        val firstStepItem = items.firstOrNull { item ->
            item.city?.id == cityId && item !is TimelineDisplayItem.SectionHeader && item !is TimelineDisplayItem.SectionFooter
        }

        firstStepItem?.let { item ->
            // Update selected step ID
            when (item) {
                is TimelineDisplayItem.Recommendations -> {
                    item.steps.firstOrNull()?.poi?.id?.let { poiId ->
                        selectedStepId = poiId
                    }
                }
                is TimelineDisplayItem.BookedActivity -> {
                    selectedStepId = item.segment.additionalData?.activityId
                }
                is TimelineDisplayItem.ManualPoi -> {
                    selectedStepId = item.step.poi?.id
                }
                else -> {}
            }

            // Update city markers to show new selection
            updateCityMarkers()
        }
    }

    /**
     * Selects a step on the map by its ID.
     * Updates the selection state in mapSteps and mapBottomItems.
     * Called when a list item is clicked or scrolled to.
     *
     * @param stepId The poiId of the step to select
     */
    fun selectStepOnMap(stepId: String) {
        // Update selected step ID
        selectedStepId = stepId

        // Update mapSteps selection
        val currentMapSteps = _mapSteps.value?.toMutableList() ?: return
        var stepCityIndex = 0

        currentMapSteps.forEach { step ->
            if (step.poiId == stepId) {
                step.isSelected = true
                stepCityIndex = step.cityIndex
            } else if (step.cityIndex == stepCityIndex) {
                // Deselect other steps in the same city
                step.isSelected = false
            }
        }

        // Find the city index of the selected step and deselect others in same city
        val selectedStep = currentMapSteps.find { it.poiId == stepId }
        if (selectedStep != null) {
            currentMapSteps.forEach { step ->
                if (step.cityIndex == selectedStep.cityIndex && step.poiId != stepId) {
                    step.isSelected = false
                }
            }
        }

        _mapSteps.value = currentMapSteps

        // Update mapBottomItems selection
        val currentBottomItems = _mapBottomItems.value?.map { item ->
            item.copy(isSelected = item.id == stepId)
        }
        currentBottomItems?.let { _mapBottomItems.value = it }

        // If in city markers mode, switch to step markers mode
        if (_mapMarkersMode.value == MapMarkersMode.CITY_MARKERS) {
            isShowingStepMarkersInMultiCity = true
            _mapMarkersMode.value = MapMarkersMode.STEP_MARKERS
        }
    }

    /**
     * Resets to overview mode (city markers + selected step marker).
     * Called when Main View button is clicked.
     */
    fun resetToOverviewMode() {
        isShowingStepMarkersInMultiCity = false
        _mapMarkersMode.value = if (hasMultipleCitiesInSelectedDay) {
            MapMarkersMode.CITY_MARKERS
        } else {
            MapMarkersMode.STEP_MARKERS
        }
    }

    /**
     * Returns the currently selected step as a MapStep for display.
     * Used in city markers mode to show the selected step marker.
     */
    fun getSelectedStepMarker(): MapStep? {
        val stepId = selectedStepId ?: return null

        // Find the step in existing mapSteps to get the correct global position
        val existingStep = _mapSteps.value?.find { it.poiId == stepId }

        return existingStep?.let { step ->
            // Return a copy with isSelected = true
            MapStep().apply {
                poiId = step.poiId
                name = step.name
                coordinate = step.coordinate
                position = step.position  // Use the global position from mapSteps
                group = step.group
                markerIcon = step.markerIcon
                isOffer = step.isOffer
                isSelected = true
                cityIndex = step.cityIndex
                isCityMarker = false
            }
        }
    }

    /**
     * Generates city markers for multi-city overview mode.
     * Called when entering map mode or switching to city markers mode.
     */
    private fun generateCityMarkers(): List<MapStep> {
        val items = _displayItems.value ?: return emptyList()
        val cities = mutableMapOf<Int, City>()

        // Collect unique cities with their coordinates
        items.forEach { item ->
            val city = item.city
            city?.let {
                if (it.id != null && it.coordinate != null) {
                    cities[it.id!!] = it
                }
            }
        }

        // Create city MapStep objects
        return cities.values.mapNotNull { city ->
            city.coordinate?.let { coord ->
                MapStep().apply {
                    poiId = "city_${city.id}"
                    coordinate = Coordinate().apply {
                        lat = coord.lat
                        lng = coord.lng
                    }
                    isCityMarker = true
                    cityId = city.id
                    group = "city"
                    position = -1 // No number badge for city markers
                    markerIcon = -1 // Will use ic_city_marker via MarkerView
                    isSelected = false
                }
            }
        }
    }

    /**
     * Updates city markers LiveData.
     * Called when entering map mode or when selection changes in city markers mode.
     */
    private fun updateCityMarkers() {
        _cityMarkers.value = generateCityMarkers()
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
     * @param date Date of the activity in "yyyy-MM-dd" format (null if not available)
     */
    fun onActivityReservationRequested(activityId: String, date: String? = null) {
        TRPCore.notifyActivityReservationRequested(activityId, date)
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

        // Track city order: cityId -> cityIndex (0 = first city, 1 = second city, etc.)
        val cityOrder = mutableMapOf<Int, Int>()
        var nextCityIndex = 0

        // Global position counter - continues across all cities like timeline list
        var globalPosition = 0

        // Helper function to get next global position
        fun getNextPosition(): Int {
            globalPosition++
            return globalPosition
        }

        items.forEach { item ->
            // Get city index for this item
            val cityId = item.city?.id ?: 0
            val currentCityIndex = if (cityId != 0) {
                cityOrder.getOrPut(cityId) { nextCityIndex++ }
            } else {
                0
            }

            when (item) {
                is TimelineDisplayItem.Recommendations -> {
                    item.steps.forEach { step ->
                        step.poi?.let { poi ->
                            poi.coordinate?.let { coord ->
                                if (coord.lat != 0.0 && coord.lng != 0.0) {
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
                                            this.position = getNextPosition()
                                            isOffer = false
                                            this.cityIndex = currentCityIndex
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                is TimelineDisplayItem.BookedActivity -> {
                    // Get coordinate from additionalData first, then fallback to segment.coordinate
                    val coord = item.segment.additionalData?.coordinate ?: item.segment.coordinate
                    coord?.let {
                        if (it.lat != 0.0 && it.lng != 0.0) {
                            mapSteps.add(
                                MapStep().apply {
                                    group = "booked"
                                    poiId = item.segment.additionalData?.activityId ?: "booked_${item.segmentIndex}"
                                    name = item.segment.additionalData?.title ?: item.segment.title ?: ""
                                    coordinate = com.tripian.one.api.pois.model.Coordinate().apply {
                                        lat = it.lat
                                        lng = it.lng
                                    }
                                    // No icon, only show order label
                                    markerIcon = -1
                                    this.position = getNextPosition()
                                    isOffer = false
                                    this.cityIndex = currentCityIndex
                                }
                            )
                        }
                    }
                }

                is TimelineDisplayItem.ManualPoi -> {
                    item.step.poi?.let { poi ->
                        poi.coordinate?.let { coord ->
                            if (coord.lat != 0.0 && coord.lng != 0.0) {
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
                                        this.position = getNextPosition()
                                        isOffer = false
                                        this.cityIndex = currentCityIndex
                                    }
                                )
                            }
                        }
                    }
                }

                else -> {}
            }
        }

        // Select first item of each city by default
        val selectedCities = mutableSetOf<Int>()
        mapSteps.forEach { step ->
            if (!selectedCities.contains(step.cityIndex)) {
                step.isSelected = true
                selectedCities.add(step.cityIndex)
                // Set initial selected step ID (first step of first city)
                if (selectedStepId == null && step.cityIndex == 0) {
                    selectedStepId = step.poiId
                }
            }
        }

        // Track if there are multiple cities in selected day (for Main View button)
        hasMultipleCitiesInSelectedDay = cityOrder.size > 1

        // Reset zoom state when map steps are updated
        isShowingStepMarkersInMultiCity = false

        // Hide Main View button when map steps are updated (reset state)
        _showMainViewButton.value = false

        // Set initial markers mode based on multi-city state
        _mapMarkersMode.value = if (hasMultipleCitiesInSelectedDay) {
            MapMarkersMode.CITY_MARKERS
        } else {
            MapMarkersMode.STEP_MARKERS
        }

        _mapSteps.value = mapSteps

        // Also update city markers for multi-city mode
        updateCityMarkers()

        // Also update map bottom items
        updateMapBottomItems()
    }

    /**
     * Updates the map bottom items for horizontal list display.
     * Converts TimelineDisplayItems to MapBottomItem format.
     * Each city has its own order starting from 1 and its own selected item.
     */
    private fun updateMapBottomItems() {
        val items = _displayItems.value ?: return
        val bottomItems = mutableListOf<MapBottomItem>()

        // Track city order: cityId -> cityIndex (0 = first city, 1 = second city, etc.)
        val cityOrder = mutableMapOf<Int, Int>()
        var nextCityIndex = 0

        // Global position counter - continues across all cities like timeline list
        var globalPosition = 0

        // Helper function to get next global position
        fun getNextPosition(): Int {
            globalPosition++
            return globalPosition
        }

        // Date formatters for display
        val inputDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val outputTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        items.forEach { item ->
            // Get city index for this item
            val cityId = item.city?.id ?: 0
            val currentCityIndex = if (cityId != 0) {
                cityOrder.getOrPut(cityId) { nextCityIndex++ }
            } else {
                0
            }

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
                                order = getNextPosition(),
                                title = step.poi?.name ?: "",
                                imageUrl = step.poi?.image?.url,
                                date = dateTime?.let { outputDateFormat.format(it) },
                                time = dateTime?.let { outputTimeFormat.format(it) },
                                type = "step",
                                stepType = step.stepType,  // "poi" or "activity"
                                cityIndex = currentCityIndex
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
                            order = getNextPosition(),
                            title = data?.title ?: item.segment.title ?: "",
                            imageUrl = data?.imageUrl,
                            date = dateTime?.let { outputDateFormat.format(it) },
                            time = dateTime?.let { outputTimeFormat.format(it) },
                            type = if (item.isReserved) "reserved" else "booked",
                            cityIndex = currentCityIndex
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
                            order = getNextPosition(),
                            title = step.poi?.name ?: "",
                            imageUrl = step.poi?.image?.url,
                            date = dateTime?.let { outputDateFormat.format(it) },
                            time = dateTime?.let { outputTimeFormat.format(it) },
                            type = "manual",
                            cityIndex = currentCityIndex
                        )
                    )
                }

                else -> {
                    // Skip headers, footers, empty states
                }
            }
        }

        // Select first item of each city by default
        val selectedCities = mutableSetOf<Int>()
        val itemsWithSelection = bottomItems.map { item ->
            if (!selectedCities.contains(item.cityIndex)) {
                selectedCities.add(item.cityIndex)
                item.copy(isSelected = true)
            } else {
                item
            }
        }

        _mapBottomItems.value = itemsWithSelection
    }

    // =====================
    // MANUAL POI
    // =====================

    private fun createManualPoiStep(data: AddPlanData) {
        // TODO: Implement manual POI step creation
        // This would call an API to add a manual step to the timeline
        showLoading()

        // For now, just refresh the timeline
        refreshTimeline()
        hideLoading()
    }

    // =====================
    // HELPERS
    // =====================

    fun hasSingleCity(): Boolean = (_cities.value?.size ?: 0) <= 1

    fun getSelectedCity(): City? = _cities.value?.firstOrNull()

    /**
     * Returns the city coordinate as a Mapbox Point for map centering.
     * Used when map has no items (empty day) to center on city instead of 0,0.
     */
    fun getSelectedDayCityCoordinate(): Point? {
        val city = _cities.value?.firstOrNull()
        val coord = city?.coordinate
        return if (coord != null && coord.lat != 0.0 && coord.lng != 0.0) {
            Point.fromLngLat(coord.lng, coord.lat)
        } else null
    }

    fun getItinerary(): ItineraryWithActivities? = itinerary

    /**
     * Returns favorites that haven't been added as booked_activity or reserved_activity yet
     * and have a valid city mapping (cityName matches a resolved destination)
     * Used when opening SavedPlans screen
     */
    fun getFilteredFavorites(): List<SegmentFavoriteItem> {
        val favourites = itinerary?.favouriteItems ?: return emptyList()
        val timeline = _timeline.value

        // Get activityIds from BOTH booked_activity AND reserved_activity segments
        val bookedAndReservedIds = timeline?.tripProfile?.segments
            ?.filter {
                it.segmentType == SegmentType.BOOKED_ACTIVITY ||
                it.segmentType == SegmentType.RESERVED_ACTIVITY
            }
            ?.mapNotNull { it.additionalData?.activityId }
            ?.toSet() ?: emptySet()

        // Return only favourites that:
        // 1. Are NOT in timeline as booked_activity or reserved_activity
        // 2. Have a valid city mapping (cityName matches a resolved destination)
        return favourites.filter { favourite ->
            favourite.activityId !in bookedAndReservedIds &&
            getResolvedCityId(favourite.cityName) != null
        }
    }

    /**
     * Returns the resolved cityId for a given cityName.
     * Uses the cityNameToIdMap built from resolved destinations.
     * @param cityName The city name from host app data
     * @return Our system's cityId, or null if not found
     */
    fun getResolvedCityId(cityName: String?): Int? {
        if (cityName.isNullOrBlank()) return null
        return cityNameToIdMap[cityName.lowercase().trim()]
    }

    /**
     * Returns a copy of the cityName to cityId mapping.
     * Used to pass resolved city mappings to other screens (e.g., SavedPlans)
     */
    fun getCityNameToIdMap(): Map<String, Int> = cityNameToIdMap.toMap()

    fun getSelectedDate(): Date? {
        val days = _availableDays.value ?: return null
        val index = _selectedDayIndex.value ?: 0
        return if (index < days.size) days[index] else null
    }

    fun getBookedActivities(): List<TimelineSegment> {
        return _timeline.value?.tripProfile?.segments
            ?.filter {
                it.segmentType == SegmentType.BOOKED_ACTIVITY ||
                        it.segmentType == SegmentType.RESERVED_ACTIVITY ||
                        it.segmentType == SegmentType.MANUAL_POI
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

    // =====================
    // ONBOARDING
    // =====================

    /**
     * Checks if onboarding should be shown based on user preferences.
     */
    fun shouldShowOnboarding(): Boolean {
        if (onboardingCompleted) return false

        val dismissed = preferences.getBoolean(Preferences.Keys.ONBOARDING_DISMISSED_PERMANENTLY, false)
        val hasSeen = preferences.getBoolean(Preferences.Keys.ONBOARDING_HAS_SEEN, false)
        val count = preferences.getInt(Preferences.Keys.ONBOARDING_CONTINUE_COUNT, 0)

        android.util.Log.d("ONBOARDING_DEBUG", "ACTimelineVM.shouldShowOnboarding: dismissed=$dismissed, hasSeen=$hasSeen, count=$count")

        if (dismissed) return false
        if (!hasSeen) return true
        return count < 3
    }

    /**
     * Triggers showing onboarding if needed.
     * Called after languages are loaded.
     */
    fun checkAndShowOnboarding() {
        android.util.Log.d("ONBOARDING_DEBUG", "ACTimelineVM.checkAndShowOnboarding called")
        if (shouldShowOnboarding()) {
            android.util.Log.d("ONBOARDING_DEBUG", "Setting _showOnboarding.value = true")
            _showOnboarding.value = true
        } else {
            android.util.Log.d("ONBOARDING_DEBUG", "Onboarding not needed, proceeding with timeline")
            onOnboardingComplete()
        }
    }

    /**
     * Called when onboarding is completed (either by Continue or Skip).
     * Waits for login to complete, then continues with city resolution and timeline.
     */
    fun onOnboardingComplete() {
        android.util.Log.d("ONBOARDING_DEBUG", "ACTimelineVM.onOnboardingComplete called")
        onboardingCompleted = true

        // Wait for login to complete (should already be done in background)
        // Then proceed with city resolution
        showLoading()
        waitForLoginThenProceed {
            android.util.Log.d("TIMELINE_DEBUG", "Login complete, proceeding with city resolution")
            resolveDestinationCitiesAndProceed()
        }
    }

    // ========================================
    // SYNC OPERATIONS (iOS Guide Implementation)
    // ========================================

    /**
     * Ana sync orchestrator
     * STEP 1: City resolution (blocking)
     * STEP 2: Parallel operations
     * STEP 3: Sequential operations
     * STEP 4: Silent refresh
     */
    private fun performSyncOperations(timeline: Timeline) {
        val tripItems = itinerary?.tripItems ?: emptyList()
        val favouriteItems = itinerary?.favouriteItems ?: emptyList()

        if (tripItems.isEmpty() && favouriteItems.isEmpty()) {
            return  // Sync'e gerek yok
        }

        // STEP 1: City resolution (BLOCKING - diğer operasyonlar bunu bekler)
        resolveCityIdsForActivitiesUseCase.on(
            params = ResolveCityIdsForActivitiesUseCase.Params(
                tripItems,
                favouriteItems,
                cityNameToIdMap.toMap()
            ),
            success = { updatedCityMap ->
                cityNameToIdMap.putAll(updatedCityMap)
                performParallelSyncOperations(timeline, tripItems, updatedCityMap)
            },
            error = { error ->
                android.util.Log.e("TIMELINE_SYNC", "City resolution failed: ${error.errorDesc}")
                // Fallback: mevcut map ile devam et
                performParallelSyncOperations(timeline, tripItems, cityNameToIdMap.toMap())
            }
        )
    }

    /**
     * STEP 2: Parallel operations (3 concurrent)
     * - Detect transitions
     * - Add missing activities
     * - Update date range
     */
    private fun performParallelSyncOperations(
        timeline: Timeline,
        tripItems: List<com.tripian.trpcore.domain.model.itinerary.SegmentActivityItem>,
        cityMap: Map<String, Int>
    ) {
        var detectedTransitions: List<TransitionInfo>? = null
        var completedOps = 0

        val onParallelComplete = {
            completedOps++
            if (completedOps == 3) {
                // Hepsi bitti, sequential operasyonlara geç
                performSequentialSyncOperations(timeline, detectedTransitions, cityMap)
            }
        }

        // Parallel Op 1: Transition detection
        detectReservedToBookedTransitionUseCase.on(
            params = DetectReservedToBookedTransitionUseCase.Params(timeline, tripItems),
            success = { transitions ->
                detectedTransitions = transitions
                onParallelComplete()
            },
            error = {
                android.util.Log.e("TIMELINE_SYNC", "Transition detection failed")
                onParallelComplete()
            }
        )

        // Parallel Op 2: Add missing activities
        addMissingBookedActivitiesUseCase.on(
            params = AddMissingBookedActivitiesUseCase.Params(_tripHash, tripItems, timeline, cityMap),
            success = { onParallelComplete() },
            error = {
                android.util.Log.e("TIMELINE_SYNC", "Add missing activities failed")
                onParallelComplete()
            }
        )

        // Parallel Op 3: Update date range
        updateDateRangeUseCase.on(
            params = UpdateDateRangeUseCase.Params(_tripHash, itinerary!!, timeline),
            success = { onParallelComplete() },
            error = {
                android.util.Log.e("TIMELINE_SYNC", "Date range update failed")
                onParallelComplete()
            }
        )
    }

    /**
     * STEP 3: Sequential operations
     * - Sync transitions (delete reserved → create booked)
     * - Remove deleted city segments
     */
    private fun performSequentialSyncOperations(
        timeline: Timeline,
        transitions: List<TransitionInfo>?,
        cityMap: Map<String, Int>
    ) {
        // Sequential Op 1: Sync transitions (if any)
        if (!transitions.isNullOrEmpty()) {
            syncReservedToBookedUseCase.on(
                params = SyncReservedToBookedUseCase.Params(_tripHash, transitions, cityMap),
                success = { performCityDeletionSync(timeline) },
                error = {
                    android.util.Log.e("TIMELINE_SYNC", "Transition sync failed")
                    performCityDeletionSync(timeline)
                }
            )
        } else {
            performCityDeletionSync(timeline)
        }
    }

    /**
     * Sequential Op 2: Remove deleted cities
     */
    private fun performCityDeletionSync(timeline: Timeline) {
        val destinations = itinerary?.destinationItems ?: emptyList()

        removeSegmentsForDeletedCitiesUseCase.on(
            params = RemoveSegmentsForDeletedCitiesUseCase.Params(_tripHash, timeline, destinations),
            success = { refreshTimelineAfterSync() },
            error = {
                android.util.Log.e("TIMELINE_SYNC", "City deletion failed")
                refreshTimelineAfterSync()
            }
        )
    }

    /**
     * STEP 4: Silent refresh (no loading indicator)
     */
    private fun refreshTimelineAfterSync() {
        fetchTimelineUseCase.on(
            params = FetchTimelineUseCase.Params(_tripHash),
            success = { timeline ->
                // processTimeline'ı çağır ama sync tekrar çalışmayacak (syncOperationsCompleted=true)
                processTimeline(timeline)
            },
            error = { error ->
                android.util.Log.e("TIMELINE_SYNC", "Final refresh failed: ${error.errorDesc}")
            }
        )
    }

    companion object {
        const val ARG_TRIP_HASH = "tripHash"

        // Multi-city zoom thresholds
        const val MULTI_CITY_ZOOM_THRESHOLD = 12.0
        const val CITY_MARKER_ZOOM_LEVEL = 13.0
        const val STEP_MARKER_ZOOM_LEVEL = 15.0
    }
}
