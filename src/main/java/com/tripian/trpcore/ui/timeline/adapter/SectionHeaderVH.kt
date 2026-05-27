package com.tripian.trpcore.ui.timeline.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.databinding.ItemTimelineSectionHeaderBinding
import com.tripian.trpcore.domain.model.timeline.TimelineDisplayItem

/**
 * SectionHeaderVH
 * Section header for city grouping. Theme 12: tappable to collapse/expand its
 * section — the chevron rotates between 0° (expanded, ▼) and -90° (collapsed, ▶).
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
        val cityId = item.city?.id
        if (cityId != null && cityId != 0 && onToggle != null) {
            binding.ivCollapseChevron.visibility = View.VISIBLE
            // Rotate the down-arrow to point right when collapsed.
            binding.ivCollapseChevron.rotation = if (isCollapsed) -90f else 0f
            binding.root.setOnClickListener { onToggle(cityId) }
        } else {
            binding.ivCollapseChevron.visibility = View.GONE
            binding.root.setOnClickListener(null)
        }
    }
}
