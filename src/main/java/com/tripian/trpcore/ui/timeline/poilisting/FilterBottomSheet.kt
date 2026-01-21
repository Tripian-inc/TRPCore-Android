package com.tripian.trpcore.ui.timeline.poilisting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tripian.one.api.pois.model.PoiCategoryGroup
import com.tripian.trpcore.R
import com.tripian.trpcore.databinding.BottomSheetPoiFilterBinding
import com.tripian.trpcore.databinding.ItemCategoryFilterBinding
import com.tripian.trpcore.domain.model.timeline.FilterData
import com.tripian.trpcore.util.LanguageConst

/**
 * FilterBottomSheet
 * Bottom sheet for filtering POI list by category groups
 * iOS Reference: POIFilterVC
 */
class FilterBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetPoiFilterBinding? = null
    private val binding get() = _binding!!

    private var categoryAdapter: CategoryFilterAdapter? = null
    private var categoryGroups: List<PoiCategoryGroup> = emptyList()
    private var selectedCategoryIds: MutableSet<Int> = mutableSetOf()
    private var onFilterApplied: ((FilterData) -> Unit)? = null
    private var getLanguage: ((String) -> String)? = null

    override fun getTheme(): Int = R.style.TrpTimelineBottomSheetDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetPoiFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Restore initial values from arguments
        arguments?.let { args ->
            @Suppress("DEPRECATION")
            val filter = args.getSerializable(ARG_FILTER) as? FilterData ?: FilterData()
            selectedCategoryIds = filter.selectedCategoryIds.toMutableSet()

            @Suppress("DEPRECATION", "UNCHECKED_CAST")
            categoryGroups = (args.getSerializable(ARG_CATEGORIES) as? ArrayList<PoiCategoryGroup>) ?: emptyList()
        }

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        // Set localized texts
        binding.tvTitle.text = getLanguageText(LanguageConst.ADD_PLAN_FILTERS)
        binding.btnClear.text = getLanguageText(LanguageConst.ADD_PLAN_FILTER_CLEAR)
        binding.btnConfirm.text = getLanguageText(LanguageConst.ADD_PLAN_CONFIRM)

        // Setup RecyclerView
        categoryAdapter = CategoryFilterAdapter(
            categoryGroups = categoryGroups,
            selectedCategoryIds = selectedCategoryIds,
            onCategoryToggled = { categoryId, isSelected ->
                if (isSelected) {
                    selectedCategoryIds.add(categoryId)
                } else {
                    selectedCategoryIds.remove(categoryId)
                }
                updateClearButtonState()
            }
        )

        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = categoryAdapter
        }

        updateClearButtonState()
    }

    private fun setupListeners() {
        binding.ivClose.setOnClickListener {
            dismiss()
        }

        binding.btnClear.setOnClickListener {
            // Clear all selections
            selectedCategoryIds.clear()
            categoryAdapter?.clearSelections()
            updateClearButtonState()
        }

        binding.btnConfirm.setOnClickListener {
            val newFilter = FilterData(selectedCategoryIds = selectedCategoryIds.toList())
            onFilterApplied?.invoke(newFilter)
            dismiss()
        }
    }

    private fun updateClearButtonState() {
        binding.btnClear.alpha = if (selectedCategoryIds.isNotEmpty()) 1f else 0.5f
    }

    private fun getLanguageText(key: String): String {
        return getLanguage?.invoke(key) ?: key
    }

    fun setOnFilterAppliedListener(listener: (FilterData) -> Unit) {
        onFilterApplied = listener
    }

    fun setLanguageProvider(provider: (String) -> String) {
        getLanguage = provider
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "FilterBottomSheet"
        private const val ARG_FILTER = "filter"
        private const val ARG_CATEGORIES = "categories"

        fun newInstance(
            currentFilter: FilterData = FilterData(),
            categoryGroups: List<PoiCategoryGroup> = emptyList()
        ): FilterBottomSheet {
            return FilterBottomSheet().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_FILTER, currentFilter)
                    putSerializable(ARG_CATEGORIES, ArrayList(categoryGroups))
                }
            }
        }
    }
}

/**
 * Adapter for category filter checkboxes
 */
class CategoryFilterAdapter(
    private val categoryGroups: List<PoiCategoryGroup>,
    private val selectedCategoryIds: MutableSet<Int>,
    private val onCategoryToggled: (Int, Boolean) -> Unit
) : RecyclerView.Adapter<CategoryFilterAdapter.CategoryViewHolder>() {

    // Flatten category groups into list of categories with group info
    private data class CategoryItem(
        val id: Int,
        val name: String,
        val groupName: String?
    )

    private val items: List<CategoryItem> = categoryGroups.flatMap { group ->
        group.categories?.map { category ->
            CategoryItem(
                id = category.id,
                name = category.name ?: "",
                groupName = group.name
            )
        } ?: emptyList()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryFilterBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item.id, item.name, selectedCategoryIds.contains(item.id))
    }

    override fun getItemCount(): Int = items.size

    fun clearSelections() {
        selectedCategoryIds.clear()
        notifyDataSetChanged()
    }

    inner class CategoryViewHolder(
        private val binding: ItemCategoryFilterBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(categoryId: Int, categoryName: String, isSelected: Boolean) {
            binding.tvCategoryName.text = categoryName
            binding.cbCategory.isChecked = isSelected

            binding.root.setOnClickListener {
                val newState = !binding.cbCategory.isChecked
                binding.cbCategory.isChecked = newState
                onCategoryToggled(categoryId, newState)
            }
        }
    }
}
