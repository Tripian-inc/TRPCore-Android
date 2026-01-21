package com.tripian.trpcore.ui.timeline.poilisting

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tripian.one.api.pois.model.Poi
import com.tripian.trpcore.R
import com.tripian.trpcore.base.BaseActivity
import com.tripian.trpcore.databinding.AcPoiListingBinding
import com.tripian.trpcore.domain.model.timeline.AddPlanData
import com.tripian.trpcore.domain.model.timeline.FilterData
import com.tripian.trpcore.domain.model.timeline.SortOption
import com.tripian.trpcore.ui.timeline.addplan.TimePickerBottomSheet
import com.tripian.trpcore.ui.timeline.poidetail.ACPOIDetail
import com.tripian.trpcore.util.LanguageConst

/**
 * ACPOIListing
 * Full screen Activity for listing POIs (Places of Interest or Eat & Drink)
 * iOS Reference: POIListingVC
 */
class ACPOIListing : BaseActivity<AcPoiListingBinding, ACPOIListingVM>() {

    private var poiAdapter: AdapterPOIListing? = null
    private var timePickerBottomSheet: TimePickerBottomSheet? = null
    private var filterBottomSheet: FilterBottomSheet? = null
    private var sortBottomSheet: SortBottomSheet? = null
    private var selectedPoi: Poi? = null

    override fun getViewBinding() = AcPoiListingBinding.inflate(layoutInflater)

    override fun setListeners() {
        setupRecyclerView()
        setupSearchBar()
        setupFilterSortButtons()
        setupClickListeners()

        // Initialize from intent
        intent?.let { intent ->
            @Suppress("DEPRECATION")
            val planData = intent.getSerializableExtra(EXTRA_PLAN_DATA) as? AddPlanData
            val tripHash = intent.getStringExtra(EXTRA_TRIP_HASH) ?: ""
            val listingType = intent.getSerializableExtra(EXTRA_LISTING_TYPE) as? POIListingType
                ?: POIListingType.PLACES_OF_INTEREST

            planData?.let {
                viewModel.initialize(it, tripHash, listingType)

                // Set title based on listing type (replace newlines with spaces)
                val title = when (listingType) {
                    POIListingType.PLACES_OF_INTEREST -> viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_TITLE_PLACES_OF_INTEREST)
                    POIListingType.EAT_AND_DRINK -> viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_CAT_MANUAL_EAT_DRINK)
                }
                binding.tvTitle.text = title.replace("\n", " ")
            }
        }
    }

    override fun setReceivers() {
        // Observe POIs
        viewModel.pois.observe(this) { pois ->
            poiAdapter?.submitList(pois)
            updateEmptyState(pois.isEmpty())
        }

        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            binding.pbLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observe searching state
        viewModel.isSearching.observe(this) { isSearching ->
            binding.pbSearchProgress.visibility = if (isSearching) View.VISIBLE else View.GONE
        }

        // Observe POI count
        viewModel.poiCount.observe(this) { count ->
            val placesText = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_TITLE_PLACES_OF_INTEREST)
            binding.tvResultCount.text = "$count $placesText"
        }

        // Observe time selection trigger
        viewModel.showTimeSelection.observe(this) { poi ->
            poi?.let { showTimeRangeBottomSheet(it) }
        }

        // Observe segment creation
        viewModel.segmentCreated.observe(this) { created ->
            if (created) {
                // Return success result
                setResult(Activity.RESULT_OK)
                finish()
            }
        }

        // Observe filter changes
        viewModel.currentFilter.observe(this) { filter ->
            updateFilterButtonState(filter)
        }

        // Observe sort changes
        viewModel.currentSort.observe(this) { sort ->
//            updateSortButtonState(sort)
        }
    }

    private fun setupRecyclerView() {
        poiAdapter = AdapterPOIListing(
            getLanguage = { key -> viewModel.getLanguageForKey(key) },
            onAddClicked = { poi -> viewModel.onPOIAddClicked(poi) },
            onItemClicked = { poi -> startActivity(ACPOIDetail.launch(this, poi)) }
        )
        binding.rvPOIs.apply {
            layoutManager = LinearLayoutManager(this@ACPOIListing)
            adapter = poiAdapter

            // Pagination
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                    val totalItemCount = layoutManager.itemCount
                    val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                    if (lastVisibleItem >= totalItemCount - 5) {
                        viewModel.loadMorePOIs()
                    }
                }
            })
        }
    }

    private fun setupSearchBar() {
        binding.searchBar.setHint("Search places...")
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
    }

    private fun setupFilterSortButtons() {
        // Set initial button texts
        binding.btnFilters.text = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_FILTERS)
        binding.btnSortBy.text = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_SORT_BY)

        // Set click listeners
        binding.btnFilters.setOnClickListener {
            showFilterBottomSheet()
        }
        binding.btnSortBy.setOnClickListener {
            showSortBottomSheet()
        }
    }

    private fun showFilterBottomSheet() {
        val currentFilter = viewModel.currentFilter.value ?: FilterData()
        val categoryGroups = viewModel.categoryGroups.value ?: emptyList()
        filterBottomSheet = FilterBottomSheet.newInstance(currentFilter, categoryGroups)
        filterBottomSheet?.setLanguageProvider { key ->
            viewModel.getLanguageForKey(key)
        }
        filterBottomSheet?.setOnFilterAppliedListener { filter ->
            viewModel.applyFilter(filter)
        }
        filterBottomSheet?.show(supportFragmentManager, FilterBottomSheet.TAG)
    }

    private fun showSortBottomSheet() {
        val currentSort = viewModel.currentSort.value ?: SortOption.DEFAULT
        sortBottomSheet = SortBottomSheet.newInstance(currentSort)
        sortBottomSheet?.setLanguageProvider { key ->
            viewModel.getLanguageForKey(key)
        }
        sortBottomSheet?.setOnSortSelectedListener { sort ->
            viewModel.applySort(sort)
        }
        sortBottomSheet?.show(supportFragmentManager, SortBottomSheet.TAG)
    }

    private fun updateFilterButtonState(filter: FilterData) {
        val baseText = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_FILTERS)
        // Update button text
        binding.btnFilters.text = if (filter.hasActiveFilter) {
            "$baseText (${filter.activeFilterCount})"
        } else {
            baseText
        }

        // Update icon - use badge version when filters are active
        val iconRes = if (filter.hasActiveFilter) {
            R.drawable.ic_filter_activity_badge
        } else {
            R.drawable.ic_filter_activity
        }
        binding.btnFilters.setIconResource(iconRes)
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        val isLoading = viewModel.isLoading.value == true
        binding.tvEmpty.visibility = if (isEmpty && !isLoading) View.VISIBLE else View.GONE
        binding.rvPOIs.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun showTimeRangeBottomSheet(poi: Poi) {
        // Store the selected POI for use when time is confirmed
        selectedPoi = poi

        // Use the existing TimePickerBottomSheet from AddPlan flow
        // Pass POI duration for auto end time calculation (null or 0 will use default 60 minutes)
        timePickerBottomSheet = TimePickerBottomSheet.newInstance(
            durationMinutes = poi.duration
        )
        timePickerBottomSheet?.setOnTimeSelectedListener { startTime, endTime ->
            val currentPoi = selectedPoi ?: return@setOnTimeSelectedListener
            val selectedDate = viewModel.getSelectedDate() ?: return@setOnTimeSelectedListener

            if (startTime != null && endTime != null) {
                viewModel.createManualPoiSegment(currentPoi, selectedDate, startTime, endTime)
            }

            // Clear the selection
            selectedPoi = null
            viewModel.clearTimeSelection()
        }
        timePickerBottomSheet?.show(supportFragmentManager, TimePickerBottomSheet.TAG)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    companion object {
        const val EXTRA_PLAN_DATA = "plan_data"
        const val EXTRA_TRIP_HASH = "trip_hash"
        const val EXTRA_LISTING_TYPE = "listing_type"

        fun launch(
            context: Context,
            planData: AddPlanData,
            tripHash: String,
            listingType: POIListingType
        ): Intent {
            return Intent(context, ACPOIListing::class.java).apply {
                putExtra(EXTRA_PLAN_DATA, planData)
                putExtra(EXTRA_TRIP_HASH, tripHash)
                putExtra(EXTRA_LISTING_TYPE, listingType)
            }
        }
    }
}
