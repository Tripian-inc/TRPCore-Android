package com.tripian.trpcore.ui.timeline.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.databinding.ItemTimelineSectionHeaderBinding
import com.tripian.trpcore.domain.model.timeline.TimelineDisplayItem

/**
 * SectionHeaderVH
 * Section header for city grouping. Display-only — no collapse/expand.
 * Collapse interactions live only on the Recommendations cards.
 */
class SectionHeaderVH(
    private val binding: ItemTimelineSectionHeaderBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(
        item: TimelineDisplayItem.SectionHeader,
        isCollapsed: Boolean = false,
        onToggle: ((cityId: Int) -> Unit)? = null
    ) {
        binding.tvCityName.text = item.cityName
        binding.ivCollapseChevron.visibility = View.GONE
        binding.root.setOnClickListener(null)
        binding.root.isClickable = false
    }
}
