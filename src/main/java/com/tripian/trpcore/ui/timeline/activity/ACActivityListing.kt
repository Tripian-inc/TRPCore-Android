package com.tripian.trpcore.ui.timeline.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.tripian.trpcore.util.extensions.applyBottomSystemBarInsetPadding
import com.tripian.trpcore.util.extensions.asIdsByDay
import com.tripian.trpcore.util.extensions.dp
import com.tripian.trpcore.util.extensions.toSerializableIdsByDay
import com.tripian.one.api.tour.model.TourProduct
import com.tripian.trpcore.R
import com.tripian.trpcore.base.BaseActivity
import com.tripian.trpcore.databinding.AcActivityListingBinding
import com.tripian.trpcore.domain.model.timeline.AddPlanData
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.widget.SearchBarView
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * ACActivityListing
 * Full screen Activity for listing tours/activities
 * iOS Reference: ActivityListingVC
 */
class ACActivityListing : BaseActivity<AcActivityListingBinding, ACActivityListingVM>() {

    private var activityAdapter: AdapterActivityListing? = null
    private var categoryAdapter: AdapterActivityCategory? = null
    private var timeSelectionBottomSheet: ActivityTimeSelectionBottomSheet? = null
    private var filterBottomSheet: ActivityFilterBottomSheet? = null
    private var sortBottomSheet: ActivitySortBottomSheet? = null
    /** True while the inline shimmer skeleton is visible; the initial load uses the full-screen Lottie instead. */
    private var isSkeletonVisible: Boolean = false
    private var pendingScrollToTop = false

    private var paginationScrollListener: RecyclerView.OnScrollListener? = null

    override fun onDestroy() {
        paginationScrollListener?.let { binding.rvActivities.removeOnScrollListener(it) }
        paginationScrollListener = null
        binding.skeletonList.root.stopShimmer()
        binding.shimmerResultCount.stopShimmer()
        super.onDestroy()
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

    override fun getViewBinding() = AcActivityListingBinding.inflate(layoutInflater)

    override fun setListeners() {
        setupRecyclerViews()
        setupSearchBar()
        setupClickListeners()
        setupCollapseGap()

        intent?.let { intent ->
            @Suppress("DEPRECATION")
            val planData = intent.getSerializableExtra(EXTRA_PLAN_DATA) as? AddPlanData
            val tripHash = intent.getStringExtra(EXTRA_TRIP_HASH) ?: ""
            val plannedActivityIdsByDay =
                intent.getSerializableExtra(EXTRA_PLANNED_ACTIVITY_IDS).asIdsByDay()
            val tripWideExcludedActivityIds =
                intent.getStringArrayListExtra(EXTRA_TRIP_WIDE_EXCLUDED_IDS).orEmpty()

            planData?.let {
                viewModel.initialize(
                    it,
                    tripHash,
                    plannedActivityIdsByDay,
                    tripWideExcludedActivityIds
                )
            }
        }
    }

    override fun setReceivers() {
        viewModel.activities.observe(this) { activities ->
            activityAdapter?.submitList(activities) {
                if (pendingScrollToTop) {
                    pendingScrollToTop = false
                    binding.rvActivities.scrollToPosition(0)
                    binding.appBarLayout.setExpanded(true, false)
                }
            }
            updateEmptyState(activities.isEmpty())
            hideSkeleton()
        }

        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                val useSkeleton = viewModel.consumeSkeletonRequest()
                val loaderSuppressed = viewModel.consumeLoaderSuppression()
                when {
                    useSkeleton -> showSkeleton()
                    loaderSuppressed -> Unit
                    else -> viewModel.showFullScreenLoader(
                        LanguageConst.LOADING_TEXT_GETTING_ACTIVITIES,
                        ""
                    )
                }
            } else {
                viewModel.hideLottieLoading()
                hideSkeleton()
            }
        }

        binding.pbSearchProgress.visibility = View.GONE

        viewModel.loadingMore.observe(this) { loadingMore ->
            binding.loadMoreIndicator.root.visibility = if (loadingMore) View.VISIBLE else View.GONE
        }

        viewModel.activityCount.observe(this) { count ->
            binding.tvResultCount.text = "$count ${viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_ACTIVITIES)}"
        }

        viewModel.selectedCategoryIndices.observe(this) { selectedIndices ->
            categoryAdapter?.setSelectedIndices(selectedIndices)
        }

        viewModel.facetCategories.observe(this) { _ ->
            rebuildCategoryAdapter()
        }

        viewModel.showTimeSelection.observe(this) { activity ->
            activity?.let { showTimeSelectionBottomSheet(it) }
        }

        viewModel.addedToItinerarySuccess.observe(this) { result ->
            result?.let { handleAddedToItinerarySuccess(it) }
        }

        viewModel.addSegmentError.observe(this) { message ->
            message?.let {
                viewModel.clearAddSegmentError()
                val sheet = timeSelectionBottomSheet
                if (sheet != null && sheet.isAdded) {
                    sheet.hideInSheetLoadingOverlay()
                    sheet.showError(it)
                } else {
                    showAlert(AlertType.ERROR, it)
                }
            }
        }

        viewModel.currentFilter.observe(this) { filter ->
            updateFilterButton(filter)
        }

        viewModel.scrollToTop.observe(this) { shouldScroll ->
            if (shouldScroll) {
                pendingScrollToTop = true
            }
        }

        binding.tvTitle.text = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_CAT_MANUAL_ACTIVITIES)

        updateFilterButton(viewModel.getCurrentFilter())
        binding.btnSortBy.text = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_SORT_BY)

        binding.tvEmpty.text = viewModel.getLanguageForKey(LanguageConst.ACTIVITY_LISTING_NO_ACTIVITIES)
    }

    /**
     * Update filter button text and icon based on active filters
     */
    private fun updateFilterButton(filter: ActivityFilterData) {
        val filterCount = filter.activeFilterCount()
        val baseText = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_FILTERS)

        binding.btnFilters.text = if (filterCount > 0) {
            "$baseText ($filterCount)"
        } else {
            baseText
        }

        val iconRes = if (filterCount > 0) {
            R.drawable.trp_ic_filter_activity_badge
        } else {
            R.drawable.trp_ic_filter_activity
        }
        binding.btnFilters.setIconResource(iconRes)
    }

    private fun setupRecyclerViews() {
        activityAdapter = AdapterActivityListing(
            getLanguage = { key -> viewModel.getLanguageForKey(key) },
            onAddClicked = { activity -> viewModel.onActivityAddClicked(activity) },
            onItemClicked = { activity ->
                val activityId = activity.productId ?: activity.id ?: return@AdapterActivityListing
                TRPCore.notifyActivityDetailRequested(activityId)
            }
        )
        binding.rvActivities.apply {
            layoutManager = LinearLayoutManager(this@ACActivityListing)
            adapter = activityAdapter

            paginationScrollListener = object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy <= 0) return
                    val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                    val totalItemCount = layoutManager.itemCount
                    val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                    if (lastVisibleItem >= totalItemCount - PAGINATION_PREFETCH_THRESHOLD) {
                        viewModel.loadMoreActivities()
                    }
                }
            }
            addOnScrollListener(paginationScrollListener!!)

            addItemDecoration(ActivitySeparatorDecoration(this@ACActivityListing))
        }
        binding.rvActivities.applyBottomSystemBarInsetPadding()

        rebuildCategoryAdapter()
    }

    private fun rebuildCategoryAdapter() {
        val items = viewModel.getFacetCategoryItems()
        categoryAdapter = AdapterActivityCategory(
            categories = items,
            getLanguage = { key -> viewModel.getLanguageForKey(key) },
            onSelectionChanged = { selectedIndices ->
                viewModel.onCategorySelectionChanged(selectedIndices)
            }
        )
        binding.rvCategories.apply {
            if (layoutManager == null) {
                layoutManager = LinearLayoutManager(
                    this@ACActivityListing,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )
            }
            adapter = categoryAdapter
        }
        categoryAdapter?.setSelectedIndices(setOf(0))
    }

    /**
     * Keeps an 8dp gap between the fixed search bar and the collapsing content
     * once the list has been scrolled up: the gap grows with the first pixels of
     * collapse and holds at 8dp, and disappears again at the fully expanded top.
     */
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
        binding.imBack.setOnClickListener {
            finish()
        }

        binding.btnFilters.setOnClickListener {
            showFilterBottomSheet()
        }

        binding.btnSortBy.setOnClickListener {
            showSortBottomSheet()
        }
    }

    /**
     * Shows the filter sheet with facet-driven slider bounds. Backend price ranges
     * arrive in minor units (cents) and are converted to whole currency units;
     * duration bounds are already in minutes.
     */
    private fun showFilterBottomSheet() {
        val priceFacet = viewModel.priceRangeFacet.value
        val durationFacet = viewModel.durationRangeFacet.value
        val minPriceBound = priceFacet?.minimum?.amount?.let { it / 100f }
        val maxPriceBound = priceFacet?.maximum?.amount?.let { it / 100f }
        val minDurationBound = durationFacet?.minimumMinutes?.toFloat()
        val maxDurationBound = durationFacet?.maximumMinutes?.toFloat()

        filterBottomSheet = ActivityFilterBottomSheet.newInstance(
            currentFilter = viewModel.getCurrentFilter(),
            currency = viewModel.getCurrency(),
            minPriceBound = minPriceBound,
            maxPriceBound = maxPriceBound,
            minDurationBound = minDurationBound,
            maxDurationBound = maxDurationBound
        )
        filterBottomSheet?.setLanguageProvider { key ->
            viewModel.getLanguageForKey(key)
        }
        filterBottomSheet?.setOnFilterConfirmedListener { filter ->
            viewModel.applyFilter(filter)
        }
        filterBottomSheet?.show(supportFragmentManager, ActivityFilterBottomSheet.TAG)
    }

    private fun showSortBottomSheet() {
        sortBottomSheet = ActivitySortBottomSheet.newInstance(
            currentSort = viewModel.getCurrentSort()
        )
        sortBottomSheet?.setLanguageProvider { key ->
            viewModel.getLanguageForKey(key)
        }
        sortBottomSheet?.setOnSortSelectedListener { sort ->
            viewModel.applySort(sort)
        }
        sortBottomSheet?.show(supportFragmentManager, ActivitySortBottomSheet.TAG)
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        val isLoading = viewModel.isLoading.value == true
        binding.tvEmpty.visibility = if (isEmpty && !isLoading) View.VISIBLE else View.GONE
        binding.rvActivities.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun showTimeSelectionBottomSheet(activity: TourProduct) {
        timeSelectionBottomSheet = ActivityTimeSelectionBottomSheet.newInstance(
            activity = activity,
            availableDays = viewModel.getAvailableDays(),
            initialSelectedDay = viewModel.getSelectedDate(),
            cityId = viewModel.getCityId(),
            plannedActivityIdsByDay = viewModel.plannedActivityIdsByDay()
        )
        timeSelectionBottomSheet?.setOnTimeSelectedListener { tour, selectedDate, timeSlot, slotPrice, isFlexible ->
            timeSelectionBottomSheet?.showInSheetLoadingOverlay(
                LanguageConst.LOADING_TEXT_ADDING_TO_ITINERARY, "Adding to itinerary"
            )
            viewModel.createReservedActivitySegment(tour, selectedDate, timeSlot, slotPrice, isFlexible)
        }
        timeSelectionBottomSheet?.show(supportFragmentManager, ActivityTimeSelectionBottomSheet.TAG)
    }

    /**
     * Final step of the add-activity flow: dismisses the time selection sheet,
     * shows a confirmation toast, and pre-arms RESULT_OK so back navigation
     * re-syncs the timeline UI. Stays on the listing for further additions.
     */
    private fun handleAddedToItinerarySuccess(result: ACActivityListingVM.AddedToItineraryResult) {
        viewModel.clearAddedToItinerarySuccess()
        timeSelectionBottomSheet?.dismiss()
        timeSelectionBottomSheet = null

        val dayLabel = SimpleDateFormat("EEEE dd/MM", Locale.getDefault())
            .format(result.selectedDate)

        val template = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_TOAST_ACTIVITY_ADDED)
            .ifBlank { "%1\$@ has been added to %2\$@" }
        val message = template
            .replace("%1\$@", result.activityName)
            .replace("%2\$@", dayLabel)

        showAlert(AlertType.SUCCESS, message)

        val resultIntent = Intent().apply {
            putExtra(RESULT_SELECTED_DAY_INDEX, viewModel.getSelectedDayIndex())
        }
        setResult(Activity.RESULT_OK, resultIntent)
    }

    /**
     * Custom ItemDecoration for drawing separators between activity items
     * Skips the last item (no separator after it)
     */
    private class ActivitySeparatorDecoration(context: Context) : RecyclerView.ItemDecoration() {
        private val paint = Paint().apply {
            color = ContextCompat.getColor(context, R.color.trp_lineWeak)
            strokeWidth = context.resources.displayMetrics.density * 0.5f
        }
        private val horizontalPadding = (context.resources.displayMetrics.density * 16).toInt()

        override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            val childCount = parent.childCount
            val itemCount = parent.adapter?.itemCount ?: 0

            for (i in 0 until childCount) {
                val child = parent.getChildAt(i)
                val position = parent.getChildAdapterPosition(child)

                if (position == RecyclerView.NO_POSITION || position >= itemCount - 1) {
                    continue
                }

                val left = parent.paddingLeft + horizontalPadding
                val right = parent.width - parent.paddingRight - horizontalPadding
                val y = child.bottom.toFloat()

                c.drawLine(left.toFloat(), y, right.toFloat(), y, paint)
            }
        }
    }

    companion object {
        private const val PAGINATION_PREFETCH_THRESHOLD = 5
        const val EXTRA_PLAN_DATA = "plan_data"
        const val EXTRA_TRIP_HASH = "trip_hash"
        const val EXTRA_PLANNED_ACTIVITY_IDS = "planned_activity_ids"
        const val EXTRA_TRIP_WIDE_EXCLUDED_IDS = "trip_wide_excluded_ids"
        const val RESULT_SELECTED_DAY_INDEX = "result_selected_day_index"

        /**
         * @param plannedActivityIdsByDay "yyyy-MM-dd" → activity ids that day already
         *   holds; days already holding the picked activity are unselectable in the
         *   time selection sheet and the chosen day's ids ship as `excludedActivityIds`.
         */
        fun launch(
            context: Context,
            planData: AddPlanData,
            tripHash: String,
            plannedActivityIdsByDay: Map<String, List<String>> = emptyMap(),
            tripWideExcludedActivityIds: List<String> = emptyList()
        ): Intent {
            return Intent(context, ACActivityListing::class.java).apply {
                putExtra(EXTRA_PLAN_DATA, planData)
                putExtra(EXTRA_TRIP_HASH, tripHash)
                putExtra(
                    EXTRA_PLANNED_ACTIVITY_IDS,
                    plannedActivityIdsByDay.toSerializableIdsByDay()
                )
                putStringArrayListExtra(
                    EXTRA_TRIP_WIDE_EXCLUDED_IDS,
                    ArrayList(tripWideExcludedActivityIds)
                )
            }
        }
    }
}
