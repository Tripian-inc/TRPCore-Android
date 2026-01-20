package com.tripian.trpcore.ui.timeline.adapter

import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.databinding.ItemTimelineEmptyStateBinding
import com.tripian.trpcore.domain.model.timeline.TimelineDisplayItem
import com.tripian.trpcore.util.LanguageConst

/**
 * EmptyStateVH
 * Empty day display with Add Plans button
 */
class EmptyStateVH(
    private val binding: ItemTimelineEmptyStateBinding
) : RecyclerView.ViewHolder(binding.root) {

    /**
     * Helper function for localization
     */
    private fun getLanguage(key: String): String {
        return try {
            TRPCore.core.miscRepository.getLanguageValueForKey(key)
        } catch (e: Exception) {
            key
        }
    }

    fun bind(
        item: TimelineDisplayItem.EmptyState,
        onAddPlanClick: (() -> Unit)?
    ) {
        // Title - localized
        binding.tvTitle.text = getLanguage(LanguageConst.NO_PLANS_YET)

        // Description - localized
        binding.tvMessage.text = getLanguage(LanguageConst.NO_PLANS_DESCRIPTION)

        // Button text - localized
        binding.btnAddPlans.text = getLanguage("Add Plans")

        // Button click
        binding.btnAddPlans.setOnClickListener {
            onAddPlanClick?.invoke()
        }
    }
}
