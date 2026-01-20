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
import com.tripian.one.api.tour.model.TourProduct
import com.tripian.trpcore.R
import com.tripian.trpcore.base.BaseActivity
import com.tripian.trpcore.databinding.AcActivityListingBinding
import com.tripian.trpcore.domain.model.timeline.AddPlanData
import com.tripian.trpcore.util.LanguageConst

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
        }

        // Observe loading state - show fullscreen loading dialog with dimmed background
        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) showLoading() else hideLoading()
        }

        // Observe searching state
        viewModel.isSearching.observe(this) { isSearching ->
            binding.pbSearchProgress.visibility = if (isSearching) View.VISIBLE else View.GONE
        }

        // Observe activity count
        viewModel.activityCount.observe(this) { count ->
            binding.tvResultCount.text = "$count ${viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_ACTIVITIES)}"
        }

        // Observe selected category indices (multiple selection)
        viewModel.selectedCategoryIndices.observe(this) { selectedIndices ->
            categoryAdapter?.setSelectedIndices(selectedIndices)
        }

        // Observe time selection trigger
        viewModel.showTimeSelection.observe(this) { activity ->
            activity?.let { showTimeSelectionBottomSheet(it) }
        }

        // Observe segment creation loading state
        viewModel.isCreatingSegment.observe(this) { isCreating ->
            if (isCreating) {
                // Dismiss bottom sheet and show loading
                timeSelectionBottomSheet?.dismiss()
                showLoading()
            }
        }

        // Observe segment creation
        viewModel.segmentCreated.observe(this) { created ->
            if (created) {
                // Return success result - loading will be hidden by parent activity
                setResult(Activity.RESULT_OK)
                finish()
            }
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
            R.drawable.ic_filter_activity_badge
        } else {
            R.drawable.ic_filter_activity
        }
        binding.btnFilters.setIconResource(iconRes)
    }

    private fun setupRecyclerViews() {
        // Activity list with language callbacks
        activityAdapter = AdapterActivityListing(
            getLanguage = { key -> viewModel.getLanguageForKey(key) },
            onAddClicked = { activity -> viewModel.onActivityAddClicked(activity) }
        )
        binding.rvActivities.apply {
            layoutManager = LinearLayoutManager(this@ACActivityListing)
            adapter = activityAdapter

            // Add separator decoration (skip last item)
            addItemDecoration(ActivitySeparatorDecoration(this@ACActivityListing))

            // Pagination
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                    val totalItemCount = layoutManager.itemCount
                    val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                    if (lastVisibleItem >= totalItemCount - 5) {
                        viewModel.loadMoreActivities()
                    }
                }
            })
        }

        // Category filter with icon and multi-selection support
        categoryAdapter = AdapterActivityCategory(
            categories = viewModel.getCategories(),
            getLanguage = { key -> viewModel.getLanguageForKey(key) },
            onSelectionChanged = { selectedIndices ->
                viewModel.onCategorySelectionChanged(selectedIndices)
            }
        )
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(this@ACActivityListing, LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
        }
    }

    private fun setupSearchBar() {
        binding.searchBar.setHint(viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_SEARCH_ACTIVITY))
        binding.searchBar.setOnTextChangedListener { query ->
            viewModel.updateSearchText(query)
        }
        binding.searchBar.setOnSearchActionListener {
            hideKeyboard()
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
        filterBottomSheet = ActivityFilterBottomSheet.newInstance(
            currentFilter = viewModel.getCurrentFilter(),
            currency = viewModel.getCurrency()
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
        timeSelectionBottomSheet?.setOnTimeSelectedListener { tour, selectedDate, timeSlot ->
            viewModel.createReservedActivitySegment(tour, selectedDate, timeSlot)
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
     * Custom ItemDecoration for drawing separators between activity items
     * Skips the last item (no separator after it)
     */
    private class ActivitySeparatorDecoration(context: Context) : RecyclerView.ItemDecoration() {
        private val paint = Paint().apply {
            color = ContextCompat.getColor(context, R.color.lineWeak)
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

        fun launch(context: Context, planData: AddPlanData, tripHash: String): Intent {
            return Intent(context, ACActivityListing::class.java).apply {
                putExtra(EXTRA_PLAN_DATA, planData)
                putExtra(EXTRA_TRIP_HASH, tripHash)
            }
        }
    }
}
