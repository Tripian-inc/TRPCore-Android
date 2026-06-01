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
import com.tripian.trpcore.domain.usecase.timeline.UpdateSegmentTimeUseCase
import com.tripian.trpcore.domain.usecase.timeline.UpdateStepTimeUseCase
import com.tripian.trpcore.domain.usecase.timeline.WaitForGenerationUseCase
import com.tripian.trpcore.domain.usecase.timeline.sync.AddMissingBookedActivitiesUseCase
import com.tripian.trpcore.domain.usecase.timeline.sync.DetectReservedToBookedTransitionUseCase
import com.tripian.trpcore.domain.usecase.timeline.sync.RemoveSegmentsForDeletedCitiesUseCase
import com.tripian.trpcore.domain.usecase.timeline.sync.ResolveCityIdsForActivitiesUseCase
import com.tripian.trpcore.domain.usecase.timeline.sync.SyncReservedToBookedUseCase
import com.tripian.trpcore.domain.usecase.timeline.sync.UpdateDateRangeUseCase
import com.tripian.trpcore.repository.CityResolveResult
import com.tripian.trpcore.sdk.TRPCoreErrorCode
import com.tripian.trpcore.ui.timeline.adapter.MapBottomItem
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.LanguageConst
import java.util.concurrent.TimeUnit
import com.tripian.trpcore.util.extensions.isFlexibleActivity
import com.tripian.trpcore.util.extensions.isPastDay
import com.tripian.trpcore.util.extensions.isTodayDate
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
    private val updateSegmentTimeUseCase: UpdateSegmentTimeUseCase,
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
    private val removeSegmentsForDeletedCitiesUseCase: RemoveSegmentsForDeletedCitiesUseCase,
    private val availabilityCheckManager: com.tripian.trpcore.domain.manager.AvailabilityCheckManager,
    private val mapItemMapper: com.tripian.trpcore.ui.timeline.mapper.MapItemMapper,
    private val displayItemBuilder: com.tripian.trpcore.ui.timeline.mapper.TimelineDisplayItemBuilder
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

    // Conflict banner dismissal: the day index on which the user dismissed it.
    // -1 = no dismissal in effect. Tracked per-day so switching back restores the dismiss.
    private var conflictBannerDismissedDayIndex: Int = -1

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

    // True once we've auto-selected the initial day (today, or trip's first day
    // when today falls outside the trip range). Subsequent timeline refreshes
    // must not override an explicit user selection.
    private var initialDayAutoSelected = false

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
     *
     * Fast path: translations already cached from TRPCore.init() → show the
     * "Getting your itinerary plan" loader and continue immediately.
     *
     * Slow path: TRPCore.init() couldn't fetch translations. Re-trigger the
     * fetch and wait for BOTH translations AND the parallel light-login to
     * finish before proceeding. While waiting, show a text-less Lottie
     * (animation only) — host-visible copy is rendered only once the
     * translation bundle is actually available.
     *
     * If translations cannot be obtained within [LANGUAGE_RETRY_TIMEOUT_SECONDS],
     * dispatches [TRPCoreErrorCode.LANGUAGE_LOAD_FAILED] and closes the SDK.
     */
    private fun ensureLanguagesLoadedThenProceed() {
        // Fast path: translations already loaded (init succeeded earlier).
        if (miscRepository.isLanguagesLoaded) {
            showFullScreenLoader(LanguageConst.LOADING_TEXT_GETTING_ITINERARY_PLAN, "")
            proceedAfterLanguagesLoaded()
            return
        }

        // Slow path: parallel translation fetch + light-login. No text until
        // translations are available; otherwise we'd flash a hardcoded string.
        showFullScreenLoaderNoText()
        attemptLanguageFetch(allowRetry = true)
    }

    /**
     * Drives the translation fetch with one explicit retry.
     *
     * First attempt piggybacks on the shared in-progress fetch (if init() is
     * still running) via [MiscRepository.waitForLanguagesLoaded]. That path can
     * silently surface a stale `false` from a previously failed init fetch or
     * miss an emission that happens after a long delay, so the retry explicitly
     * calls [MiscRepository.refetchLanguages] to force a brand-new /languages
     * request regardless of subject state.
     */
    private fun attemptLanguageFetch(allowRetry: Boolean) {
        val source = if (allowRetry) {
            miscRepository.waitForLanguagesLoaded()
        } else {
            miscRepository.refetchLanguages()
        }
        source
            .timeout(LANGUAGE_RETRY_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { loaded ->
                    if (loaded && miscRepository.isLanguagesLoaded) {
                        // Translations ready. Now block on the parallel light-login
                        // so the timeline fetch never runs without an auth header.
                        waitForLoginThenProceed {
                            showFullScreenLoader(LanguageConst.LOADING_TEXT_GETTING_ITINERARY_PLAN, "")
                            proceedAfterLanguagesLoaded()
                        }
                    } else if (allowRetry) {
                        attemptLanguageFetch(allowRetry = false)
                    } else {
                        failLanguageLoad("Translation fetch returned no data")
                    }
                },
                { error ->
                    if (allowRetry) {
                        attemptLanguageFetch(allowRetry = false)
                    } else {
                        failLanguageLoad(error?.message ?: "Translation fetch failed")
                    }
                }
            )
    }

    /**
     * Hides the loader, surfaces a typed [TRPCoreErrorCode.LANGUAGE_LOAD_FAILED]
     * to the host so it can react, and closes the SDK.
     */
    private fun failLanguageLoad(message: String) {
        hideLottieLoading()
        TRPCore.notifyError(message, TRPCoreErrorCode.LANGUAGE_LOAD_FAILED)
        finishActivity()
    }

    /**
     * Called after languages are loaded.
     * Sets language, checks onboarding, then resolves cities and starts login flow.
     * The unified loader stays open — fetchTimeline() reuses it.
     */
    private fun proceedAfterLanguagesLoaded() {
        // Apply language change after languages are loaded
        val language = arguments?.getString(TRPCore.EXTRA_APP_LANGUAGE)
        if (!language.isNullOrEmpty()) {
            miscRepository.changeLanguage(language)
        }

        // Loader intentionally stays visible — the next step (timeline fetch)
        // continues to use the same loader so the user sees one continuous
        // "Getting your itinerary plan" screen until the timeline is ready.
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

        // Keep the unified "Getting your itinerary plan" text — showLoading()
        // would post the rotating default and replace it before the fetch step
        // can re-assert the single text.
        showFullScreenLoader(LanguageConst.LOADING_TEXT_GETTING_ITINERARY_PLAN, "")

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
            },
            error = { errorModel ->
                isLoginInProgress = false
                _error.value = errorModel.errorDesc ?: "Login failed"
                TRPCore.notifyError(errorModel.errorDesc ?: "Login failed")
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

        // İlk açılış akışı: rotating yerine tek "Getting your itinerary plan" mesajı.
        showFullScreenLoader(LanguageConst.LOADING_TEXT_GETTING_ITINERARY_PLAN, "")
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
                    hideLottieLoading()
                }
            },
            error = { errorModel ->
                _error.value = errorModel.errorDesc
                TRPCore.notifyError(errorModel.errorDesc ?: "Timeline creation failed")
                hideLottieLoading()
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

        // İlk açılış akışı: rotating yerine tek "Getting your itinerary plan" mesajı.
        showFullScreenLoader(LanguageConst.LOADING_TEXT_GETTING_ITINERARY_PLAN, "")
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
                    hideLottieLoading()
                }
            },
            error = { errorModel ->
                _error.value = errorModel.errorDesc
                TRPCore.notifyError(errorModel.errorDesc ?: "Timeline creation failed")
                hideLottieLoading()
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
                hideLottieLoading()
            },
            error = { errorModel ->
                hideLottieLoading()
                _error.value = errorModel.errorDesc ?: "Timeline generation failed"
                TRPCore.notifyError(errorModel.errorDesc ?: "Timeline generation failed")
            }
        )
    }

    // =====================
    // FETCH & REFRESH
    // =====================

    fun fetchTimeline() {
        // Theme 17: a new fetch invalidates any in-flight availability sweep.
        availabilityCheckManager.reset()
        // Initial fetch: keep the same single-text loader from the language-load step.
        // postValue() collapses to the latest value, so we must keep using the same
        // Single text or the rotating defaults would race ahead and replace it
        // before the observer fires.
        showFullScreenLoader(LanguageConst.LOADING_TEXT_GETTING_ITINERARY_PLAN, "")
        _error.value = null

        fetchTimelineUseCase.on(
            params = FetchTimelineUseCase.Params(_tripHash),
            success = { timeline ->
                processTimeline(timeline)
                hideLottieLoading()
            },
            error = { errorModel ->
                _error.value = errorModel.errorDesc
                TRPCore.notifyError(errorModel.errorDesc ?: "Timeline fetch failed")
                hideLottieLoading()
            }
        )
    }

    fun refreshTimeline() {
        // Theme 17: a refresh invalidates any in-flight availability sweep.
        availabilityCheckManager.reset()
        // Theme 10: publish the refresh state so external observers (Saved Plans,
        // Time Selection bottom sheet) can show their own loader / toast.
        com.tripian.trpcore.domain.manager.TimelineRefreshState.setRefreshing()
        // Theme 1: same full-screen Lottie as fetchTimeline — refresh is a
        // long-running operation including post-load availability sweep.
        showLottieLoading()
        // Clear route info cache to ensure fresh calculations
        clearRouteInfoCache()

        fetchTimelineUseCase.on(
            params = FetchTimelineUseCase.Params(_tripHash),
            success = { timeline ->
                processTimeline(timeline)
                hideLottieLoading()
                com.tripian.trpcore.domain.manager.TimelineRefreshState.setCompleted()
                // Idle quickly so subscribers can distinguish a fresh completion
                // event from the "static" idle state on next subscribe.
                com.tripian.trpcore.domain.manager.TimelineRefreshState.setIdle()
            },
            error = { error ->
                hideLottieLoading()
                com.tripian.trpcore.domain.manager.TimelineRefreshState
                    .setFailed(Throwable(error?.errorDesc ?: "Timeline refresh failed"))
                com.tripian.trpcore.domain.manager.TimelineRefreshState.setIdle()
            }
        )
    }

    // =====================
    // DATA PROCESSING
    // =====================

    private fun populateCitiesInSegments(timeline: Timeline) {
        val segments = timeline.tripProfile?.segments ?: return
        val plans = timeline.plans ?: return
        segments.forEachIndexed { index, segment ->
            if ((segment.cityId ?: 0) > 0) return@forEachIndexed
            val planCityId = plans.getOrNull(index)?.city?.id ?: return@forEachIndexed
            if (planCityId > 0) {
                segment.cityId = planCityId
            }
        }
    }

    /**
     * Light-weight UI refresh for in-place timeline mutations (e.g. the
     * TimelineDate optimistic update). Re-publishes the LiveData, recomputes
     * available days, clamps the selected index, and re-renders display items
     * without re-running sync or the availability sweep.
     */
    private fun republishCurrentTimeline() {
        val timeline = _timeline.value ?: return
        _timeline.value = timeline

        val days = calculateAvailableDays(timeline)
        _availableDays.value = days

        if (days.isNotEmpty()) {
            val currentIndex = _selectedDayIndex.value ?: 0
            if (currentIndex >= days.size) {
                _selectedDayIndex.value = 0
            }
        }

        updateDisplayItems()
    }

    private fun processTimeline(timeline: Timeline) {
        // Server returns booked/reserved/itinerary segments without a cityId in
        // many multi-city responses; the plan at the same index always carries
        // the resolved city. Backfill via index mapping (NOT coordinate match —
        // close-by destinations would collide) before anything downstream reads
        // tripProfile.segments.
        populateCitiesInSegments(timeline)

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

        // First load: pick today if it falls within the trip, otherwise the
        // trip's first day. Later refreshes only clamp out-of-bounds indices
        // so user selections aren't overwritten.
        if (days.isNotEmpty()) {
            if (!initialDayAutoSelected) {
                val todayIndex = days.indexOfFirst { it.isTodayDate() }
                _selectedDayIndex.value = if (todayIndex >= 0) todayIndex else 0
                initialDayAutoSelected = true
            } else {
                val currentIndex = _selectedDayIndex.value ?: 0
                if (currentIndex >= days.size) {
                    _selectedDayIndex.value = 0
                }
            }
        }

        // Generate display items for selected day
        updateDisplayItems()

        // iOS Guide: Perform sync operations after initial timeline fetch
        if (!syncOperationsCompleted && itinerary != null) {
            syncOperationsCompleted = true
            performSyncOperations(timeline)
        }

        // Theme 17: every processed timeline is a fresh snapshot — the prior
        // sweep's `hasRunInitialCheck` guard must be cleared here, otherwise
        // paths that hand a new Timeline straight to processTimeline (smart
        // recommendation generation, post-step-add wait, sync ops) would skip
        // the sweep on the new instance and lose the @Transient expired flag,
        // letting conflict styling shadow the red "Not available" badge.
        availabilityCheckManager.reset()
        triggerAvailabilitySweep(timeline)
    }

    /**
     * Theme 17: invokes the post-load availability sweep against `/schedule-bulk`
     * and applies the resulting `isAvailabilityExpired` flags to timeline segments and
     * itinerary steps. Safe to call repeatedly — the manager guards against duplicate
     * runs (call [com.tripian.trpcore.domain.manager.AvailabilityCheckManager.reset]
     * to restart on a brand-new timeline).
     */
    override fun onCleared() {
        // Theme 17: cancel any in-flight availability sweep when the VM goes away.
        availabilityCheckManager.cancel()
        super.onCleared()
    }

    private fun triggerAvailabilitySweep(timeline: Timeline) {
        val selectedIdx = _selectedDayIndex.value ?: 0
        val selectedDate = _availableDays.value?.getOrNull(selectedIdx)
        val currency = TRPCore.core.appConfig.appCurrency
        val lang = TRPCore.core.appConfig.appLanguage

        availabilityCheckManager.runInitialAvailabilityCheck(
            timeline = timeline,
            selectedDate = selectedDate,
            currency = currency,
            lang = lang,
            listener = object :
                com.tripian.trpcore.domain.manager.AvailabilityCheckManager.ItemUpdateListener {
                override fun onItemUpdated(segmentIndex: Int, stepId: Int?, isExpired: Boolean) {
                    val tl = _timeline.value ?: return
                    val segment = tl.tripProfile?.segments?.getOrNull(segmentIndex) ?: return
                    if (stepId == null) {
                        segment.additionalData?.isAvailabilityExpired = isExpired
                    } else {
                        val step = tl.plans?.getOrNull(segmentIndex)?.steps
                            ?.firstOrNull { it.id == stepId }
                        step?.isAvailabilityExpired = isExpired
                    }
                }
            },
            onCompleted = {
                // Trigger a recompose of the display items so the new expired flags
                // surface in the UI (red badge + "Not available" suffix).
                updateDisplayItems()
            }
        )
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

        // STEP 1 — Prefer the TimelineDate sentinel segment. It is the single
        // source of truth for the trip's date range; orphan segments left
        // outside the current range (e.g. after the host shortened the trip)
        // could otherwise distort the boundaries.
        segments.firstOrNull { it.title == "TimelineDate" && !it.available }
            ?.let { sentinel ->
                val start = sentinel.startDate?.toDate()
                val end = sentinel.endDate?.toDate()
                if (start != null && end != null) {
                    return generateDateRange(start, end)
                }
            }

        // STEP 2 — Fallback: scan every segment for min/max (legacy timelines
        // that pre-date the TimelineDate sentinel).
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
        // Theme 12: collapse state is per-screen, not per-day — but we reset on
        // day change so the user always sees expanded sections when switching.
        collapsedSectionCityIds.clear()
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
        val items = displayItemBuilder.build(
            timeline = timeline,
            date = selectedDate,
            cities = _cities.value ?: emptyList(),
            collapsedSectionCityIds = collapsedSectionCityIds,
            emptyStateMessage = getLanguageForKey(LanguageConst.NO_PLANS_FOR_DAY)
        )

        // Apply preserved collapse states + cached route info to new items.
        // Route info cache survives updateDisplayItems() so async refreshes (availability sweep,
        // generation polling, etc.) don't wipe distance separators off the UI.
        val itemsWithPreservedState = items.map { item ->
            if (item is TimelineDisplayItem.Recommendations) {
                val savedExpanded = existingExpandStates[item.plan.id]
                val cachedRoutes = item.segmentIndex?.let { _routeInfoCache[it] }
                item.copy(
                    isExpanded = savedExpanded ?: item.isExpanded,
                    routeInfoList = if (!cachedRoutes.isNullOrEmpty()) cachedRoutes else item.routeInfoList
                )
            } else {
                item
            }
        }

        _displayItems.value = injectConflictBannerIfNeeded(itemsWithPreservedState, selectedIndex)

        // Always update map steps so they're ready when user switches to map mode
        updateMapSteps()
    }

    /**
     * Prepends a [TimelineDisplayItem.ConflictWarning] when the day's items contain a
     * conflict and the user hasn't dismissed the banner on this day. The banner now lives
     * inside the RecyclerView so it scrolls with the content instead of sitting sticky.
     */
    private fun injectConflictBannerIfNeeded(
        items: List<TimelineDisplayItem>,
        dayIndex: Int
    ): List<TimelineDisplayItem> {
        if (dayIndex == conflictBannerDismissedDayIndex) return items
        val hasConflict = items.any {
            (it is TimelineDisplayItem.BookedActivity && it.hasConflict) ||
                    (it is TimelineDisplayItem.ManualPoi && it.hasConflict) ||
                    (it is TimelineDisplayItem.Recommendations && it.conflictingStepIds.isNotEmpty())
        }
        if (!hasConflict) return items
        return listOf(TimelineDisplayItem.ConflictWarning) + items
    }

    /**
     * Dismiss the conflict banner for the current day. The flag is per-day, so switching
     * to a different day reopens the banner there if conflicts exist; coming back later
     * still keeps it dismissed on the original day.
     */
    fun dismissConflictBanner() {
        conflictBannerDismissedDayIndex = _selectedDayIndex.value ?: 0
        updateDisplayItems()
    }

    /**
     * City IDs whose section is currently collapsed in the Timeline list. When a
     * city is in this set, [TimelineDisplayItemBuilder] emits only the header +
     * footer for that group (its actual content items are filtered out). Reset
     * on day change and on full timeline refresh.
     */
    private val collapsedSectionCityIds: MutableSet<Int> = mutableSetOf()

    fun isSectionCollapsed(cityId: Int): Boolean = cityId in collapsedSectionCityIds

    fun toggleSectionCollapsed(cityId: Int) {
        if (cityId == 0) return
        if (cityId in collapsedSectionCityIds) {
            collapsedSectionCityIds.remove(cityId)
        } else {
            collapsedSectionCityIds.add(cityId)
        }
        updateDisplayItems()
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
        showLottieLoading()

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
                hideLottieLoading()
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

                hideLottieLoading()
            },
            error = {
                hideLottieLoading()
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
        showBottomSheetLoader(LanguageConst.LOADING_TEXT_REMOVING_FROM_PLAN, "Removing from plan")

        deleteSegmentUseCase.on(
            params = DeleteSegmentUseCase.Params(_tripHash, segmentIndex),
            success = {
                refreshTimeline()
                hideLottieLoading()
            },
            error = { errorModel ->
                _error.value = errorModel.errorDesc
                hideLottieLoading()
            }
        )
    }

    fun deleteStep(stepId: Int) {
        showBottomSheetLoader(LanguageConst.LOADING_TEXT_REMOVING_FROM_PLAN, "Removing from plan")

        deleteStepUseCase.on(
            params = DeleteStepUseCase.Params(stepId),
            success = {
                // Hand the loader off to refreshTimeline — it keeps the Lottie visible
                // while the timeline is re-fetched and hides it on completion. Hiding
                // here would close the loader the instant delete returns, before the
                // timeline refresh finishes.
                refreshTimeline()
            },
            error = { errorModel ->
                _error.value = errorModel.errorDesc
                hideLottieLoading()
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

        showBottomSheetLoader(LanguageConst.LOADING_TEXT_CHANGING_TIME, "Changing time")

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
                hideLottieLoading()
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

    /**
     * Segment-level change time. Used for reserved_activity and flexible activities
     * (booked activities don't expose change-time). Carries both the original
     * segment payload and its index so the edit goes in-place via segmentIndex.
     */
    data class SegmentTimePickerRequest(
        val segment: TimelineSegment,
        val segmentIndex: Int
    )

    private val _showChangeTimePickerSegment = MutableLiveData<SegmentTimePickerRequest?>()
    val showChangeTimePickerSegment: LiveData<SegmentTimePickerRequest?> =
        _showChangeTimePickerSegment

    fun showSegmentChangeTimePicker(segment: TimelineSegment, segmentIndex: Int) {
        _showChangeTimePickerSegment.value = SegmentTimePickerRequest(segment, segmentIndex)
    }

    fun clearChangeTimePickerSegment() {
        _showChangeTimePickerSegment.value = null
    }

    /**
     * Edit a top-level segment's start/end time. Times are "HH:mm"; the date is
     * preserved from the segment's existing start.
     */
    fun updateSegmentTime(
        segment: TimelineSegment,
        segmentIndex: Int,
        startTime: String?,
        endTime: String?
    ) {
        if (startTime == null || endTime == null) return

        showBottomSheetLoader(LanguageConst.LOADING_TEXT_CHANGING_TIME, "Changing time")

        updateSegmentTimeUseCase.on(
            params = UpdateSegmentTimeUseCase.Params(
                tripHash = _tripHash,
                segmentIndex = segmentIndex,
                original = segment,
                newStartTime = startTime,
                newEndTime = endTime
            ),
            success = {
                refreshTimeline()
            },
            error = { errorModel ->
                hideLottieLoading()
                _error.value = errorModel.errorDesc
            }
        )
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
    /**
     * Theme 15: clears any selected marker / preview card. The map redraws its
     * marker selection and the bottom preview list collapses to its idle state.
     */
    fun clearMapSelection() {
        selectedStepId = null
        val currentMapSteps = _mapSteps.value?.toMutableList() ?: return
        currentMapSteps.forEach { it.isSelected = false }
        _mapSteps.value = currentMapSteps
        updateMapBottomItems()
    }

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
     * Theme 15: when [allowToggle] is true (a marker tap) and the same step is
     * already selected, the selection is cleared so the bottom preview card
     * closes. List-driven calls leave [allowToggle] false and always set.
     *
     * @param stepId    The poiId of the step to select
     * @param allowToggle  Whether to deselect when re-selecting the same step
     */
    @JvmOverloads
    fun selectStepOnMap(stepId: String, allowToggle: Boolean = false) {
        if (allowToggle && selectedStepId == stepId) {
            clearMapSelection()
            return
        }
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
    /**
     * Updates city markers LiveData.
     * Called when entering map mode or when selection changes in city markers mode.
     */
    private fun updateCityMarkers() {
        val items = _displayItems.value ?: emptyList()
        _cityMarkers.value = mapItemMapper.buildCityMarkers(items)
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

        val result = mapItemMapper.buildMapSteps(items)

        // Adopt the mapper's "first marker of city 0" suggestion when we don't yet
        // have a selection — keeps step focus consistent with the legacy behavior.
        if (selectedStepId == null) {
            selectedStepId = result.firstStepIdOfFirstCity
        }

        // Track if there are multiple cities in selected day (for Main View button)
        hasMultipleCitiesInSelectedDay = result.hasMultipleCities

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

        _mapSteps.value = result.mapSteps

        // Also update city markers for multi-city mode
        updateCityMarkers()

        // Also update map bottom items
        updateMapBottomItems()
    }

    /**
     * Updates the map bottom items LiveData via [MapItemMapper.buildMapBottomItems].
     * The mapper auto-selects the first item per city that has a real map marker.
     */
    private fun updateMapBottomItems() {
        val items = _displayItems.value ?: return
        val markerIds = _mapSteps.value?.mapNotNull { it.poiId }?.toSet() ?: emptySet()
        _mapBottomItems.value = mapItemMapper.buildMapBottomItems(items, markerIds)
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

    /**
     * Returns the coordinate for the given cityId, used to center the camera on a
     * bottom-list item that has no map marker (no exact location). Returns null when
     * the city has no usable coordinate.
     */
    fun getCityCoordinate(cityId: Int?): Coordinate? {
        if (cityId == null) return null
        val city = _cities.value?.find { it.id == cityId } ?: return null
        val coord = city.coordinate ?: return null
        return if (coord.lat != 0.0 && coord.lng != 0.0) coord else null
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

    /** Theme 3: true when the currently-viewed day is in the past. */
    val isSelectedDayPast: Boolean
        get() = getSelectedDate()?.isPastDay() == true

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

        if (dismissed) return false
        if (!hasSeen) return true
        return count < 3
    }

    /**
     * Triggers showing onboarding if needed.
     * Called after languages are loaded.
     */
    fun checkAndShowOnboarding() {
        if (shouldShowOnboarding()) {
            // Drop the full-screen loader before the onboarding bottom sheet is
            // shown — otherwise the loader Dialog sits on top of the sheet and
            // the user has no way to dismiss onboarding, leaving the SDK stuck
            // on "Getting your itinerary plan". onOnboardingComplete() re-shows
            // the loader before proceeding with city resolution / fetch.
            hideLottieLoading()
            _showOnboarding.value = true
        } else {
            onOnboardingComplete()
        }
    }

    /**
     * Called when onboarding is completed (either by Continue or Skip).
     * Waits for login to complete, then continues with city resolution and timeline.
     */
    fun onOnboardingComplete() {
        onboardingCompleted = true

        // Wait for login to complete (should already be done in background)
        // Then proceed with city resolution.
        // Keep the same single "Getting your itinerary plan" text — calling the
        // generic showLoading() here posts the rotating default and races ahead
        // of the SingleLiveEvent observer, replacing the intended single text.
        showFullScreenLoader(LanguageConst.LOADING_TEXT_GETTING_ITINERARY_PLAN, "")
        waitForLoginThenProceed {
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

        // Host can re-open the SDK with an extended/shortened date range without
        // changing tripItems/favourites — the TimelineDate segment must still be
        // realigned to the new range, so date sync runs independently of the
        // activity-driven sync pipeline.
        if (tripItems.isEmpty() && favouriteItems.isEmpty()) {
            syncDateRangeOnly(timeline)
            return
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
            error = { _ ->
                // Fallback: mevcut map ile devam et
                performParallelSyncOperations(timeline, tripItems, cityNameToIdMap.toMap())
            }
        )
    }

    /**
     * Date-only sync path. Runs when no tripItems/favourites are supplied so
     * the heavy parallel/sequential pipeline is unnecessary, but the
     * TimelineDate segment still needs to reflect the host-supplied range.
     */
    private fun syncDateRangeOnly(timeline: Timeline) {
        updateDateRangeUseCase.on(
            params = UpdateDateRangeUseCase.Params(_tripHash, itinerary!!, timeline),
            success = { result ->
                if (result.mutated) republishCurrentTimeline()
            },
            error = { /* fire-and-forget — silent */ }
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
                onParallelComplete()
            }
        )

        // Parallel Op 2: Add missing activities
        addMissingBookedActivitiesUseCase.on(
            params = AddMissingBookedActivitiesUseCase.Params(_tripHash, itinerary!!, timeline),
            success = { onParallelComplete() },
            error = {
                onParallelComplete()
            }
        )

        // Parallel Op 3: Update date range — iOS optimistic flow. Mutation is
        // already applied to the in-memory segment by the use case; we refresh
        // the UI here before signalling completion so the user sees the new day
        // range without waiting for the rest of the sync pipeline.
        updateDateRangeUseCase.on(
            params = UpdateDateRangeUseCase.Params(_tripHash, itinerary!!, timeline),
            success = { result ->
                if (result.mutated) republishCurrentTimeline()
                onParallelComplete()
            },
            error = {
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
            error = { _ -> }
        )
    }

    companion object {
        const val ARG_TRIP_HASH = "tripHash"

        // Multi-city zoom thresholds
        const val MULTI_CITY_ZOOM_THRESHOLD = 12.0
        const val CITY_MARKER_ZOOM_LEVEL = 13.0
        const val STEP_MARKER_ZOOM_LEVEL = 15.0

        // Upper bound for translation fetch on SDK launch. Beyond this, the
        // host is informed via LANGUAGE_LOAD_FAILED and the SDK closes so the
        // user is not left staring at the loader forever.
        private const val LANGUAGE_RETRY_TIMEOUT_SECONDS = 30L
    }
}
