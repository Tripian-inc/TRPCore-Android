package com.tripian.trpcore.ui.timeline.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.util.extensions.applyBottomSystemBarInsetPadding
import com.tripian.one.api.tour.model.TourProduct
import com.tripian.trpcore.R
import com.tripian.trpcore.base.BaseActivity
import com.tripian.trpcore.databinding.AcActivityListingBinding
import com.tripian.trpcore.domain.model.timeline.AddPlanData
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.widget.BottomToast
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
    // True while the inline shimmer skeleton is visible (filter/sort/category/
    // search reloads). The initial load uses the full-screen Lottie instead.
    private var isSkeletonVisible: Boolean = false

    override fun onDestroy() {
        binding.skeletonList.root.stopShimmer()
        super.onDestroy()
    }

    private fun showSkeleton() {
        with(binding.skeletonList.root) {
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
        isSkeletonVisible = false
    }

    override fun getViewBinding() = AcActivityListingBinding.inflate(layoutInflater)

    override fun setListeners() {
        setupRecyclerViews()
        setupSearchBar()
        setupClickListeners()

        // Initialize from intent
        intent?.let { intent ->
            @Suppress("DEPRECATION")
            val planData = intent.getSerializableExtra(EXTRA_PLAN_DATA) as? AddPlanData
            val tripHash = intent.getStringExtra(EXTRA_TRIP_HASH) ?: ""

            planData?.let {
                viewModel.initialize(it, tripHash)
            }
        }
    }

    override fun setReceivers() {
        // Observe activities
        viewModel.activities.observe(this) { activities ->
            activityAdapter?.submitList(activities)
            updateEmptyState(activities.isEmpty())
            hideSkeleton()
        }

        // Observe loading state. Default: full-screen Lottie with the
        // "getting activities" text. Filter/sort reloads switch to an inline
        // shimmer skeleton; category reloads switch to a bottom-sheet Lottie.
        // Those alternative paths set flags on the VM that we consume here.
        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                // Consume both flags up-front — filter/sort sets both so the
                // skeleton branch wins, but the suppression flag must still be
                // cleared so the next non-skeleton reload doesn't accidentally
                // skip its Lottie.
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

        // The in-flight search spinner became obsolete once filtering moved
        // fully client-side — keep the view hidden permanently.
        binding.pbSearchProgress.visibility = View.GONE

        // Observe activity count
        viewModel.activityCount.observe(this) { count ->
            binding.tvResultCount.text = "$count ${viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_ACTIVITIES)}"
        }

        // Observe selected category indices (multiple selection)
        viewModel.selectedCategoryIndices.observe(this) { selectedIndices ->
            categoryAdapter?.setSelectedIndices(selectedIndices)
        }

        // Rebuild the category chip strip when facets arrive (or change between searches).
        viewModel.facetCategories.observe(this) { _ ->
            rebuildCategoryAdapter()
        }

        // Observe time selection trigger
        viewModel.showTimeSelection.observe(this) { activity ->
            activity?.let { showTimeSelectionBottomSheet(it) }
        }

        // Add-to-itinerary completion: VM keeps the time-selection sheet open while it
        // shows its own bottom-sheet "Adding to itinerary" loader, runs the segment
        // create → timeline fetch chain, then signals success here. The sheet then
        // dismisses, a confirmation toast appears, and we finish with RESULT_OK after
        // the toast has had time to play.
        viewModel.addedToItinerarySuccess.observe(this) { result ->
            result?.let { handleAddedToItinerarySuccess(it) }
        }

        // Observe filter state changes
        viewModel.currentFilter.observe(this) { filter ->
            updateFilterButton(filter)
        }

        // Observe scroll to top event
        viewModel.scrollToTop.observe(this) { shouldScroll ->
            if (shouldScroll) {
                binding.rvActivities.scrollToPosition(0)
            }
        }

        // Set title
        binding.tvTitle.text = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_CAT_MANUAL_ACTIVITIES)

        // Set button texts from language service
        updateFilterButton(viewModel.getCurrentFilter())
        binding.btnSortBy.text = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_SORT_BY)

        // Set empty state text from language service
        binding.tvEmpty.text = viewModel.getLanguageForKey(LanguageConst.ACTIVITY_LISTING_NO_ACTIVITIES)
    }

    /**
     * Update filter button text and icon based on active filters
     */
    private fun updateFilterButton(filter: ActivityFilterData) {
        val filterCount = filter.activeFilterCount()
        val baseText = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_FILTERS)

        // Update button text
        binding.btnFilters.text = if (filterCount > 0) {
            "$baseText ($filterCount)"
        } else {
            baseText
        }

        // Update icon - use badge version when filters are active
        val iconRes = if (filterCount > 0) {
            R.drawable.trp_ic_filter_activity_badge
        } else {
            R.drawable.trp_ic_filter_activity
        }
        binding.btnFilters.setIconResource(iconRes)
    }

    private fun setupRecyclerViews() {
        // Activity list with language callbacks
        activityAdapter = AdapterActivityListing(
            getLanguage = { key -> viewModel.getLanguageForKey(key) },
            onAddClicked = { activity -> viewModel.onActivityAddClicked(activity) },
            onItemClicked = { activity ->
                // Notify host app that user tapped on an activity to see details
                val activityId = activity.productId ?: activity.id ?: return@AdapterActivityListing
                TRPCore.notifyActivityDetailRequested(activityId)
            }
        )
        binding.rvActivities.apply {
            layoutManager = LinearLayoutManager(this@ACActivityListing)
            adapter = activityAdapter

            // Add separator decoration (skip last item)
            addItemDecoration(ActivitySeparatorDecoration(this@ACActivityListing))
        }
        // Stack the device navigation bar inset onto the XML's base bottom
        // padding so the last card clears gesture / 3-button bars.
        binding.rvActivities.applyBottomSystemBarInsetPadding()

        // Category filter with icon and multi-selection support
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
        // Re-apply selection state in case the new facet set changed positions —
        // we always default to "All" (index 0) after a rebuild since indices may shift.
        categoryAdapter?.setSelectedIndices(setOf(0))
    }

    private fun setupSearchBar() {
        binding.searchBar.setHint(viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_SEARCH_ACTIVITY))
        binding.searchBar.setOnTextChangedListener { query ->
            viewModel.updateSearchText(query)
        }
        binding.searchBar.setOnSearchActionListener {
            hideKeyboard()
            viewModel.submitSearch()
        }
    }

    private fun setupClickListeners() {
        binding.imBack.setOnClickListener {
            finish()
        }

        // Filter button - shows filter bottom sheet
        binding.btnFilters.setOnClickListener {
            showFilterBottomSheet()
        }

        // Sort button - shows sort bottom sheet
        binding.btnSortBy.setOnClickListener {
            showSortBottomSheet()
        }
    }

    private fun showFilterBottomSheet() {
        // Pull facet-driven slider bounds (when present). Backend price ranges arrive
        // in minor units (cents) — convert to whole currency units to match the
        // slider's price scale. Duration bounds are already in minutes.
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
            initialSelectedDay = viewModel.getSelectedDate()
        )
        timeSelectionBottomSheet?.setOnTimeSelectedListener { tour, selectedDate, timeSlot, slotPrice, isFlexible ->
            viewModel.createReservedActivitySegment(tour, selectedDate, timeSlot, slotPrice, isFlexible)
        }
        timeSelectionBottomSheet?.show(supportFragmentManager, ActivityTimeSelectionBottomSheet.TAG)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    /**
     * Final step of the add-activity flow. Dismisses the still-open time selection
     * sheet and shows a confirmation toast — but stays on the listing so the user
     * can add more activities in the same session. RESULT_OK is set eagerly so when
     * the user eventually navigates back, AddPlanContainerBottomSheet sees the
     * positive result and re-syncs the timeline UI.
     */
    private fun handleAddedToItinerarySuccess(result: ACActivityListingVM.AddedToItineraryResult) {
        viewModel.clearAddedToItinerarySuccess()
        timeSelectionBottomSheet?.dismiss()
        timeSelectionBottomSheet = null

        // Day label, e.g. "Friday 29/05" — locale-aware day name, fixed dd/MM date.
        val dayLabel = SimpleDateFormat("EEEE dd/MM", Locale.getDefault())
            .format(result.selectedDate)

        // iOS-style placeholders: backend default is "%1$@ has been added to %2$@".
        val template = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_TOAST_ACTIVITY_ADDED)
            .ifBlank { "%1\$@ has been added to %2\$@" }
        val message = template
            .replace("%1\$@", result.activityName)
            .replace("%2\$@", dayLabel)

        BottomToast.show(
            activity = this,
            message = message,
            alertType = AlertType.SUCCESS
        )

        // Pre-arm the result so back navigation hands control back to AddPlan with
        // the day index that should be reselected. We don't finish here — the user
        // may add more activities in the same session.
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
            strokeWidth = context.resources.displayMetrics.density * 0.5f // 0.5dp
        }
        private val horizontalPadding = (context.resources.displayMetrics.density * 16).toInt() // 16dp

        override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            val childCount = parent.childCount
            val itemCount = parent.adapter?.itemCount ?: 0

            for (i in 0 until childCount) {
                val child = parent.getChildAt(i)
                val position = parent.getChildAdapterPosition(child)

                // Skip last item
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
        const val EXTRA_PLAN_DATA = "plan_data"
        const val EXTRA_TRIP_HASH = "trip_hash"
        const val RESULT_SELECTED_DAY_INDEX = "result_selected_day_index"

        fun launch(context: Context, planData: AddPlanData, tripHash: String): Intent {
            return Intent(context, ACActivityListing::class.java).apply {
                putExtra(EXTRA_PLAN_DATA, planData)
                putExtra(EXTRA_TRIP_HASH, tripHash)
            }
        }
    }
}
