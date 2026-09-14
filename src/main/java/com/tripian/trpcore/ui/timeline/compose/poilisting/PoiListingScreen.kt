package com.tripian.trpcore.ui.timeline.compose.poilisting

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.BundleCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.tripian.one.api.pois.model.Poi
import com.tripian.trpcore.R
import com.tripian.trpcore.databinding.AcPoiListingBinding
import com.tripian.trpcore.domain.model.timeline.FilterData
import com.tripian.trpcore.domain.model.timeline.SortOption
import com.tripian.trpcore.ui.timeline.TimeSelectionBottomSheet
import com.tripian.trpcore.ui.timeline.compose.core.BindingHost
import com.tripian.trpcore.ui.timeline.compose.core.TimelineComposeScreen
import com.tripian.trpcore.ui.timeline.compose.core.findActivity
import com.tripian.trpcore.ui.timeline.compose.core.timelineViewModel
import com.tripian.trpcore.ui.timeline.compose.nav.LocalTimelineNavigator
import com.tripian.trpcore.ui.timeline.compose.nav.PoiListingArgs
import com.tripian.trpcore.ui.timeline.compose.nav.TimelineNavigator
import com.tripian.trpcore.ui.timeline.compose.nav.TimelineResults
import com.tripian.trpcore.ui.timeline.poilisting.ACPOIListing
import com.tripian.trpcore.ui.timeline.poilisting.ACPOIListingVM
import com.tripian.trpcore.ui.timeline.poilisting.AdapterPOIListing
import com.tripian.trpcore.ui.timeline.poilisting.FilterBottomSheet
import com.tripian.trpcore.ui.timeline.poilisting.POIListingType
import com.tripian.trpcore.ui.timeline.poilisting.SortBottomSheet
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
 * Compose route of [ACPOIListing]: lists Places of Interest or Eat & Drink POIs
 * for a manual plan. Adding a POI reports [TimelineResults.SEGMENT_CREATED_DAY_INDEX]
 * to the Timeline without leaving the screen; tapping a POI opens its detail route.
 */
@Composable
internal fun PoiListingScreen(
    args: PoiListingArgs,
    viewModel: ACPOIListingVM = timelineViewModel()
) {
    val navigator = LocalTimelineNavigator.current
    val context = LocalContext.current
    val sheets = remember { PoiListingSheets() }

    LaunchedEffect(Unit) {
        if (viewModel.arguments == null) {
            viewModel.arguments = ACPOIListing.launch(
                context = context,
                planData = args.planData,
                tripHash = args.tripHash,
                listingType = args.listingType,
                tripStartDate = args.tripStartDate,
                tripEndDate = args.tripEndDate
            ).extras
            viewModel.onViewCreated(null)
            viewModel.initialize(args.planData, args.tripHash, args.listingType)
        }
    }

    TimelineComposeScreen(viewModel = viewModel, onExit = { navigator.back() }) {
        BindingHost(
            inflate = AcPoiListingBinding::inflate,
            onSaveViewState = { binding, state ->
                binding.rvPOIs.layoutManager?.onSaveInstanceState()?.let { state.putParcelable(STATE_LIST, it) }
            }
        ) { binding, owner, viewState ->
            val fragmentManager =
                (binding.root.context.findActivity() as? FragmentActivity)?.supportFragmentManager
            val adapter = AdapterPOIListing(
                getLanguage = { key -> viewModel.getLanguageForKey(key) },
                onAddClicked = { poi -> viewModel.onPOIAddClicked(poi) },
                onItemClicked = { poi -> navigator.openPoiDetail(poi, args.tripStartDate, args.tripEndDate) }
            )
            binding.tvTitle.text = titleFor(args.listingType, viewModel).replace("\n", " ")
            binding.setupList(adapter, viewModel)
            binding.searchBar.setHint(viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_SEARCH_POI))
            binding.searchBar.setOnQueryChangedListener(SearchBarView.Mode.REMOTE) { query ->
                viewModel.search(query)
            }
            binding.btnFilters.text = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_FILTERS)
            binding.btnSortBy.text = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_SORT_BY)
            binding.btnFilters.setOnClickListener { showFilterBottomSheet(fragmentManager, viewModel) }
            binding.btnSortBy.setOnClickListener { showSortBottomSheet(fragmentManager, viewModel) }
            binding.imBack.setOnClickListener { navigator.back() }
            binding.setupCollapseGap()
            binding.observe(adapter, viewModel, navigator, sheets, fragmentManager, owner, viewState)
        }
    }
}

private class PoiListingSheets {
    var timeSelectionBottomSheet: TimeSelectionBottomSheet? = null
    var selectedPoi: Poi? = null
    var pendingScrollToTop = false
}

private fun titleFor(listingType: POIListingType, viewModel: ACPOIListingVM): String = when (listingType) {
    POIListingType.PLACES_OF_INTEREST ->
        viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_TITLE_PLACES_OF_INTEREST)
    POIListingType.EAT_AND_DRINK ->
        viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_CAT_MANUAL_EAT_DRINK)
}

private fun AcPoiListingBinding.setupList(adapter: AdapterPOIListing, viewModel: ACPOIListingVM) {
    rvPOIs.applyBottomSystemBarInsetPadding()
    rvPOIs.layoutManager = LinearLayoutManager(root.context)
    rvPOIs.adapter = adapter
    rvPOIs.itemAnimator = null
    rvPOIs.addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            if (dy <= 0) return
            val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
            if (layoutManager.findLastVisibleItemPosition() >= layoutManager.itemCount - PAGINATION_PREFETCH_THRESHOLD) {
                viewModel.loadMorePOIs()
            }
        }
    })
}

/**
 * Keeps an 8dp gap between the fixed search bar and the collapsing content once
 * the list has been scrolled up, and removes it again at the fully expanded top.
 */
private fun AcPoiListingBinding.setupCollapseGap() {
    val gapPx = 8.dp
    appBarLayout.addOnOffsetChangedListener(
        AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            val target = minOf(gapPx, -verticalOffset).coerceAtLeast(0)
            val params = searchContainer.layoutParams as? ViewGroup.MarginLayoutParams
                ?: return@OnOffsetChangedListener
            if (params.bottomMargin != target) {
                params.bottomMargin = target
                searchContainer.layoutParams = params
            }
        }
    )
}

private fun AcPoiListingBinding.observe(
    adapter: AdapterPOIListing,
    viewModel: ACPOIListingVM,
    navigator: TimelineNavigator,
    sheets: PoiListingSheets,
    fragmentManager: FragmentManager?,
    owner: LifecycleOwner,
    viewState: Bundle
) {
    viewModel.pois.observe(owner) { pois ->
        adapter.submitList(pois) {
            if (sheets.pendingScrollToTop) {
                sheets.pendingScrollToTop = false
                rvPOIs.scrollToPosition(0)
                appBarLayout.setExpanded(true, false)
            }
            restoreListState(viewState)
        }
        tvEmpty.visibility = if (pois.isEmpty()) View.VISIBLE else View.GONE
        rvPOIs.visibility = if (pois.isEmpty()) View.GONE else View.VISIBLE
    }
    viewModel.loadingMore.observe(owner) { loadingMore ->
        loadMoreIndicator.root.visibility = if (loadingMore) View.VISIBLE else View.GONE
    }
    viewModel.skeletonLoading.observe(owner) { loading ->
        with(skeletonList.root) {
            if (loading) {
                visibility = View.VISIBLE
                startShimmer()
            } else {
                stopShimmer()
                visibility = View.GONE
            }
        }
    }
    viewModel.poiCount.observe(owner) { count ->
        val placesText = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_TITLE_PLACES_OF_INTEREST)
        tvResultCount.text = "$count $placesText"
    }
    viewModel.showTimeSelection.observe(owner) { poi ->
        poi?.let { showTimeRangeBottomSheet(it, viewModel, sheets, fragmentManager) }
    }
    viewModel.addedToItinerarySuccess.observe(owner) { result ->
        result?.let { handleAddedToItinerarySuccess(it, viewModel, navigator, sheets) }
    }
    viewModel.addSegmentError.observe(owner) { message ->
        message?.let {
            viewModel.clearAddSegmentError()
            sheets.timeSelectionBottomSheet?.hideInSheetLoadingOverlay()
            viewModel.showAlert(AlertType.ERROR, it)
        }
    }
    viewModel.currentFilter.observe(owner) { filter -> updateFilterButtonState(filter, viewModel) }
    viewModel.scrollToTop.observe(owner) { shouldScroll ->
        if (shouldScroll) {
            sheets.pendingScrollToTop = true
            viewModel.clearScrollToTop()
        }
    }
}

private fun AcPoiListingBinding.restoreListState(viewState: Bundle) {
    val state = BundleCompat.getParcelable(viewState, STATE_LIST, Parcelable::class.java) ?: return
    viewState.remove(STATE_LIST)
    rvPOIs.layoutManager?.onRestoreInstanceState(state)
}

private fun AcPoiListingBinding.updateFilterButtonState(filter: FilterData, viewModel: ACPOIListingVM) {
    val baseText = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_FILTERS)
    btnFilters.text = if (filter.hasActiveFilter) "$baseText (${filter.activeFilterCount})" else baseText
    btnFilters.setIconResource(
        if (filter.hasActiveFilter) R.drawable.trp_ic_filter_activity_badge else R.drawable.trp_ic_filter_activity
    )
}

private fun showFilterBottomSheet(fragmentManager: FragmentManager?, viewModel: ACPOIListingVM) {
    val fm = fragmentManager ?: return
    val sheet = FilterBottomSheet.newInstance(
        viewModel.currentFilter.value ?: FilterData(),
        viewModel.categoryGroups.value ?: emptyList()
    )
    sheet.setLanguageProvider { key -> viewModel.getLanguageForKey(key) }
    sheet.setOnFilterAppliedListener { filter -> viewModel.applyFilter(filter) }
    sheet.show(fm, FilterBottomSheet.TAG)
}

private fun showSortBottomSheet(fragmentManager: FragmentManager?, viewModel: ACPOIListingVM) {
    val fm = fragmentManager ?: return
    val sheet = SortBottomSheet.newInstance(viewModel.currentSort.value ?: SortOption.DEFAULT)
    sheet.setLanguageProvider { key -> viewModel.getLanguageForKey(key) }
    sheet.setOnSortSelectedListener { sort -> viewModel.applySort(sort) }
    sheet.show(fm, SortBottomSheet.TAG)
}

private fun showTimeRangeBottomSheet(
    poi: Poi,
    viewModel: ACPOIListingVM,
    sheets: PoiListingSheets,
    fragmentManager: FragmentManager?
) {
    val fm = fragmentManager ?: return
    sheets.selectedPoi = poi
    val sheet = TimeSelectionBottomSheet.newInstance(
        minTime = viewModel.minSelectableTimeForSelectedDay(),
        defaultStartTime = viewModel.defaultStartTimeForSelectedDay(),
        openingHours = poi.hours,
        selectedDay = viewModel.getSelectedDate()
    )
    sheet.setOnTimeSelectedListener { startTime, endTime ->
        val currentPoi = sheets.selectedPoi ?: return@setOnTimeSelectedListener
        val selectedDate = viewModel.getSelectedDate() ?: return@setOnTimeSelectedListener
        if (startTime != null && endTime != null) {
            val loadingText = viewModel.getLanguageForKey(LanguageConst.LOADING_TEXT_ADDING_TO_ITINERARY)
            sheet.showInSheetLoadingOverlay(LanguageConst.LOADING_TEXT_ADDING_TO_ITINERARY, loadingText)
            viewModel.createManualPoiSegment(currentPoi, selectedDate, startTime, endTime)
        }
        sheets.selectedPoi = null
        viewModel.clearTimeSelection()
    }
    sheets.timeSelectionBottomSheet = sheet
    sheet.show(fm, TimeSelectionBottomSheet.TAG)
}

/**
 * Final step of the add-POI flow: dismisses the time selection sheet, shows a
 * confirmation toast and reports the day to the Timeline. Does not leave the
 * screen so the user can keep adding places in the same session.
 */
private fun handleAddedToItinerarySuccess(
    result: ACPOIListingVM.AddedToItineraryResult,
    viewModel: ACPOIListingVM,
    navigator: TimelineNavigator,
    sheets: PoiListingSheets
) {
    viewModel.clearAddedToItinerarySuccess()
    sheets.timeSelectionBottomSheet?.dismiss()
    sheets.timeSelectionBottomSheet = null

    val dayLabel = SimpleDateFormat("EEEE dd/MM", Locale.getDefault()).format(result.selectedDate)
    val message = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_TOAST_ACTIVITY_ADDED)
        .replace("%1\$@", result.poiName)
        .replace("%2\$@", dayLabel)
    viewModel.showAlert(AlertType.SUCCESS, message)

    navigator.setResult(TimelineResults.SEGMENT_CREATED_DAY_INDEX, viewModel.getSelectedDayIndex())
}
