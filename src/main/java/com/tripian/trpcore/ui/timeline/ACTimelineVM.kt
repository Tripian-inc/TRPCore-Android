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
import com.tripian.trpcore.domain.model.timeline.PlannedActivitySource
import com.tripian.trpcore.domain.model.timeline.StepRouteInfo
import com.tripian.trpcore.domain.model.timeline.FlatRouteChain
import com.tripian.trpcore.domain.model.timeline.generatedWithoutPois
import com.tripian.trpcore.domain.model.timeline.planFor
import com.tripian.trpcore.domain.model.timeline.TimelineDisplayItem
import com.tripian.trpcore.domain.model.timeline.TransitionInfo
import com.tripian.trpcore.domain.model.timeline.generateDateRange
import com.tripian.trpcore.domain.model.timeline.plannedActivities
import com.tripian.trpcore.domain.model.timeline.plannedActivityIdsByDay
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
import com.tripian.trpcore.domain.usecase.timeline.sync.RemoveObsoleteSegmentsUseCase
import com.tripian.trpcore.domain.usecase.timeline.sync.ResolveCityIdsForActivitiesUseCase
import com.tripian.trpcore.domain.usecase.timeline.sync.SyncReservedToBookedUseCase
import com.tripian.trpcore.domain.usecase.timeline.sync.UpdateDateRangeUseCase
import com.tripian.trpcore.repository.CityResolveResult
import com.tripian.trpcore.repository.base.ErrorModel
import com.tripian.trpcore.sdk.TRPCoreErrorCode
import com.tripian.trpcore.ui.timeline.adapter.MapBottomItem
import com.tripian.trpcore.util.ActivityIdFormat
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.LanguageConst
import java.util.concurrent.TimeUnit
import com.tripian.trpcore.util.extensions.applyScheduledPrice
import com.tripian.trpcore.util.extensions.cityNameKey
import com.tripian.trpcore.util.extensions.isFlexibleActivity
import com.tripian.trpcore.util.extensions.isPastDay
import com.tripian.trpcore.util.extensions.isTodayDate
import com.tripian.trpcore.util.Preferences
import com.tripian.trpcore.util.extensions.hideLoading
import com.tripian.trpcore.util.extensions.showLoading
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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
    private val timelineRepository: com.tripian.trpcore.repository.TimelineRepository,
    private val preferences: Preferences,
    private val resolveCityIdsForActivitiesUseCase: ResolveCityIdsForActivitiesUseCase,
    private val detectReservedToBookedTransitionUseCase: DetectReservedToBookedTransitionUseCase,
    private val syncReservedToBookedUseCase: SyncReservedToBookedUseCase,
    private val addMissingBookedActivitiesUseCase: AddMissingBookedActivitiesUseCase,
    private val updateDateRangeUseCase: UpdateDateRangeUseCase,
    private val removeObsoleteSegmentsUseCase: RemoveObsoleteSegmentsUseCase,
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

    // One-shot result of an inline change-time operation: true = success,
    // false = failure (sheet stays open for retry).
    private val _changeTimeFinished = MutableLiveData<Boolean?>()
    val changeTimeFinished: LiveData<Boolean?> = _changeTimeFinished

    private val _selectedCity = MutableLiveData<City?>()

    private val _showMainViewButton = MutableLiveData(false)
    val showMainViewButton: LiveData<Boolean> = _showMainViewButton

    private val _cityMarkers = MutableLiveData<List<MapStep>>()
    val cityMarkers: LiveData<List<MapStep>> = _cityMarkers

    private val _mapRoutes = MutableLiveData<List<StepRouteInfo>>(emptyList())
    val mapRoutes: LiveData<List<StepRouteInfo>> = _mapRoutes
    private var mapRoutesJob: Job? = null

    private val _mapMarkersMode = MutableLiveData(MapMarkersMode.STEP_MARKERS)
    val mapMarkersMode: LiveData<MapMarkersMode> = _mapMarkersMode

    // Route info cache - maps segmentIndex to route info list
    private val _routeInfoCache = mutableMapOf<Int, List<StepRouteInfo>>()

    // Conflict banner dismissal: the day index on which the user dismissed it.
    // -1 = no dismissal in effect. Tracked per-day so switching back restores the dismiss.
    private var conflictBannerDismissedDayIndex: Int = -1

    private val _routeInfoUpdated = MutableLiveData<Int?>()
    val routeInfoUpdated: LiveData<Int?> = _routeInfoUpdated

    /** Flat timeline: route legs per [FlatRouteChain.key], and the requests in flight. */
    private val flatRouteCache = mutableMapOf<String, List<StepRouteInfo>>()
    private val flatRouteJobs = mutableMapOf<String, Job>()

    /** Plans already reported as generated without places, so each alerts once. */
    private val emptyRecommendationPlanIds = mutableSetOf<String>()

    private val usesFlatTimeline: Boolean
        get() = TRPCore.host.usesFlatTimeline()

    // No cities available state - shown when all destinations have invalid cityId
    private val _noCitiesAvailable = MutableLiveData<Boolean>()
    val noCitiesAvailable: LiveData<Boolean> = _noCitiesAvailable

    // Partial unavailable alert event - contains list of invalid city names
    private val _showPartialUnavailableAlert = MutableLiveData<List<String>?>()
    val showPartialUnavailableAlert: LiveData<List<String>?> = _showPartialUnavailableAlert

    private val _showOnboarding = MutableLiveData<Boolean>()
    val showOnboarding: LiveData<Boolean> = _showOnboarding
    private var onboardingCompleted = false

    // Translations for the requested language are in memory: static texts laid
    // out before the fetch completed must be re-applied.
    private val _languagesReady = MutableLiveData<Boolean>()
    val languagesReady: LiveData<Boolean> = _languagesReady

    // Scroll to new segment event - contains plan.id to scroll to
    private val _scrollToNewSegmentPlanId = MutableLiveData<String?>()
    val scrollToNewSegmentPlanId: LiveData<String?> = _scrollToNewSegmentPlanId

    // Smart recommendation: emits the selected day index once the initial
    // segment create call succeeds.
    private val _smartSegmentCreated = MutableLiveData<Int?>()
    val smartSegmentCreated: LiveData<Int?> = _smartSegmentCreated

    fun clearSmartSegmentCreated() {
        _smartSegmentCreated.value = null
    }

    // Smart recommendation: emits the error message when the initial create fails.
    private val _smartCreateError = MutableLiveData<String?>()
    val smartCreateError: LiveData<String?> = _smartCreateError

    fun clearSmartCreateError() {
        _smartCreateError.value = null
    }

    // Smart recommendation: true while the initial create-segment call is in flight.
    private val _smartCreateInProgress = MutableLiveData<Boolean>()
    val smartCreateInProgress: LiveData<Boolean> = _smartCreateInProgress

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

    /** Whether the selected day spans more than one city. */
    val hasMultipleCities: Boolean get() = hasMultipleCitiesInSelectedDay

    // Maps resolved city names (lowercase) to our system's cityIds.
    private val cityNameToIdMap = mutableMapOf<String, Int>()

    // Sync operations flag - ensures sync only runs once after initial fetch
    private var syncOperationsCompleted = false

    /**
     * Segment indices queued for background deletion (city removed from
     * itinerary, day outside trip range). Hidden from the timeline list
     * until the background DELETEs complete and the silent refresh reconciles.
     */
    private var pendingDeletionSegmentIndices: Set<Int> = emptySet()

    // Onboarding is dispatched only once, after the first timeline load.
    private var onboardingDispatched = false

    // True once the initial day is auto-selected; later refreshes must not
    // override an explicit user selection.
    private var initialDayAutoSelected = false

    private var isShowingStepMarkersInMultiCity: Boolean = false
    private var selectedStepId: String? = null

    // =====================
    // LIFECYCLE
    // =====================

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

        _tripHash = arguments?.getString(TRPCore.EXTRA_TRIP_HASH) ?: ""
        itinerary = arguments?.getParcelable(TRPCore.EXTRA_ITINERARY)
        uniqueId = arguments?.getString(TRPCore.EXTRA_UNIQUE_ID)

        val language = arguments?.getString(TRPCore.EXTRA_APP_LANGUAGE)
        if (!language.isNullOrEmpty()) {
            TRPCore.core.appConfig.appLanguage = language
            miscRepository.changeLanguage(language)
        }

        val currencyFromIntent = arguments?.getString(TRPCore.EXTRA_APP_CURRENCY)
        val currencyFromPrefs = TRPCore.core.miscRepository.getSavedCurrency()

        val currencyInput = when {
            !currencyFromIntent.isNullOrEmpty() -> currencyFromIntent
            currencyFromPrefs.isNotEmpty() -> currencyFromPrefs
            else -> "EUR"
        }

        val currencyCode = com.tripian.trpcore.util.CurrencyUtil.resolveCurrencyCode(currencyInput)
        TRPCore.core.appConfig.appCurrency = currencyCode
        TRPCore.core.trpRest.setCurrency(currencyCode)

        if (_tripHash.isEmpty()) {
            _tripHash = arguments?.getString(ARG_TRIP_HASH) ?: ""
        }

        performLightLoginInBackground()

        ensureLanguagesLoadedThenProceed()
    }

    /**
     * Ensures languages are loaded before proceeding with timeline operations.
     * If translations cannot be obtained within [LANGUAGE_RETRY_TIMEOUT_SECONDS],
     * dispatches [TRPCoreErrorCode.LANGUAGE_LOAD_FAILED] and closes the SDK.
     */
    private fun ensureLanguagesLoadedThenProceed() {
        if (miscRepository.isLanguagesLoaded) {
            _languagesReady.value = true
            showFullScreenLoader(LanguageConst.LOADING_TEXT_GETTING_ITINERARY_PLAN, "")
            proceedAfterLanguagesLoaded()
            return
        }

        showFullScreenLoaderNoText()
        attemptLanguageFetch(allowRetry = true)
    }

    /**
     * Drives the translation fetch with one explicit retry; the retry forces a
     * brand-new /languages request via [MiscRepository.refetchLanguages].
     */
    private fun attemptLanguageFetch(allowRetry: Boolean) {
        viewModelScope.launch {
            try {
                val loaded = kotlinx.coroutines.withTimeout(
                    LANGUAGE_RETRY_TIMEOUT_SECONDS * 1000
                ) {
                    if (allowRetry) {
                        miscRepository.waitForLanguagesLoadedAsync()
                    } else {
                        miscRepository.refetchLanguagesAsync()
                    }
                }
                if (loaded && miscRepository.isLanguagesLoaded) {
                    _languagesReady.value = true
                    waitForLoginThenProceed {
                        showFullScreenLoader(LanguageConst.LOADING_TEXT_GETTING_ITINERARY_PLAN, "")
                        proceedAfterLanguagesLoaded()
                    }
                } else if (allowRetry) {
                    attemptLanguageFetch(allowRetry = false)
                } else {
                    failLanguageLoad("Translation fetch returned no data")
                }
            } catch (error: Throwable) {
                if (allowRetry) {
                    attemptLanguageFetch(allowRetry = false)
                } else {
                    failLanguageLoad(error.message ?: "Translation fetch failed")
                }
            }
        }
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
     * Applies the language, waits for the parallel light-login to finish,
     * then kicks off city resolution + the timeline fetch.
     */
    private fun proceedAfterLanguagesLoaded() {
        val language = arguments?.getString(TRPCore.EXTRA_APP_LANGUAGE)
        if (!language.isNullOrEmpty()) {
            miscRepository.changeLanguage(language)
        }

        waitForLoginThenProceed {
            resolveDestinationCitiesAndProceed()
        }
    }

    /**
     * Resolves destination cities and proceeds with timeline operations.
     * Called after login is complete (login runs in background when ACTimeline opens).
     * Host-provided cityIds are unreliable; resolution uses coordinates and names only.
     * If city resolution fails (cityId=0), shows error and closes SDK.
     */
    private fun resolveDestinationCitiesAndProceed() {
        val destinationItems = itinerary?.destinationItems

        if (destinationItems.isNullOrEmpty()) {
            proceedWithTimelineOperations()
            return
        }

        showFullScreenLoader(LanguageConst.LOADING_TEXT_GETTING_ITINERARY_PLAN, "")

        val resolvedCities = mutableListOf<City>()
        val unresolvedCoordinates = mutableListOf<Coordinate>()
        val unresolvedCityNames = mutableListOf<String>()

        destinationItems.forEach { item ->
            val city = item.getCoordinateObject()?.let { coord ->
                tripRepository.findCityByCoordinate(coord.lat, coord.lng)
            } ?: tripRepository.findCityByName(item.title, item.countryName)

            if (city != null) {
                resolvedCities.add(city)
            } else {
                item.getCoordinateObject()?.let { coord ->
                    unresolvedCoordinates.add(Coordinate().apply {
                        lat = coord.lat
                        lng = coord.lng
                    })
                }
                item.title?.let { unresolvedCityNames.add(it) }
            }
        }

        if (unresolvedCoordinates.isEmpty()) {
            val uniqueCities = resolvedCities.distinctBy { it.id }
            if (uniqueCities.isNotEmpty()) {
                _cities.value = uniqueCities
                updateItineraryWithResolvedCities(resolvedCities)
            }
            proceedWithTimelineOperations()
            return
        }

        viewModelScope.launch {
            runCatching {
                tripRepository.resolveCitiesByCoordinatesAsync(unresolvedCoordinates)
            }.onSuccess { result ->
                handleCityResolveResult(result, resolvedCities, unresolvedCityNames)
            }.onFailure {
                if (resolvedCities.isNotEmpty()) {
                    _cities.value = resolvedCities.distinctBy { it.id }
                    updateItineraryWithResolvedCities(resolvedCities)
                    proceedWithTimelineOperations()
                } else if (_tripHash.isNotEmpty()) {
                    val warningMsg = getLanguageForKey(LanguageConst.CITY_NOT_SUPPORTED)
                        .replace("%s", unresolvedCityNames.joinToString(", "))
                    showAlert(AlertType.WARNING, warningMsg)
                    proceedWithTimelineOperations()
                } else {
                    hideLoading()
                    val errorMsg = getLanguageForKey(LanguageConst.CITY_NOT_SUPPORTED)
                        .replace("%s", unresolvedCityNames.joinToString(", "))
                    TRPCore.notifyError(errorMsg)
                    TRPCore.closeSDK()
                }
            }
        }
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
                updateItineraryWithResolvedCities(cachedCities)
                proceedWithTimelineOperations()
            }
            is CityResolveResult.PartialSuccess -> {
                cachedCities.addAll(result.cities)
                val uniqueCities = cachedCities.distinctBy { it.id }
                _cities.value = uniqueCities
                updateItineraryWithResolvedCities(cachedCities)

                val warningMsg = getLanguageForKey(LanguageConst.CITY_NOT_SUPPORTED)
                    .replace("%s", result.unresolvedCityNames.joinToString(", "))
                showAlert(AlertType.WARNING, warningMsg)

                proceedWithTimelineOperations()
            }
            is CityResolveResult.AllFailed -> {
                if (cachedCities.isNotEmpty()) {
                    _cities.value = cachedCities.distinctBy { it.id }
                    updateItineraryWithResolvedCities(cachedCities)
                    proceedWithTimelineOperations()
                } else if (_tripHash.isNotEmpty()) {
                    val warningMsg = getLanguageForKey(LanguageConst.CITY_NOT_SUPPORTED)
                        .replace("%s", fallbackCityNames.joinToString(", "))
                    showAlert(AlertType.WARNING, warningMsg)
                    proceedWithTimelineOperations()
                } else {
                    hideLoading()
                    _noCitiesAvailable.value = true
                }
            }
        }
    }

    /**
     * Updates itinerary.destinationItems with resolved cityIds and builds the
     * cityName → cityId mapping.
     */
    private fun updateItineraryWithResolvedCities(resolvedCities: List<City>) {
        val currentItinerary = itinerary ?: return

        val updatedDestinations = currentItinerary.destinationItems.map { item ->
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

        resolvedCities.forEach { city ->
            city.name?.takeIf { it.isNotBlank() }?.let { name ->
                cityNameToIdMap[name.cityNameKey()] = city.id
            }
        }
    }

    /**
     * Performs light login in background immediately when ACTimeline opens.
     * Runs in parallel with language loading and onboarding.
     */
    private fun performLightLoginInBackground() {
        if (isLoginInProgress || isLoggedIn) return

        isLoginInProgress = true
        _error.value = null

        viewModelScope.launch {
            runCatching { doLightLogin(DoLightLogin.Params(uniqueId = uniqueId)) }
                .onSuccess {
                    isLoggedIn = true
                    isLoginInProgress = false
                }
                .onFailure { t ->
                    val errorModel = t.toErrorModel()
                    isLoginInProgress = false
                    _error.value = errorModel.errorDesc ?: "Login failed"
                    TRPCore.notifyError(errorModel.errorDesc ?: "Login failed")
                }
        }
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
            performLightLoginInBackground()
        }

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
        updateSavedPlansCount()

        when {
            _tripHash.isNotEmpty() -> {
                fetchTimeline()
            }

            itinerary != null && TRPCore.host.createsTimelineWithoutCityResolution(itinerary!!) -> {
                // Host policy: destinations already carry trusted cityIds — create
                // the timeline WITHOUT re-resolving them.
                createTimelineFromItinerary()
            }

            itinerary != null -> {
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
     * Host-provided cityIds are cleared first — they may be invalid.
     */
    private fun resolveCitiesAndCreateTimeline() {
        val destinations = itinerary!!.destinationItems.map { it.copy(cityId = null) }

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

        viewModelScope.launch {
            runCatching { resolveCitiesUseCase(ResolveCitiesUseCase.Params(coordinates)) }
                .onSuccess { resolvedCities ->
                    val updatedDestinations = updateDestinationsWithCityIds(destinations, resolvedCities)
                    itinerary = itinerary!!.copy(destinationItems = updatedDestinations)
                    validateAndCreateTimeline(updatedDestinations)
                }
                .onFailure { t ->
                    val errorModel = t.toErrorModel()
                    hideLoading()
                    _error.value = errorModel.errorDesc ?: "City resolve failed"
                    TRPCore.notifyError(errorModel.errorDesc ?: "City resolve failed")
                }
        }
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
            validDestinations.isEmpty() -> {
                hideLoading()
                _noCitiesAvailable.value = true
            }

            invalidDestinations.isNotEmpty() -> {
                val invalidCityNames = invalidDestinations.map { it.title }
                _showPartialUnavailableAlert.value = invalidCityNames
                createTimelineWithValidDestinations(validDestinations)
            }

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

        showFullScreenLoader(LanguageConst.LOADING_TEXT_GETTING_ITINERARY_PLAN, "")
        _error.value = null

        viewModelScope.launch {
            runCatching { createTimelineUseCase(CreateTimelineUseCase.Params(modifiedItinerary)) }
                .onSuccess { timeline ->
                    _tripHash = timeline.tripHash ?: ""
                    if (_tripHash.isNotEmpty()) {
                        TRPCore.notifyTimelineCreated(_tripHash)
                        // Host policy: persist the hash if the host manages it internally.
                        TRPCore.host.onTimelineCreated(preferences, _tripHash)
                        waitForTimelineGeneration()
                    } else {
                        processTimeline(timeline)
                        hideLottieLoading()
                    }
                }
                .onFailure { t ->
                    val errorModel = t.toErrorModel()
                    _error.value = errorModel.errorDesc
                    TRPCore.notifyError(errorModel.errorDesc ?: "Timeline creation failed")
                    hideLottieLoading()
                }
        }
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
     * Updates the saved plans badge count via [getFilteredFavorites].
     * `timeline` is accepted for API compatibility but not consulted;
     * [getFilteredFavorites] reads `_timeline.value`.
     */
    @Suppress("UNUSED_PARAMETER")
    private fun updateSavedPlansCount(timeline: Timeline? = null) {
        _savedPlansCount.value = getFilteredFavorites().size
    }

    // =====================
    // TIMELINE CREATION FROM ITINERARY
    // =====================

    /**
     * Creates timeline from ItineraryWithActivities: calls createTimeline,
     * stores the returned hash, polls generation, then displays the timeline.
     */
    private fun createTimelineFromItinerary() {
        val itineraryData = itinerary ?: return

        showFullScreenLoader(LanguageConst.LOADING_TEXT_GETTING_ITINERARY_PLAN, "")
        _error.value = null

        viewModelScope.launch {
            resolveActivityCityIds(itineraryData, knownActivityIds = emptySet())
            val resolvedItinerary = itinerary ?: itineraryData
            runCatching { createTimelineUseCase(CreateTimelineUseCase.Params(resolvedItinerary)) }
                .onSuccess { timeline ->
                    _tripHash = timeline.tripHash ?: ""
                    if (_tripHash.isNotEmpty()) {
                        TRPCore.notifyTimelineCreated(_tripHash)
                        // Host policy: persist the hash if the host manages it internally.
                        TRPCore.host.onTimelineCreated(preferences, _tripHash)
                        waitForTimelineGeneration()
                    } else {
                        processTimeline(timeline)
                        hideLottieLoading()
                    }
                }
                .onFailure { t ->
                    val errorModel = t.toErrorModel()
                    _error.value = errorModel.errorDesc
                    TRPCore.notifyError(errorModel.errorDesc ?: "Timeline creation failed")
                    hideLottieLoading()
                }
        }
    }

    /**
     * Waits until timeline generation is complete
     */
    private fun waitForTimelineGeneration() {
        viewModelScope.launch {
            runCatching { waitForGenerationUseCase(WaitForGenerationUseCase.Params(_tripHash)) }
                .onSuccess { timeline ->
                    processTimeline(timeline)
                    hideLottieLoading()
                }
                .onFailure { t ->
                    val errorModel = t.toErrorModel()
                    hideLottieLoading()
                    _error.value = errorModel.errorDesc ?: "Timeline generation failed"
                    TRPCore.notifyError(errorModel.errorDesc ?: "Timeline generation failed")
                }
        }
    }

    // =====================
    // FETCH & REFRESH
    // =====================

    /**
     * Initial timeline fetch. Reuses the single-text loader from the language-load step.
     */
    fun fetchTimeline() {
        availabilityCheckManager.reset()
        showFullScreenLoader(LanguageConst.LOADING_TEXT_GETTING_ITINERARY_PLAN, "")
        _error.value = null

        viewModelScope.launch {
            runCatching { fetchTimelineUseCase(FetchTimelineUseCase.Params(_tripHash)) }
                .onSuccess { timeline ->
                    val itineraryData = itinerary
                    when {
                        // Host policy: when the new reservations fall entirely
                        // outside the stored timeline's date range → drop the stored
                        // reference locally and create a fresh one. The old timeline
                        // is left on the server. Default hosts keep the existing one.
                        itineraryData != null &&
                            TRPCore.host.recreatesTimelineOnDateMismatch(timeline) &&
                            !timelineDatesOverlapItinerary(timeline, itineraryData) ->
                            recreateTimelineForNewDates()

                        !syncOperationsCompleted && itinerary != null ->
                            runInitialSyncThenFinalize(timeline)

                        else -> {
                            processTimeline(timeline)
                            hideLottieLoading()
                        }
                    }
                }
                .onFailure { t ->
                    val errorModel = t.toErrorModel()
                    _error.value = errorModel.errorDesc
                    TRPCore.notifyError(errorModel.errorDesc ?: "Timeline fetch failed")
                    hideLottieLoading()
                }
        }
    }

    /**
     * nexus: true when the stored timeline's date range overlaps the new
     * itinerary's range (inclusive). When it doesn't, the timeline is recreated.
     * Falls back to true (keep the timeline) when either range can't be parsed.
     */
    private fun timelineDatesOverlapItinerary(
        timeline: Timeline,
        itineraryData: ItineraryWithActivities
    ): Boolean {
        val days = calculateAvailableDays(timeline)
        val existingStart = days.firstOrNull()
        val existingEnd = days.lastOrNull()
        val newStart = itineraryData.startDatetime.toDate()
        val newEnd = itineraryData.endDatetime.toDate()
        if (existingStart == null || existingEnd == null || newStart == null || newEnd == null) {
            return true
        }
        // Overlap iff existingStart <= newEnd AND newStart <= existingEnd.
        return !existingStart.after(newEnd) && !newStart.after(existingEnd)
    }

    /**
     * nexus: forget the stored timeline locally and create a fresh one for the
     * new dates. The old timeline is intentionally NOT deleted on the server —
     * we only clear the local stored hash so the next open resolves to the new
     * trip.
     */
    private fun recreateTimelineForNewDates() {
        showFullScreenLoader(LanguageConst.LOADING_TEXT_GETTING_ITINERARY_PLAN, "")
        _error.value = null
        viewModelScope.launch {
            TRPCore.host.clearStoredTripHash(preferences)
            _tripHash = ""
            createTimelineFromItinerary()
        }
    }

    fun refreshTimeline() {
        availabilityCheckManager.reset()
        com.tripian.trpcore.domain.manager.TimelineRefreshState.setRefreshing()
        showLottieLoading()
        clearRouteInfoCache()

        viewModelScope.launch {
            runCatching { fetchTimelineUseCase(FetchTimelineUseCase.Params(_tripHash)) }
                .onSuccess { timeline ->
                    processTimeline(timeline)
                    hideLottieLoading()
                    com.tripian.trpcore.domain.manager.TimelineRefreshState.setCompleted()
                    com.tripian.trpcore.domain.manager.TimelineRefreshState.setIdle()
                }
                .onFailure { t ->
                    val errorModel = t.toErrorModel()
                    hideLottieLoading()
                    com.tripian.trpcore.domain.manager.TimelineRefreshState
                        .setFailed(Throwable(errorModel.errorDesc ?: "Timeline refresh failed"))
                    com.tripian.trpcore.domain.manager.TimelineRefreshState.setIdle()
                }
        }
    }

    /**
     * Called when returning from the SavedPlans screen. Applies the cached
     * freshly-generated timeline when present; otherwise refreshes the
     * saved-plans badge only.
     */
    fun onReturnFromSavedPlans() {
        val cached = timelineRepository.consumeGeneratedTimeline(_tripHash)
        if (cached != null) {
            processTimeline(cached)
        } else {
            updateSavedPlansCount()
        }
    }

    /**
     * Called when returning from the AddPlan / manual listing flow after a segment
     * was created. Applies the cached freshly-generated timeline when present;
     * falls back to a refresh only when no cache is present.
     */
    fun onReturnFromAddPlan() {
        val cached = timelineRepository.consumeGeneratedTimeline(_tripHash)
        if (cached != null) {
            processTimeline(cached)
        } else {
            refreshTimeline()
        }
    }

    // =====================
    // DATA PROCESSING
    // =====================

    /**
     * Backfills missing segment cityIds from the plan at the same index
     * (plans are parallel-indexed with segments; coordinate matching would
     * collide for close-by destinations).
     */
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

        updateSavedPlansCount()
    }

    /**
     * Publishes a timeline snapshot to the UI: backfills segment cityIds, recomputes
     * cities/days/display items, and restarts the availability sweep. Onboarding is
     * dispatched once, posted to the next looper cycle so the rendered timeline
     * commits before the bottom sheet appears.
     */
    private fun processTimeline(timeline: Timeline) {
        populateCitiesInSegments(timeline)
        applyCachedAvailabilityPrices(timeline)

        _timeline.value = timeline

        updateSavedPlansCount(timeline)

        timeline.tripHash.let { hash ->
            if (hash.isNotEmpty()) {
                TRPCore.notifyTimelineLoaded(hash)
            }
        }

        val uniqueCities = extractCities(timeline)
        _cities.value = uniqueCities
        com.tripian.trpcore.util.CityTimeZones.register(uniqueCities)

        val days = calculateAvailableDays(timeline)
        _availableDays.value = days

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

        updateDisplayItems()

        if (!onboardingDispatched) {
            onboardingDispatched = true
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                checkAndShowOnboarding()
            }
        }

        availabilityCheckManager.reset()
        triggerAvailabilitySweep(timeline)
    }

    override fun onCleared() {
        availabilityCheckManager.cancel()
        super.onCleared()
    }

    /**
     * Invokes the post-load availability sweep against `/schedule-bulk` and applies
     * the resulting `isAvailabilityExpired` flags to timeline segments and itinerary
     * steps. Safe to call repeatedly — the manager guards against duplicate runs.
     */
    private fun triggerAvailabilitySweep(timeline: Timeline) {
        // Host policy: a host can disable the schedule-bulk availability sweep.
        if (!TRPCore.host.runsAvailabilitySweep()) return
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
                override fun onItemUpdated(
                    segmentIndex: Int,
                    stepId: Int?,
                    isExpired: Boolean,
                    price: Double?
                ) {
                    val tl = _timeline.value ?: return
                    val segment = tl.tripProfile?.segments?.getOrNull(segmentIndex) ?: return
                    if (stepId == null) {
                        segment.additionalData?.isAvailabilityExpired = isExpired
                        segment.additionalData?.applyScheduledPrice(price, currency)
                    } else {
                        val step = tl.plans?.getOrNull(segmentIndex)?.steps
                            ?.firstOrNull { it.id == stepId }
                        step?.isAvailabilityExpired = isExpired
                        step?.poi?.applyScheduledPrice(price, currency)
                    }
                }

                override fun onDayCompleted() {
                    updateDisplayItems()
                }
            },
            onCompleted = {
                updateDisplayItems()
            }
        )
    }

    /**
     * Re-renders the timeline in the language that just became active: cards read
     * their labels at bind time, so re-publishing the model is enough.
     */
    fun onLanguageApplied() {
        if (_timeline.value == null) return
        republishCurrentTimeline()
    }

    /**
     * Re-applies prices an earlier sweep resolved, so a freshly fetched timeline
     * does not surface the price stored on the segment while the next sweep runs.
     */
    private fun applyCachedAvailabilityPrices(timeline: Timeline) {
        val currency = TRPCore.core.appConfig.appCurrency
        availabilityCheckManager.applyCachedPrices(timeline) { segmentIndex, stepId, price, cached ->
            val segment = timeline.tripProfile?.segments?.getOrNull(segmentIndex)
            if (stepId == null) {
                segment?.additionalData?.applyScheduledPrice(price, cached ?: currency)
            } else {
                timeline.plans?.getOrNull(segmentIndex)?.steps
                    ?.firstOrNull { it.id == stepId }
                    ?.poi
                    ?.applyScheduledPrice(price, cached ?: currency)
            }
        }
    }

    /**
     * Extracts cities from the timeline, preferring cached City models for complete
     * data. destinationItems (SDK input) take priority, then timeline cities,
     * deduplicated by id. Host-provided cityIds are unreliable; resolution uses
     * coordinates and names only.
     */
    private fun extractCities(timeline: Timeline): List<City> {
        val cities = mutableListOf<City>()
        val addedCityIds = mutableSetOf<Int>()

        itinerary?.destinationItems?.forEach { item ->
            val city = item.getCoordinateObject()?.let { coord ->
                tripRepository.findCityByCoordinate(coord.lat, coord.lng)
            } ?: tripRepository.findCityByName(item.title, item.countryName)

            if (city != null && !addedCityIds.contains(city.id)) {
                city.name = item.title
                cities.add(city)
                addedCityIds.add(city.id)
            }
        }

        val cityIds = mutableSetOf<Int>()
        val cityNames = mutableSetOf<String>()

        timeline.plans?.forEach { plan ->
            plan.city?.id?.let { if (it != 0) cityIds.add(it) }
            plan.city?.name?.let { cityNames.add(it) }
        }

        timeline.city?.id?.let { if (it != 0) cityIds.add(it) }
        timeline.city?.name?.let { cityNames.add(it) }

        timeline.tripProfile?.segments?.forEach { segment ->
            segment.cityId?.let { if (it != 0) cityIds.add(it) }
        }

        cityIds.forEach { cityId ->
            if (!addedCityIds.contains(cityId)) {
                val cachedCity = tripRepository.getCachedCityById(cityId)
                if (cachedCity != null) {
                    cities.add(cachedCity)
                    addedCityIds.add(cachedCity.id)
                } else {
                    val timelineCity = timeline.plans?.find { it.city?.id == cityId }?.city
                        ?: if (timeline.city?.id == cityId) timeline.city else null
                    timelineCity?.let {
                        cities.add(it)
                        addedCityIds.add(it.id)
                    }
                }
            }
        }

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

    /**
     * Computes the trip's day range. The host-supplied itinerary range is the
     * primary source (the server's TimelineDate segment can lag behind it while
     * the background date-range sync is still in flight); falls back to the
     * TimelineDate sentinel segment, then to a min/max scan of all segments.
     */
    private fun calculateAvailableDays(timeline: Timeline): List<Date> {
        itinerary?.buildTimelineDateSegment()?.let { hostRange ->
            val start = hostRange.startDate?.toDate()
            val end = hostRange.endDate?.toDate()
            if (start != null && end != null) {
                return generateDateRange(start, end)
            }
        }

        val segments = timeline.tripProfile?.segments ?: return emptyList()

        segments.firstOrNull { it.title == "TimelineDate" && !it.available }
            ?.let { sentinel ->
                val start = sentinel.startDate?.toDate()
                val end = sentinel.endDate?.toDate()
                if (start != null && end != null) {
                    return generateDateRange(start, end)
                }
            }

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
        selectedStepId = null
        collapsedSectionCityIds.clear()
        updateDisplayItems()
    }

    /**
     * Rebuilds display items for the selected day, preserving expand states and
     * cached route info so async refreshes don't wipe them off the UI.
     */
    private fun updateDisplayItems() {
        val items = buildDisplayItems() ?: return
        _displayItems.value = items

        if (usesFlatTimeline) {
            requestMissingFlatRoutes(items)
            reportPlanGeneratedWithoutPois()
        }

        updateMapSteps()
    }

    /**
     * Builds the selected day's display items, carrying over the Recommendations
     * expand state and cached route info, with the conflict banner prepended when due.
     * Null when the timeline or the selected day is not available.
     */
    private fun buildDisplayItems(): List<TimelineDisplayItem>? {
        val timeline = _timeline.value ?: return null
        val days = _availableDays.value ?: return null
        val selectedIndex = _selectedDayIndex.value ?: 0

        if (selectedIndex >= days.size) return null

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
            emptyStateMessage = getLanguageForKey(LanguageConst.NO_PLANS_YET),
            hiddenSegmentIndices = pendingDeletionSegmentIndices,
            flatRoutes = flatRouteCache
        )

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

        return injectConflictBannerIfNeeded(itemsWithPreservedState, selectedIndex)
    }

    /**
     * Prepends a [TimelineDisplayItem.ConflictWarning] when the day's items contain a
     * conflict and the user hasn't dismissed the banner on this day.
     */
    private fun injectConflictBannerIfNeeded(
        items: List<TimelineDisplayItem>,
        dayIndex: Int
    ): List<TimelineDisplayItem> {
        if (dayIndex == conflictBannerDismissedDayIndex) return items
        val hasConflict = items.any {
            (it is TimelineDisplayItem.BookedActivity && it.hasConflict) ||
                    (it is TimelineDisplayItem.ManualPoi && it.hasConflict) ||
                    (it is TimelineDisplayItem.PlanStep && it.hasConflict) ||
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

    /**
     * Flips the expanded/collapsed state of the Recommendations cell for [planId].
     * State is written back into `_displayItems` so [updateDisplayItems]'
     * preservation pass keeps the choice across full refreshes.
     */
    fun toggleRecommendationExpanded(planId: String) {
        val current = _displayItems.value ?: return
        val updated = current.map { item ->
            if (item is TimelineDisplayItem.Recommendations && item.plan.id == planId) {
                item.copy(isExpanded = !item.isExpanded)
            } else {
                item
            }
        }
        _displayItems.value = updated
    }

    // =====================
    // SMART RECOMMENDATIONS
    // =====================

    /**
     * "yyyy-MM-dd" → the activity ids that day already holds, in API form. Handed to
     * the AddPlan flow, which blocks days already holding the picked activity and
     * sends the chosen day's ids as `excludedActivityIds`.
     */
    fun plannedActivityIdsByDay(): Map<String, List<String>> =
        _timeline.value?.plannedActivityIdsByDay() ?: emptyMap()

    /**
     * Activity ids the engine must not suggest for [dayKey]: booked/reserved activities
     * anywhere in the trip, plus activity steps already planned on that day.
     *
     * @param dayKey "yyyy-MM-dd" of the day the new segment covers.
     * @param cityId fallback city for planned activities that carry none.
     */
    /**
     * Exclusions that hold for every day of the trip: activities already booked or
     * reserved anywhere in it, plus favorites the user removed from the timeline.
     * The AddPlan flow adds the chosen day's own ids on top of these.
     */
    /** Trip window as "yyyy-MM-dd"; POI detail scopes its product query to it. */
    fun tripDateRange(): Pair<String?, String?> {
        val days = _availableDays.value.orEmpty()
        return days.firstOrNull()?.toApiDateString() to days.lastOrNull()?.toApiDateString()
    }

    fun tripWideExcludedActivityIds(): List<String> {
        val bookings = _timeline.value
            ?.plannedActivities()
            ?.filter { it.source == PlannedActivitySource.BOOKING }
            ?.map { ActivityIdFormat.make(it.productId, it.providerId, it.cityId) }
            .orEmpty()

        val removedFavorites = com.tripian.trpcore.util.RemovedFavoritesStore
            .removedBaseIds(preferences, _tripHash)
            .map { ActivityIdFormat.make(it) }

        return (bookings + removedFavorites)
            .filter { it.isNotEmpty() }
            .distinct()
    }

    private fun collectExcludedActivityIds(dayKey: String, cityId: Int): List<String> {
        val planned = _timeline.value
            ?.plannedActivities()
            ?.filter { it.source == PlannedActivitySource.BOOKING || it.day == dayKey }
            ?.map { ActivityIdFormat.make(it.productId, it.providerId, it.cityId ?: cityId) }
            .orEmpty()

        val removedFavorites = com.tripian.trpcore.util.RemovedFavoritesStore
            .removedBaseIds(preferences, _tripHash)
            .map { ActivityIdFormat.make(it, cityId = cityId) }

        return (planned + removedFavorites)
            .filter { it.isNotEmpty() }
            .distinct()
    }

    fun createSmartRecommendationSegment(data: AddPlanData) {
        val city = data.selectedCity
        val selectedDate = data.selectedDate

        if (city == null || selectedDate == null) {
            return
        }

        val validCity = if (city.id == 0 && city.name != null) {
            tripRepository.findCityByName(city.name!!) ?: city
        } else {
            city
        }

        if (validCity.id == 0) {
            _error.value = getLanguageForKey(com.tripian.trpcore.util.LanguageConst.COMMON_ERROR)
            return
        }

        existingPlanIds = _timeline.value?.plans
            ?.map { it.id }
            ?.filter { it.isNotEmpty() }
            ?.toSet() ?: emptySet()

        _smartCreateInProgress.value = true

        val title = generateSegmentTitle(validCity, selectedDate)

        val dateStr = selectedDate.toApiDateString()
        val startDateTimeStr = if (data.startTime != null) {
            "$dateStr ${data.startTime}"
        } else {
            "$dateStr 10:00"
        }
        val endDateTimeStr = if (data.endTime != null) {
            "$dateStr ${data.endTime}"
        } else {
            "$dateStr 18:00"
        }

        val excludedActivityIds = collectExcludedActivityIds(dateStr, validCity.id)

        viewModelScope.launch {
            runCatching {
                createSegmentUseCase(
                    CreateSegmentUseCase.Params(
                        tripHash = _tripHash,
                        title = title,
                        cityId = validCity.id,
                        startDate = startDateTimeStr,
                        endDate = endDateTimeStr,
                        adults = data.travelers,
                        children = 0,
                        activityFreeText = data.smartCategoriesAsString,
                        excludedActivityIds = excludedActivityIds,
                        smartRecommendation = true,
                        accommodation = data.startingPointAccommodation
                    )
                )
            }
                .onSuccess {
                    _smartCreateInProgress.value = false
                    _smartSegmentCreated.value = data.selectedDayIndex
                    showLottieLoading()
                    waitForSegmentGeneration()
                }
                .onFailure { t ->
                    val errorModel = t.toErrorModel()
                    _smartCreateInProgress.value = false
                    _smartCreateError.value = errorModel.errorDesc
                }
        }
    }

    private fun waitForSegmentGeneration() {
        viewModelScope.launch {
            runCatching { waitForGenerationUseCase(WaitForGenerationUseCase.Params(_tripHash)) }
                .onSuccess { timeline ->
                    val newPlanId = try {
                        timeline.plans?.find { plan ->
                            plan.id.isNotEmpty() && plan.id !in existingPlanIds
                        }?.id
                    } catch (e: Exception) {
                        null
                    }
                    processTimeline(timeline)
                    if (!newPlanId.isNullOrEmpty()) {
                        _scrollToNewSegmentPlanId.value = newPlanId
                    }
                    hideLottieLoading()
                }
                .onFailure {
                    hideLottieLoading()
                    refreshTimeline()
                }
        }
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

        viewModelScope.launch {
            runCatching { deleteSegmentUseCase(DeleteSegmentUseCase.Params(_tripHash, segmentIndex)) }
                .onSuccess {
                    val mutated = applyLocalSegmentDelete(segmentIndex)
                    hideLottieLoading()
                    if (mutated) {
                        republishCurrentTimeline()
                    } else {
                        refreshTimeline()
                    }
                }
                .onFailure { t ->
                    val errorModel = t.toErrorModel()
                    _error.value = errorModel.errorDesc
                    hideLottieLoading()
                }
        }
    }

    /**
     * Removes the segment at [segmentIndex] from the in-memory timeline cache.
     * `plans` is parallel-indexed with `tripProfile.segments`, so the matching
     * plan is dropped too. Returns `true` when the cache actually mutated.
     */
    private fun applyLocalSegmentDelete(segmentIndex: Int): Boolean {
        val tl = _timeline.value ?: return false
        val segments = tl.tripProfile?.segments ?: return false
        if (segmentIndex !in segments.indices) return false

        val mutableSegments = segments as? MutableList<TimelineSegment>
            ?: segments.toMutableList().also { tl.tripProfile?.segments = it }
        mutableSegments.removeAt(segmentIndex)

        tl.plans?.let { plans ->
            if (segmentIndex < plans.size) {
                val mutablePlans =
                    plans as? MutableList<com.tripian.one.api.timeline.model.TimelinePlan>
                        ?: plans.toMutableList().also { tl.plans = it }
                mutablePlans.removeAt(segmentIndex)
            }
        }
        return true
    }

    fun deleteStep(stepId: Int) {
        showBottomSheetLoader(LanguageConst.LOADING_TEXT_REMOVING_FROM_PLAN, "Removing from plan")

        viewModelScope.launch {
            runCatching { deleteStepUseCase(DeleteStepUseCase.Params(stepId)) }
                .onSuccess {
                    val mutated = applyLocalStepDelete(stepId)
                    hideLottieLoading()
                    if (mutated) {
                        republishCurrentTimeline()
                    } else {
                        refreshTimeline()
                    }
                }
                .onFailure { t ->
                    val errorModel = t.toErrorModel()
                    _error.value = errorModel.errorDesc
                    hideLottieLoading()
                }
        }
    }

    /**
     * Removes the step with [stepId] from the in-memory timeline cache.
     * Returns `true` if the cached structure actually changed — callers use
     * this to decide between a cheap republish and a full refetch.
     */
    private fun applyLocalStepDelete(stepId: Int): Boolean {
        val tl = _timeline.value ?: return false
        var removedSomewhere = false
        tl.plans?.forEach { plan ->
            val current = plan.steps ?: return@forEach
            val mutable = current as? MutableList<com.tripian.one.api.timeline.model.TimelineStep>
                ?: current.toMutableList().also { plan.steps = it }
            if (mutable.removeAll { it.id == stepId }) {
                removedSomewhere = true
            }
        }
        return removedSomewhere
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
    fun updateStepTime(
        stepId: Int,
        startTime: String?,
        endTime: String?,
        // See [updateSegmentTime]'s useInlineLoader.
        useInlineLoader: Boolean = false,
        // Direct result callback for callers that own their own sheet instance
        // (e.g. a plain TimeSelectionBottomSheet local val) instead of routing
        // through the shared [changeTimeFinished] LiveData.
        onInlineResult: ((success: Boolean) -> Unit)? = null
    ) {
        if (startTime == null && endTime == null) return

        val resolvedEndTime = endTime ?: startTime?.let { start -> stepEndTimeFor(stepId, start) }

        if (!useInlineLoader) {
            showBottomSheetLoader(LanguageConst.LOADING_TEXT_CHANGING_TIME, "Changing time")
        }

        viewModelScope.launch {
            runCatching {
                updateStepTimeUseCase(
                    UpdateStepTimeUseCase.Params(
                        stepId = stepId,
                        startTime = startTime,
                        endTime = resolvedEndTime
                    )
                )
            }
                .onSuccess {
                    reloadTimelineAndFinishTimeChange(useInlineLoader) {
                        applyLocalStepTimeUpdate(stepId, startTime, resolvedEndTime)
                    }
                    onInlineResult?.invoke(true)
                }
                .onFailure { t ->
                    val errorModel = t.toErrorModel()
                    if (!useInlineLoader) hideLottieLoading()
                    _error.value = errorModel.errorDesc
                    if (useInlineLoader) _changeTimeFinished.value = false
                    onInlineResult?.invoke(false)
                }
        }
    }

    /**
     * End time for a step moved to [newStartTime], keeping the span it already had.
     * The step endpoint rejects an update that carries no end time, and the caller
     * only knows one when the activity reported a duration.
     */
    private fun stepEndTimeFor(stepId: Int, newStartTime: String): String {
        val step = _timeline.value?.plans
            ?.firstNotNullOfOrNull { plan -> plan.steps?.firstOrNull { it.id == stepId } }
        val minutes = minutesBetween(step?.startDateTimes, step?.endDateTimes)
            ?: DEFAULT_STEP_DURATION_MINUTES
        return addMinutesToHourMinute(newStartTime, minutes)
    }

    private fun minutesBetween(start: String?, end: String?): Int? {
        val startMinutes = hourMinuteToMinutes(start) ?: return null
        val endMinutes = hourMinuteToMinutes(end) ?: return null
        val diff = endMinutes - startMinutes
        return diff.takeIf { it > 0 }
    }

    private fun hourMinuteToMinutes(dateTime: String?): Int? {
        val time = dateTime?.substringAfter(' ', "")?.takeIf { it.length >= 5 } ?: return null
        val hour = time.substring(0, 2).toIntOrNull() ?: return null
        val minute = time.substring(3, 5).toIntOrNull() ?: return null
        return hour * 60 + minute
    }

    private fun addMinutesToHourMinute(hourMinute: String, minutes: Int): String {
        val base = hourMinuteToMinutes("d $hourMinute") ?: return hourMinute
        val total = (base + minutes) % (24 * 60)
        return String.format(Locale.US, "%02d:%02d", total / 60, total % 60)
    }

    private fun applyLocalStepTimeUpdate(
        stepId: Int,
        newStartTime: String?,
        newEndTime: String?
    ): Boolean {
        val tl = _timeline.value ?: return false
        tl.plans?.forEach { plan ->
            plan.steps?.firstOrNull { it.id == stepId }?.let { step ->
                availabilityCheckManager.invalidatePricesFor(step.poi?.additionalData?.productId)
                newStartTime?.let { step.startDateTimes = replaceHourMinute(step.startDateTimes, it) }
                newEndTime?.let { step.endDateTimes = replaceHourMinute(step.endDateTimes, it) }
                return true
            }
        }
        return false
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
     * Edit a top-level segment's start/end time. Times are "HH:mm". When [newDate]
     * ("yyyy-MM-dd") is provided the segment is moved to that day; otherwise the
     * segment's existing date is preserved.
     */
    fun updateSegmentTime(
        segment: TimelineSegment,
        segmentIndex: Int,
        startTime: String?,
        endTime: String?,
        newDate: String? = null,
        newPrice: Double? = null,
        // true when the caller (ActivityTimeSelection change-time sheet) shows an
        // inline loader inside the open sheet and dismisses it via
        // [changeTimeFinished]; the VM then skips its own bottom-sheet loader.
        useInlineLoader: Boolean = false,
        // See [updateStepTime]'s onInlineResult.
        onInlineResult: ((success: Boolean) -> Unit)? = null
    ) {
        if (startTime == null || endTime == null) return

        if (!useInlineLoader) {
            showBottomSheetLoader(LanguageConst.LOADING_TEXT_CHANGING_TIME, "Changing time")
        }

        viewModelScope.launch {
            runCatching {
                updateSegmentTimeUseCase(
                    UpdateSegmentTimeUseCase.Params(
                        tripHash = _tripHash,
                        segmentIndex = segmentIndex,
                        original = segment,
                        newStartTime = startTime,
                        newEndTime = endTime,
                        newDate = newDate,
                        newPrice = newPrice
                    )
                )
            }
                .onSuccess {
                    reloadTimelineAndFinishTimeChange(useInlineLoader) {
                        applyLocalSegmentTimeUpdate(segmentIndex, startTime, endTime, newDate, newPrice)
                    }
                    onInlineResult?.invoke(true)
                }
                .onFailure { t ->
                    val errorModel = t.toErrorModel()
                    if (!useInlineLoader) hideLottieLoading()
                    _error.value = errorModel.errorDesc
                    if (useInlineLoader) _changeTimeFinished.value = false
                    onInlineResult?.invoke(false)
                }
        }
    }

    /**
     * Shared tail of a change-time operation: re-fetches and publishes the timeline,
     * then closes the loader. On a fetch failure, falls back to the optimistic local
     * mutation. Closes the inline sheet via [changeTimeFinished] when
     * [useInlineLoader], otherwise hides the bottom-sheet loader.
     *
     * Clears the route-info cache first: a time change can re-sequence a
     * Recommendations segment's steps (a different step becomes first), and the
     * cached routes are keyed by segmentIndex only, not by step order — stale
     * routes would otherwise be re-stamped onto the rebuilt item, most visibly
     * as the wrong "distance to first step" label.
     */
    private suspend fun reloadTimelineAndFinishTimeChange(
        useInlineLoader: Boolean,
        applyLocalFallback: () -> Boolean
    ) {
        clearRouteInfoCache()
        runCatching { fetchTimelineUseCase(FetchTimelineUseCase.Params(_tripHash)) }
            .onSuccess { timeline -> processTimeline(timeline) }
            .onFailure {
                if (applyLocalFallback()) republishCurrentTimeline()
            }
        if (!useInlineLoader) hideLottieLoading()
        if (useInlineLoader) _changeTimeFinished.value = true
    }

    private fun applyLocalSegmentTimeUpdate(
        segmentIndex: Int,
        newStartTime: String,
        newEndTime: String,
        newDate: String? = null,
        newPrice: Double? = null
    ): Boolean {
        val tl = _timeline.value ?: return false
        val segment = tl.tripProfile?.segments?.getOrNull(segmentIndex) ?: return false
        availabilityCheckManager.invalidatePricesFor(segment.additionalData?.activityId)
        segment.startDate = applyDateAndTime(segment.startDate, newDate, newStartTime)
        segment.endDate = applyDateAndTime(segment.endDate, newDate, newEndTime)
        segment.additionalData?.let { add ->
            add.startDatetime = applyDateAndTime(add.startDatetime, newDate, newStartTime)
            add.endDatetime = applyDateAndTime(add.endDatetime, newDate, newEndTime)
            if (newPrice != null) add.price = newPrice
        }
        return true
    }

    /**
     * Rewrites [original]'s date and time. When [newDate] ("yyyy-MM-dd") is given
     * it replaces the date prefix; otherwise the existing date is kept and only
     * the HH:mm portion is replaced.
     */
    private fun applyDateAndTime(original: String?, newDate: String?, newHourMinute: String): String? {
        if (!newDate.isNullOrBlank()) return "$newDate $newHourMinute"
        return replaceHourMinute(original, newHourMinute)
    }

    /**
     * Replaces the HH:mm portion of a `yyyy-MM-dd HH:mm[:ss]` datetime string
     * with [newHourMinute]. Returns the input unchanged when the date prefix
     * is missing or malformed — callers fall back to a full refresh in that
     * case rather than writing a bad value.
     */
    private fun replaceHourMinute(original: String?, newHourMinute: String): String? {
        if (original.isNullOrBlank()) return original
        val datePart = original.take(10)
        if (datePart.length < 10 || datePart[4] != '-' || datePart[7] != '-') return original
        return "$datePart $newHourMinute"
    }

    fun clearChangeTimePickerStep() {
        _showChangeTimePickerStep.value = null
    }

    /** Clears the one-shot [changeTimeFinished] event. */
    fun resetChangeTimeFinished() {
        _changeTimeFinished.value = null
    }

    // =====================
    // UI ACTIONS
    // =====================

    fun toggleMapMode() {
        val newMapMode = !(_isMapMode.value ?: false)
        _isMapMode.value = newMapMode

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
                    _launchPoiSelection.value = data
                }
            }

            AddPlanMode.NONE -> {
            }
        }
    }

    fun selectCity(city: City) {
        _selectedCity.value = city
        updateDisplayItems()
    }

    fun showNearMePois() {
        // TODO: Implement Near Me POI fetching based on user location
    }

    fun clearPoiSelectionTrigger() {
        _launchPoiSelection.value = null
    }

    /**
     * Clears any selected marker / preview card. The map redraws its marker
     * selection and the bottom preview list collapses to its idle state.
     */
    fun clearMapSelection() {
        selectedStepId = null
        val currentMapSteps = _mapSteps.value?.toMutableList() ?: return
        currentMapSteps.forEach { it.isSelected = false }
        _mapSteps.value = currentMapSteps
        updateMapBottomItems()
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
        if (!hasMultipleCitiesInSelectedDay) return

        val shouldShowStepMarkers = zoomLevel > MULTI_CITY_ZOOM_THRESHOLD

        if (shouldShowStepMarkers == isShowingStepMarkersInMultiCity) return

        isShowingStepMarkersInMultiCity = shouldShowStepMarkers

        if (shouldShowStepMarkers) {
            _mapMarkersMode.value = MapMarkersMode.STEP_MARKERS
            _showMainViewButton.value = true
        } else {
            _mapMarkersMode.value = MapMarkersMode.CITY_MARKERS
            _showMainViewButton.value = false
        }
    }

    /**
     * Selects a step on the map by its ID, updating the selection state in
     * mapSteps and mapBottomItems.
     *
     * @param stepId    The poiId of the step to select
     * @param allowToggle  When true (marker tap), re-selecting the same step clears the selection
     */
    @JvmOverloads
    fun selectStepOnMap(stepId: String, allowToggle: Boolean = false) {
        if (allowToggle && selectedStepId == stepId) {
            clearMapSelection()
            return
        }
        selectedStepId = stepId

        val currentMapSteps = _mapSteps.value?.toMutableList() ?: return
        var stepCityIndex = 0

        currentMapSteps.forEach { step ->
            if (step.poiId == stepId) {
                step.isSelected = true
                stepCityIndex = step.cityIndex
            } else if (step.cityIndex == stepCityIndex) {
                step.isSelected = false
            }
        }

        val selectedStep = currentMapSteps.find { it.poiId == stepId }
        if (selectedStep != null) {
            currentMapSteps.forEach { step ->
                if (step.cityIndex == selectedStep.cityIndex && step.poiId != stepId) {
                    step.isSelected = false
                }
            }
        }

        _mapSteps.value = currentMapSteps

        val currentBottomItems = _mapBottomItems.value?.map { item ->
            item.copy(isSelected = item.id == stepId)
        }
        currentBottomItems?.let { _mapBottomItems.value = it }

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

        val existingStep = _mapSteps.value?.find { it.poiId == stepId }

        return existingStep?.let { step ->
            MapStep().apply {
                poiId = step.poiId
                name = step.name
                coordinate = step.coordinate
                position = step.position
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
     * Updates city markers LiveData for multi-city overview mode.
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
     * Called when user taps on a booked_activity card.
     * Forwards booking detail request to host app.
     *
     * @param bookingId ID of the tapped booking
     */
    fun onBookingDetailRequested(bookingId: String) {
        TRPCore.notifyBookingDetailRequested(bookingId)
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

        if (selectedStepId == null) {
            selectedStepId = result.firstStepIdOfFirstCity
        }

        hasMultipleCitiesInSelectedDay = result.hasMultipleCities

        isShowingStepMarkersInMultiCity = false

        _showMainViewButton.value = false

        _mapMarkersMode.value = if (hasMultipleCitiesInSelectedDay) {
            MapMarkersMode.CITY_MARKERS
        } else {
            MapMarkersMode.STEP_MARKERS
        }

        _mapSteps.value = result.mapSteps

        updateCityMarkers()

        updateMapBottomItems()

        if (usesFlatTimeline) {
            publishFlatMapRoutes(items)
        } else {
            calculateMapRoutes(result.mapSteps)
        }
    }

    /**
     * Routes the day's located, ordered map items city by city so the map can draw
     * walking/driving legs. Flexible items have no place in the sequence and are skipped.
     * No-op unless the host draws routes.
     */
    private fun calculateMapRoutes(mapSteps: List<MapStep>) {
        mapRoutesJob?.cancel()
        _mapRoutes.value = emptyList()
        if (!TRPCore.host.drawsRoutesOnMap()) return

        val cityGroups = mapSteps
            .filter { !it.isCityMarker && !it.isFlexible }
            .groupBy { it.cityIndex }
            .values
            .map { group -> group.mapNotNull { it.coordinate } }
            .filter { it.size > 1 }
        if (cityGroups.isEmpty()) return

        mapRoutesJob = viewModelScope.launch {
            val legs = cityGroups.flatMap { coordinates ->
                runCatching {
                    getTimelineStepRoutesUseCase(GetTimelineStepRoutesUseCase.Params.forCoordinates(coordinates))
                }.getOrDefault(emptyList())
            }
            _mapRoutes.value = legs
        }
    }

    // =====================
    // FLAT TIMELINE ROUTES
    // =====================

    /**
     * Requests legs for every city chain of [items] with no cached result and no
     * request in flight. Arrived legs are cached under the chain key and the day is
     * re-laid out so its separators and map route appear. A chain whose rows only
     * changed time keeps its key, so no request is repeated for it.
     */
    private fun requestMissingFlatRoutes(items: List<TimelineDisplayItem>) {
        FlatRouteChain.collect(items)
            .filter { it.isRoutable && it.key !in flatRouteCache && it.key !in flatRouteJobs }
            .forEach { chain ->
                flatRouteJobs[chain.key] = viewModelScope.launch {
                    val legs = runCatching {
                        getTimelineStepRoutesUseCase(
                            GetTimelineStepRoutesUseCase.Params(
                                chain.waypoints.map {
                                    GetTimelineStepRoutesUseCase.Waypoint(it.coordinate, it.id)
                                }
                            )
                        )
                    }.getOrNull()
                    flatRouteJobs.remove(chain.key)
                    if (legs != null) {
                        flatRouteCache[chain.key] = legs
                        relayoutWithFlatRoutes()
                    }
                }
            }
    }

    /** Re-renders the day with the legs now cached, leaving the map selection state untouched. */
    private fun relayoutWithFlatRoutes() {
        val items = buildDisplayItems() ?: return
        _displayItems.value = items
        publishFlatMapRoutes(items)
    }

    /**
     * Draws the cached legs of the day's chains on the map. The starting point has no
     * marker, so its leg is shown in the list only.
     */
    private fun publishFlatMapRoutes(items: List<TimelineDisplayItem>) {
        mapRoutesJob?.cancel()
        if (!TRPCore.host.drawsRoutesOnMap()) {
            _mapRoutes.value = emptyList()
            return
        }
        _mapRoutes.value = FlatRouteChain.collect(items)
            .flatMap { chain -> flatRouteCache[chain.key].orEmpty() }
            .filter { it.fromStepId != null }
    }

    /**
     * Alerts once per plan when generation finished without finding any place for
     * the selected day; the list simply stays without rows for that plan.
     */
    private fun reportPlanGeneratedWithoutPois() {
        val timeline = _timeline.value ?: return
        val days = _availableDays.value ?: return
        val dateStr = days.getOrNull(_selectedDayIndex.value ?: 0)?.toApiDateString() ?: return
        val emptyPlan = timeline.tripProfile?.segments
            ?.asSequence()
            ?.filter { it.startDate?.startsWith(dateStr) == true && it.title != "TimelineDate" }
            ?.filter { it.segmentType == SegmentType.ITINERARY || it.segmentType == SegmentType.GENERATED }
            ?.mapNotNull { timeline.planFor(it) }
            ?.firstOrNull { it.generatedWithoutPois && it.id !in emptyRecommendationPlanIds }
            ?: return
        emptyRecommendationPlanIds.add(emptyPlan.id)
        _error.value = getLanguageForKey(LanguageConst.ADD_PLAN_NO_RECOMMENDATIONS)
            .takeIf { it.isNotBlank() && it != LanguageConst.ADD_PLAN_NO_RECOMMENDATIONS }
            ?: NO_RECOMMENDATIONS_FALLBACK
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
        showLoading()

        refreshTimeline()
        hideLoading()
    }

    // =====================
    // HELPERS
    // =====================

    fun hasSingleCity(): Boolean = (_cities.value?.size ?: 0) <= 1

    /**
     * City the AddPlan flow starts on: the one the selected day belongs to, so a
     * multi-city trip doesn't offer the first city's catalog on another city's day.
     */
    fun getSelectedCity(): City? = cityOfSelectedDay() ?: _cities.value?.firstOrNull()

    /**
     * The host's destination list owns the day → city mapping (a destination
     * carries the dates spent in it); segments only fill the gap for days the
     * host didn't describe.
     */
    private fun cityOfSelectedDay(): City? {
        val days = _availableDays.value ?: return null
        val date = days.getOrNull(_selectedDayIndex.value ?: 0) ?: return null
        val dateStr = date.toApiDateString()

        val destinationCityId = itinerary?.destinationItems
            ?.firstOrNull { item -> item.dates?.any { it.take(10) == dateStr } == true }
            ?.let { item -> item.cityId ?: resolveCityIdForDestination(item) }

        val segmentCityId = _timeline.value?.tripProfile?.segments
            ?.asSequence()
            ?.filter { it.startDate?.startsWith(dateStr) == true }
            ?.mapNotNull { it.cityId?.takeIf { id -> id > 0 } }
            ?.firstOrNull()

        val cityId = (destinationCityId ?: segmentCityId)?.takeIf { it > 0 } ?: return null
        return _cities.value?.firstOrNull { it.id == cityId }
            ?: tripRepository.getCachedCityById(cityId)
    }

    private fun resolveCityIdForDestination(item: SegmentDestinationItem): Int? {
        val byCoordinate = item.getCoordinateObject()?.let { coord ->
            tripRepository.findCityByCoordinate(coord.lat, coord.lng)
        }
        return (byCoordinate ?: tripRepository.findCityByName(item.title, item.countryName))?.id
    }

    /**
     * Returns the city coordinate as a Mapbox Point for map centering.
     * Used when map has no items (empty day) to center on city instead of 0,0.
     */
    fun getSelectedDayCityCoordinate(): Point? {
        val city = getSelectedCity()
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

    /**
     * Returns the map coordinates (as Mapbox Points) of every located step in the
     * given city. Used to fit the camera to a city's steps when its marker is
     * tapped, mirroring the single-city map's fit-to-all-markers behavior.
     */
    fun getStepCoordinatesForCity(cityId: Int?): List<Point> {
        if (cityId == null) return emptyList()
        return _mapSteps.value
            ?.filter { !it.isCityMarker && it.cityId == cityId }
            ?.mapNotNull { it.coordinate }
            ?.filter { it.lat != 0.0 && it.lng != 0.0 }
            ?.map { Point.fromLngLat(it.lng, it.lat) }
            ?: emptyList()
    }

    fun getItinerary(): ItineraryWithActivities? = itinerary

    /**
     * Returns favorites that haven't been added as booked_activity or reserved_activity,
     * have a valid city mapping (cityName matches a resolved destination), and haven't
     * been locally removed from saved plans. Used when opening SavedPlans screen.
     */
    fun getFilteredFavorites(): List<SegmentFavoriteItem> {
        val favourites = itinerary?.favouriteItems ?: return emptyList()

        val plannedBaseIds = _timeline.value
            ?.plannedActivities()
            ?.map { it.productId }
            ?.toSet()
            .orEmpty()

        val removedBaseIds = com.tripian.trpcore.util.RemovedFavoritesStore
            .removedBaseIds(preferences, _tripHash)

        return favourites.filter { favourite ->
            val baseId = ActivityIdFormat.base(favourite.activityId)
            baseId !in plannedBaseIds &&
                baseId !in removedBaseIds &&
                (favourite.cityId?.takeIf { it > 0 }
                    ?: getResolvedCityId(favourite.cityName)) != null
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
        return cityNameToIdMap[cityName.cityNameKey()]
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

    /** True when the currently-viewed day is in the past. */
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
     * Calculate routes for a Recommendations segment. Routes are cached by
     * segmentIndex; failures are silent (steps display without route info).
     *
     * @param recommendations The Recommendations item to calculate routes for
     */
    fun calculateRoutesForRecommendations(recommendations: TimelineDisplayItem.Recommendations) {
        val segmentIndex = recommendations.segmentIndex ?: return

        if (_routeInfoCache.containsKey(segmentIndex)) {
            updateRecommendationsWithRouteInfo(segmentIndex)
            return
        }

        if (recommendations.steps.isEmpty()) return

        viewModelScope.launch {
            runCatching {
                getTimelineStepRoutesUseCase(
                    GetTimelineStepRoutesUseCase.Params.forSteps(
                        startingPointCoordinate = recommendations.startingPointCoordinate,
                        steps = recommendations.steps
                    )
                )
            }
                .onSuccess { routeInfoList ->
                    _routeInfoCache[segmentIndex] = routeInfoList
                    updateRecommendationsWithRouteInfo(segmentIndex)
                }
                .onFailure { }
        }
    }

    /**
     * Updates the display items with route info from cache.
     * Finds the Recommendations item with matching segmentIndex and updates its routeInfoList.
     */
    private fun updateRecommendationsWithRouteInfo(segmentIndex: Int) {
        val routeInfoList = _routeInfoCache[segmentIndex] ?: return
        val currentItems = _displayItems.value?.toMutableList() ?: return

        val updatedItems = currentItems.map { item ->
            if (item is TimelineDisplayItem.Recommendations && item.segmentIndex == segmentIndex) {
                item.copy(routeInfoList = routeInfoList)
            } else {
                item
            }
        }

        _displayItems.value = updatedItems

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
        // Host policy: a host with its own onboarding suppresses the SDK's.
        if (!TRPCore.host.showsOnboarding()) return false

        val dismissed = preferences.getBoolean(Preferences.Keys.ONBOARDING_DISMISSED_PERMANENTLY, false)
        val hasSeen = preferences.getBoolean(Preferences.Keys.ONBOARDING_HAS_SEEN, false)
        val count = preferences.getInt(Preferences.Keys.ONBOARDING_CONTINUE_COUNT, 0)

        if (dismissed) return false
        if (!hasSeen) return true
        return count < 3
    }

    /**
     * Triggers showing onboarding if needed. Called after languages are loaded.
     * The full-screen loader is hidden first so it doesn't sit on top of the
     * onboarding sheet.
     */
    fun checkAndShowOnboarding() {
        if (shouldShowOnboarding()) {
            hideLottieLoading()
            _showOnboarding.value = true
        } else {
            onOnboardingComplete()
        }
    }

    /**
     * Called when onboarding is completed (either by Continue or Skip).
     * Defensively kicks off the fetch chain if no timeline is loaded yet.
     */
    fun onOnboardingComplete() {
        onboardingCompleted = true

        if (_timeline.value == null) {
            showFullScreenLoader(LanguageConst.LOADING_TEXT_GETTING_ITINERARY_PLAN, "")
            waitForLoginThenProceed {
                resolveDestinationCitiesAndProceed()
            }
        }
    }

    // ========================================
    // SYNC OPERATIONS
    // ========================================

    /**
     * Tracks whether each sync op actually mutated server state. When all
     * ops are no-ops we skip the post-sync re-fetch (and the second
     * processTimeline + availability sweep that would come with it).
     */
    private class SyncMutationTracker(
        var addMissing: Boolean = false,
        var dateRange: Boolean = false,
        var transitions: Boolean = false,
    ) {
        val anyMutated: Boolean get() = addMissing || dateRange || transitions
    }

    /**
     * Initial-fetch sync orchestrator. Runs city resolution → parallel ops →
     * sequential ops; once the pipeline settles, [finalizeInitialFetch] decides
     * whether the server state was actually mutated. When tripItems and
     * favourites are both empty, only the date range is realigned.
     */
    private fun runInitialSyncThenFinalize(initialTimeline: Timeline) {
        syncOperationsCompleted = true

        val tripItems = itinerary?.tripItems ?: emptyList()
        val favouriteItems = itinerary?.favouriteItems ?: emptyList()

        if (tripItems.isEmpty() && favouriteItems.isEmpty()) {
            viewModelScope.launch {
                runCatching {
                    updateDateRangeUseCase(
                        UpdateDateRangeUseCase.Params(_tripHash, itinerary!!, initialTimeline)
                    )
                }
                    .onSuccess { result -> finalizeInitialFetch(initialTimeline, result.mutated) }
                    .onFailure { finalizeInitialFetch(initialTimeline, false) }
            }
            return
        }

        val tracker = SyncMutationTracker()

        // STEP 1: City resolution is a per-host policy. The default resolves
        // cityIds from coordinates via the cities/resolve API; a host that
        // already resolved them upstream builds the name→id map locally.
        viewModelScope.launch {
            val itineraryData = itinerary
            if (itineraryData != null) {
                val cityMap = TRPCore.host.resolveActivityCityMap(
                    itineraryData.tripItems ?: emptyList(),
                    itineraryData.favouriteItems ?: emptyList(),
                    cityNameToIdMap.toMap()
                ) {
                    resolveActivityCityIds(itineraryData, timelineActivityIds(initialTimeline))
                    cityNameToIdMap.toMap()
                }
                cityNameToIdMap.putAll(cityMap)
            }
            runParallelSyncForInitial(
                initialTimeline,
                itinerary?.tripItems ?: tripItems,
                cityNameToIdMap.toMap(),
                tracker
            )
        }
    }

    /**
     * Fills in the cityId of the host's booked/favourite activities and stores the
     * result back on [itinerary]. Runs while the loader is still up — a city that
     * lands after the days are drawn would pop a new activity onto the user's screen.
     *
     * @param knownActivityIds activities the timeline already holds; they keep the
     *   city their segment was created with instead of being looked up again.
     */
    private suspend fun resolveActivityCityIds(
        itineraryData: ItineraryWithActivities,
        knownActivityIds: Set<String>
    ) {
        val tripItems = itineraryData.tripItems ?: emptyList()
        val favouriteItems = itineraryData.favouriteItems ?: emptyList()
        if (tripItems.isEmpty() && favouriteItems.isEmpty()) return

        runCatching {
            resolveCityIdsForActivitiesUseCase(
                ResolveCityIdsForActivitiesUseCase.Params(
                    tripItems = tripItems,
                    favouriteItems = favouriteItems,
                    existingCityMap = cityNameToIdMap.toMap(),
                    knownActivityIds = knownActivityIds
                )
            )
        }.onSuccess { result ->
            cityNameToIdMap.putAll(result.cityMap)
            // Absent lists stay absent: "host sent no tripItems" must not read as
            // "host reports no bookings", which would strip every booked segment.
            itinerary = itineraryData.copy(
                tripItems = itineraryData.tripItems?.let { result.tripItems },
                favouriteItems = itineraryData.favouriteItems?.let { result.favouriteItems }
            )
        }
    }

    /** Base activity ids already present on the timeline as booked/reserved segments. */
    private fun timelineActivityIds(timeline: Timeline): Set<String> =
        timeline.tripProfile?.segments
            ?.mapNotNull { segment -> ActivityIdFormat.base(segment.additionalData?.activityId) }
            ?.toSet()
            .orEmpty()

    /**
     * STEP 2: 3 parallel ops — transition detection, AddMissing, UpdateDateRange.
     * Each reports its own mutation verdict into [tracker]; once all three
     * resolve, STEP 3 (sequential) takes over. The AddMissing verdict is
     * precomputed here because the use case doesn't expose it.
     */
    private fun runParallelSyncForInitial(
        initialTimeline: Timeline,
        tripItems: List<com.tripian.trpcore.domain.model.itinerary.SegmentActivityItem>,
        cityMap: Map<String, Int>,
        tracker: SyncMutationTracker
    ) {
        var detectedTransitions: List<TransitionInfo>? = null
        var completed = 0

        val onParallelComplete = {
            completed++
            if (completed == 3) {
                tracker.transitions = !detectedTransitions.isNullOrEmpty()
                runSequentialSyncForInitial(initialTimeline, detectedTransitions, cityMap, tracker)
            }
        }

        val existingActivityIds = initialTimeline.tripProfile?.segments
            ?.mapNotNull { it.additionalData?.activityId }?.toSet().orEmpty()
        tracker.addMissing = tripItems.any { item ->
            item.activityId != null && item.activityId !in existingActivityIds
        }

        viewModelScope.launch {
            runCatching {
                detectReservedToBookedTransitionUseCase(
                    DetectReservedToBookedTransitionUseCase.Params(initialTimeline, tripItems)
                )
            }
                .onSuccess { transitions ->
                    detectedTransitions = transitions
                    onParallelComplete()
                }
                .onFailure { onParallelComplete() }
        }

        viewModelScope.launch {
            runCatching {
                addMissingBookedActivitiesUseCase(
                    AddMissingBookedActivitiesUseCase.Params(_tripHash, itinerary!!, initialTimeline)
                )
            }
                .onSuccess { onParallelComplete() }
                .onFailure { onParallelComplete() }
        }

        viewModelScope.launch {
            runCatching {
                updateDateRangeUseCase(
                    UpdateDateRangeUseCase.Params(_tripHash, itinerary!!, initialTimeline)
                )
            }
                .onSuccess { result ->
                    tracker.dateRange = result.mutated
                    onParallelComplete()
                }
                .onFailure { onParallelComplete() }
        }
    }

    /**
     * STEP 3: Sequential ops — sync reserved→booked transitions if any.
     * Either way, finalize once this resolves.
     */
    private fun runSequentialSyncForInitial(
        initialTimeline: Timeline,
        transitions: List<TransitionInfo>?,
        cityMap: Map<String, Int>,
        tracker: SyncMutationTracker
    ) {
        if (!transitions.isNullOrEmpty()) {
            viewModelScope.launch {
                runCatching {
                    syncReservedToBookedUseCase(
                        SyncReservedToBookedUseCase.Params(
                            tripHash = _tripHash,
                            transitions = transitions,
                            cityNameToIdMap = cityMap,
                            itinerary = itinerary!!
                        )
                    )
                }
                    .onSuccess { finalizeInitialFetch(initialTimeline, tracker.anyMutated) }
                    .onFailure { finalizeInitialFetch(initialTimeline, tracker.anyMutated) }
            }
        } else {
            finalizeInitialFetch(initialTimeline, tracker.anyMutated)
        }
    }

    /**
     * Closes the initial-fetch flow. If sync ran any mutations the in-memory
     * `initialTimeline` snapshot is stale, so re-fetch and render the new
     * state. If nothing changed, render the initial snapshot directly.
     */
    private fun finalizeInitialFetch(initialTimeline: Timeline, anyMutated: Boolean) {
        if (!anyMutated) {
            processTimeline(initialTimeline)
            hideLottieLoading()
            schedulePostSyncDeletion(initialTimeline)
            return
        }

        viewModelScope.launch {
            runCatching { fetchTimelineUseCase(FetchTimelineUseCase.Params(_tripHash)) }
                .onSuccess { freshTimeline ->
                    processTimeline(freshTimeline)
                    hideLottieLoading()
                    schedulePostSyncDeletion(freshTimeline)
                }
                .onFailure {
                    processTimeline(initialTimeline)
                    hideLottieLoading()
                }
        }
    }

    /**
     * Background deletion of segments that no longer belong on the timeline —
     * see [computeBackgroundDeletionIndices] for the reasons. The union is
     * computed client-side, hidden from the UI immediately by stashing it in
     * [pendingDeletionSegmentIndices], then deleted in one descending pass. A
     * silent fetch reconciles local state with the server and clears the hidden set.
     */
    private fun schedulePostSyncDeletion(timeline: Timeline) {
        val itineraryData = itinerary ?: return
        val indices = computeBackgroundDeletionIndices(timeline, itineraryData)
        if (indices.isEmpty()) return

        pendingDeletionSegmentIndices = indices
        updateDisplayItems()

        viewModelScope.launch {
            runCatching {
                removeObsoleteSegmentsUseCase(
                    RemoveObsoleteSegmentsUseCase.Params(_tripHash, indices)
                )
            }
                .onSuccess { silentRefreshAfterBackgroundDeletion() }
                .onFailure { silentRefreshAfterBackgroundDeletion() }
        }
    }

    /**
     * Silently reconciles local state with the server after background deletion.
     * On failure the hidden set is kept so the UI doesn't snap items back; the
     * next successful refresh reconciles.
     */
    private fun silentRefreshAfterBackgroundDeletion() {
        viewModelScope.launch {
            runCatching { fetchTimelineUseCase(FetchTimelineUseCase.Params(_tripHash)) }
                .onSuccess { fresh ->
                    pendingDeletionSegmentIndices = emptySet()
                    processTimeline(fresh)
                }
                .onFailure { }
        }
    }

    /**
     * Every reason a segment no longer belongs on the timeline, unioned by index
     * against a single snapshot: its city left the itinerary, its day fell outside
     * the trip range, or it is a booked activity the host no longer reports.
     * The UI hides the union immediately and [RemoveObsoleteSegmentsUseCase]
     * deletes it in one descending pass.
     *
     * The host's `tripItems` is the full booking state at startup, so a booked
     * segment missing from it — including when the host sends none at all — is
     * stale. Booked segments are the only host-owned ones; a reserved activity the
     * user added inside the SDK is never touched.
     *
     * Never a candidate: the TimelineDate sentinel, a segment whose city is simply
     * unresolved, and a booked segment carrying no activityId to match on.
     */
    private fun computeBackgroundDeletionIndices(
        timeline: Timeline,
        itineraryData: com.tripian.trpcore.domain.model.itinerary.ItineraryWithActivities
    ): Set<Int> {
        val segments = timeline.tripProfile?.segments ?: return emptySet()
        val currentCityIds = itineraryData.destinationItems
            .mapNotNull { d -> d.cityId }
            .filter { id -> id > 0 }
            .toSet()
        val tripStart = itineraryData.startDatetime.take(10).takeIf { d ->
            d.length == 10 && d[4] == '-' && d[7] == '-'
        }
        val tripEnd = itineraryData.endDatetime.take(10).takeIf { d ->
            d.length == 10 && d[4] == '-' && d[7] == '-'
        }
        val hostBookedIds = itineraryData.tripItems.orEmpty()
            .mapNotNull { item -> ActivityIdFormat.base(item.activityId) }
            .toSet()

        val out = mutableSetOf<Int>()
        segments.forEachIndexed { idx, seg ->
            if (seg.title == "TimelineDate" && !seg.available) return@forEachIndexed

            val cityId = seg.cityId
            if (currentCityIds.isNotEmpty() && cityId != null && cityId > 0 &&
                cityId !in currentCityIds
            ) {
                out += idx
                return@forEachIndexed
            }

            if (seg.segmentType == SegmentType.BOOKED_ACTIVITY) {
                val activityId = ActivityIdFormat.base(seg.additionalData?.activityId)
                if (activityId != null && activityId !in hostBookedIds) {
                    out += idx
                    return@forEachIndexed
                }
            }

            if (tripStart != null && tripEnd != null) {
                val day = seg.startDate?.take(10)?.takeIf {
                    it.length == 10 && it[4] == '-' && it[7] == '-'
                }
                if (day != null && (day < tripStart || day > tripEnd)) {
                    out += idx
                }
            }
        }
        return out
    }

    companion object {
        const val ARG_TRIP_HASH = "tripHash"
        private const val NO_RECOMMENDATIONS_FALLBACK = "No recommendations found for this area"

        /** Span applied to a moved step whose own start/end can't be read. */
        private const val DEFAULT_STEP_DURATION_MINUTES = 60

        // Multi-city zoom thresholds. Lower threshold = the user must zoom out
        // FARTHER before step markers collapse back into city markers.
        const val MULTI_CITY_ZOOM_THRESHOLD = 10.0
        const val CITY_MARKER_ZOOM_LEVEL = 13.0
        const val STEP_MARKER_ZOOM_LEVEL = 15.0

        // Upper bound for translation fetch on SDK launch. Beyond this, the
        // host is informed via LANGUAGE_LOAD_FAILED and the SDK closes.
        private const val LANGUAGE_RETRY_TIMEOUT_SECONDS = 30L
    }

    private fun Throwable.toErrorModel(): ErrorModel = (this as? ErrorModel)
        ?: ErrorModel().apply { errorDesc = this@toErrorModel.message ?: "" }
}
