package com.tripian.trpcore.ui.timeline.poilisting

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.tripian.one.api.pois.model.Poi
import com.tripian.trpcore.R
import com.tripian.trpcore.base.BaseActivity
import com.tripian.trpcore.databinding.AcPoiListingBinding
import com.tripian.trpcore.domain.model.timeline.AddPlanData
import com.tripian.trpcore.domain.model.timeline.FilterData
import com.tripian.trpcore.domain.model.timeline.SortOption
import com.tripian.trpcore.ui.timeline.TimeSelectionBottomSheet
import com.tripian.trpcore.ui.timeline.poidetail.ACPOIDetail
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.dp
import com.tripian.trpcore.util.widget.SearchBarView
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * ACPOIListing
 * Full screen Activity for listing POIs (Places of Interest or Eat & Drink)
 * iOS Reference: POIListingVC
 */
class ACPOIListing : BaseActivity<AcPoiListingBinding, ACPOIListingVM>() {

    private var poiAdapter: AdapterPOIListing? = null
    private var timeSelectionBottomSheet: TimeSelectionBottomSheet? = null
    private var filterBottomSheet: FilterBottomSheet? = null
    private var sortBottomSheet: SortBottomSheet? = null
    private var selectedPoi: Poi? = null
    private var pendingScrollToTop = false

    override fun getViewBinding() = AcPoiListingBinding.inflate(layoutInflater)

    override fun setListeners() {
        setupRecyclerView()
        setupSearchBar()
        setupFilterSortButtons()
        setupClickListeners()
        setupCollapseGap()

        intent?.let { intent ->
            @Suppress("DEPRECATION")
            val planData = intent.getSerializableExtra(EXTRA_PLAN_DATA) as? AddPlanData
            val tripHash = intent.getStringExtra(EXTRA_TRIP_HASH) ?: ""
            val listingType = intent.getSerializableExtra(EXTRA_LISTING_TYPE) as? POIListingType
                ?: POIListingType.PLACES_OF_INTEREST

            planData?.let {
                viewModel.initialize(it, tripHash, listingType)

                val title = when (listingType) {
                    POIListingType.PLACES_OF_INTEREST -> viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_TITLE_PLACES_OF_INTEREST)
                    POIListingType.EAT_AND_DRINK -> viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_CAT_MANUAL_EAT_DRINK)
                }
                binding.tvTitle.text = title.replace("\n", " ")
            }
        }
    }

    override fun setReceivers() {
        viewModel.pois.observe(this) { pois ->
            poiAdapter?.submitList(pois) {
                if (pendingScrollToTop) {
                    pendingScrollToTop = false
                    binding.rvPOIs.scrollToPosition(0)
                    binding.appBarLayout.setExpanded(true, false)
                }
            }
            updateEmptyState(pois.isEmpty())
        }

        viewModel.loadingMore.observe(this) { loadingMore ->
            binding.loadMoreIndicator.root.visibility = if (loadingMore) View.VISIBLE else View.GONE
        }

        viewModel.skeletonLoading.observe(this) { loading ->
            with(binding.skeletonList.root) {
                if (loading) {
                    visibility = View.VISIBLE
                    startShimmer()
                } else {
                    stopShimmer()
                    visibility = View.GONE
                }
            }
        }

        viewModel.poiCount.observe(this) { count ->
            val placesText =
                viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_TITLE_PLACES_OF_INTEREST)
            binding.tvResultCount.text = "$count $placesText"
        }

        viewModel.showTimeSelection.observe(this) { poi ->
            poi?.let { showTimeRangeBottomSheet(it) }
        }

        viewModel.addedToItinerarySuccess.observe(this) { result ->
            result?.let { handleAddedToItinerarySuccess(it) }
        }

        viewModel.addSegmentError.observe(this) { message ->
            message?.let {
                viewModel.clearAddSegmentError()
                timeSelectionBottomSheet?.hideInSheetLoadingOverlay()
                showAlert(AlertType.ERROR, it)
            }
        }

        viewModel.currentFilter.observe(this) { filter ->
            updateFilterButtonState(filter)
        }

        viewModel.currentSort.observe(this) { sort ->
        }

        viewModel.scrollToTop.observe(this) { shouldScroll ->
            if (shouldScroll) {
                pendingScrollToTop = true
                viewModel.clearScrollToTop()
            }
        }
    }

    private fun setupRecyclerView() {
        poiAdapter = AdapterPOIListing(
            getLanguage = { key -> viewModel.getLanguageForKey(key) },
            onAddClicked = { poi -> viewModel.onPOIAddClicked(poi) },
            onItemClicked = { poi ->
                startActivity(
                    ACPOIDetail.launch(
                        context = this,
                        poi = poi,
                        tripStartDate = intent.getStringExtra(EXTRA_TRIP_START_DATE),
                        tripEndDate = intent.getStringExtra(EXTRA_TRIP_END_DATE)
                    )
                )
            }
        )
        binding.rvPOIs.apply {
            layoutManager = LinearLayoutManager(this@ACPOIListing)
            adapter = poiAdapter
            itemAnimator = null

            paginationScrollListener = object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy <= 0) return
                    val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                    val totalItemCount = layoutManager.itemCount
                    val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                    if (lastVisibleItem >= totalItemCount - 5) {
                        viewModel.loadMorePOIs()
                    }
                }
            }
            addOnScrollListener(paginationScrollListener!!)
        }
    }

    private var paginationScrollListener: RecyclerView.OnScrollListener? = null

    override fun onDestroy() {
        paginationScrollListener?.let { binding.rvPOIs.removeOnScrollListener(it) }
        paginationScrollListener = null
        binding.skeletonList.root.stopShimmer()
        super.onDestroy()
    }

    private fun setupSearchBar() {
        binding.searchBar.setHint(viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_SEARCH_POI))
        binding.searchBar.setOnQueryChangedListener(SearchBarView.Mode.REMOTE) { query ->
            viewModel.search(query)
        }
    }

    private fun setupClickListeners() {
        binding.imBack.setOnClickListener {
            finish()
        }
    }

    private fun setupFilterSortButtons() {
        binding.btnFilters.text = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_FILTERS)
        binding.btnSortBy.text = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_SORT_BY)

        binding.btnFilters.setOnClickListener {
            showFilterBottomSheet()
        }
        binding.btnSortBy.setOnClickListener {
            showSortBottomSheet()
        }
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
        binding.btnFilters.text = if (filter.hasActiveFilter) {
            "$baseText (${filter.activeFilterCount})"
        } else {
            baseText
        }

        val iconRes = if (filter.hasActiveFilter) {
            R.drawable.trp_ic_filter_activity_badge
        } else {
            R.drawable.trp_ic_filter_activity
        }
        binding.btnFilters.setIconResource(iconRes)
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvPOIs.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun showTimeRangeBottomSheet(poi: Poi) {
        selectedPoi = poi

        timeSelectionBottomSheet = TimeSelectionBottomSheet.newInstance(
            minTime = viewModel.minSelectableTimeForSelectedDay(),
            defaultStartTime = viewModel.defaultStartTimeForSelectedDay(),
            openingHours = poi.hours,
            selectedDay = viewModel.getSelectedDate()
        )
        timeSelectionBottomSheet?.setOnTimeSelectedListener { startTime, endTime ->
            val currentPoi = selectedPoi ?: return@setOnTimeSelectedListener
            val selectedDate = viewModel.getSelectedDate() ?: return@setOnTimeSelectedListener

            if (startTime != null && endTime != null) {
                timeSelectionBottomSheet?.showInSheetLoadingOverlay(
                    LanguageConst.LOADING_TEXT_ADDING_TO_ITINERARY, "Adding to itinerary"
                )
                viewModel.createManualPoiSegment(currentPoi, selectedDate, startTime, endTime)
            }

            selectedPoi = null
            viewModel.clearTimeSelection()
        }
        timeSelectionBottomSheet?.show(supportFragmentManager, TimeSelectionBottomSheet.TAG)
    }

    /**
     * Final step of the add-POI flow: dismisses the time selection sheet, shows a
     * confirmation toast and pre-arms RESULT_OK for back navigation. Does not finish —
     * the user can keep adding places in the same session.
     */
    private fun handleAddedToItinerarySuccess(result: ACPOIListingVM.AddedToItineraryResult) {
        viewModel.clearAddedToItinerarySuccess()
        timeSelectionBottomSheet?.dismiss()
        timeSelectionBottomSheet = null

        val dayLabel = SimpleDateFormat("EEEE dd/MM", Locale.getDefault())
            .format(result.selectedDate)

        val template = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_TOAST_ACTIVITY_ADDED)
            .ifBlank { "%1\$@ has been added to %2\$@" }
        val message = template
            .replace("%1\$@", result.poiName)
            .replace("%2\$@", dayLabel)

        showAlert(AlertType.SUCCESS, message)

        val resultIntent = Intent().apply {
            putExtra(RESULT_SELECTED_DAY_INDEX, viewModel.getSelectedDayIndex())
        }
        setResult(Activity.RESULT_OK, resultIntent)
    }

    companion object {
        const val EXTRA_PLAN_DATA = "plan_data"
        const val EXTRA_TRIP_HASH = "trip_hash"
        const val EXTRA_LISTING_TYPE = "listing_type"
        const val RESULT_SELECTED_DAY_INDEX = "result_selected_day_index"

        const val EXTRA_TRIP_START_DATE = "extra_trip_start_date"
        const val EXTRA_TRIP_END_DATE = "extra_trip_end_date"

        fun launch(
            context: Context,
            planData: AddPlanData,
            tripHash: String,
            listingType: POIListingType,
            tripStartDate: String? = null,
            tripEndDate: String? = null
        ): Intent {
            return Intent(context, ACPOIListing::class.java).apply {
                putExtra(EXTRA_PLAN_DATA, planData)
                putExtra(EXTRA_TRIP_HASH, tripHash)
                putExtra(EXTRA_LISTING_TYPE, listingType)
                putExtra(EXTRA_TRIP_START_DATE, tripStartDate)
                putExtra(EXTRA_TRIP_END_DATE, tripEndDate)
            }
        }
    }
}
