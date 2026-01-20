package com.tripian.trpcore.ui.timeline.poi

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.tripian.one.api.cities.model.City
import com.tripian.one.api.pois.model.Poi
import com.tripian.trpcore.R
import com.tripian.trpcore.base.BaseActivity
import com.tripian.trpcore.databinding.ActivityPoiSelectionBinding
import com.tripian.trpcore.util.LanguageConst

/**
 * ACPOISelection
 * Manuel POI ekleme için POI seçim ekranı
 */
class ACPOISelection : BaseActivity<ActivityPoiSelectionBinding, ACPOISelectionVM>() {

    private var poiAdapter: POISelectionAdapter? = null

    override fun getViewBinding() = ActivityPoiSelectionBinding.inflate(layoutInflater)

    override fun setListeners() {
        setupRecyclerView()
        setupSearchBar()
        setupClickListeners()
        setupCategoryChips()

        // Initialize from arguments
        intent?.let { intent ->
            val city = intent.getSerializableExtra(ARG_CITY) as? City
            city?.let { viewModel.setCity(it) }
        }

        // Initial load
        viewModel.loadPois()
    }

    override fun setReceivers() {
        viewModel.pois.observe(this) { pois ->
            poiAdapter?.submitList(pois)
            updateEmptyState(pois.isEmpty())
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
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
        }
    }

    private fun setupSearchBar() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString() ?: ""
                binding.ivClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                viewModel.search(query)
            }
        })

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard()
                true
            } else {
                false
            }
        }

        binding.ivClearSearch.setOnClickListener {
            binding.etSearch.setText("")
            viewModel.search("")
        }
    }

    private fun setupClickListeners() {
        binding.ivBack.setOnClickListener {
            finish()
        }
    }

    private fun setupCategoryChips() {
        // Initial categories will be set via LiveData observation
    }

    private fun updateCategoryChips(categories: List<POICategory>) {
        binding.chipGroupCategories.removeAllViews()

        // Add "All" chip first
        val allChip = createCategoryChip(
            POICategory(id = "", name = getLanguageForKey(LanguageConst.ALL), isSelected = viewModel.selectedCategory.value == null)
        )
        allChip.setOnClickListener {
            viewModel.selectCategory(null)
        }
        binding.chipGroupCategories.addView(allChip)

        // Add category chips
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
                if (category.isSelected) R.color.timeline_primary else R.color.timeline_background
            )
            setTextColor(
                getColor(if (category.isSelected) R.color.white else R.color.text_primary)
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

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    companion object {
        const val ARG_CITY = "city"
        const val RESULT_POI = "selected_poi"
        const val REQUEST_CODE = 1001

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
