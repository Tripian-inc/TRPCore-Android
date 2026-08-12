package com.tripian.trpcore.ui.timeline

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.tripian.one.api.pois.model.Poi
import com.tripian.trpcore.base.BaseActivity
import com.tripian.trpcore.base.FRWarning
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.databinding.ActivityTimelineBinding
import com.tripian.trpcore.domain.model.MapStep
import com.tripian.trpcore.domain.model.timeline.AddPlanData
import com.tripian.trpcore.domain.model.timeline.AddPlanMode
import com.tripian.trpcore.domain.model.timeline.MapMarkersMode
import com.tripian.trpcore.domain.model.timeline.TimelineDisplayItem
import com.tripian.trpcore.domain.model.timeline.toApiDateString
import com.tripian.trpcore.domain.model.timeline.toDate
import com.tripian.trpcore.domain.model.timeline.toReservationDateTime
import com.tripian.trpcore.ui.onboarding.OnboardingBottomSheet
import com.tripian.trpcore.ui.timeline.activity.ActivityTimeSelectionBottomSheet
import com.tripian.trpcore.ui.timeline.adapter.MapBottomListAdapter
import com.tripian.trpcore.ui.timeline.adapter.TimelineAdapter
import com.tripian.trpcore.ui.timeline.addplan.AddPlanContainerBottomSheet
import com.tripian.trpcore.ui.timeline.poi.ACPOISelection
import com.tripian.trpcore.ui.timeline.poidetail.ACPOIDetail
import com.tripian.trpcore.ui.timeline.savedplans.ACSavedPlans
import com.tripian.trpcore.ui.timeline.views.NoCityView
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.dialog.DGActionListener
import com.tripian.trpcore.util.extensions.applyBottomSystemBarInsetPadding
import com.tripian.trpcore.util.extensions.consumeSystemBarPadding
import com.tripian.trpcore.util.extensions.dp
import kotlinx.coroutines.launch
import java.util.Date

/**
 * ACTimeline
 * Timeline Itinerary main screen
 */
class ACTimeline : BaseActivity<ActivityTimelineBinding, ACTimelineVM>() {

    private lateinit var timelineAdapter: TimelineAdapter
    private var addPlanSheet: AddPlanContainerBottomSheet? = null
    private var pendingAddPlanData: AddPlanData? = null
    private var mapBottomListAdapter: MapBottomListAdapter? = null
    private var isBottomListVisible = false
    private var isBottomListCompletelyHidden = true
    private var lastMapEmptyClickAtMs = 0L
    private var navigationBarInsetBottom = 0
    private var bottomListHeight = 0
    private var fabAddInitialBottomMargin = 0
    private var fabListInitialBottomMargin = 0
    private var contentInitialBottomPadding = 0

    private var changeTimeSheet: ActivityTimeSelectionBottomSheet? = null

    private val poiSelectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedPoi = result.data?.getSerializableExtra(ACPOISelection.RESULT_POI) as? Poi
            selectedPoi?.let { poi ->
                pendingAddPlanData?.let { data ->
                    data.selectedPoi = poi
                    viewModel.onAddPlanComplete(data)
                    pendingAddPlanData = null
                }
            }
        }
    }

    private val savedPlansLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onReturnFromSavedPlans()
        }
    }

    // =====================
    // LIFECYCLE
    // =====================

    override fun getViewBinding() = ActivityTimelineBinding.inflate(layoutInflater)

    override fun setListeners() {
        fabAddInitialBottomMargin =
            (binding.fabAddPlan.layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin ?: 0
        val rvMapBottomListBottomMargin =
            (binding.rvMapBottomList.layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin ?: 0

        val extraFabSpacing = 16.dp

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { _, insets ->
            navigationBarInsetBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom

            binding.rvMapBottomList.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = maxOf(rvMapBottomListBottomMargin, navigationBarInsetBottom)
            }
            updateFabPositions()

            insets
        }


        binding.rvMapBottomList.doOnLayout {
            bottomListHeight = it.height
            updateFabPositions()
        }

        binding.ivBack.setOnClickListener {
            handleBackNavigation()
        }

        binding.fabMap.setOnClickListener {
            viewModel.toggleMapMode()
        }

        binding.fabList.setOnClickListener {
            viewModel.toggleMapMode()
        }

        binding.fabAddPlan.setOnClickListener {
            showAddPlanSheet()
        }

        binding.dayFilterView.setOnDaySelectedListener { index ->
            viewModel.selectDay(index)
            binding.rvTimeline.scrollToPosition(0)
            if (viewModel.isMapMode.value == true) {
                lifecycleScope.launch {
                    binding.mapView.moveCameraTo(viewModel.getSelectedDayCityCoordinate())
                }
            }
        }

        binding.btnSavedPlans.setOnClickListener {
            openSavedPlans()
        }

        binding.swipeRefresh.isEnabled = false
        binding.swipeRefresh.setOnRefreshListener {
        }

        binding.btnNearMe.setOnClickListener {
            viewModel.showNearMePois()
        }

        binding.mapView.setOnMapClickListener { mapStep ->
            handleMapItemClick(mapStep)
        }

        binding.mapView.setOnMapEmptyClickListener {
            if (viewModel.mapSteps.value.isNullOrEmpty()) return@setOnMapEmptyClickListener
            val now = System.currentTimeMillis()
            if (now - lastMapEmptyClickAtMs < MAP_EMPTY_CLICK_DEBOUNCE_MS) {
                return@setOnMapEmptyClickListener
            }
            lastMapEmptyClickAtMs = now
            if (isBottomListVisible) {
                hideMapBottomList()
            } else {
                showMapBottomList()
            }
        }

        binding.mapView.setOnMapLoadListener {
        }

        binding.mapView.setOnMapInteractionListener {
            hideMapBottomList()
        }

        binding.mapView.setOnZoomLevelListener { zoomLevel ->
            viewModel.onZoomLevelChanged(zoomLevel)
        }

        binding.btnMainView.setOnClickListener {
            viewModel.onMainViewClicked()

            hideMapBottomList()

            lifecycleScope.launch {
                binding.mapView.moveCameraTo(viewModel.getSelectedDayCityCoordinate())
            }
        }
    }

    /**
     * Alerts must land on top of whichever bottom sheet is open, otherwise a
     * failure raised while the sheet is up is drawn behind it.
     */
    override fun alertParent(): android.view.ViewGroup? {
        val openSheet = listOfNotNull(changeTimeSheet, addPlanSheet)
            .lastOrNull { it.isAdded && it.dialog?.isShowing == true }
        return openSheet?.dialog?.window?.decorView as? android.view.ViewGroup
    }

    override fun setReceivers() {
        viewModel.languagesReady.observe(this) { ready ->
            if (ready) setupUI()
        }

        viewModel.showOnboarding.observe(this) { shouldShow ->
            if (shouldShow) {
                showOnboardingBottomSheet()
            }
        }

        viewModel.timeline.observe(this) { timeline ->
            binding.swipeRefresh.isRefreshing = false
            updateUI(timeline != null)
        }

        viewModel.displayItems.observe(this) { items ->
            timelineAdapter.submitList(items)
            updateEmptyState(items.isEmpty() || items.all { it is TimelineDisplayItem.EmptyState })
        }

        viewModel.availableDays.observe(this) { days ->
            binding.dayFilterView.setDays(days)
        }

        viewModel.selectedDayIndex.observe(this) { index ->
            binding.dayFilterView.setSelectedDay(index)
            binding.fabAddPlan.visibility =
                if (isPastDayLocked()) View.GONE else View.VISIBLE
        }

        viewModel.cities.observe(this) { cities ->
            binding.dayFilterView.timeZoneId = cities.firstOrNull()?.timezone
        }

        viewModel.isMapMode.observe(this) { isMapMode ->
            updateMapMode(isMapMode)
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                showAlert(AlertType.ERROR, it)
            }
        }

        viewModel.noCitiesAvailable.observe(this) { noCities ->
            if (noCities == true) {
                showNoCityState()
            }
        }

        viewModel.showPartialUnavailableAlert.observe(this) { cityNames ->
            cityNames?.let {
                showPartialUnavailableAlert(it)
                viewModel.clearPartialUnavailableAlert()
            }
        }

        viewModel.scrollToNewSegmentPlanId.observe(this) { planId ->
            planId?.let {
                scrollToNewSegment(it)
                viewModel.clearScrollToNewSegment()
            }
        }

        viewModel.showAddPlanSheet.observe(this) { show ->
            if (show == true) {
                showAddPlanSheet()
            }
        }

        viewModel.smartSegmentCreated.observe(this) { selectedDayIndex ->
            selectedDayIndex?.let { dayIndex ->
                addPlanSheet?.dismiss()
                viewModel.selectDay(dayIndex)
                viewModel.clearSmartSegmentCreated()
            }
        }

        viewModel.smartCreateError.observe(this) { error ->
            error?.let {
                addPlanSheet?.showCreateError(it)
                viewModel.clearSmartCreateError()
            }
        }

        viewModel.smartCreateInProgress.observe(this) { inProgress ->
            val sheetVm = addPlanSheet?.viewModel ?: return@observe
            if (inProgress == true) {
                sheetVm.showInSheetLoaderNoText()
            } else {
                sheetVm.hideLottieLoading()
            }
        }

        viewModel.mapSteps.observe(this) { mapSteps ->
            if (viewModel.isMapMode.value == true) {
                when (viewModel.mapMarkersMode.value) {
                    MapMarkersMode.CITY_MARKERS -> showCityMarkersMode()
                    MapMarkersMode.STEP_MARKERS -> showStepMarkersMode()
                    else -> showStepMarkersMode()
                }
            }
        }

        viewModel.mapMarkersMode.observe(this) { mode ->
            if (viewModel.isMapMode.value == true) {
                when (mode) {
                    MapMarkersMode.CITY_MARKERS -> showCityMarkersMode()
                    MapMarkersMode.STEP_MARKERS -> showStepMarkersMode()
                }
            }
        }

        viewModel.mapBottomItems.observe(this) { items ->
            mapBottomListAdapter?.submitList(items)
            if (viewModel.isMapMode.value == true) {
                if (items.isNullOrEmpty()) {
                    hideMapBottomListCompletely()
                } else {
                    showMapBottomList()
                }
            }
        }

        viewModel.launchPoiSelection.observe(this) { data ->
            data?.let {
                pendingAddPlanData = it
                val city = it.selectedCity
                if (city != null) {
                    val intent = ACPOISelection.launch(this, city)
                    poiSelectionLauncher.launch(intent)
                }
                viewModel.clearPoiSelectionTrigger()
            }
        }

        viewModel.showNearMeButton.observe(this) { show ->
            binding.btnNearMe.visibility = if (show) View.VISIBLE else View.GONE
        }

        viewModel.savedPlansCount.observe(this) { count ->
            if (count > 0) {
                binding.btnSavedPlans.visibility = View.VISIBLE
                binding.tvSavedPlansBadge.text = count.toString()
            } else {
                binding.btnSavedPlans.visibility = View.GONE
            }
        }

        viewModel.showChangeTimePickerStep.observe(this) { step ->
            step?.let {
                showChangeTimePicker(it)
                viewModel.clearChangeTimePickerStep()
            }
        }

        viewModel.showChangeTimePickerSegment.observe(this) { request ->
            request?.let {
                showSegmentChangeTimePicker(it.segment, it.segmentIndex)
                viewModel.clearChangeTimePickerSegment()
            }
        }

        viewModel.changeTimeFinished.observe(this) { result ->
            result?.let {
                viewModel.resetChangeTimeFinished()
                if (it) {
                    changeTimeSheet?.dismiss()
                    changeTimeSheet = null
                } else {
                    changeTimeSheet?.hideInSheetLoadingOverlay()
                }
            }
        }

        viewModel.routeInfoUpdated.observe(this) { segmentIndex ->
            segmentIndex?.let {
                viewModel.clearRouteInfoUpdate()
            }
        }

        viewModel.showMainViewButton.observe(this) {
            updateMainViewButtonVisibility()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.requestApplyInsets(binding.root)
        setupRecyclerView()
        setupMapBottomList()
        setupUI()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        handleBackNavigation()
    }

    /**
     * Handles back navigation logic.
     * If map mode is active, switches to list mode.
     * Otherwise, closes the SDK and finishes the activity.
     */
    private fun handleBackNavigation() {
        if (viewModel.isMapMode.value == true) {
            viewModel.toggleMapMode()
            return
        }
        viewModel.onSDKDismissed()
        finish()
    }

    // =====================
    // SETUP
    // =====================

    private fun setupUI() {
        binding.tvTitle.text = getLanguageForKey(LanguageConst.ITINERARY)

        binding.tvSavedPlansText.text = getLanguageForKey(LanguageConst.ADD_PLAN_EMPTY_SAVED)

        binding.btnNearMe.text = getLanguageForKey(LanguageConst.ADD_PLAN_NEAR_ME)

        binding.btnMainView.text = getLanguageForKey(LanguageConst.TIMELINE_MAIN_VIEW)

        binding.tvEmptyTitle.text = getLanguageForKey(LanguageConst.NO_PLANS_YET)
        binding.tvEmptySubtitle.text = getLanguageForKey(LanguageConst.NO_PLANS_DESCRIPTION)
    }

    /**
     * A day already behind the traveller can't be replanned: its cards keep their
     * affordances but the mutating handlers do nothing.
     */
    private fun isPastDayLocked(): Boolean = viewModel.isSelectedDayPast

    private fun setupRecyclerView() {
        timelineAdapter = TimelineAdapter(
            onItemClick = { item ->
                handleItemClick(item)
            },
            onDeleteClick = { item, segmentIndex ->
                if (isPastDayLocked()) return@TimelineAdapter
                handleDeleteClick(item, segmentIndex)
            },
            onExpandClick = { item ->
                handleExpandClick(item)
            },
            onStepClick = { step ->
                if (step.stepType == "poi") {
                    step.poi?.let { poi ->
                        startActivity(openPoiDetail(poi))
                    }
                } else {
                    val activityId = step.poi?.additionalData?.productId
                        ?: step.poi?.id
                    activityId?.let { viewModel.onActivityDetailRequested(it) }
                }
            },
            onChangeTimeClick = { manualPoi ->
                if (isPastDayLocked()) return@TimelineAdapter
                handleManualPoiChangeTimeClick(manualPoi)
            },
            onAddPlanClick = {
                showAddPlanSheet()
            },
            onReservedActivityChangeTimeClick = { reservedActivity ->
                if (isPastDayLocked()) return@TimelineAdapter
                reservedActivity.segmentIndex?.let { idx ->
                    showActivityChangeTimeSheet(
                        activityId = reservedActivity.segment.additionalData?.activityId,
                        cityId = reservedActivity.segment.cityId?.takeIf { it > 0 },
                        title = reservedActivity.title,
                        duration = reservedActivity.segment.additionalData?.duration,
                        initialDateTime = reservedActivity.startDateTime,
                        seedInitialTimeSlot = true,
                        isNotAvailable = reservedActivity.isAvailabilityExpired,
                        onRemove = {
                            showDeleteConfirmationDialog(
                                title = getLanguageForKey(LanguageConst.REMOVE_ACTIVITY),
                                message = getLanguageForKey(LanguageConst.REMOVE_ACTIVITY_MESSAGE),
                                onConfirm = {
                                    changeTimeSheet?.dismiss()
                                    changeTimeSheet = null
                                    viewModel.deleteSegment(idx)
                                }
                            )
                        }
                    ) { selectedDate, startTime, endTime, slotPrice ->
                        viewModel.updateSegmentTime(
                            reservedActivity.segment, idx, startTime, endTime,
                            newDate = selectedDate.toApiDateString(),
                            newPrice = slotPrice,
                            useInlineLoader = true
                        )
                    }
                }
            },
            onFlexibleActivityChangeTimeClick = { flexibleActivity ->
                if (isPastDayLocked()) return@TimelineAdapter
                flexibleActivity.segmentIndex?.let { idx ->
                    showActivityChangeTimeSheet(
                        activityId = flexibleActivity.segment.additionalData?.activityId,
                        cityId = flexibleActivity.segment.cityId?.takeIf { it > 0 },
                        title = flexibleActivity.title,
                        duration = flexibleActivity.segment.additionalData?.duration,
                        initialDateTime = flexibleActivity.segment.startDate,
                        seedInitialTimeSlot = false,
                        isNotAvailable = flexibleActivity.isAvailabilityExpired,
                        onRemove = {
                            showDeleteConfirmationDialog(
                                title = getLanguageForKey(LanguageConst.REMOVE_ACTIVITY),
                                message = getLanguageForKey(LanguageConst.REMOVE_ACTIVITY_MESSAGE),
                                onConfirm = {
                                    changeTimeSheet?.dismiss()
                                    changeTimeSheet = null
                                    viewModel.deleteSegment(idx)
                                }
                            )
                        }
                    ) { selectedDate, startTime, endTime, slotPrice ->
                        viewModel.updateSegmentTime(
                            flexibleActivity.segment, idx, startTime, endTime,
                            newDate = selectedDate.toApiDateString(),
                            newPrice = slotPrice,
                            useInlineLoader = true
                        )
                    }
                }
            },
            onReservationClick = { bookedActivity ->
                if (isPastDayLocked()) return@TimelineAdapter
                bookedActivity.segment.additionalData?.activityId?.let { activityId ->
                    val dateTime = bookedActivity.startDateTime.toReservationDateTime()
                    viewModel.onActivityReservationRequested(activityId, dateTime)
                }
            },
            onFlexibleReservationClick = { flexibleActivity ->
                if (isPastDayLocked()) return@TimelineAdapter
                flexibleActivity.segment.additionalData?.activityId?.let { activityId ->
                    val dateTime = flexibleActivity.segment.startDate
                        .toReservationDateTime(isFlexible = true)
                    viewModel.onActivityReservationRequested(activityId, dateTime)
                }
            },
            onStepChangeTimeClick = { step ->
                handleStepChangeTimeClick(step)
            },
            onStepDeleteClick = { step ->
                handleStepDeleteClick(step)
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
            onSectionToggle = { cityId ->
                viewModel.toggleSectionCollapsed(cityId)
            },
            isSectionCollapsed = { cityId ->
                viewModel.isSectionCollapsed(cityId)
            },
            onConflictTap = {
            },
            onConflictDismiss = {
                viewModel.dismissConflictBanner()
            }
        )

        binding.rvTimeline.apply {
            layoutManager = LinearLayoutManager(this@ACTimeline)
            adapter = timelineAdapter
            setHasFixedSize(false)
            itemAnimator = null
        }
        binding.rvTimeline.applyBottomSystemBarInsetPadding()
    }

    /**
     * Setup horizontal list at bottom of map.
     * Shows timeline items as cards when annotation is clicked.
     * Uses PagerSnapHelper for paging scroll behavior.
     */
    private fun setupMapBottomList() {
        mapBottomListAdapter = MapBottomListAdapter { item ->
            if (item.isSelected) {
                when {
                    item.type == "booked" || item.type == "reserved" || item.type == "flexible" -> {
                        viewModel.onActivityDetailRequested(item.id)
                    }
                    item.type == "step" && item.stepType == "activity" -> {
                        findPoiById(item.id)?.let { poi ->
                            val activityId = poi.additionalData?.productId ?: poi.id ?: item.id
                            viewModel.onActivityDetailRequested(activityId)
                        }
                    }
                    else -> {
                        findPoiById(item.id)?.let { poi ->
                            startActivity(openPoiDetail(poi))
                        }
                    }
                }
            } else {
                val mapStep = viewModel.mapSteps.value?.find { it.poiId == item.id }
                val markerCoord = mapStep?.coordinate

                if (markerCoord != null) {
                    viewModel.selectStepOnMap(item.id)
                    binding.mapView.selectMarker(item.id)
                    binding.mapView.zoomToCoordinate(
                        lng = markerCoord.lng,
                        lat = markerCoord.lat,
                        zoomLevel = ACTimelineVM.STEP_MARKER_ZOOM_LEVEL
                    )
                } else {
                    mapBottomListAdapter?.selectItem(item.id)
                    viewModel.getCityCoordinate(item.cityId)?.let { cityCoord ->
                        binding.mapView.zoomToCoordinate(
                            lng = cityCoord.lng,
                            lat = cityCoord.lat,
                            zoomLevel = ACTimelineVM.CITY_MARKER_ZOOM_LEVEL
                        )
                    }
                }

                viewModel.onMarkerFocused()

                showMapBottomList()

                val position = mapBottomListAdapter?.currentList?.indexOfFirst { it.id == item.id } ?: -1
                if (position >= 0) {
                    binding.rvMapBottomList.smoothScrollToPosition(position)
                }
            }
        }

        val snapHelper = PagerSnapHelper()

        binding.rvMapBottomList.apply {
            layoutManager = LinearLayoutManager(
                this@ACTimeline,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = mapBottomListAdapter

            snapHelper.attachToRecyclerView(this)

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                        showMapBottomList()
                    }

                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                        val snapView = snapHelper.findSnapView(layoutManager)
                        snapView?.let { view ->
                            val position = layoutManager.getPosition(view)
                            val item = mapBottomListAdapter?.currentList?.getOrNull(position)
                            item?.let { bottomItem ->
                                if (!bottomItem.isSelected) {
                                    viewModel.selectStepOnMap(bottomItem.id)

                                    binding.mapView.selectMarker(bottomItem.id)

                                    val mapStep = viewModel.mapSteps.value?.find { it.poiId == bottomItem.id }
                                    mapStep?.coordinate?.let { coord ->
                                        binding.mapView.zoomToCoordinate(
                                            lng = coord.lng,
                                            lat = coord.lat,
                                            zoomLevel = ACTimelineVM.STEP_MARKER_ZOOM_LEVEL
                                        )
                                    }

                                    viewModel.onMarkerFocused()
                                }
                            }
                        }
                    }
                }
            })

            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    if (!isBottomListVisible) {
                        showMapBottomList()
                    }
                }
                false
            }
        }
    }

    /**
     * Find POI by id from current display items.
     * Searches through Recommendations steps and ManualPoi items.
     */
    private fun findPoiById(poiId: String): com.tripian.one.api.pois.model.Poi? {
        val displayItems = viewModel.displayItems.value ?: return null

        for (item in displayItems) {
            when (item) {
                is TimelineDisplayItem.Recommendations -> {
                    item.steps.forEach { step ->
                        if (step.poi?.id == poiId) {
                            return step.poi
                        }
                    }
                }
                is TimelineDisplayItem.ManualPoi -> {
                    if (item.step.poi?.id == poiId) {
                        return item.step.poi
                    }
                }
                else -> {}
            }
        }
        return null
    }

    // =====================
    // UI UPDATES
    // =====================

    private fun updateUI(hasData: Boolean) {
        binding.rvTimeline.visibility = if (hasData) View.VISIBLE else View.GONE
    }

    private fun updateEmptyState(isEmpty: Boolean) {
    }

    /**
     * Switches between list and map UI. In map mode the root's top status-bar
     * padding is dropped so the map fills behind a transparent status bar, and
     * the header offsets itself using the real system-bar inset to stay clear
     * of it; list mode restores the consumed top inset padding and the white bar.
     */
    private fun updateMapMode(isMapMode: Boolean) {
        binding.swipeRefresh.visibility = if (isMapMode) View.GONE else View.VISIBLE
        binding.mapContainer.visibility = if (isMapMode) View.VISIBLE else View.GONE

        if (isMapMode) {
            binding.btnSavedPlans.visibility = View.GONE
        } else {
            hideMapBottomListCompletely()
            val count = viewModel.savedPlansCount.value ?: 0
            binding.btnSavedPlans.visibility = if (count > 0) View.VISIBLE else View.GONE
        }

        val elevationDp = 8f * resources.displayMetrics.density
        val defaultPadding = (16 * resources.displayMetrics.density).toInt()

        if (isMapMode) {
            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
                v.updatePadding(top = 0)
                val statusBarInset = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).top
                binding.headerContainer.updatePadding(top = statusBarInset + defaultPadding)
                WindowInsetsCompat.CONSUMED
            }
            window.statusBarColor = android.graphics.Color.TRANSPARENT

            binding.headerContainer.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            binding.headerContainer.elevation = elevationDp
            binding.dayFilterView.elevation = elevationDp
        } else {
            binding.root.consumeSystemBarPadding(top = true)
            window.statusBarColor = android.graphics.Color.WHITE

            binding.headerContainer.setBackgroundColor(android.graphics.Color.WHITE)
            binding.headerContainer.elevation = 0f
            binding.dayFilterView.elevation = 0f
            binding.headerContainer.updatePadding(top = defaultPadding)
        }
        ViewCompat.requestApplyInsets(binding.root)

        val addPlanVisibility = if (isPastDayLocked()) View.GONE else View.VISIBLE
        if (isMapMode) {
            binding.fabMap.visibility = View.GONE
            binding.fabAddPlan.visibility = addPlanVisibility
            binding.fabList.visibility = View.VISIBLE
        } else {
            binding.fabMap.visibility = View.VISIBLE
            binding.fabAddPlan.visibility = addPlanVisibility
            binding.fabList.visibility = View.GONE
        }

        if (isMapMode) {
            val mapSteps = viewModel.mapSteps.value
            binding.mapView.clearMap()
            if (!mapSteps.isNullOrEmpty()) {
                binding.mapView.showMapIcons(mapSteps)
            }
            lifecycleScope.launch {
                binding.mapView.moveCameraTo(viewModel.getSelectedDayCityCoordinate())
            }
            if (viewModel.mapBottomItems.value.isNullOrEmpty()) {
                hideMapBottomListCompletely()
            } else {
                binding.rvMapBottomList.visibility = View.VISIBLE
                showMapBottomList()
            }
        } else {
            binding.mapView.clearMap()
            isBottomListVisible = false
            binding.rvMapBottomList.visibility = View.GONE
            binding.rvMapBottomList.translationY = 0f
            binding.fabList.translationY = 0f
            binding.fabAddPlan.translationY = 0f
            binding.btnMainView.visibility = View.GONE
        }
    }

    /**
     * Handles map marker taps. City-marker taps (and any tap while in
     * city-markers mode) only re-center the city without changing selection;
     * step-marker taps update the selection and bottom list.
     */
    private fun handleMapItemClick(mapStep: MapStep) {
        if (mapStep.isCityMarker) {
            focusCityOnMap(mapStep)
            return
        }

        if (viewModel.mapMarkersMode.value == MapMarkersMode.CITY_MARKERS) {
            focusCityOnMap(mapStep)
            return
        }

        viewModel.selectStepOnMap(mapStep.poiId)

        binding.mapView.selectMarker(mapStep.poiId)

        mapStep.coordinate?.let { coord ->
            binding.mapView.zoomToCoordinate(
                lng = coord.lng,
                lat = coord.lat,
                zoomLevel = ACTimelineVM.STEP_MARKER_ZOOM_LEVEL
            )
        }

        viewModel.onMarkerFocused()

        showMapBottomList()
        scrollToMapBottomItem(mapStep.position)
    }

    /**
     * Centers the map on a city by fitting all of its steps (like opening a
     * single-city map), without changing the step selection. Falls back to the
     * marker's own coordinate when the city has no located steps.
     */
    private fun focusCityOnMap(mapStep: MapStep) {
        viewModel.onMarkerFocused()
        val cityStepPoints = viewModel.getStepCoordinatesForCity(mapStep.cityId)
        if (cityStepPoints.isNotEmpty()) {
            lifecycleScope.launch {
                binding.mapView.fitCameraToPoints(cityStepPoints)
            }
        } else {
            mapStep.coordinate?.let { coord ->
                binding.mapView.zoomToCoordinate(
                    lng = coord.lng,
                    lat = coord.lat,
                    zoomLevel = ACTimelineVM.CITY_MARKER_ZOOM_LEVEL
                )
            }
        }
    }

    /**
     * Show the horizontal item list at the bottom of the map with slide-up animation.
     * Also moves the fabList button above the list.
     */
    private fun showMapBottomList() {
        if (isBottomListVisible) return
        isBottomListVisible = true
        isBottomListCompletelyHidden = false
        updateMainViewButtonVisibility()

        binding.rvMapBottomList.visibility = View.VISIBLE

        binding.rvMapBottomList.animate()
            .translationY(0f)
            .setDuration(300)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
        updateFabPositions()
    }

    /**
     * Hide the horizontal item list with slide-down animation.
     * Keeps part of the card visible at the bottom for a peek effect.
     */
    private fun hideMapBottomList() {
        if (!isBottomListVisible) return
        isBottomListVisible = false
        isBottomListCompletelyHidden = false
        updateMainViewButtonVisibility()

        val itemHeight = 104f * resources.displayMetrics.density
        val translationY = itemHeight * 0.5f

        binding.rvMapBottomList.animate()
            .translationY(translationY)
            .setDuration(300)
            .setInterpolator(android.view.animation.AccelerateInterpolator())
            .start()
        updateFabPositions()
    }

    /**
     * Completely hide the bottom list (for empty days).
     * Unlike hideMapBottomList, this doesn't show peek effect.
     * Keeps FAB at its normal position.
     */
    private fun hideMapBottomListCompletely() {
        isBottomListVisible = false
        isBottomListCompletelyHidden = true
        binding.rvMapBottomList.visibility = View.GONE
        binding.fabList.translationY = 0f
        binding.fabAddPlan.translationY = 0f
        updateMainViewButtonVisibility()
        updateFabPositions()
    }

    /**
     * Syncs btnMainView visibility: visible only while the bottom list is
     * expanded and the selected day spans multiple cities.
     */
    private fun updateMainViewButtonVisibility() {
        val shouldShow = isBottomListVisible && viewModel.hasMultipleCities
        val isCurrentlyShown =
            binding.btnMainView.visibility == View.VISIBLE && binding.btnMainView.alpha > 0f

        if (shouldShow && !isCurrentlyShown) {
            binding.btnMainView.animate().cancel()
            binding.btnMainView.alpha = 0f
            binding.btnMainView.visibility = View.VISIBLE
            binding.btnMainView.animate()
                .alpha(1f)
                .setDuration(200)
                .start()
        } else if (!shouldShow && binding.btnMainView.visibility == View.VISIBLE) {
            binding.btnMainView.animate().cancel()
            binding.btnMainView.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction { binding.btnMainView.visibility = View.GONE }
                .start()
        }
    }

    /**
     * Shows city markers mode for multi-city days.
     * Displays city icons + selected step marker only.
     * Does NOT move camera - camera position is controlled by user interactions.
     */
    private fun showCityMarkersMode() {
        binding.mapView.clearMap()

        viewModel.cityMarkers.value?.let { cities ->
            binding.mapView.showMapIcons(cities)
        }

        viewModel.getSelectedStepMarker()?.let { selectedStep ->
            binding.mapView.showMapIcons(listOf(selectedStep))
        }
    }

    /**
     * Shows step markers mode.
     * Displays all step markers (for single-city days or zoomed-in multi-city).
     * Does NOT move camera - camera position is controlled by user interactions.
     */
    private fun showStepMarkersMode() {
        binding.mapView.clearMap()

        viewModel.mapSteps.value?.let { steps ->
            binding.mapView.showMapIcons(steps)
        }
    }

    /** Keeps the add-plan FAB above the bottom list's visible top edge (full, peek, or hidden). */
    private fun updateFabPositions() {
        val extraFabSpacing = 16.dp
        val peekHidePx = (104f * resources.displayMetrics.density * 0.5f).toInt()

        val safeBottomForAddFab = maxOf(
            fabAddInitialBottomMargin,
            navigationBarInsetBottom + extraFabSpacing
        )

        val listExtra = when {
            isBottomListCompletelyHidden -> 0
            isBottomListVisible -> bottomListHeight
            else -> (bottomListHeight - peekHidePx).coerceAtLeast(0)
        }
        animateBottomMargin(
            view = binding.fabAddPlan,
            targetMargin = safeBottomForAddFab + listExtra,
            animated = true
        )
    }

    private fun animateBottomMargin(
        view: View,
        targetMargin: Int,
        animated: Boolean
    ) {
        val params = view.layoutParams as? ViewGroup.MarginLayoutParams ?: return
        val currentMargin = params.bottomMargin

        if (currentMargin == targetMargin) return

        if (!animated) {
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = targetMargin
            }
            return
        }

        val animator = ValueAnimator.ofInt(currentMargin, targetMargin).apply {
            duration = 300L
            interpolator = DecelerateInterpolator()
            addUpdateListener { valueAnimator ->
                val animatedMargin = valueAnimator.animatedValue as Int
                view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = animatedMargin
                }
            }
        }

        animator.start()
    }
    /**
     * Scroll the bottom list to show the item at the given position (order number).
     */
    private fun scrollToMapBottomItem(position: Int) {
        val index = mapBottomListAdapter?.currentList?.indexOfFirst { it.order == position } ?: -1
        if (index >= 0) {
            binding.rvMapBottomList.smoothScrollToPosition(index)
        }
    }

    // =====================
    // ITEM INTERACTIONS
    // =====================

    private fun handleItemClick(item: TimelineDisplayItem) {
        when (item) {
            is TimelineDisplayItem.BookedActivity -> {
                val data = item.segment.additionalData
                if (item.isReserved) {
                    data?.activityId?.let { viewModel.onActivityDetailRequested(it) }
                } else {
                    data?.bookingId?.let { viewModel.onBookingDetailRequested(it) }
                }
            }
            is TimelineDisplayItem.Recommendations -> {
            }
            is TimelineDisplayItem.ManualPoi -> {
                item.step.poi?.let { poi ->
                    startActivity(openPoiDetail(poi))
                }
            }
            else -> {}
        }
    }

    private fun handleDeleteClick(item: TimelineDisplayItem, segmentIndex: Int?) {
        segmentIndex?.let { index ->
            val (title, message) = when (item) {
                is TimelineDisplayItem.Recommendations -> {
                    Pair(
                        getLanguageForKey(LanguageConst.REMOVE_RECOMMENDATIONS),
                        getLanguageForKey(LanguageConst.REMOVE_RECOMMENDATIONS_MESSAGE)
                    )
                }
                else -> {
                    Pair(
                        getLanguageForKey(LanguageConst.REMOVE_ACTIVITY),
                        getLanguageForKey(LanguageConst.REMOVE_ACTIVITY_MESSAGE)
                    )
                }
            }

            showDeleteConfirmationDialog(
                title = title,
                message = message,
                onConfirm = {
                    viewModel.deleteSegment(index)
                }
            )
        }
    }

    private fun handleExpandClick(item: TimelineDisplayItem) {
        if (item is TimelineDisplayItem.Recommendations) {
            viewModel.toggleRecommendationExpanded(item.plan.id)
        }
    }

    private fun scrollToNewSegment(planId: String) {
        val position = timelineAdapter.currentList.indexOfFirst { item ->
            item is TimelineDisplayItem.Recommendations && item.plan.id == planId
        }
        if (position != -1) {
            binding.rvTimeline.smoothScrollToPosition(position)
        }
    }

    /**
     * Activity-type steps open the availability time-slot picker; other steps
     * open the plain HH:mm picker.
     */
    private fun handleStepChangeTimeClick(step: com.tripian.one.api.timeline.model.TimelineStep) {
        if (step.stepType == "activity") {
            val poi = step.poi ?: return
            showActivityChangeTimeSheet(
                activityId = poi.additionalData?.productId ?: poi.id,
                cityId = poi.cityId,
                title = poi.name.orEmpty(),
                duration = poi.duration?.toDouble(),
                initialDateTime = step.startDateTimes,
                seedInitialTimeSlot = true,
                isNotAvailable = step.isAvailabilityExpired,
                restrictToInitialDay = true,
                onRemove = {
                    showDeleteConfirmationDialog(
                        title = getLanguageForKey(LanguageConst.REMOVE_STEP),
                        message = getLanguageForKey(LanguageConst.REMOVE_STEP_MESSAGE),
                        onConfirm = {
                            changeTimeSheet?.dismiss()
                            changeTimeSheet = null
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

    private fun handleManualPoiChangeTimeClick(manualPoi: TimelineDisplayItem.ManualPoi) {
        viewModel.showStepChangeTimePicker(manualPoi.step)
    }

    /**
     * Opens [ActivityTimeSelectionBottomSheet] for an activity-bearing cell
     * (Recommendations activity step, reserved or flexible activity).
     *
     * @param activityId required for slot loading; when null the sheet is not opened.
     * @param seedInitialTimeSlot `true` to pre-select the existing HH:mm in the
     *   grid; `false` for flexible cells whose recorded time is a placeholder.
     * @param restrictToInitialDay `true` hides the day filter and locks the sheet to
     *   [initialDateTime]'s day so only its time slots can be picked; used for
     *   Recommendations activity steps, which have no way to move to a different
     *   day. Reserved/flexible activities keep full day selection.
     */
    private fun showActivityChangeTimeSheet(
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

        val sheet = ActivityTimeSelectionBottomSheet.newInstanceForStepEdit(
            activityId = activityId,
            cityId = cityId,
            title = title,
            duration = duration,
            availableDays = availableDays,
            initialSelectedDay = initialDay,
            initialTimeSlot = initialTimeSlot,
            isNotAvailable = isNotAvailable,
            hideDaySelector = restrictToInitialDay
        )
        sheet.setOnStepTimeSelectedListener { date, startTime, endTime, slotPrice ->
            changeTimeSheet?.showInSheetLoadingOverlay(
                LanguageConst.LOADING_TEXT_CHANGING_TIME, "Changing time"
            )
            onConfirm(date, startTime, endTime, slotPrice)
        }
        sheet.setOnRemoveListener(onRemove)
        changeTimeSheet = sheet
        sheet.show(supportFragmentManager, ActivityTimeSelectionBottomSheet.TAG)
    }

    private fun handleStepDeleteClick(step: com.tripian.one.api.timeline.model.TimelineStep) {
        showDeleteConfirmationDialog(
            title = getLanguageForKey(LanguageConst.REMOVE_ACTIVITY),
            message = getLanguageForKey(LanguageConst.REMOVE_ACTIVITY_MESSAGE),
            onConfirm = {
                viewModel.deleteStep(step)
            }
        )
    }

    /**
     * Shows the start/end time picker for a step, floored at the city's "now"
     * for the step's day so the time can't be moved into the past.
     */
    private fun showChangeTimePicker(step: com.tripian.one.api.timeline.model.TimelineStep) {
        val startTime = step.startDateTimes?.let { dateTime ->
            if (dateTime.length >= 16) dateTime.substring(11, 16) else null
        }

        val endTime = step.endDateTimes?.let { dateTime ->
            if (dateTime.length >= 16) dateTime.substring(11, 16) else null
        }

        val stepDay = step.startDateTimes.toDate()
        val minTime = stepDay?.let {
            com.tripian.trpcore.util.CityTimeZones.minSelectableTime(it, step.poi?.cityId)
        }

        val timeSelectionSheet = TimeSelectionBottomSheet.newInstance(
            startTime = startTime,
            endTime = endTime,
            minTime = minTime
        )

        timeSelectionSheet.setOnTimeSelectedListener { newStartTime, newEndTime ->
            timeSelectionSheet.showInSheetLoadingOverlay(
                LanguageConst.LOADING_TEXT_CHANGING_TIME, "Changing time"
            )
            viewModel.updateStepTime(
                step.id, newStartTime, newEndTime,
                useInlineLoader = true,
                onInlineResult = { success ->
                    if (success) {
                        timeSelectionSheet.dismiss()
                    } else {
                        timeSelectionSheet.hideInSheetLoadingOverlay()
                    }
                }
            )
        }

        timeSelectionSheet.show(supportFragmentManager, TimeSelectionBottomSheet.TAG)
    }

    /**
     * Shows the time picker for a top-level segment (reserved/flexible activity).
     * The segment carries "yyyy-MM-dd HH:mm" datetimes; we only edit the HH:mm part.
     */
    private fun showSegmentChangeTimePicker(
        segment: com.tripian.one.api.timeline.model.TimelineSegment,
        segmentIndex: Int
    ) {
        fun timePart(dt: String?): String? =
            if (dt != null && dt.length >= 16) dt.substring(11, 16) else null

        val startTime = timePart(segment.startDate)
            ?: timePart(segment.additionalData?.startDatetime)
        val endTime = timePart(segment.endDate)
            ?: timePart(segment.additionalData?.endDatetime)

        val segmentDay = (segment.startDate ?: segment.additionalData?.startDatetime).toDate()
        val minTime = segmentDay?.let {
            com.tripian.trpcore.util.CityTimeZones.minSelectableTime(it, segment.cityId?.takeIf { c -> c > 0 })
        }

        val sheet = TimeSelectionBottomSheet.newInstance(
            startTime = startTime,
            endTime = endTime,
            minTime = minTime
        )

        sheet.setOnTimeSelectedListener { newStartTime, newEndTime ->
            sheet.showInSheetLoadingOverlay(
                LanguageConst.LOADING_TEXT_CHANGING_TIME, "Changing time"
            )
            viewModel.updateSegmentTime(
                segment, segmentIndex, newStartTime, newEndTime,
                useInlineLoader = true,
                onInlineResult = { success ->
                    if (success) {
                        sheet.dismiss()
                    } else {
                        sheet.hideInSheetLoadingOverlay()
                    }
                }
            )
        }

        sheet.show(supportFragmentManager, TimeSelectionBottomSheet.TAG)
    }

    /**
     * Shows a confirmation dialog before delete operations
     */
    private fun showDeleteConfirmationDialog(
        title: String,
        message: String,
        onConfirm: () -> Unit
    ) {
        viewModel.showDialog(
            title = title,
            contentText = message,
            positiveBtn = getLanguageForKey(LanguageConst.REMOVE_BUTTON),
            negativeBtn = getLanguageForKey(LanguageConst.CANCEL),
            positive = object : DGActionListener {
                override fun onClicked(o: Any?) {
                    onConfirm()
                }
            },
            isCloseEnable = false
        )
    }

    // =====================
    // SAVED PLANS
    // =====================

    private fun openSavedPlans() {
        val filteredFavorites = viewModel.getFilteredFavorites()
        if (filteredFavorites.isEmpty()) return

        val availableDays = viewModel.availableDays.value ?: emptyList()

        val cityMap = viewModel.getCityNameToIdMap()

        val intent = ACSavedPlans.launch(
            context = this,
            favorites = filteredFavorites,
            tripHash = viewModel.tripHash,
            availableDays = availableDays,
            cityNameToIdMap = cityMap,
            plannedActivityIdsByDay = viewModel.plannedActivityIdsByDay()
        )
        savedPlansLauncher.launch(intent)
    }

    // =====================
    // ADD PLAN
    // =====================

    /** POI detail needs the trip window so it can query that POI's bookable products. */
    private fun openPoiDetail(poi: com.tripian.one.api.pois.model.Poi): android.content.Intent {
        val (tripStart, tripEnd) = viewModel.tripDateRange()
        return ACPOIDetail.launch(this, poi, tripStart, tripEnd)
    }

    private fun showAddPlanSheet() {
        addPlanSheet = AddPlanContainerBottomSheet.newInstance(
            availableDays = viewModel.availableDays.value ?: emptyList(),
            cities = viewModel.cities.value ?: emptyList(),
            selectedDayIndex = viewModel.selectedDayIndex.value ?: 0,
            selectedCity = viewModel.getSelectedCity(),
            tripHash = viewModel.tripHash,
            bookedActivities = viewModel.getBookedActivities(),
            plannedActivityIdsByDay = viewModel.plannedActivityIdsByDay(),
            tripWideExcludedActivityIds = viewModel.tripWideExcludedActivityIds()
        )

        addPlanSheet?.setOnAddPlanCompleteListener { data ->
            when {
                data.mode == AddPlanMode.MANUAL && data.selectedPoi == null -> {
                    pendingAddPlanData = data
                    val city = data.selectedCity
                    if (city != null) {
                        val intent = ACPOISelection.launch(this, city)
                        poiSelectionLauncher.launch(intent)
                    }
                }
                data.mode == AddPlanMode.SMART || data.mode == AddPlanMode.SMART_RECOMMENDATIONS -> {
                    viewModel.onAddPlanComplete(data)
                }
                else -> {
                    addPlanSheet?.dismiss()
                    binding.root.postDelayed({
                        viewModel.onAddPlanComplete(data)
                        viewModel.selectDay(data.selectedDayIndex)
                    }, 300)
                }
            }
        }

        addPlanSheet?.setOnSegmentCreatedListener { selectedDayIndex ->
            viewModel.onReturnFromAddPlan()
            viewModel.selectDay(selectedDayIndex)
        }

        addPlanSheet?.show(supportFragmentManager, AddPlanContainerBottomSheet.TAG)
    }

    // =====================
    // NO CITY AVAILABLE
    // =====================

    /**
     * Shows the NoCityView when all destination cities are unavailable.
     * Hides all timeline UI elements and displays the no city state.
     */
    private fun showNoCityState() {
        binding.dayFilterView.visibility = View.GONE
        binding.btnSavedPlans.visibility = View.GONE
        binding.swipeRefresh.visibility = View.GONE
        binding.mapContainer.visibility = View.GONE
        binding.fabMap.visibility = View.GONE
        binding.fabList.visibility = View.GONE
        binding.fabAddPlan.visibility = View.GONE
        binding.emptyStateView.visibility = View.GONE
        binding.btnNearMe.visibility = View.GONE

        binding.noCityView.visibility = View.VISIBLE
        binding.noCityView.setup(
            title = getLanguageForKey(LanguageConst.TIMELINE_NO_CITY_TITLE),
            description = getLanguageForKey(LanguageConst.TIMELINE_NO_CITY_DESCRIPTION),
            buttonText = getLanguageForKey(LanguageConst.TIMELINE_NO_CITY_BUTTON)
        )
        binding.noCityView.listener = object : NoCityView.Listener {
            override fun onGoToMyTripClicked() {
                TRPCore.notifySDKDismissed()
                finish()
            }
        }
    }

    /**
     * Shows an alert when some destination cities are unavailable.
     * The timeline continues to create with available cities.
     */
    private fun showPartialUnavailableAlert(cityNames: List<String>) {
        val cityList = cityNames.joinToString(", ")
        if (cityList.isEmpty()) return

        val titleTemplate = getLanguageForKey(LanguageConst.TIMELINE_PARTIAL_UNAVAILABLE_TITLE)
        val title = titleTemplate.replace("%@", cityList)

        val description = getLanguageForKey(LanguageConst.TIMELINE_PARTIAL_UNAVAILABLE_DESCRIPTION)
        val buttonText = getLanguageForKey(LanguageConst.TIMELINE_PARTIAL_UNAVAILABLE_BUTTON)

        val dialog = FRWarning.newInstance(
            title = title,
            contentText = description,
            positiveBtn = buttonText,
            negativeBtn = null,
            isCloseEnable = false
        )

        dialog.positiveListener = object : DGActionListener {
            override fun onClicked(o: Any?) {
                dialog.dismiss()
            }
        }

        dialog.show(supportFragmentManager, "PartialUnavailableAlert")
    }

    // =====================
    // ONBOARDING
    // =====================

    /**
     * Shows the onboarding bottom sheet.
     * Called when SDK starts and onboarding should be shown.
     */
    private fun showOnboardingBottomSheet() {
        val bottomSheet = OnboardingBottomSheet.newInstance()
        bottomSheet.setOnCompleteListener {
            viewModel.onOnboardingComplete()
        }
        bottomSheet.show(supportFragmentManager, "onboarding")
    }

    // =====================
    // COMPANION
    // =====================

    companion object {
        private const val EXTRA_TRIP_HASH = "tripHash"
        private const val MAP_EMPTY_CLICK_DEBOUNCE_MS = 400L

        fun newIntent(context: Context, tripHash: String): Intent {
            return Intent(context, ACTimeline::class.java).apply {
                putExtra(EXTRA_TRIP_HASH, tripHash)
            }
        }
    }
}
