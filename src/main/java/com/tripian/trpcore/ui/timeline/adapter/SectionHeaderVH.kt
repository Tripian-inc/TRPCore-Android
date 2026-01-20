package com.tripian.trpcore.ui.timeline.adapter

import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.databinding.ItemTimelineSectionHeaderBinding
import com.tripian.trpcore.domain.model.timeline.TimelineDisplayItem

/**
 * SectionHeaderVH
 * Şehir gruplaması için section header
 */
class SectionHeaderVH(
    private val binding: ItemTimelineSectionHeaderBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: TimelineDisplayItem.SectionHeader) {
        binding.tvCityName.text = item.cityName
    }
}
