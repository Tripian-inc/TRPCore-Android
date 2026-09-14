package com.tripian.trpcore.ui.timeline.compose.poi

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.os.BundleCompat
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.tripian.trpcore.R
import com.tripian.trpcore.databinding.ActivityPoiSelectionBinding
import com.tripian.trpcore.ui.timeline.compose.core.BindingHost
import com.tripian.trpcore.ui.timeline.compose.core.TimelineComposeScreen
import com.tripian.trpcore.ui.timeline.compose.core.timelineViewModel
import com.tripian.trpcore.ui.timeline.compose.nav.LocalTimelineNavigator
import com.tripian.trpcore.ui.timeline.compose.nav.PoiSelectionArgs
import com.tripian.trpcore.ui.timeline.compose.nav.TimelineResults
import com.tripian.trpcore.ui.timeline.poi.ACPOISelection
import com.tripian.trpcore.ui.timeline.poi.ACPOISelectionVM
import com.tripian.trpcore.ui.timeline.poi.POICategory
import com.tripian.trpcore.ui.timeline.poi.POISelectionAdapter
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.applyBottomSystemBarInsetPadding
import com.tripian.trpcore.util.widget.SearchBarView

private const val PAGINATION_PREFETCH_THRESHOLD = 5
private const val STATE_LIST = "state_list"

/**
 * Compose route of [ACPOISelection]: picks a POI for a manual plan and hands it
 * back to the Timeline as [TimelineResults.SELECTED_POI].
 */
@Composable
internal fun PoiSelectionScreen(
    args: PoiSelectionArgs,
    viewModel: ACPOISelectionVM = timelineViewModel()
) {
    val navigator = LocalTimelineNavigator.current

    LaunchedEffect(Unit) {
        if (viewModel.arguments == null) {
            viewModel.arguments = Bundle()
            viewModel.onViewCreated(null)
            viewModel.setCity(args.city)
            viewModel.loadPois()
        }
    }

    TimelineComposeScreen(viewModel = viewModel, onExit = { navigator.back() }) {
        BindingHost(
            inflate = ActivityPoiSelectionBinding::inflate,
            onSaveViewState = { binding, state ->
                binding.rvPois.layoutManager?.onSaveInstanceState()?.let { state.putParcelable(STATE_LIST, it) }
            }
        ) { binding, owner, viewState ->
            val adapter = POISelectionAdapter { poi ->
                navigator.finishWithResult(TimelineResults.SELECTED_POI, poi)
            }
            binding.setupList(adapter, viewModel)
            binding.searchBar.setHint(viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_SEARCH_POI))
            binding.searchBar.setOnQueryChangedListener(SearchBarView.Mode.REMOTE) { query ->
                viewModel.search(query)
            }
            binding.ivBack.setOnClickListener { navigator.back() }
            binding.tvEmptyMessage.text = viewModel.getLanguageForKey(LanguageConst.POI_SELECTION_NO_PLACES)
            binding.observe(adapter, viewModel, owner, viewState)
        }
    }
}

private fun ActivityPoiSelectionBinding.setupList(adapter: POISelectionAdapter, viewModel: ACPOISelectionVM) {
    rvPois.applyBottomSystemBarInsetPadding()
    rvPois.layoutManager = LinearLayoutManager(root.context)
    rvPois.adapter = adapter
    rvPois.addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            if (dy <= 0) return
            val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
            if (layoutManager.findLastVisibleItemPosition() >= layoutManager.itemCount - PAGINATION_PREFETCH_THRESHOLD) {
                viewModel.loadMorePois()
            }
        }
    })
}

private fun ActivityPoiSelectionBinding.observe(
    adapter: POISelectionAdapter,
    viewModel: ACPOISelectionVM,
    owner: LifecycleOwner,
    viewState: Bundle
) {
    viewModel.pois.observe(owner) { pois ->
        adapter.submitList(pois) { restoreListState(viewState) }
        val isEmpty = pois.isEmpty()
        emptyStateView.visibility =
            if (isEmpty && viewModel.isLoading.value != true) View.VISIBLE else View.GONE
        rvPois.visibility = if (isEmpty) View.GONE else View.VISIBLE
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
    viewModel.categories.observe(owner) { categories ->
        renderCategoryChips(categories, viewModel)
    }
    viewModel.selectedCategory.observe(owner) { selectedId ->
        val categories = viewModel.categories.value ?: return@observe
        renderCategoryChips(categories.map { it.copy(isSelected = it.id == selectedId) }, viewModel)
    }
}

private fun ActivityPoiSelectionBinding.restoreListState(viewState: Bundle) {
    val state = BundleCompat.getParcelable(viewState, STATE_LIST, android.os.Parcelable::class.java) ?: return
    viewState.remove(STATE_LIST)
    rvPois.layoutManager?.onRestoreInstanceState(state)
}

private fun ActivityPoiSelectionBinding.renderCategoryChips(
    categories: List<POICategory>,
    viewModel: ACPOISelectionVM
) {
    chipGroupCategories.removeAllViews()
    val allCategory = POICategory(
        id = "",
        name = viewModel.getLanguageForKey(LanguageConst.ALL),
        isSelected = viewModel.selectedCategory.value == null
    )
    chipGroupCategories.addView(createCategoryChip(allCategory).apply {
        setOnClickListener { viewModel.selectCategory(null) }
    })
    categories.forEach { category ->
        chipGroupCategories.addView(createCategoryChip(category).apply {
            setOnClickListener { viewModel.selectCategory(category.id) }
        })
    }
}

private fun ActivityPoiSelectionBinding.createCategoryChip(category: POICategory): Chip {
    val context = root.context
    return Chip(context).apply {
        text = category.name
        isCheckable = true
        isChecked = category.isSelected
        chipBackgroundColor = context.getColorStateList(
            if (category.isSelected) R.color.trp_timeline_primary else R.color.trp_timeline_background
        )
        setTextColor(context.getColor(if (category.isSelected) R.color.trp_white else R.color.trp_text_primary))
        chipStrokeWidth = 0f
    }
}
