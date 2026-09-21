package com.tripian.trpcore.ui.timeline.compose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.onConsumedWindowInsetsChanged
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.tripian.one.api.pois.model.Poi
import com.tripian.one.api.timeline.model.TimelineSegment
import com.tripian.one.api.timeline.model.TimelineStep
import com.tripian.trpcore.R
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.databinding.ItemTimelineBookedActivityBinding
import com.tripian.trpcore.databinding.ItemTimelineEmptyStateBinding
import com.tripian.trpcore.databinding.ItemTimelineFlexibleActivityBinding
import com.tripian.trpcore.databinding.ItemTimelineManualPoiBinding
import com.tripian.trpcore.databinding.ItemTimelineRecommendationsBinding
import com.tripian.trpcore.databinding.ItemTimelineReservedActivityBinding
import com.tripian.trpcore.databinding.ItemTimelineSectionFooterBinding
import com.tripian.trpcore.databinding.ItemTimelineSectionHeaderBinding
import com.tripian.trpcore.domain.model.MapStep
import com.tripian.trpcore.domain.model.itinerary.ItineraryWithActivities
import com.tripian.trpcore.domain.model.timeline.AddPlanData
import com.tripian.trpcore.domain.model.timeline.AddPlanMode
import com.tripian.trpcore.domain.model.timeline.MapMarkersMode
import com.tripian.trpcore.domain.model.timeline.TimelineDisplayItem
import com.tripian.trpcore.domain.model.timeline.toApiDateString
import com.tripian.trpcore.domain.model.timeline.toReservationDateTime
import com.tripian.trpcore.domain.model.timeline.toDate
import com.tripian.trpcore.ui.timeline.ACTimelineVM
import com.tripian.trpcore.ui.timeline.adapter.BookedActivityVH
import com.tripian.trpcore.ui.timeline.adapter.ConflictWarningVH
import com.tripian.trpcore.ui.timeline.adapter.EmptyStateVH
import com.tripian.trpcore.ui.timeline.adapter.FlexibleActivityVH
import com.tripian.trpcore.ui.timeline.adapter.ManualPoiVH
import com.tripian.trpcore.ui.timeline.adapter.MapBottomListAdapter
import com.tripian.trpcore.ui.timeline.adapter.RecommendationsVH
import com.tripian.trpcore.ui.timeline.adapter.ReservedActivityVH
import com.tripian.trpcore.ui.timeline.adapter.SectionFooterVH
import com.tripian.trpcore.ui.timeline.adapter.SectionHeaderVH
import com.tripian.trpcore.ui.timeline.addplan.AddPlanContainerVM
import com.tripian.trpcore.ui.timeline.compose.addplan.AddPlanSheet
import com.tripian.trpcore.ui.timeline.compose.core.LocalTimelineViewModelFactory
import com.tripian.trpcore.ui.timeline.compose.core.TimelineComposeScreen
import com.tripian.trpcore.ui.timeline.compose.core.TimelineFontFamily
import com.tripian.trpcore.ui.timeline.compose.core.timelineViewModel
import com.tripian.trpcore.ui.timeline.compose.nav.LocalTimelineNavigator
import com.tripian.trpcore.ui.timeline.compose.nav.ResultEffect
import com.tripian.trpcore.ui.timeline.compose.nav.StartingPointResult
import com.tripian.trpcore.ui.timeline.compose.nav.TimelineResults
import com.tripian.trpcore.ui.timeline.compose.sheets.ActivityTimeSelection
import com.tripian.trpcore.ui.timeline.compose.sheets.ActivityTimeSelectionMode
import com.tripian.trpcore.ui.timeline.compose.sheets.ActivityTimeSelectionRequest
import com.tripian.trpcore.ui.timeline.compose.sheets.ActivityTimeSelectionSheet
import com.tripian.trpcore.ui.timeline.compose.sheets.OnboardingSheet
import com.tripian.trpcore.ui.timeline.compose.sheets.PoiTimeSelectionRequest
import com.tripian.trpcore.ui.timeline.compose.sheets.PoiTimeSelectionSheet
import com.tripian.trpcore.ui.timeline.poilisting.POIListingType
import com.tripian.trpcore.domain.model.timeline.ManualCategory
import com.tripian.one.api.pois.model.Coordinate
import com.tripian.trpcore.ui.timeline.views.ConflictWarningView
import com.tripian.trpcore.ui.timeline.views.NoCityView
import com.tripian.trpcore.ui.timeline.views.TimelineDayFilterView
import com.tripian.trpcore.util.CityTimeZones
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.toSerializableIdsByDay
import com.tripian.trpcore.util.dialog.DGActionListener
import com.tripian.trpcore.util.widget.MapView as TrpMapView
import java.util.Date
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Compose twin of the ACTimeline main screen (list mode). Shares ACTimelineVM
 * with the Activity flow; list items reuse the existing ViewHolder bind logic
 * through AndroidView interop so behavior and visuals stay identical. Bottom
 * sheets and dialogs are Compose, so any ComponentActivity can host the screen.
 */
@Composable
fun TimelineScreen(
    itinerary: ItineraryWithActivities?,
    tripHash: String?,
    uniqueId: String?,
    appLanguage: String?,
    appCurrency: String?,
    onDismiss: () -> Unit,
    navEntry: NavBackStackEntry,
    viewModel: ACTimelineVM = timelineViewModel()
) {
    val context = LocalContext.current
    val sheets = remember { TimelineSheetHolder() }
    val mapUi = remember { MapModeUiState() }
    val vmFactory = LocalTimelineViewModelFactory.current
    val navigator = LocalTimelineNavigator.current
    val flowState: TimelineFlowState = viewModel(navEntry)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    LaunchedEffect(Unit) {
        if (viewModel.arguments == null) {
            viewModel.arguments = Bundle().apply {
                putString(TRPCore.EXTRA_TRIP_HASH, tripHash)
                putParcelable(TRPCore.EXTRA_ITINERARY, itinerary)
                putString(TRPCore.EXTRA_UNIQUE_ID, uniqueId)
                putString(TRPCore.EXTRA_APP_LANGUAGE, appLanguage)
                putString(TRPCore.EXTRA_APP_CURRENCY, appCurrency)
            }
            viewModel.onViewCreated(null)
        }
    }

    ResultEffect<Boolean>(navEntry, TimelineResults.SAVED_PLANS_CHANGED) {
        viewModel.onReturnFromSavedPlans()
    }

    ResultEffect<Int>(navEntry, TimelineResults.SEGMENT_CREATED_DAY_INDEX) { selectedDayIndex ->
        viewModel.onReturnFromAddPlan()
        viewModel.selectDay(selectedDayIndex)
        flowState.addPlanVisible = false
    }

    ResultEffect<StartingPointResult>(navEntry, TimelineResults.STARTING_POINT) { result ->
        val coordinate = Coordinate().apply {
            lat = result.lat
            lng = result.lng
        }
        flowState.addPlanViewModel?.setStartingPoint(result.name, coordinate, result.accommodation)
    }

    fun openPoiDetail(poi: Poi) {
        val (tripStart, tripEnd) = viewModel.tripDateRange()
        navigator.openPoiDetail(poi, tripStart, tripEnd)
    }

    fun openManualListing(category: ManualCategory) {
        val sheetVm = flowState.addPlanViewModel ?: return
        val planData = sheetVm.getValidPlanData() ?: return
        val tripHash = sheetVm.getTripHash() ?: return
        val (tripStart, tripEnd) = viewModel.tripDateRange()
        when (category) {
            ManualCategory.ACTIVITIES -> navigator.openActivityListing(
                planData = planData,
                tripHash = tripHash,
                plannedActivityIdsByDay = viewModel.plannedActivityIdsByDay(),
                tripWideExcludedActivityIds = viewModel.tripWideExcludedActivityIds()
            )
            ManualCategory.PLACES_OF_INTEREST -> navigator.openPoiListing(
                planData, tripHash, POIListingType.PLACES_OF_INTEREST, tripStart, tripEnd
            )
            ManualCategory.EAT_AND_DRINK -> navigator.openPoiListing(
                planData, tripHash, POIListingType.EAT_AND_DRINK, tripStart, tripEnd
            )
        }
    }

    fun openStartingPoint() {
        val sheetVm = flowState.addPlanViewModel ?: return
        navigator.openStartingPoint(
            city = sheetVm.selectedCity.value,
            bookedActivities = sheetVm.getBookedActivities(),
            favouriteItems = emptyList(),
            userLocation = sheetVm.getUserLocation()
        )
    }

    fun showDeleteConfirmation(title: String, message: String, onConfirm: () -> Unit) {
        viewModel.showDialog(
            title = title,
            contentText = message,
            positiveBtn = viewModel.getLanguageForKey(LanguageConst.REMOVE_BUTTON),
            negativeBtn = viewModel.getLanguageForKey(LanguageConst.CANCEL),
            positive = object : DGActionListener {
                override fun onClicked(o: Any?) {
                    onConfirm()
                }
            },
            isCloseEnable = false
        )
    }

    fun showAddPlanSheet() {
        flowState.addPlanStore.clear()
        val owner = object : ViewModelStoreOwner {
            override val viewModelStore = flowState.addPlanStore
        }
        val sheetVm = ViewModelProvider(owner, vmFactory)[AddPlanContainerVM::class.java]
        sheetVm.initializeFromArgs(Bundle().apply {
            putSerializable(
                AddPlanContainerVM.ARG_AVAILABLE_DAYS,
                ArrayList(viewModel.availableDays.value ?: emptyList())
            )
            putSerializable(
                AddPlanContainerVM.ARG_CITIES,
                ArrayList(viewModel.cities.value ?: emptyList())
            )
            putInt(AddPlanContainerVM.ARG_SELECTED_DAY_INDEX, viewModel.selectedDayIndex.value ?: 0)
            viewModel.getSelectedCity()?.let {
                putSerializable(AddPlanContainerVM.ARG_SELECTED_CITY, it)
            }
            putString(AddPlanContainerVM.ARG_TRIP_HASH, viewModel.tripHash)
            putSerializable(
                AddPlanContainerVM.ARG_BOOKED_ACTIVITIES,
                ArrayList(viewModel.getBookedActivities())
            )
            putSerializable(
                AddPlanContainerVM.ARG_PLANNED_ACTIVITY_IDS,
                viewModel.plannedActivityIdsByDay().toSerializableIdsByDay()
            )
            putStringArrayList(
                AddPlanContainerVM.ARG_TRIP_WIDE_EXCLUDED_IDS,
                ArrayList(viewModel.tripWideExcludedActivityIds())
            )
        })
        flowState.addPlanError = null
        flowState.addPlanViewModel = sheetVm
        flowState.addPlanVisible = true
    }

    fun openSavedPlans() {
        val filteredFavorites = viewModel.getFilteredFavorites()
        if (filteredFavorites.isEmpty()) return
        navigator.openSavedPlans(
            favorites = filteredFavorites,
            tripHash = viewModel.tripHash,
            availableDays = viewModel.availableDays.value ?: emptyList(),
            cityNameToIdMap = viewModel.getCityNameToIdMap(),
            plannedActivityIdsByDay = viewModel.plannedActivityIdsByDay()
        )
    }

    fun handleStepChangeTimeClick(step: TimelineStep) {
        if (step.stepType == "activity") {
            val poi = step.poi ?: return
            showActivityChangeTimeSheet(
                sheets = sheets,
                viewModel = viewModel,
                activityId = poi.additionalData?.productId ?: poi.id,
                cityId = poi.cityId,
                title = poi.name.orEmpty(),
                duration = poi.duration?.toDouble(),
                initialDateTime = step.startDateTimes,
                seedInitialTimeSlot = true,
                isNotAvailable = step.isAvailabilityExpired,
                restrictToInitialDay = true,
                onRemove = {
                    showDeleteConfirmation(
                        title = viewModel.getLanguageForKey(LanguageConst.REMOVE_STEP),
                        message = viewModel.getLanguageForKey(LanguageConst.REMOVE_STEP_MESSAGE),
                        onConfirm = {
                            sheets.dismissChangeTime()
                            viewModel.deleteStep(step)
                        }
                    )
                }
            ) { _, startTime, endTime, _ ->
                viewModel.updateStepTime(step.id, startTime, endTime, useInlineLoader = true)
            }
        } else {
            viewModel.showStepChangeTimePicker(step)
        }
    }

    fun handleSegmentChangeTime(
        segment: TimelineSegment,
        segmentIndex: Int,
        title: String,
        startDateTime: String?,
        isAvailabilityExpired: Boolean,
        seedInitialTimeSlot: Boolean
    ) {
        showActivityChangeTimeSheet(
            sheets = sheets,
            viewModel = viewModel,
            activityId = segment.additionalData?.activityId,
            cityId = segment.cityId?.takeIf { it > 0 },
            title = title,
            duration = segment.additionalData?.duration,
            initialDateTime = startDateTime,
            seedInitialTimeSlot = seedInitialTimeSlot,
            isNotAvailable = isAvailabilityExpired,
            onRemove = {
                showDeleteConfirmation(
                    title = viewModel.getLanguageForKey(LanguageConst.REMOVE_FROM_ITINERARY),
                    message = viewModel.getLanguageForKey(LanguageConst.REMOVE_FROM_ITINERARY_MESSAGE),
                    onConfirm = {
                        sheets.dismissChangeTime()
                        viewModel.deleteSegment(segmentIndex)
                    }
                )
            }
        ) { selectedDate, startTime, endTime, slotPrice ->
            viewModel.updateSegmentTime(
                segment, segmentIndex, startTime, endTime,
                newDate = selectedDate.toApiDateString(),
                newPrice = slotPrice,
                useInlineLoader = true
            )
        }
    }

    fun showMapBottomList() {
        if (mapUi.bottomListVisible) return
        mapUi.bottomListVisible = true
        mapUi.bottomListCompletelyHidden = false
    }

    fun hideMapBottomList() {
        if (!mapUi.bottomListVisible) return
        mapUi.bottomListVisible = false
        mapUi.bottomListCompletelyHidden = false
    }

    fun hideMapBottomListCompletely() {
        mapUi.bottomListVisible = false
        mapUi.bottomListCompletelyHidden = true
    }

    fun findPoiById(poiId: String): Poi? {
        val items = viewModel.displayItems.value ?: return null
        for (item in items) {
            when (item) {
                is TimelineDisplayItem.Recommendations ->
                    item.steps.forEach { step -> if (step.poi?.id == poiId) return step.poi }
                is TimelineDisplayItem.ManualPoi ->
                    if (item.step.poi?.id == poiId) return item.step.poi
                else -> {}
            }
        }
        return null
    }

    fun scrollToMapBottomItem(position: Int) {
        val index = mapUi.bottomListAdapter?.currentList?.indexOfFirst { it.order == position } ?: -1
        if (index >= 0) {
            mapUi.bottomListRecycler?.smoothScrollToPosition(index)
        }
    }

    fun focusCityOnMap(mapStep: MapStep) {
        viewModel.onMarkerFocused()
        val cityStepPoints = viewModel.getStepCoordinatesForCity(mapStep.cityId)
        if (cityStepPoints.isNotEmpty()) {
            scope.launch { mapUi.mapView?.fitCameraToPoints(cityStepPoints) }
        } else {
            mapStep.coordinate?.let { coord ->
                mapUi.mapView?.zoomToCoordinate(
                    lng = coord.lng,
                    lat = coord.lat,
                    zoomLevel = ACTimelineVM.CITY_MARKER_ZOOM_LEVEL
                )
            }
        }
    }

    fun handleMapItemClick(mapStep: MapStep) {
        if (mapStep.isCityMarker || viewModel.mapMarkersMode.value == MapMarkersMode.CITY_MARKERS) {
            focusCityOnMap(mapStep)
            return
        }
        viewModel.selectStepOnMap(mapStep.poiId)
        mapUi.mapView?.selectMarker(mapStep.poiId)
        mapStep.coordinate?.let { coord ->
            mapUi.mapView?.zoomToCoordinate(
                lng = coord.lng,
                lat = coord.lat,
                zoomLevel = ACTimelineVM.STEP_MARKER_ZOOM_LEVEL
            )
        }
        viewModel.onMarkerFocused()
        showMapBottomList()
        scrollToMapBottomItem(mapStep.position)
    }

    fun showCityMarkersMode() {
        val map = mapUi.mapView ?: return
        map.clearMap()
        viewModel.cityMarkers.value?.let { map.showMapIcons(it) }
        viewModel.getSelectedStepMarker()?.let { map.showMapIcons(listOf(it)) }
    }

    fun showStepMarkersMode() {
        val map = mapUi.mapView ?: return
        map.clearMap()
        viewModel.mapSteps.value?.let { map.showMapIcons(it) }
    }

    val actions = remember(viewModel) {
        TimelineItemActions(
            onItemClick = { item ->
                when (item) {
                    is TimelineDisplayItem.BookedActivity -> {
                        val data = item.segment.additionalData
                        if (item.isReserved) {
                            data?.activityId?.let { viewModel.onActivityDetailRequested(it) }
                        } else {
                            data?.bookingId?.let { viewModel.onBookingDetailRequested(it) }
                        }
                    }
                    is TimelineDisplayItem.ManualPoi -> {
                        item.step.poi?.let { poi -> openPoiDetail(poi) }
                    }
                    else -> {}
                }
            },
            onDeleteClick = { item, segmentIndex ->
                segmentIndex?.let { index ->
                    val (title, message) = if (item is TimelineDisplayItem.Recommendations) {
                        viewModel.getLanguageForKey(LanguageConst.REMOVE_RECOMMENDATIONS) to
                            viewModel.getLanguageForKey(LanguageConst.REMOVE_RECOMMENDATIONS_MESSAGE)
                    } else {
                        viewModel.getLanguageForKey(LanguageConst.REMOVE_FROM_ITINERARY) to
                            viewModel.getLanguageForKey(LanguageConst.REMOVE_FROM_ITINERARY_MESSAGE)
                    }
                    showDeleteConfirmation(title, message) {
                        viewModel.deleteSegment(index)
                    }
                }
            },
            onExpandClick = { item ->
                if (item is TimelineDisplayItem.Recommendations) {
                    viewModel.toggleRecommendationExpanded(item.plan.id)
                }
            },
            onStepClick = { step ->
                if (step.stepType == "poi") {
                    step.poi?.let { poi -> openPoiDetail(poi) }
                } else {
                    val activityId = step.poi?.additionalData?.productId ?: step.poi?.id
                    activityId?.let { viewModel.onActivityDetailRequested(it) }
                }
            },
            onChangeTimeClick = { manualPoi ->
                viewModel.showStepChangeTimePicker(manualPoi.step)
            },
            onReservedActivityChangeTimeClick = { reservedActivity ->
                reservedActivity.segmentIndex?.let { idx ->
                    handleSegmentChangeTime(
                        segment = reservedActivity.segment,
                        segmentIndex = idx,
                        title = reservedActivity.title,
                        startDateTime = reservedActivity.startDateTime,
                        isAvailabilityExpired = reservedActivity.isAvailabilityExpired,
                        seedInitialTimeSlot = true
                    )
                }
            },
            onFlexibleActivityChangeTimeClick = { flexibleActivity ->
                flexibleActivity.segmentIndex?.let { idx ->
                    handleSegmentChangeTime(
                        segment = flexibleActivity.segment,
                        segmentIndex = idx,
                        title = flexibleActivity.title,
                        startDateTime = flexibleActivity.segment.startDate,
                        isAvailabilityExpired = flexibleActivity.isAvailabilityExpired,
                        seedInitialTimeSlot = false
                    )
                }
            },
            onReservationClick = { bookedActivity ->
                bookedActivity.segment.additionalData?.activityId?.let { activityId ->
                    val dateTime = bookedActivity.startDateTime.toReservationDateTime()
                    viewModel.onActivityReservationRequested(activityId, dateTime)
                }
            },
            onFlexibleReservationClick = { flexibleActivity ->
                flexibleActivity.segment.additionalData?.activityId?.let { activityId ->
                    val dateTime = flexibleActivity.segment.startDate
                        .toReservationDateTime(isFlexible = true)
                    viewModel.onActivityReservationRequested(activityId, dateTime)
                }
            },
            onAddPlanClick = { showAddPlanSheet() },
            onStepChangeTimeClick = { step -> handleStepChangeTimeClick(step) },
            onStepDeleteClick = { step ->
                showDeleteConfirmation(
                    title = viewModel.getLanguageForKey(LanguageConst.REMOVE_FROM_ITINERARY),
                    message = viewModel.getLanguageForKey(LanguageConst.REMOVE_FROM_ITINERARY_MESSAGE),
                    onConfirm = { viewModel.deleteStep(step) }
                )
            },
            onStepReservationClick = { step ->
                val activityId = step.poi?.additionalData?.productId ?: step.poi?.id
                activityId?.let { id ->
                    val dateTime = step.startDateTimes.toReservationDateTime()
                    viewModel.onActivityReservationRequested(id, dateTime)
                }
            },
            onRequestRouteCalculation = { recommendations ->
                viewModel.calculateRoutesForRecommendations(recommendations)
            },
            onSectionToggle = { cityId -> viewModel.toggleSectionCollapsed(cityId) },
            isSectionCollapsed = { cityId -> viewModel.isSectionCollapsed(cityId) },
            onConflictDismiss = { viewModel.dismissConflictBanner() }
        )
    }

    val timeline by viewModel.timeline.observeAsState()
    val displayItems by viewModel.displayItems.observeAsState(emptyList())
    val availableDays by viewModel.availableDays.observeAsState(emptyList())
    val selectedDayIndex by viewModel.selectedDayIndex.observeAsState(0)
    val pastDayLocked = remember(selectedDayIndex, availableDays) { viewModel.isSelectedDayPast }
    val cities by viewModel.cities.observeAsState(emptyList())
    val isMapMode by viewModel.isMapMode.observeAsState(false)
    val savedPlansCount by viewModel.savedPlansCount.observeAsState(0)
    val noCitiesAvailable by viewModel.noCitiesAvailable.observeAsState(false)
    val mapSteps by viewModel.mapSteps.observeAsState()
    val mapMarkersMode by viewModel.mapMarkersMode.observeAsState()
    val mapBottomItems by viewModel.mapBottomItems.observeAsState()
    val showNearMeButton by viewModel.showNearMeButton.observeAsState(false)
    val mainViewTrigger by viewModel.showMainViewButton.observeAsState()

    LaunchedEffect(isMapMode) {
        val map = mapUi.mapView ?: return@LaunchedEffect
        if (isMapMode) {
            map.clearMap()
            viewModel.mapSteps.value?.takeIf { it.isNotEmpty() }?.let { map.showMapIcons(it) }
            map.moveCameraTo(viewModel.getSelectedDayCityCoordinate())
            if (viewModel.mapBottomItems.value.isNullOrEmpty()) {
                hideMapBottomListCompletely()
            } else {
                showMapBottomList()
            }
        } else {
            map.clearMap()
            hideMapBottomListCompletely()
        }
    }

    LaunchedEffect(mapSteps, mapMarkersMode, isMapMode) {
        if (!isMapMode) return@LaunchedEffect
        when (mapMarkersMode) {
            MapMarkersMode.CITY_MARKERS -> showCityMarkersMode()
            else -> showStepMarkersMode()
        }
    }

    LaunchedEffect(mapBottomItems, isMapMode) {
        if (!isMapMode) return@LaunchedEffect
        if (mapBottomItems.isNullOrEmpty()) {
            hideMapBottomListCompletely()
        } else {
            showMapBottomList()
        }
    }

    val error by viewModel.error.observeAsState()
    LaunchedEffect(error) {
        error?.let { viewModel.showAlert(com.tripian.trpcore.util.AlertType.ERROR, it) }
    }

    val showOnboarding by viewModel.showOnboarding.observeAsState()
    LaunchedEffect(showOnboarding) {
        if (showOnboarding == true && viewModel.shouldShowOnboarding()) sheets.onboardingVisible = true
    }

    val showAddPlanEvent by viewModel.showAddPlanSheet.observeAsState()
    LaunchedEffect(showAddPlanEvent) {
        if (showAddPlanEvent == true) showAddPlanSheet()
    }

    val smartSegmentCreated by viewModel.smartSegmentCreated.observeAsState()
    LaunchedEffect(smartSegmentCreated) {
        smartSegmentCreated?.let { dayIndex ->
            flowState.addPlanVisible = false
            viewModel.selectDay(dayIndex)
            viewModel.clearSmartSegmentCreated()
        }
    }

    val smartCreateError by viewModel.smartCreateError.observeAsState()
    LaunchedEffect(smartCreateError) {
        smartCreateError?.let {
            flowState.addPlanError = it
            viewModel.clearSmartCreateError()
        }
    }

    val smartCreateInProgress by viewModel.smartCreateInProgress.observeAsState()
    LaunchedEffect(smartCreateInProgress) {
        val sheetVm = flowState.addPlanViewModel ?: return@LaunchedEffect
        if (smartCreateInProgress == true) {
            sheetVm.showInSheetLoaderNoText()
        } else {
            sheetVm.hideLottieLoading()
        }
    }

    val scrollToPlanId by viewModel.scrollToNewSegmentPlanId.observeAsState()
    LaunchedEffect(scrollToPlanId) {
        scrollToPlanId?.let { planId ->
            val position = displayItems.indexOfFirst { item ->
                item is TimelineDisplayItem.Recommendations && item.plan.id == planId
            }
            if (position != -1) listState.animateScrollToItem(position)
            viewModel.clearScrollToNewSegment()
        }
    }

    val showChangeTimePickerStep by viewModel.showChangeTimePickerStep.observeAsState()
    LaunchedEffect(showChangeTimePickerStep) {
        showChangeTimePickerStep?.let { step ->
            showStepTimeSelectionSheet(sheets, viewModel, step)
            viewModel.clearChangeTimePickerStep()
        }
    }

    val showChangeTimePickerSegment by viewModel.showChangeTimePickerSegment.observeAsState()
    LaunchedEffect(showChangeTimePickerSegment) {
        showChangeTimePickerSegment?.let { request ->
            showSegmentTimeSelectionSheet(sheets, viewModel, request.segment, request.segmentIndex)
            viewModel.clearChangeTimePickerSegment()
        }
    }

    val changeTimeFinished by viewModel.changeTimeFinished.observeAsState()
    LaunchedEffect(changeTimeFinished) {
        changeTimeFinished?.let { success ->
            viewModel.resetChangeTimeFinished()
            if (success) {
                sheets.dismissChangeTime()
            } else {
                sheets.changeTimeLoadingText = null
            }
        }
    }

    val partialUnavailable by viewModel.showPartialUnavailableAlert.observeAsState()
    LaunchedEffect(partialUnavailable) {
        partialUnavailable?.let { cityNames ->
            showPartialUnavailableAlert(viewModel, cityNames)
            viewModel.clearPartialUnavailableAlert()
        }
    }

    val routeInfoUpdated by viewModel.routeInfoUpdated.observeAsState()
    LaunchedEffect(routeInfoUpdated) {
        routeInfoUpdated?.let { viewModel.clearRouteInfoUpdate() }
    }

    BackHandler {
        if (viewModel.isMapMode.value == true) {
            viewModel.toggleMapMode()
        } else {
            viewModel.onSDKDismissed()
            currentOnDismiss()
        }
    }

    TimelineComposeScreen(
        viewModel = viewModel,
        onExit = { currentOnDismiss() }
    ) {
        if (noCitiesAvailable) {
            NoCityContent(viewModel) {
                TRPCore.notifySDKDismissed()
                currentOnDismiss()
            }
            return@TimelineComposeScreen
        }

        AndroidView(
            factory = { ctx ->
                TrpMapView(ctx).apply {
                    setOnMapClickListener { mapStep -> handleMapItemClick(mapStep) }
                    setOnMapEmptyClickListener {
                        if (viewModel.mapSteps.value.isNullOrEmpty()) return@setOnMapEmptyClickListener
                        val now = System.currentTimeMillis()
                        if (now - mapUi.lastMapInteractionAtMs < MAP_INTERACTION_CLICK_GUARD_MS) {
                            return@setOnMapEmptyClickListener
                        }
                        if (now - mapUi.lastMapEmptyClickAtMs < MAP_EMPTY_CLICK_DEBOUNCE_MS) {
                            return@setOnMapEmptyClickListener
                        }
                        mapUi.lastMapEmptyClickAtMs = now
                        if (mapUi.bottomListVisible) hideMapBottomList() else showMapBottomList()
                    }
                    setOnMapLoadListener { }
                    setOnMapInteractionListener {
                        mapUi.lastMapInteractionAtMs = System.currentTimeMillis()
                        hideMapBottomList()
                    }
                    setOnZoomLevelListener { zoomLevel -> viewModel.onZoomLevelChanged(zoomLevel) }
                    mapUi.mapView = this
                }
            },
            modifier = if (isMapMode) Modifier.fillMaxSize() else Modifier.size(0.dp)
        )

        Column(Modifier.fillMaxSize()) {
            TimelineHeader(
                title = viewModel.getLanguageForKey(LanguageConst.ITINERARY),
                transparentBackground = isMapMode,
                onBackClick = {
                    if (viewModel.isMapMode.value == true) {
                        viewModel.toggleMapMode()
                    } else {
                        viewModel.onSDKDismissed()
                        currentOnDismiss()
                    }
                }
            )

            if (!isMapMode && savedPlansCount > 0) {
                SavedPlansCard(
                    text = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_EMPTY_SAVED),
                    badgeCount = savedPlansCount,
                    onClick = { openSavedPlans() }
                )
            }

            AndroidView(
                factory = { ctx ->
                    TimelineDayFilterView(ctx).apply {
                        setOnDaySelectedListener { index ->
                            viewModel.selectDay(index)
                            scope.launch { listState.scrollToItem(0) }
                            if (viewModel.isMapMode.value == true) {
                                scope.launch {
                                    mapUi.mapView?.moveCameraTo(viewModel.getSelectedDayCityCoordinate())
                                }
                            }
                        }
                    }
                },
                update = { view ->
                    if (view.tag != availableDays) {
                        view.tag = availableDays
                        view.setDays(availableDays)
                    }
                    view.setSelectedDay(selectedDayIndex)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            )

            val hasMultipleCitiesNow = mainViewTrigger.let { viewModel.hasMultipleCities }
            AnimatedVisibility(
                visible = isMapMode && mapUi.bottomListVisible && hasMultipleCitiesNow,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(200)),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 20.dp)
            ) {
                MainViewButton(
                    text = viewModel.getLanguageForKey(LanguageConst.TIMELINE_MAIN_VIEW),
                    onClick = {
                        viewModel.onMainViewClicked()
                        hideMapBottomList()
                        scope.launch {
                            mapUi.mapView?.moveCameraTo(viewModel.getSelectedDayCityCoordinate())
                        }
                    }
                )
            }

            if (timeline != null && !isMapMode) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colorResource(R.color.trp_white)),
                    contentPadding = PaddingValues(bottom = 180.dp)
                ) {
                    items(
                        count = displayItems.size,
                        contentType = { index -> displayItems[index]::class }
                    ) { index ->
                        TimelineListItem(displayItems[index], actions, pastDayLocked)
                    }
                }
            }
        }

        val density = LocalDensity.current
        var consumedInsets by remember { mutableStateOf(WindowInsets(0, 0, 0, 0)) }
        Box(Modifier.matchParentSize().onConsumedWindowInsetsChanged { consumedInsets = it })
        val rawNavBottom = WindowInsets.navigationBars.getBottom(density)
        val consumedBottom = consumedInsets.getBottom(density)
        val navBottom = with(density) { (rawNavBottom - consumedBottom).coerceAtLeast(0).toDp() }
        val fabSafeBottom = if (consumedBottom > 0) 16.dp else maxOf(32.dp, navBottom + 16.dp)
        val listHeightDp = with(density) { mapUi.bottomListHeightPx.toDp() }
        val listExtra = when {
            !isMapMode || mapUi.bottomListCompletelyHidden -> 0.dp
            mapUi.bottomListVisible -> listHeightDp
            else -> (listHeightDp - MAP_BOTTOM_LIST_PEEK_HIDE).coerceAtLeast(0.dp)
        }
        val fabBottom by animateDpAsState(
            targetValue = fabSafeBottom + listExtra,
            animationSpec = tween(300),
            label = "fabBottom"
        )
        val bottomListPeekOffset by animateDpAsState(
            targetValue = if (mapUi.bottomListVisible) 0.dp else MAP_BOTTOM_LIST_PEEK_HIDE,
            animationSpec = tween(300),
            label = "bottomListPeek"
        )

        AnimatedVisibility(
            visible = isMapMode && !mapUi.bottomListCompletelyHidden,
            enter = slideInVertically(tween(300)) { it },
            exit = slideOutVertically(tween(300)) { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            AndroidView(
                factory = { ctx ->
                    val d = ctx.resources.displayMetrics.density
                    RecyclerView(ctx).apply {
                        clipToPadding = false
                        setPadding((16 * d).toInt(), 0, (16 * d).toInt(), (32 * d).toInt())
                        layoutManager = LinearLayoutManager(ctx, LinearLayoutManager.HORIZONTAL, false)
                        val bottomAdapter = MapBottomListAdapter { item ->
                            if (item.isSelected) {
                                when {
                                    item.type == "booked" || item.type == "reserved" || item.type == "flexible" ->
                                        viewModel.onActivityDetailRequested(item.id)
                                    item.type == "step" && item.stepType == "activity" ->
                                        findPoiById(item.id)?.let { poi ->
                                            val activityId = poi.additionalData?.productId ?: poi.id ?: item.id
                                            viewModel.onActivityDetailRequested(activityId)
                                        }
                                    else ->
                                        findPoiById(item.id)?.let { poi -> openPoiDetail(poi) }
                                }
                            } else {
                                val mapStep = viewModel.mapSteps.value?.find { it.poiId == item.id }
                                val markerCoord = mapStep?.coordinate
                                if (markerCoord != null) {
                                    viewModel.selectStepOnMap(item.id)
                                    mapUi.mapView?.selectMarker(item.id)
                                    mapUi.mapView?.zoomToCoordinate(
                                        lng = markerCoord.lng,
                                        lat = markerCoord.lat,
                                        zoomLevel = ACTimelineVM.STEP_MARKER_ZOOM_LEVEL
                                    )
                                } else {
                                    mapUi.bottomListAdapter?.selectItem(item.id)
                                    viewModel.getCityCoordinate(item.cityId)?.let { cityCoord ->
                                        mapUi.mapView?.zoomToCoordinate(
                                            lng = cityCoord.lng,
                                            lat = cityCoord.lat,
                                            zoomLevel = ACTimelineVM.CITY_MARKER_ZOOM_LEVEL
                                        )
                                    }
                                }
                                viewModel.onMarkerFocused()
                                showMapBottomList()
                                val position = mapUi.bottomListAdapter?.currentList
                                    ?.indexOfFirst { it.id == item.id } ?: -1
                                if (position >= 0) smoothScrollToPosition(position)
                            }
                        }
                        adapter = bottomAdapter
                        val snapHelper = PagerSnapHelper()
                        snapHelper.attachToRecyclerView(this)
                        addOnScrollListener(object : RecyclerView.OnScrollListener() {
                            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                                    showMapBottomList()
                                }
                                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                                    val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
                                    val snapView = snapHelper.findSnapView(lm) ?: return
                                    val position = lm.getPosition(snapView)
                                    val bottomItem = mapUi.bottomListAdapter?.currentList?.getOrNull(position)
                                        ?: return
                                    if (!bottomItem.isSelected) {
                                        viewModel.selectStepOnMap(bottomItem.id)
                                        mapUi.mapView?.selectMarker(bottomItem.id)
                                        viewModel.mapSteps.value
                                            ?.find { it.poiId == bottomItem.id }
                                            ?.coordinate?.let { coord ->
                                                mapUi.mapView?.zoomToCoordinate(
                                                    lng = coord.lng,
                                                    lat = coord.lat,
                                                    zoomLevel = ACTimelineVM.STEP_MARKER_ZOOM_LEVEL
                                                )
                                            }
                                        viewModel.onMarkerFocused()
                                    }
                                }
                            }
                        })
                        setOnTouchListener { _, event ->
                            if (event.action == MotionEvent.ACTION_DOWN && !mapUi.bottomListVisible) {
                                showMapBottomList()
                            }
                            false
                        }
                        mapUi.bottomListAdapter = bottomAdapter
                        mapUi.bottomListRecycler = this
                    }
                },
                update = { rv ->
                    (rv.adapter as? MapBottomListAdapter)?.submitList(mapBottomItems ?: emptyList())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .onSizeChanged { mapUi.bottomListHeightPx = it.height }
                    .offset(y = bottomListPeekOffset)
            )
        }

        if (showNearMeButton) {
            OutlinedButton(
                onClick = { viewModel.showNearMePois() },
                border = BorderStroke(1.dp, colorResource(R.color.trp_timeline_primary)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = colorResource(R.color.trp_timeline_primary)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = fabBottom + 56.dp + 16.dp)
                    .height(40.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.trp_ic_near_me),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(colorResource(R.color.trp_timeline_primary)),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_NEAR_ME),
                    fontSize = 12.sp
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = fabBottom)
        ) {
            TimelineFab(
                iconRes = if (isMapMode) R.drawable.trp_ic_list else R.drawable.trp_ic_map,
                containerColor = colorResource(R.color.trp_text_primary),
                onClick = { viewModel.toggleMapMode() }
            )
            if (!pastDayLocked) {
                Spacer(Modifier.height(16.dp))
                TimelineFab(
                    iconRes = R.drawable.trp_ic_plus_bold,
                    containerColor = colorResource(R.color.trp_timeline_fab_color),
                    onClick = { showAddPlanSheet() }
                )
            }
        }

        if (sheets.onboardingVisible) {
            OnboardingSheet(
                onComplete = { viewModel.onOnboardingComplete() },
                onDismiss = { sheets.onboardingVisible = false }
            )
        }

        sheets.changeTimeRequest?.let { request ->
            ActivityTimeSelectionSheet(
                request = request.request,
                inSheetLoadingText = sheets.changeTimeLoadingText,
                onDismiss = { sheets.dismissChangeTime() },
                onTimeSelected = { selection ->
                    sheets.changeTimeLoadingText =
                        viewModel.getLanguageForKey(LanguageConst.LOADING_TEXT_CHANGING_TIME)
                    request.onConfirm(selection)
                },
                onRemove = request.onRemove
            )
        }

        sheets.poiTimeRequest?.let { request ->
            PoiTimeSelectionSheet(
                request = request.request,
                inSheetLoadingText = sheets.poiTimeLoadingText,
                onDismiss = { sheets.dismissPoiTime() },
                onConfirm = { startTime, endTime ->
                    sheets.poiTimeLoadingText =
                        viewModel.getLanguageForKey(LanguageConst.LOADING_TEXT_CHANGING_TIME)
                    request.onConfirm(startTime, endTime)
                }
            )
        }

        if (flowState.addPlanVisible) {
            flowState.addPlanViewModel?.let { addPlanVm ->
                AddPlanSheet(
                    viewModel = addPlanVm,
                    errorMessage = flowState.addPlanError,
                    onErrorDismiss = { flowState.addPlanError = null },
                    onDismissRequest = { flowState.addPlanVisible = false },
                    onOpenManualListing = { category -> openManualListing(category) },
                    onSelectStartingPoint = { openStartingPoint() },
                    onAddPlanComplete = { data ->
                        when {
                            data.mode == AddPlanMode.SMART ||
                                data.mode == AddPlanMode.SMART_RECOMMENDATIONS -> {
                                viewModel.onAddPlanComplete(data)
                            }
                            else -> {
                                flowState.addPlanVisible = false
                                scope.launch {
                                    delay(300)
                                    viewModel.onAddPlanComplete(data)
                                    viewModel.selectDay(data.selectedDayIndex)
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

private class ChangeTimeSheetRequest(
    val request: ActivityTimeSelectionRequest,
    val onRemove: () -> Unit,
    val onConfirm: (ActivityTimeSelection) -> Unit
)

private class PoiTimeSheetRequest(
    val request: PoiTimeSelectionRequest,
    val onConfirm: (startTime: String?, endTime: String?) -> Unit
)

/** Which Timeline sheets are up and what they are showing. */
private class TimelineSheetHolder {
    var onboardingVisible by mutableStateOf(false)
    var changeTimeRequest by mutableStateOf<ChangeTimeSheetRequest?>(null)
    var changeTimeLoadingText by mutableStateOf<String?>(null)
    var poiTimeRequest by mutableStateOf<PoiTimeSheetRequest?>(null)
    var poiTimeLoadingText by mutableStateOf<String?>(null)

    fun dismissChangeTime() {
        changeTimeRequest = null
        changeTimeLoadingText = null
    }

    fun dismissPoiTime() {
        poiTimeRequest = null
        poiTimeLoadingText = null
    }
}

private const val MAP_INTERACTION_CLICK_GUARD_MS = 250L
private const val MAP_EMPTY_CLICK_DEBOUNCE_MS = 400L
private val MAP_BOTTOM_LIST_PEEK_HIDE = 52.dp

private class MapModeUiState {
    var bottomListVisible by mutableStateOf(false)
    var bottomListCompletelyHidden by mutableStateOf(true)
    var bottomListHeightPx by mutableIntStateOf(0)
    var mapView: TrpMapView? = null
    var bottomListAdapter: MapBottomListAdapter? = null
    var bottomListRecycler: RecyclerView? = null
    var lastMapInteractionAtMs = 0L
    var lastMapEmptyClickAtMs = 0L
}

private class TimelineItemActions(
    val onItemClick: (TimelineDisplayItem) -> Unit,
    val onDeleteClick: (TimelineDisplayItem, Int?) -> Unit,
    val onExpandClick: (TimelineDisplayItem) -> Unit,
    val onStepClick: (TimelineStep) -> Unit,
    val onChangeTimeClick: (TimelineDisplayItem.ManualPoi) -> Unit,
    val onReservedActivityChangeTimeClick: (TimelineDisplayItem.BookedActivity) -> Unit,
    val onFlexibleActivityChangeTimeClick: (TimelineDisplayItem.FlexibleActivity) -> Unit,
    val onReservationClick: (TimelineDisplayItem.BookedActivity) -> Unit,
    val onFlexibleReservationClick: (TimelineDisplayItem.FlexibleActivity) -> Unit,
    val onAddPlanClick: () -> Unit,
    val onStepChangeTimeClick: (TimelineStep) -> Unit,
    val onStepDeleteClick: (TimelineStep) -> Unit,
    val onStepReservationClick: (TimelineStep) -> Unit,
    val onRequestRouteCalculation: (TimelineDisplayItem.Recommendations) -> Unit,
    val onSectionToggle: (Int) -> Unit,
    val isSectionCollapsed: (Int) -> Boolean,
    val onConflictDismiss: () -> Unit
)

/**
 * Hosts an existing RecyclerView ViewHolder inside Compose so the item's
 * inflate/bind logic is shared verbatim with the Activity flow.
 */
@Composable
private fun <VH : RecyclerView.ViewHolder> TimelineVHItem(
    create: (LayoutInflater, ViewGroup) -> VH,
    bind: (VH) -> Unit
) {
    val currentBind by rememberUpdatedState(bind)
    AndroidView(
        factory = { ctx ->
            val container = FrameLayout(ctx)
            val vh = create(LayoutInflater.from(ctx), container)
            container.addView(vh.itemView)
            container.tag = vh
            container
        },
        update = { container ->
            @Suppress("UNCHECKED_CAST")
            currentBind(container.tag as VH)
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun TimelineListItem(
    item: TimelineDisplayItem,
    actions: TimelineItemActions,
    pastDayLocked: Boolean
) {
    when (item) {
        is TimelineDisplayItem.SectionHeader -> TimelineVHItem(
            create = { inflater, parent ->
                SectionHeaderVH(ItemTimelineSectionHeaderBinding.inflate(inflater, parent, false))
            },
            bind = { vh ->
                val cityId = item.city?.id ?: 0
                vh.bind(item, actions.isSectionCollapsed(cityId), actions.onSectionToggle)
            }
        )
        is TimelineDisplayItem.BookedActivity -> if (item.isReserved) {
            TimelineVHItem(
                create = { inflater, parent ->
                    ReservedActivityVH(ItemTimelineReservedActivityBinding.inflate(inflater, parent, false))
                },
                bind = { vh ->
                    vh.bind(
                        item,
                        actions.onItemClick,
                        actions.onReservedActivityChangeTimeClick,
                        actions.onDeleteClick,
                        actions.onReservationClick,
                        pastDayLocked
                    )
                }
            )
        } else {
            TimelineVHItem(
                create = { inflater, parent ->
                    BookedActivityVH(ItemTimelineBookedActivityBinding.inflate(inflater, parent, false))
                },
                bind = { vh -> vh.bind(item, actions.onItemClick, actions.onDeleteClick) }
            )
        }
        is TimelineDisplayItem.FlexibleActivity -> TimelineVHItem(
            create = { inflater, parent ->
                FlexibleActivityVH(ItemTimelineFlexibleActivityBinding.inflate(inflater, parent, false))
            },
            bind = { vh ->
                vh.bind(
                    item,
                    actions.onItemClick,
                    actions.onFlexibleActivityChangeTimeClick,
                    actions.onDeleteClick,
                    actions.onFlexibleReservationClick,
                    pastDayLocked
                )
            }
        )
        is TimelineDisplayItem.Recommendations -> TimelineVHItem(
            create = { inflater, parent ->
                RecommendationsVH(ItemTimelineRecommendationsBinding.inflate(inflater, parent, false))
            },
            bind = { vh ->
                vh.bind(
                    item = item,
                    onItemClick = actions.onItemClick,
                    onDeleteClick = actions.onDeleteClick,
                    onExpandClick = actions.onExpandClick,
                    onStepClick = actions.onStepClick,
                    onStepChangeTimeClick = actions.onStepChangeTimeClick,
                    onStepDeleteClick = actions.onStepDeleteClick,
                    onStepReservationClick = actions.onStepReservationClick,
                    onRequestRouteCalculation = actions.onRequestRouteCalculation,
                    pastDayLocked = pastDayLocked
                )
            }
        )
        is TimelineDisplayItem.ManualPoi -> TimelineVHItem(
            create = { inflater, parent ->
                ManualPoiVH(ItemTimelineManualPoiBinding.inflate(inflater, parent, false))
            },
            bind = { vh ->
                vh.bind(
                    item,
                    actions.onItemClick,
                    actions.onChangeTimeClick,
                    actions.onDeleteClick,
                    pastDayLocked
                )
            }
        )
        is TimelineDisplayItem.EmptyState -> TimelineVHItem(
            create = { inflater, parent ->
                EmptyStateVH(ItemTimelineEmptyStateBinding.inflate(inflater, parent, false))
            },
            bind = { vh -> vh.bind(item, actions.onAddPlanClick) }
        )
        is TimelineDisplayItem.SectionFooter -> TimelineVHItem(
            create = { inflater, parent ->
                SectionFooterVH(ItemTimelineSectionFooterBinding.inflate(inflater, parent, false))
            },
            bind = { }
        )
        is TimelineDisplayItem.ConflictWarning -> TimelineVHItem(
            create = { inflater, parent ->
                val view = inflater.inflate(
                    R.layout.item_timeline_conflict_warning, parent, false
                ) as ConflictWarningView
                ConflictWarningVH(view)
            },
            bind = { vh -> vh.bind(onTap = {}, onDismiss = actions.onConflictDismiss) }
        )
    }
}

@Composable
private fun TimelineHeader(
    title: String,
    transparentBackground: Boolean,
    onBackClick: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(
                if (transparentBackground) Color.Transparent else colorResource(R.color.trp_white)
            )
            .padding(16.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.trp_ic_back),
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.CenterStart)
                .clickable { onBackClick() }
        )
        Text(
            text = title,
            color = colorResource(R.color.trp_text_primary),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp)
        )
    }
}

@Composable
private fun SavedPlansCard(text: String, badgeCount: Int, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(colorResource(R.color.trp_bgCloudy), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Box(Modifier.size(42.dp)) {
            Image(
                painter = painterResource(R.drawable.trp_ic_heart),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .background(colorResource(R.color.trp_bgPink), CircleShape)
                    .padding(10.dp)
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.BottomEnd)
                    .background(colorResource(R.color.trp_primary), CircleShape)
            ) {
                Text(
                    text = badgeCount.toString(),
                    color = colorResource(R.color.trp_white),
                    fontFamily = TimelineFontFamily,
                    fontSize = 11.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    style = LocalTextStyle.current.copy(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        }
        Spacer(Modifier.width(11.dp))
        Text(
            text = text,
            color = colorResource(R.color.trp_text_primary),
            fontFamily = TimelineFontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(16.dp))
        Image(
            painter = painterResource(R.drawable.trp_ic_saved_plans_next),
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun TimelineFab(
    iconRes: Int,
    containerColor: Color,
    onClick: () -> Unit
) {
    FloatingActionButton(
        onClick = onClick,
        shape = CircleShape,
        containerColor = containerColor,
        contentColor = colorResource(R.color.trp_white),
        elevation = FloatingActionButtonDefaults.elevation(4.dp, 4.dp, 4.dp, 4.dp),
        modifier = Modifier.size(56.dp)
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colorResource(R.color.trp_white)),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun MainViewButton(text: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(32.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .background(colorResource(R.color.trp_white), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = text,
            color = colorResource(R.color.trp_text_primary),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun NoCityContent(viewModel: ACTimelineVM, onGoToMyTrip: () -> Unit) {
    val currentOnGoToMyTrip by rememberUpdatedState(onGoToMyTrip)
    AndroidView(
        factory = { ctx ->
            NoCityView(ctx).apply {
                setup(
                    title = viewModel.getLanguageForKey(LanguageConst.TIMELINE_NO_CITY_TITLE),
                    description = viewModel.getLanguageForKey(LanguageConst.TIMELINE_NO_CITY_DESCRIPTION),
                    buttonText = viewModel.getLanguageForKey(LanguageConst.TIMELINE_NO_CITY_BUTTON)
                )
                listener = object : NoCityView.Listener {
                    override fun onGoToMyTripClicked() {
                        currentOnGoToMyTrip()
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

private fun showActivityChangeTimeSheet(
    sheets: TimelineSheetHolder,
    viewModel: ACTimelineVM,
    activityId: String?,
    cityId: Int?,
    title: String,
    duration: Double?,
    initialDateTime: String?,
    seedInitialTimeSlot: Boolean,
    isNotAvailable: Boolean = false,
    restrictToInitialDay: Boolean = false,
    onRemove: () -> Unit,
    onConfirm: (selectedDate: Date, startTime: String, endTime: String?, slotPrice: Double?) -> Unit
) {
    if (activityId.isNullOrEmpty()) return
    val initialDay = initialDateTime.toDate()
    val availableDays = if (restrictToInitialDay && initialDay != null) {
        listOf(initialDay)
    } else {
        viewModel.availableDays.value ?: emptyList()
    }
    if (availableDays.isEmpty()) return

    val initialTimeSlot = if (seedInitialTimeSlot) {
        initialDateTime?.takeIf { it.length >= 16 }?.substring(11, 16)
    } else null

    sheets.changeTimeLoadingText = null
    sheets.changeTimeRequest = ChangeTimeSheetRequest(
        request = ActivityTimeSelectionRequest(
            mode = ActivityTimeSelectionMode.StepEdit(
                activityId = activityId,
                cityId = cityId,
                title = title,
                duration = duration,
                initialTimeSlot = initialTimeSlot,
                isNotAvailable = isNotAvailable,
                hideDaySelector = restrictToInitialDay
            ),
            availableDays = availableDays,
            initialSelectedDay = initialDay
        ),
        onRemove = onRemove,
        onConfirm = { selection ->
            onConfirm(selection.selectedDate, selection.startTime, selection.endTime, selection.slotPrice)
        }
    )
}

private fun showStepTimeSelectionSheet(
    sheets: TimelineSheetHolder,
    viewModel: ACTimelineVM,
    step: TimelineStep
) {
    val startTime = step.startDateTimes?.takeIf { it.length >= 16 }?.substring(11, 16)
    val endTime = step.endDateTimes?.takeIf { it.length >= 16 }?.substring(11, 16)
    val stepDay = step.startDateTimes.toDate()
    val minTime = stepDay?.let { CityTimeZones.minSelectableTime(it, step.poi?.cityId) }

    sheets.poiTimeLoadingText = null
    sheets.poiTimeRequest = PoiTimeSheetRequest(
        request = PoiTimeSelectionRequest(startTime = startTime, endTime = endTime, minTime = minTime),
        onConfirm = { newStartTime, newEndTime ->
            viewModel.updateStepTime(
                step.id, newStartTime, newEndTime,
                useInlineLoader = true,
                onInlineResult = { success ->
                    if (success) {
                        sheets.dismissPoiTime()
                    } else {
                        sheets.poiTimeLoadingText = null
                    }
                }
            )
        }
    )
}

private fun showSegmentTimeSelectionSheet(
    sheets: TimelineSheetHolder,
    viewModel: ACTimelineVM,
    segment: TimelineSegment,
    segmentIndex: Int
) {
    fun timePart(dt: String?): String? =
        if (dt != null && dt.length >= 16) dt.substring(11, 16) else null

    val startTime = timePart(segment.startDate) ?: timePart(segment.additionalData?.startDatetime)
    val endTime = timePart(segment.endDate) ?: timePart(segment.additionalData?.endDatetime)
    val segmentDay = (segment.startDate ?: segment.additionalData?.startDatetime).toDate()
    val minTime = segmentDay?.let {
        CityTimeZones.minSelectableTime(it, segment.cityId?.takeIf { c -> c > 0 })
    }

    sheets.poiTimeLoadingText = null
    sheets.poiTimeRequest = PoiTimeSheetRequest(
        request = PoiTimeSelectionRequest(startTime = startTime, endTime = endTime, minTime = minTime),
        onConfirm = { newStartTime, newEndTime ->
            viewModel.updateSegmentTime(
                segment, segmentIndex, newStartTime, newEndTime,
                useInlineLoader = true,
                onInlineResult = { success ->
                    if (success) {
                        sheets.dismissPoiTime()
                    } else {
                        sheets.poiTimeLoadingText = null
                    }
                }
            )
        }
    )
}

private fun showPartialUnavailableAlert(viewModel: ACTimelineVM, cityNames: List<String>) {
    val cityList = cityNames.joinToString(", ")
    if (cityList.isEmpty()) return

    viewModel.showDialog(
        title = viewModel.getLanguageForKey(LanguageConst.TIMELINE_PARTIAL_UNAVAILABLE_TITLE)
            .replace("%@", cityList),
        contentText = viewModel.getLanguageForKey(LanguageConst.TIMELINE_PARTIAL_UNAVAILABLE_DESCRIPTION),
        positiveBtn = viewModel.getLanguageForKey(LanguageConst.TIMELINE_PARTIAL_UNAVAILABLE_BUTTON),
        negativeBtn = null,
        isCloseEnable = false
    )
}
