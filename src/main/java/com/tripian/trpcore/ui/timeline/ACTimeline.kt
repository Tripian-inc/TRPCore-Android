package com.tripian.trpcore.ui.timeline

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tripian.one.api.pois.model.Poi
import com.tripian.trpcore.base.BaseActivity
import com.tripian.trpcore.databinding.ActivityTimelineBinding
import com.tripian.trpcore.domain.model.MapStep
import com.tripian.trpcore.domain.model.timeline.AddPlanData
import com.tripian.trpcore.domain.model.timeline.AddPlanMode
import com.tripian.trpcore.domain.model.timeline.TimelineDisplayItem
import com.tripian.trpcore.ui.timeline.adapter.MapBottomListAdapter
import com.tripian.trpcore.ui.timeline.adapter.TimelineAdapter
import com.tripian.trpcore.ui.timeline.addplan.AddPlanContainerBottomSheet
import com.tripian.trpcore.ui.timeline.addplan.TimePickerBottomSheet
import com.tripian.trpcore.ui.timeline.poi.ACPOISelection
import com.tripian.trpcore.ui.timeline.poidetail.ACPOIDetail
import com.tripian.trpcore.ui.timeline.savedplans.ACSavedPlans
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.dialog.DGActionListener
import kotlinx.coroutines.launch

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

    // POI Selection launcher
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

    // Saved Plans launcher
    private val savedPlansLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Activity was added from saved plans, refresh timeline
            viewModel.refreshTimeline()
        }
    }

    // =====================
    // LIFECYCLE
    // =====================

    override fun getViewBinding() = ActivityTimelineBinding.inflate(layoutInflater)

    override fun setListeners() {
        // Back button
        binding.ivBack.setOnClickListener {
            viewModel.onSDKDismissed()
            finish()
        }

        // Map FAB - switches to map mode
        binding.fabMap.setOnClickListener {
            viewModel.toggleMapMode()
        }

        // List FAB - switches back to list mode
        binding.fabList.setOnClickListener {
            viewModel.toggleMapMode()
        }

        // Add plan FAB
        binding.fabAddPlan.setOnClickListener {
            showAddPlanSheet()
        }

        // Day filter
        binding.dayFilterView.setOnDaySelectedListener { index ->
            viewModel.selectDay(index)
            // Scroll to top when day changes
            binding.rvTimeline.scrollToPosition(0)
        }

        // Saved plans button
        binding.btnSavedPlans.setOnClickListener {
            openSavedPlans()
        }

        // Swipe refresh
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshTimeline()
        }

        // Near Me button
        binding.btnNearMe.setOnClickListener {
            viewModel.showNearMePois()
        }

        // Map click listener
        binding.mapView.setOnMapClickListener { mapStep ->
            handleMapItemClick(mapStep)
        }

        // Map load listener
        binding.mapView.setOnMapLoadListener {
            // Map is ready, we can now show markers
        }

        // Map interaction listener - hide bottom list when user pans or zooms
        binding.mapView.setOnMapInteractionListener {
            hideMapBottomList()
        }
    }

    override fun setReceivers() {
        // Timeline data
        viewModel.timeline.observe(this) { timeline ->
            binding.swipeRefresh.isRefreshing = false
            updateUI(timeline != null)
        }

        // Display items
        viewModel.displayItems.observe(this) { items ->
            timelineAdapter.submitList(items)
            updateEmptyState(items.isEmpty() || items.all { it is TimelineDisplayItem.EmptyState })
        }

        // Available days
        viewModel.availableDays.observe(this) { days ->
            binding.dayFilterView.setDays(days)
        }

        // Selected day
        viewModel.selectedDayIndex.observe(this) { index ->
            binding.dayFilterView.setSelectedDay(index)
        }

        // Map mode
        viewModel.isMapMode.observe(this) { isMapMode ->
            updateMapMode(isMapMode)
        }

        // Loading
        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) showLoading() else hideLoading()
        }

        // Error
        viewModel.error.observe(this) { error ->
            error?.let {
                showAlert(AlertType.ERROR, it)
            }
        }

        // Add plan sheet
        viewModel.showAddPlanSheet.observe(this) { show ->
            if (show == true) {
                showAddPlanSheet()
            }
        }

        // Map steps for map mode
        viewModel.mapSteps.observe(this) { mapSteps ->
            if (viewModel.isMapMode.value == true) {
                binding.mapView.clearMap()
                binding.mapView.showMapIcons(mapSteps)
                lifecycleScope.launch {
                    binding.mapView.moveCameraTo()
                }
            }
        }

        // Map bottom items for horizontal list
        viewModel.mapBottomItems.observe(this) { items ->
            mapBottomListAdapter?.submitList(items)
        }

        // Launch POI selection
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

        // Near Me visibility
        viewModel.showNearMeButton.observe(this) { show ->
            binding.btnNearMe.visibility = if (show) View.VISIBLE else View.GONE
        }

        // Saved Plans visibility and badge
        viewModel.savedPlansCount.observe(this) { count ->
            if (count > 0) {
                binding.btnSavedPlans.visibility = View.VISIBLE
                binding.tvSavedPlansBadge.text = count.toString()
            } else {
                binding.btnSavedPlans.visibility = View.GONE
            }
        }

        // Change time picker for step
        viewModel.showChangeTimePickerStep.observe(this) { step ->
            step?.let {
                showChangeTimePicker(it)
                viewModel.clearChangeTimePickerStep()
            }
        }

        // Route info updated - refresh the adapter when route calculations complete
        viewModel.routeInfoUpdated.observe(this) { segmentIndex ->
            segmentIndex?.let {
                // Re-submit the display items to refresh route info in UI
                viewModel.displayItems.value?.let { items ->
                    timelineAdapter.submitList(null) // Clear first
                    timelineAdapter.submitList(items) // Re-submit to trigger rebind
                }
                viewModel.clearRouteInfoUpdate()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupRecyclerView()
        setupMapBottomList()
        setupUI()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        viewModel.onSDKDismissed()
        super.onBackPressed()
    }

    // =====================
    // SETUP
    // =====================

    private fun setupUI() {
        // Title
        binding.tvTitle.text = getLanguageForKey(LanguageConst.ITINERARY)

        // Saved plans card text
        binding.tvSavedPlansText.text = getLanguageForKey(LanguageConst.ADD_PLAN_EMPTY_SAVED)

        // Near Me button
        binding.btnNearMe.text = getLanguageForKey(LanguageConst.ADD_PLAN_NEAR_ME)

        // Empty state texts
        binding.tvEmptyTitle.text = getLanguageForKey(LanguageConst.NO_PLANS_YET)
        binding.tvEmptySubtitle.text = getLanguageForKey(LanguageConst.NO_PLANS_DESCRIPTION)
    }

    private fun setupRecyclerView() {
        // Get distance format from language service with fallback

        timelineAdapter = TimelineAdapter(
            onItemClick = { item ->
                handleItemClick(item)
            },
            onDeleteClick = { item, segmentIndex ->
                handleDeleteClick(item, segmentIndex)
            },
            onExpandClick = { item ->
                handleExpandClick(item)
            },
            onStepClick = { step ->
                // When a step (POI within recommendations) is clicked
                // Check stepType: "poi" -> open POI Detail, otherwise notify host app
                if (step.stepType == "poi") {
                    // Open POI Detail screen for POI steps
                    step.poi?.let { poi ->
                        startActivity(ACPOIDetail.launch(this, poi))
                    }
                } else {
                    // Activity steps - notify host app
                    val activityId = step.poi?.additionalData?.productId
                        ?: step.poi?.id
                    activityId?.let { viewModel.onActivityDetailRequested(it) }
                }
            },
            onChangeTimeClick = { manualPoi ->
                // Handle change time for ManualPoi
                handleManualPoiChangeTimeClick(manualPoi)
            },
            onAddPlanClick = {
                // When Add Plans button is clicked from empty state
                showAddPlanSheet()
            },
            // Step callbacks for Recommendations
            onStepChangeTimeClick = { step ->
                // Handle change time for POI step
                handleStepChangeTimeClick(step)
            },
            onStepDeleteClick = { step ->
                // Handle delete for step
                handleStepDeleteClick(step)
            },
            onStepReservationClick = { step ->
                // Handle reservation for activity step
                step.poi?.additionalData?.productId?.let { productId ->
                    viewModel.onActivityReservationRequested(productId)
                } ?: step.poi?.id?.let { poiId ->
                    viewModel.onActivityReservationRequested(poiId)
                }
            },
            // Route calculation callback for Recommendations
            onRequestRouteCalculation = { recommendations ->
                viewModel.calculateRoutesForRecommendations(recommendations)
            }
        )

        binding.rvTimeline.apply {
            layoutManager = LinearLayoutManager(this@ACTimeline)
            adapter = timelineAdapter
            setHasFixedSize(false)
        }
    }

    /**
     * Setup horizontal list at bottom of map.
     * Shows timeline items as cards when annotation is clicked.
     */
    private fun setupMapBottomList() {
        mapBottomListAdapter = MapBottomListAdapter { item ->
            // Handle item click - navigate to detail
            when {
                // Booked or reserved activities
                item.type == "booked" || item.type == "reserved" -> {
                    viewModel.onActivityDetailRequested(item.id)
                }
                // Step with activity type - treat like reserved
                item.type == "step" && item.stepType == "activity" -> {
                    // Get productId from POI additionalData
                    findPoiById(item.id)?.let { poi ->
                        val activityId = poi.additionalData?.productId ?: poi.id ?: item.id
                        viewModel.onActivityDetailRequested(activityId)
                    }
                }
                // POI types (step with poi stepType, manual)
                else -> {
                    findPoiById(item.id)?.let { poi ->
                        startActivity(ACPOIDetail.launch(this, poi))
                    }
                }
            }
        }

        binding.rvMapBottomList.apply {
            layoutManager = LinearLayoutManager(
                this@ACTimeline,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = mapBottomListAdapter
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
//        binding.emptyStateView.visibility = if (hasData) View.GONE else View.VISIBLE
    }

    private fun updateEmptyState(isEmpty: Boolean) {
//        binding.emptyStateView.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun updateMapMode(isMapMode: Boolean) {
        binding.swipeRefresh.visibility = if (isMapMode) View.GONE else View.VISIBLE
        binding.mapContainer.visibility = if (isMapMode) View.VISIBLE else View.GONE

        // Hide savedPlans in map mode
        if (isMapMode) {
            binding.btnSavedPlans.visibility = View.GONE
        } else {
            // Restore savedPlans visibility based on count
            val count = viewModel.savedPlansCount.value ?: 0
            binding.btnSavedPlans.visibility = if (count > 0) View.VISIBLE else View.GONE
        }

        // Header and DayFilter elevation + background for map mode
        val elevationDp = 8f * resources.displayMetrics.density
        if (isMapMode) {
            // Transparent background and elevation for map overlay
            binding.headerContainer.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            binding.headerContainer.elevation = elevationDp
            binding.dayFilterView.elevation = elevationDp
        } else {
            // White background and no elevation for list mode
            binding.headerContainer.setBackgroundColor(android.graphics.Color.WHITE)
            binding.headerContainer.elevation = 0f
            binding.dayFilterView.elevation = 0f
        }

        // FAB visibility
        if (isMapMode) {
            // Map view: only List FAB visible
            binding.fabMap.visibility = View.GONE
            binding.fabAddPlan.visibility = View.GONE
            binding.fabList.visibility = View.VISIBLE
        } else {
            // List view: Map + AddPlan FABs visible
            binding.fabMap.visibility = View.VISIBLE
            binding.fabAddPlan.visibility = View.VISIBLE
            binding.fabList.visibility = View.GONE
        }

        // Show map markers when switching to map mode
        if (isMapMode) {
            val mapSteps = viewModel.mapSteps.value
            if (!mapSteps.isNullOrEmpty()) {
                binding.mapView.clearMap()
                binding.mapView.showMapIcons(mapSteps)
                lifecycleScope.launch {
                    binding.mapView.moveCameraTo()
                }
            }
        } else {
            binding.mapView.clearMap()
            // Hide bottom list when exiting map mode
            if (isBottomListVisible) {
                isBottomListVisible = false
                binding.rvMapBottomList.visibility = View.GONE
                binding.rvMapBottomList.translationY = 200f * resources.displayMetrics.density
                // Reset fabList position
                binding.fabList.translationY = 0f
            }
        }
    }

    private fun handleMapItemClick(mapStep: MapStep) {
        // Show bottom list and scroll to clicked item
        showMapBottomList()
        scrollToMapBottomItem(mapStep.position)
    }

    /**
     * Show the horizontal item list at the bottom of the map with slide-up animation.
     * Also moves the fabList button above the list.
     */
    private fun showMapBottomList() {
        if (isBottomListVisible) return
        isBottomListVisible = true

        binding.rvMapBottomList.visibility = View.VISIBLE
        binding.rvMapBottomList.animate()
            .translationY(0f)
            .setDuration(300)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()

        // Move fabList above the bottom list (list height ~104dp + 16dp spacing)
        val fabOffset = -120f * resources.displayMetrics.density
        binding.fabList.animate()
            .translationY(fabOffset)
            .setDuration(300)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }

    /**
     * Hide the horizontal item list with slide-down animation.
     * Also moves the fabList button back to its original position.
     */
    private fun hideMapBottomList() {
        if (!isBottomListVisible) return
        isBottomListVisible = false

        val translationY = 200f * resources.displayMetrics.density
        binding.rvMapBottomList.animate()
            .translationY(translationY)
            .setDuration(300)
            .setInterpolator(android.view.animation.AccelerateInterpolator())
            .withEndAction {
                binding.rvMapBottomList.visibility = View.GONE
            }
            .start()

        // Move fabList back to original position
        binding.fabList.animate()
            .translationY(0f)
            .setDuration(300)
            .setInterpolator(android.view.animation.AccelerateInterpolator())
            .start()
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
                // Notify host app for activity detail
                item.segment.additionalData?.activityId?.let { activityId ->
                    viewModel.onActivityDetailRequested(activityId)
                }
            }
            is TimelineDisplayItem.Recommendations -> {
                // Show recommendations details or expand
            }
            is TimelineDisplayItem.ManualPoi -> {
                // Open POI Detail screen for ManualPoi
                item.step.poi?.let { poi ->
                    startActivity(ACPOIDetail.launch(this, poi))
                }
            }
            else -> {}
        }
    }

    /**
     * Called when user taps "Reserve" or "Book" button.
     * Forwards reservation request to host app.
     */
    fun handleReservationClick(activityId: String) {
        viewModel.onActivityReservationRequested(activityId)
    }

    private fun handleDeleteClick(item: TimelineDisplayItem, segmentIndex: Int?) {
        segmentIndex?.let { index ->
            // Determine title and message based on item type
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

            // Show confirmation dialog before deleting segment
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
            // Toggle expansion
            val currentItems = timelineAdapter.currentList.toMutableList()
            val position = currentItems.indexOf(item)
            if (position != -1) {
                currentItems[position] = item.copy(isExpanded = !item.isExpanded)
                timelineAdapter.submitList(currentItems)
            }
        }
    }

    private fun handleStepChangeTimeClick(step: com.tripian.one.api.timeline.model.TimelineStep) {
        // Show change time picker for the step
        viewModel.showStepChangeTimePicker(step)
    }

    private fun handleManualPoiChangeTimeClick(manualPoi: TimelineDisplayItem.ManualPoi) {
        // Show change time picker for the ManualPoi step
        viewModel.showStepChangeTimePicker(manualPoi.step)
    }

    private fun handleStepDeleteClick(step: com.tripian.one.api.timeline.model.TimelineStep) {
        // Show confirmation dialog before deleting step
        showDeleteConfirmationDialog(
            title = getLanguageForKey(LanguageConst.REMOVE_ACTIVITY),
            message = getLanguageForKey(LanguageConst.REMOVE_ACTIVITY_MESSAGE),
            onConfirm = {
                viewModel.deleteStep(step)
            }
        )
    }

    private fun showChangeTimePicker(step: com.tripian.one.api.timeline.model.TimelineStep) {
        // Parse start and end times from step
        val startTime = step.startDateTimes?.let { dateTime ->
            // Extract HH:mm from datetime string
            if (dateTime.length >= 16) dateTime.substring(11, 16) else null
        }

        val endTime = step.endDateTimes?.let { dateTime ->
            if (dateTime.length >= 16) dateTime.substring(11, 16) else null
        }

        // Get estimated duration from POI
        val estimatedDuration = step.poi?.duration ?: 60

        val timePickerSheet = TimePickerBottomSheet.newInstance(
            startTime = startTime,
            endTime = endTime,
            durationMinutes = estimatedDuration
        )

        timePickerSheet.setOnTimeSelectedListener { newStartTime, newEndTime ->
            // Update step time via ViewModel
            viewModel.updateStepTime(step.id, newStartTime, newEndTime)
        }

        timePickerSheet.show(supportFragmentManager, TimePickerBottomSheet.TAG)
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

        val intent = ACSavedPlans.launch(
            context = this,
            favorites = filteredFavorites,
            tripHash = viewModel.tripHash,
            availableDays = availableDays
        )
        savedPlansLauncher.launch(intent)
    }

    // =====================
    // ADD PLAN
    // =====================

    private fun showAddPlanSheet() {
        addPlanSheet = AddPlanContainerBottomSheet.newInstance(
            availableDays = viewModel.availableDays.value ?: emptyList(),
            cities = viewModel.cities.value ?: emptyList(),
            selectedDayIndex = viewModel.selectedDayIndex.value ?: 0,
            selectedCity = viewModel.getSelectedCity(),
            tripHash = viewModel.tripHash,
            bookedActivities = viewModel.getBookedActivities()
        )

        addPlanSheet?.setOnAddPlanCompleteListener { data ->
            if (data.mode == AddPlanMode.MANUAL && data.selectedPoi == null) {
                // Manual mode: need to select POI first
                pendingAddPlanData = data
                val city = data.selectedCity
                if (city != null) {
                    val intent = ACPOISelection.launch(this, city)
                    poiSelectionLauncher.launch(intent)
                }
            } else {
                // Smart mode or already has POI - dismiss sheet and process after animation
                addPlanSheet?.dismiss()
                // Post to next main loop to allow dismiss animation to complete
                binding.root.postDelayed({
                    viewModel.onAddPlanComplete(data)
                }, 300) // Wait for dismiss animation
            }
        }

        addPlanSheet?.setOnSegmentCreatedListener {
            // Segment was created from manual listing (ACActivityListing), re-fetch timeline
            viewModel.refreshTimeline()
        }

        addPlanSheet?.show(supportFragmentManager, AddPlanContainerBottomSheet.TAG)
    }

    // =====================
    // COMPANION
    // =====================

    companion object {
        private const val EXTRA_TRIP_HASH = "tripHash"

        fun newIntent(context: Context, tripHash: String): Intent {
            return Intent(context, ACTimeline::class.java).apply {
                putExtra(EXTRA_TRIP_HASH, tripHash)
            }
        }
    }
}
