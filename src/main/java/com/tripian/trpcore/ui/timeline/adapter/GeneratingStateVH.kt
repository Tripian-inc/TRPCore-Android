package com.tripian.trpcore.ui.timeline.adapter

import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.databinding.ItemTimelineGeneratingBinding
import com.tripian.trpcore.domain.model.timeline.TimelineDisplayItem

/**
 * GeneratingStateVH
 * Plan oluşturuluyor gösterimi
 */
class GeneratingStateVH(
    private val binding: ItemTimelineGeneratingBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: TimelineDisplayItem.GeneratingState) {
        binding.tvMessage.text = item.message
    }
}
