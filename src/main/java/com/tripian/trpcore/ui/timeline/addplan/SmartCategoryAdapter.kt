package com.tripian.trpcore.ui.timeline.addplan

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.databinding.ItemSmartCategoryBinding
import com.tripian.trpcore.domain.model.timeline.SmartCategory
import com.tripian.trpcore.R

/**
 * SmartCategoryAdapter
 * Adapter for 7-category grid in Smart Recommendations mode
 * iOS Reference: CategorySelection grid items
 */
class SmartCategoryAdapter(
    private val getLanguageForKey: (String) -> String,
    private val onCategoryClicked: (SmartCategory) -> Unit
) : RecyclerView.Adapter<SmartCategoryAdapter.CategoryViewHolder>() {

    private val categories = SmartCategory.values().toList()
    private val selectedCategories = mutableSetOf<SmartCategory>()
    private val spanCount = 3

    fun updateSelectedCategories(selected: List<SmartCategory>) {
        selectedCategories.clear()
        selectedCategories.addAll(selected)
        notifyDataSetChanged()
    }

    fun isCategorySelected(category: SmartCategory): Boolean {
        return selectedCategories.contains(category)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemSmartCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val isLastRowSingleItem = isLastRowWithSingleItem(position)
        holder.bind(categories[position], isLastRowSingleItem)
    }

    override fun getItemCount() = categories.size

    private fun isLastRowWithSingleItem(position: Int): Boolean {
        val itemsInLastRow = categories.size % spanCount
        return itemsInLastRow == 1 && position == categories.size - 1
    }

    inner class CategoryViewHolder(
        private val binding: ItemSmartCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: SmartCategory, isLastRowSingleItem: Boolean) {
            // Set icon
            binding.ivIcon.setImageResource(category.iconRes)

            // Set title
            binding.tvTitle.text = getLanguageForKey(category.titleKey)

            // Set selection state
            val isSelected = selectedCategories.contains(category)
            binding.llContent.isSelected = isSelected

            // Update visual state
            updateSelectionState(isSelected)

            // Adjust width for last row single item (centered, 1/3 width)
            val contentParams = binding.llContent.layoutParams as FrameLayout.LayoutParams
            if (isLastRowSingleItem) {
                // When spanning 3 columns but we want same visual width as other items
                // Set width to WRAP_CONTENT and let the FrameLayout center it
                contentParams.width = binding.root.context.resources.displayMetrics.widthPixels / 3 -
                    binding.root.context.resources.getDimensionPixelSize(R.dimen.category_grid_spacing) * 2
                contentParams.gravity = android.view.Gravity.CENTER
            } else {
                contentParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                contentParams.gravity = android.view.Gravity.NO_GRAVITY
            }
            binding.llContent.layoutParams = contentParams

            // Click listener
            binding.llContent.setOnClickListener {
                onCategoryClicked(category)
            }
        }

        private fun updateSelectionState(isSelected: Boolean) {
            // Only update selection state for border change
            // Icon and text colors stay the same (text_primary)
            binding.llContent.isSelected = isSelected
        }
    }
}
