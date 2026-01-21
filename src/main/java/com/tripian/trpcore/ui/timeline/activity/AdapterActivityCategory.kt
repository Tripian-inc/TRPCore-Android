package com.tripian.trpcore.ui.timeline.activity

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.databinding.ItemActivityCategoryBinding

/**
 * AdapterActivityCategory
 * Horizontal RecyclerView adapter for activity category with icon + text
 * Supports multiple selection (except "All" which resets all others)
 * iOS Reference: CategoryFilterCell
 */
class AdapterActivityCategory(
    private val categories: List<ActivityCategoryItem>,
    private val getLanguage: (String) -> String,
    private val onSelectionChanged: (Set<Int>) -> Unit
) : RecyclerView.Adapter<AdapterActivityCategory.CategoryViewHolder>() {

    private val selectedIndices = mutableSetOf(0) // "All" is selected by default

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemActivityCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position], selectedIndices.contains(position))
    }

    override fun getItemCount(): Int = categories.size

    fun toggleSelection(index: Int) {
        if (index == 0) {
            // "All" selected - clear all others
            val previousSelected = selectedIndices.toSet()
            selectedIndices.clear()
            selectedIndices.add(0)
            // Notify all previously selected items
            previousSelected.forEach { notifyItemChanged(it) }
            notifyItemChanged(0)
        } else {
            // Other category selected
            if (selectedIndices.contains(0)) {
                selectedIndices.remove(0)
                notifyItemChanged(0)
            }

            if (selectedIndices.contains(index)) {
                selectedIndices.remove(index)
            } else {
                selectedIndices.add(index)
            }
            notifyItemChanged(index)

            // If nothing is selected, select "All"
            if (selectedIndices.isEmpty()) {
                selectedIndices.add(0)
                notifyItemChanged(0)
            }
        }
        onSelectionChanged(selectedIndices.toSet())
    }

    fun setSelectedIndices(indices: Set<Int>) {
        val previousSelected = selectedIndices.toSet()
        selectedIndices.clear()
        selectedIndices.addAll(indices)

        // Notify changed items
        (previousSelected + indices).forEach { notifyItemChanged(it) }
    }

    fun getSelectedIndices(): Set<Int> = selectedIndices.toSet()

    fun getSelectedCategories(): List<ActivityCategoryItem> {
        return selectedIndices.mapNotNull { categories.getOrNull(it) }
    }

    inner class CategoryViewHolder(
        private val binding: ItemActivityCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: ActivityCategoryItem, isSelected: Boolean) {
            // Set icon
            binding.ivCategoryIcon.setImageResource(category.iconRes)

            // Set text using language key
            binding.tvCategoryName.text = getLanguage(category.languageKey)

            // Set selection state (for tint selector)
            binding.ivCategoryIcon.isSelected = isSelected
            binding.ivCategoryIcon.isActivated = isSelected
            binding.tvCategoryName.isSelected = isSelected
            binding.tvCategoryName.isActivated = isSelected

            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    toggleSelection(position)
                }
            }
        }
    }
}

/**
 * Activity Category model for UI
 * @param id Unique identifier for the category
 * @param languageKey Language key for localized name (from LanguageConst)
 * @param iconRes Drawable resource for category icon
 * @param keywords Keywords for API filtering (null for "All")
 */
data class ActivityCategoryItem(
    val id: String,
    val languageKey: String,
    @DrawableRes val iconRes: Int,
    val keywords: String?
)
