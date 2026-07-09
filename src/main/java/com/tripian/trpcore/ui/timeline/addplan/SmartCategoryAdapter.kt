package com.tripian.trpcore.ui.timeline.addplan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.R
import com.tripian.trpcore.databinding.ItemSmartCategoryBinding
import com.tripian.trpcore.domain.model.timeline.SmartCategory

/**
 * SmartCategoryAdapter
 * Adapter for 7-category grid in Smart Recommendations mode
 * iOS Reference: CategorySelection grid items
 */
class SmartCategoryAdapter(
    private val getLanguageForKey: (String) -> String,
    private val onCategoryClicked: (SmartCategory) -> Unit
) : RecyclerView.Adapter<SmartCategoryAdapter.CategoryViewHolder>() {

    private val categories = SmartCategory.entries
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
            binding.ivIcon.setImageResource(category.iconRes)

            binding.tvTitle.text = getLanguageForKey(category.titleKey)

            val isSelected = selectedCategories.contains(category)
            binding.llContent.isSelected = isSelected

            updateSelectionState(isSelected)

            binding.llContent.setOnClickListener {
                onCategoryClicked(category)
            }
        }

        private fun updateSelectionState(isSelected: Boolean) {
            binding.llContent.isSelected = isSelected
            val iconColor = ContextCompat.getColor(
                binding.ivIcon.context,
                if (isSelected) R.color.trp_text_primary else R.color.trp_fgWeak
            )
            binding.ivIcon.setColorFilter(iconColor)
        }
    }
}
