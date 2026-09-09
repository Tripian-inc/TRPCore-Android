package com.tripian.trpcore.ui.timeline.adapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.R
import com.tripian.trpcore.databinding.ItemTimelineStartingPointBinding
import com.tripian.trpcore.domain.model.timeline.TimelineDisplayItem

/**
 * StartingPointVH
 * Single-row starting point of the flat timeline (accommodation or city centre),
 * drawn as an outlined pill.
 */
class StartingPointVH(
    private val binding: ItemTimelineStartingPointBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: TimelineDisplayItem.StartingPoint) {
        binding.tvStartingPointName.text = item.name
        binding.startingPointContainer.post { applyPillBackground() }
    }

    private fun applyPillBackground() {
        val container = binding.startingPointContainer
        val height = container.height
        if (height <= 0) return
        val strokeWidth = (container.context.resources.displayMetrics.density).toInt()
        container.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = height / 2f
            setStroke(strokeWidth, ContextCompat.getColor(container.context, R.color.trp_lineWeak))
            setColor(Color.TRANSPARENT)
        }
    }
}
