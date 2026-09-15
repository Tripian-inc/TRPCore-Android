package com.tripian.trpcore.ui.timeline.compose.activity

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.os.BundleCompat
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.tripian.one.api.tour.model.TourProduct
import com.tripian.trpcore.R
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.databinding.AcActivityListingBinding
import com.tripian.trpcore.ui.timeline.activity.ACActivityListing
import com.tripian.trpcore.ui.timeline.activity.ACActivityListingVM
import com.tripian.trpcore.ui.timeline.activity.ActivityFilterData
import com.tripian.trpcore.ui.timeline.activity.AdapterActivityCategory
import com.tripian.trpcore.ui.timeline.activity.AdapterActivityListing
import com.tripian.trpcore.ui.timeline.compose.core.BindingHost
import com.tripian.trpcore.ui.timeline.compose.core.TimelineComposeScreen
import com.tripian.trpcore.ui.timeline.compose.core.timelineViewModel
import com.tripian.trpcore.ui.timeline.compose.nav.ActivityListingArgs
import com.tripian.trpcore.ui.timeline.compose.nav.LocalTimelineNavigator
import com.tripian.trpcore.ui.timeline.compose.nav.TimelineNavigator
import com.tripian.trpcore.ui.timeline.compose.nav.TimelineResults
import com.tripian.trpcore.ui.timeline.compose.sheets.ActivityFilterSheet
import com.tripian.trpcore.ui.timeline.compose.sheets.ActivitySortSheet
import com.tripian.trpcore.ui.timeline.compose.sheets.ActivityTimeSelectionMode
import com.tripian.trpcore.ui.timeline.compose.sheets.ActivityTimeSelectionRequest
import com.tripian.trpcore.ui.timeline.compose.sheets.ActivityTimeSelectionSheet
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.applyBottomSystemBarInsetPadding
import com.tripian.trpcore.util.extensions.dp
import com.tripian.trpcore.util.widget.SearchBarView
import java.text.SimpleDateFormat
import java.util.Locale

private const val PAGINATION_PREFETCH_THRESHOLD = 5
private const val STATE_LIST = "state_list"

/**
 * Compose route of [ACActivityListing]: lists bookable tours for a manual plan
 * and reports every added segment to the Timeline through
 * [TimelineResults.SEGMENT_CREATED_DAY_INDEX] without leaving the list.
 */
@Composable
internal fun ActivityListingScreen(
    args: ActivityListingArgs,
    viewModel: ACActivityListingVM = timelineViewModel()
) {
    val navigator = LocalTimelineNavigator.current
    val context = LocalContext.current
    val sheets = remember { ActivityListingSheetState() }

    LaunchedEffect(Unit) {
        if (viewModel.arguments == null) {
            viewModel.arguments = ACActivityListing.launch(
                context = context,
                planData = args.planData,
                tripHash = args.tripHash,
                plannedActivityIdsByDay = args.plannedActivityIdsByDay,
                tripWideExcludedActivityIds = args.tripWideExcludedActivityIds
            ).extras
            viewModel.onViewCreated(null)
            viewModel.initialize(
                planData = args.planData,
                tripHash = args.tripHash,
                plannedActivityIdsByDay = args.plannedActivityIdsByDay,
                tripWideExcludedActivityIds = args.tripWideExcludedActivityIds
            )
        }
    }

    TimelineComposeScreen(viewModel = viewModel, onExit = { navigator.back() }) {
        BindingHost(
            inflate = AcActivityListingBinding::inflate,
            onSaveViewState = { binding, state ->
                binding.rvActivities.layoutManager?.onSaveInstanceState()
                    ?.let { state.putParcelable(STATE_LIST, it) }
                binding.skeletonList.root.stopShimmer()
                binding.shimmerResultCount.stopShimmer()
            }
        ) { binding, owner, viewState ->
            val ui = ActivityListingUi(binding, viewModel, navigator, sheets)
            ui.setupViews()
            ui.observe(owner, viewState)
        }
        ActivityListingSheets(sheets, viewModel)
    }
}

/** Which of the listing's sheets is open, kept as Compose state so the sheets survive recomposition. */
private class ActivityListingSheetState {
    var timeRequest by mutableStateOf<ActivityTimeSelectionRequest?>(null)
    var timeLoadingText by mutableStateOf<String?>(null)
    var filterOpen by mutableStateOf(false)
    var sortOpen by mutableStateOf(false)
}

@Composable
private fun ActivityListingSheets(sheets: ActivityListingSheetState, viewModel: ACActivityListingVM) {
    sheets.timeRequest?.let { request ->
        ActivityTimeSelectionSheet(
            request = request,
            inSheetLoadingText = sheets.timeLoadingText,
            onDismiss = {
                sheets.timeRequest = null
                sheets.timeLoadingText = null
            },
            onTimeSelected = { selection ->
                val tour = (request.mode as? ActivityTimeSelectionMode.Tour)?.activity
                    ?: return@ActivityTimeSelectionSheet
                sheets.timeLoadingText =
                    viewModel.getLanguageForKey(LanguageConst.LOADING_TEXT_ADDING_TO_ITINERARY)
                viewModel.createReservedActivitySegment(
                    tour,
                    selection.selectedDate,
                    selection.startTime,
                    selection.slotPrice,
                    selection.isFlexible
                )
            }
        )
    }
    if (sheets.filterOpen) {
        val priceFacet = viewModel.priceRangeFacet.value
        val durationFacet = viewModel.durationRangeFacet.value
        ActivityFilterSheet(
            currentFilter = viewModel.getCurrentFilter(),
            currency = viewModel.getCurrency(),
            minPriceBound = priceFacet?.minimum?.amount?.let { it / 100f },
            maxPriceBound = priceFacet?.maximum?.amount?.let { it / 100f },
            minDurationBound = durationFacet?.minimumMinutes?.toFloat(),
            maxDurationBound = durationFacet?.maximumMinutes?.toFloat(),
            getLanguage = { key -> viewModel.getLanguageForKey(key) },
            onFilterConfirmed = { filter -> viewModel.applyFilter(filter) },
            onDismiss = { sheets.filterOpen = false }
        )
    }
    if (sheets.sortOpen) {
        ActivitySortSheet(
            currentSort = viewModel.getCurrentSort(),
            getLanguage = { key -> viewModel.getLanguageForKey(key) },
            onSortSelected = { sort -> viewModel.applySort(sort) },
            onDismiss = { sheets.sortOpen = false }
        )
    }
}

/** Binds one inflated listing layout to the ViewModel, mirroring the Activity's setup. */
private class ActivityListingUi(
    private val binding: AcActivityListingBinding,
    private val viewModel: ACActivityListingVM,
    private val navigator: TimelineNavigator,
    private val sheets: ActivityListingSheetState
) {
    private val activityAdapter = AdapterActivityListing(
        getLanguage = { key -> viewModel.getLanguageForKey(key) },
        onAddClicked = { activity -> viewModel.onActivityAddClicked(activity) },
        onItemClicked = { activity ->
            val activityId = activity.productId ?: activity.id ?: return@AdapterActivityListing
            TRPCore.notifyActivityDetailRequested(activityId)
        }
    )
    private var categoryAdapter: AdapterActivityCategory? = null
    private var isSkeletonVisible = false
    private var pendingScrollToTop = false

    fun setupViews() {
        setupRecyclerViews()
        setupSearchBar()
        setupClickListeners()
        setupCollapseGap()
        binding.pbSearchProgress.visibility = View.GONE
        binding.tvTitle.text = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_CAT_MANUAL_ACTIVITIES)
        updateFilterButton(viewModel.getCurrentFilter())
        binding.btnSortBy.text = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_SORT_BY)
        binding.tvEmpty.text = viewModel.getLanguageForKey(LanguageConst.ACTIVITY_LISTING_NO_ACTIVITIES)
    }

    fun observe(owner: LifecycleOwner, viewState: Bundle) {
        viewModel.activities.observe(owner) { activities ->
            activityAdapter.submitList(activities) {
                restoreListState(viewState)
                if (pendingScrollToTop) {
                    pendingScrollToTop = false
                    binding.rvActivities.scrollToPosition(0)
                    binding.appBarLayout.setExpanded(true, false)
                }
            }
            updateEmptyState(activities.isEmpty())
            hideSkeleton()
        }
        viewModel.isLoading.observe(owner) { isLoading ->
            if (isLoading) {
                val useSkeleton = viewModel.consumeSkeletonRequest()
                val loaderSuppressed = viewModel.consumeLoaderSuppression()
                when {
                    useSkeleton -> showSkeleton()
                    loaderSuppressed -> Unit
                    else -> viewModel.showFullScreenLoader(LanguageConst.LOADING_TEXT_GETTING_ACTIVITIES, "")
                }
            } else {
                viewModel.hideLottieLoading()
                hideSkeleton()
            }
        }
        viewModel.loadingMore.observe(owner) { loadingMore ->
            binding.loadMoreIndicator.root.visibility = if (loadingMore) View.VISIBLE else View.GONE
        }
        viewModel.activityCount.observe(owner) { count ->
            binding.tvResultCount.text =
                "$count ${viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_ACTIVITIES)}"
        }
        viewModel.selectedCategoryIndices.observe(owner) { selectedIndices ->
            categoryAdapter?.setSelectedIndices(selectedIndices)
        }
        viewModel.facetCategories.observe(owner) { rebuildCategoryAdapter() }
        viewModel.showTimeSelection.observe(owner) { activity ->
            activity?.let { showTimeSelectionBottomSheet(it) }
        }
        viewModel.addedToItinerarySuccess.observe(owner) { result ->
            result?.let { handleAddedToItinerarySuccess(it) }
        }
        viewModel.addSegmentError.observe(owner) { message ->
            message?.let {
                viewModel.clearAddSegmentError()
                sheets.timeLoadingText = null
                viewModel.showAlert(AlertType.ERROR, it)
            }
        }
        viewModel.currentFilter.observe(owner) { filter -> updateFilterButton(filter) }
        viewModel.scrollToTop.observe(owner) { shouldScroll ->
            if (shouldScroll) pendingScrollToTop = true
        }
    }

    private fun restoreListState(viewState: Bundle) {
        val state = BundleCompat.getParcelable(viewState, STATE_LIST, Parcelable::class.java) ?: return
        viewState.remove(STATE_LIST)
        binding.rvActivities.layoutManager?.onRestoreInstanceState(state)
    }

    private fun showSkeleton() {
        with(binding.skeletonList.root) {
            visibility = View.VISIBLE
            startShimmer()
        }
        binding.tvResultCount.visibility = View.INVISIBLE
        with(binding.shimmerResultCount) {
            visibility = View.VISIBLE
            startShimmer()
        }
        isSkeletonVisible = true
    }

    private fun hideSkeleton() {
        if (!isSkeletonVisible) return
        with(binding.skeletonList.root) {
            stopShimmer()
            visibility = View.GONE
        }
        with(binding.shimmerResultCount) {
            stopShimmer()
            visibility = View.GONE
        }
        binding.tvResultCount.visibility = View.VISIBLE
        isSkeletonVisible = false
    }

    private fun updateFilterButton(filter: ActivityFilterData) {
        val filterCount = filter.activeFilterCount()
        val baseText = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_FILTERS)
        binding.btnFilters.text = if (filterCount > 0) "$baseText ($filterCount)" else baseText
        binding.btnFilters.setIconResource(
            if (filterCount > 0) R.drawable.trp_ic_filter_activity_badge else R.drawable.trp_ic_filter_activity
        )
    }

    private fun setupRecyclerViews() {
        binding.rvActivities.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = activityAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy <= 0) return
                    val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                    if (layoutManager.findLastVisibleItemPosition() >=
                        layoutManager.itemCount - PAGINATION_PREFETCH_THRESHOLD
                    ) {
                        viewModel.loadMoreActivities()
                    }
                }
            })
            addItemDecoration(ActivitySeparatorDecoration(context))
        }
        binding.rvActivities.applyBottomSystemBarInsetPadding()
        rebuildCategoryAdapter()
    }

    private fun rebuildCategoryAdapter() {
        val adapter = AdapterActivityCategory(
            categories = viewModel.getFacetCategoryItems(),
            getLanguage = { key -> viewModel.getLanguageForKey(key) },
            onSelectionChanged = { selectedIndices -> viewModel.onCategorySelectionChanged(selectedIndices) }
        )
        categoryAdapter = adapter
        binding.rvCategories.apply {
            if (layoutManager == null) {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            }
            this.adapter = adapter
        }
        adapter.setSelectedIndices(setOf(0))
    }

    private fun setupCollapseGap() {
        val gapPx = 8.dp
        binding.appBarLayout.addOnOffsetChangedListener(
            AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
                val target = minOf(gapPx, -verticalOffset).coerceAtLeast(0)
                val params = binding.searchContainer.layoutParams as? ViewGroup.MarginLayoutParams
                    ?: return@OnOffsetChangedListener
                if (params.bottomMargin != target) {
                    params.bottomMargin = target
                    binding.searchContainer.layoutParams = params
                }
            }
        )
    }

    private fun setupSearchBar() {
        binding.searchBar.setHint(viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_SEARCH_ACTIVITY))
        binding.searchBar.setOnQueryChangedListener(SearchBarView.Mode.REMOTE) { query ->
            viewModel.search(query)
        }
    }

    private fun setupClickListeners() {
        binding.imBack.setOnClickListener { navigator.back() }
        binding.btnFilters.setOnClickListener { sheets.filterOpen = true }
        binding.btnSortBy.setOnClickListener { sheets.sortOpen = true }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        val isLoading = viewModel.isLoading.value == true
        binding.tvEmpty.visibility = if (isEmpty && !isLoading) View.VISIBLE else View.GONE
        binding.rvActivities.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun showTimeSelectionBottomSheet(activity: TourProduct) {
        sheets.timeLoadingText = null
        sheets.timeRequest = ActivityTimeSelectionRequest(
            mode = ActivityTimeSelectionMode.Tour(
                activity = activity,
                cityId = viewModel.getCityId(),
                plannedActivityIdsByDay = viewModel.plannedActivityIdsByDay()
            ),
            availableDays = viewModel.getAvailableDays(),
            initialSelectedDay = viewModel.getSelectedDate()
        )
    }

    private fun handleAddedToItinerarySuccess(result: ACActivityListingVM.AddedToItineraryResult) {
        viewModel.clearAddedToItinerarySuccess()
        sheets.timeRequest = null
        sheets.timeLoadingText = null

        val dayLabel = SimpleDateFormat("EEEE dd/MM", Locale.getDefault()).format(result.selectedDate)
        val message = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_TOAST_ACTIVITY_ADDED)
            .replace("%1\$@", result.activityName)
            .replace("%2\$@", dayLabel)
        viewModel.showAlert(AlertType.SUCCESS, message)

        navigator.setResult(TimelineResults.SEGMENT_CREATED_DAY_INDEX, viewModel.getSelectedDayIndex())
    }
}

/** Draws a hairline between activity rows, skipping the last one. */
private class ActivitySeparatorDecoration(context: Context) : RecyclerView.ItemDecoration() {
    private val paint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.trp_lineWeak)
        strokeWidth = context.resources.displayMetrics.density * 0.5f
    }
    private val horizontalPadding = (context.resources.displayMetrics.density * 16).toInt()

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val itemCount = parent.adapter?.itemCount ?: 0
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val position = parent.getChildAdapterPosition(child)
            if (position == RecyclerView.NO_POSITION || position >= itemCount - 1) continue
            val left = (parent.paddingLeft + horizontalPadding).toFloat()
            val right = (parent.width - parent.paddingRight - horizontalPadding).toFloat()
            val y = child.bottom.toFloat()
            c.drawLine(left, y, right, y, paint)
        }
    }
}
