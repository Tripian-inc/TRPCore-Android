package com.tripian.trpcore.ui.timeline.addplan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.tripian.trpcore.databinding.FrAddPlanCategorySelectionBinding
import com.tripian.trpcore.domain.model.timeline.SmartCategory
import com.tripian.trpcore.util.LanguageConst

/**
 * FRCategorySelection
 * Step 3: Smart Recommendation Categories (7 categories)
 * iOS Reference: AddPlanCategorySelectionVC.swift
 */
class FRCategorySelection : Fragment() {

    private var _binding: FrAddPlanCategorySelectionBinding? = null
    private val binding get() = _binding!!

    // Shared ViewModel from parent
    private val sharedVM: AddPlanContainerVM by lazy {
        ViewModelProvider(requireParentFragment())[AddPlanContainerVM::class.java]
    }

    private var categoryAdapter: SmartCategoryAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FrAddPlanCategorySelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupLabels()
        setupCategoryGrid()
        observeViewModel()
    }

    /**
     * Set all label texts using language service
     */
    private fun setupLabels() {
        binding.tvTitle.text = getLanguageForKey(LanguageConst.ADD_PLAN_SELECT_CATEGORIES)
    }

    private fun setupCategoryGrid() {
        categoryAdapter = SmartCategoryAdapter(
            getLanguageForKey = { key -> getLanguageForKey(key) },
            onCategoryClicked = { category ->
                sharedVM.toggleSmartCategory(category)
            }
        )

        val spanCount = 3
        val gridLayoutManager = GridLayoutManager(context, spanCount)

        // Use SpanSizeLookup to center last row item
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val itemCount = categoryAdapter?.itemCount ?: 0
                val itemsInLastRow = itemCount % spanCount

                // If last row has 1 item, make it span all 3 columns (will be centered by item layout)
                if (itemsInLastRow == 1 && position == itemCount - 1) {
                    return spanCount
                }
                return 1
            }
        }

        binding.rvCategories.apply {
            layoutManager = gridLayoutManager
            adapter = categoryAdapter

            // Add item spacing
            addItemDecoration(object : androidx.recyclerview.widget.RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: android.graphics.Rect,
                    view: View,
                    parent: androidx.recyclerview.widget.RecyclerView,
                    state: androidx.recyclerview.widget.RecyclerView.State
                ) {
                    val spacing = resources.getDimensionPixelSize(
                        com.tripian.trpcore.R.dimen.category_grid_spacing
                    )
                    outRect.left = spacing / 2
                    outRect.right = spacing / 2
                    outRect.top = spacing / 2
                    outRect.bottom = spacing / 2
                }
            })
        }
    }

    private fun observeViewModel() {
        // Selected categories
        sharedVM.selectedSmartCategories.observe(viewLifecycleOwner) { categories ->
            categoryAdapter?.updateSelectedCategories(categories)
        }
    }

    private fun getLanguageForKey(key: String): String {
        // Get from parent's view model or use key as fallback
        return try {
            (parentFragment as? AddPlanContainerBottomSheet)?.getLanguageForKey(key) ?: key
        } catch (e: Exception) {
            key
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
