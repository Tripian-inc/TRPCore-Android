package com.tripian.trpcore.ui.timeline.poi

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.tripian.one.api.cities.model.City
import com.tripian.one.api.pois.model.Poi
import com.tripian.trpcore.R
import com.tripian.trpcore.base.BaseActivity
import com.tripian.trpcore.databinding.ActivityPoiSelectionBinding
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.widget.SearchBarView

/**
 * ACPOISelection
 * POI selection screen for manual POI addition
 */
class ACPOISelection : BaseActivity<ActivityPoiSelectionBinding, ACPOISelectionVM>() {

    private var poiAdapter: POISelectionAdapter? = null
    private var paginationScrollListener: RecyclerView.OnScrollListener? = null

    override fun getViewBinding() = ActivityPoiSelectionBinding.inflate(layoutInflater)

    override fun setListeners() {
        setupRecyclerView()
        setupSearchBar()
        setupClickListeners()
        setupCategoryChips()

        intent?.let { intent ->
            val city = intent.getSerializableExtra(ARG_CITY) as? City
            city?.let { viewModel.setCity(it) }
        }

        viewModel.loadPois()
    }

    override fun setReceivers() {
        viewModel.pois.observe(this) { pois ->
            poiAdapter?.submitList(pois)
            updateEmptyState(pois.isEmpty())
        }

        viewModel.loadingMore.observe(this) { loadingMore ->
            binding.loadMoreIndicator.root.visibility = if (loadingMore) View.VISIBLE else View.GONE
        }

        viewModel.categories.observe(this) { categories ->
            updateCategoryChips(categories)
        }

        viewModel.selectedCategory.observe(this) { selectedCategory ->
            updateSelectedCategoryChip(selectedCategory)
        }
    }

    private fun setupRecyclerView() {
        poiAdapter = POISelectionAdapter { poi ->
            selectPoi(poi)
        }
        binding.rvPois.apply {
            layoutManager = LinearLayoutManager(this@ACPOISelection)
            adapter = poiAdapter

            paginationScrollListener = object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy <= 0) return
                    val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                    val totalItemCount = layoutManager.itemCount
                    val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                    if (lastVisibleItem >= totalItemCount - PAGINATION_PREFETCH_THRESHOLD) {
                        viewModel.loadMorePois()
                    }
                }
            }
            addOnScrollListener(paginationScrollListener!!)
        }

        binding.tvEmptyMessage.text = getLanguageForKey(LanguageConst.POI_SELECTION_NO_PLACES)
            .let { if (it.isBlank() || it == LanguageConst.POI_SELECTION_NO_PLACES) "No places found" else it }
    }

    override fun onDestroy() {
        paginationScrollListener?.let { binding.rvPois.removeOnScrollListener(it) }
        paginationScrollListener = null
        super.onDestroy()
    }

    private fun setupSearchBar() {
        binding.searchBar.setHint(getLanguageForKey(LanguageConst.ADD_PLAN_SEARCH_POI))
        binding.searchBar.setOnQueryChangedListener(SearchBarView.Mode.REMOTE) { query ->
            viewModel.search(query)
        }
    }

    private fun setupClickListeners() {
        binding.ivBack.setOnClickListener {
            finish()
        }
    }

    /** Categories are populated via LiveData observation. */
    private fun setupCategoryChips() {
    }

    private fun updateCategoryChips(categories: List<POICategory>) {
        binding.chipGroupCategories.removeAllViews()

        val allChip = createCategoryChip(
            POICategory(id = "", name = getLanguageForKey(LanguageConst.ALL), isSelected = viewModel.selectedCategory.value == null)
        )
        allChip.setOnClickListener {
            viewModel.selectCategory(null)
        }
        binding.chipGroupCategories.addView(allChip)

        categories.forEach { category ->
            val chip = createCategoryChip(category)
            chip.setOnClickListener {
                viewModel.selectCategory(category.id)
            }
            binding.chipGroupCategories.addView(chip)
        }
    }

    private fun createCategoryChip(category: POICategory): Chip {
        return Chip(this).apply {
            text = category.name
            isCheckable = true
            isChecked = category.isSelected
            chipBackgroundColor = getColorStateList(
                if (category.isSelected) R.color.trp_timeline_primary else R.color.trp_timeline_background
            )
            setTextColor(
                getColor(if (category.isSelected) R.color.trp_white else R.color.trp_text_primary)
            )
            chipStrokeWidth = 0f
        }
    }

    private fun updateSelectedCategoryChip(selectedCategoryId: String?) {
        val categories = viewModel.categories.value ?: return
        updateCategoryChips(categories.map {
            it.copy(isSelected = it.id == selectedCategoryId)
        })
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyStateView.visibility = if (isEmpty && viewModel.isLoading.value != true) {
            View.VISIBLE
        } else {
            View.GONE
        }
        binding.rvPois.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun selectPoi(poi: Poi) {
        val resultIntent = Intent()
        resultIntent.putExtra(RESULT_POI, poi)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    companion object {
        const val ARG_CITY = "city"
        const val RESULT_POI = "selected_poi"
        const val REQUEST_CODE = 1001
        private const val PAGINATION_PREFETCH_THRESHOLD = 5

        fun launch(context: Context, city: City): Intent {
            return Intent(context, ACPOISelection::class.java).apply {
                putExtra(ARG_CITY, city)
            }
        }
    }
}

/**
 * POICategory model for UI
 */
data class POICategory(
    val id: String,
    val name: String,
    var isSelected: Boolean = false
)
