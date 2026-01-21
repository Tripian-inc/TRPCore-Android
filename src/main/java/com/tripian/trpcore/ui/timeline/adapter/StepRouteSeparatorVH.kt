package com.tripian.trpcore.ui.timeline.adapter

import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.R
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.databinding.ItemTimelineStepRouteSeparatorBinding
import com.tripian.trpcore.domain.model.timeline.StepRouteInfo
import com.tripian.trpcore.util.LanguageConst

/**
 * StepRouteSeparatorVH
 * ViewHolder for displaying route information between steps
 * Shows walking icon and duration/distance text with separator line
 */
class StepRouteSeparatorVH(
    private val binding: ItemTimelineStepRouteSeparatorBinding
) : RecyclerView.ViewHolder(binding.root) {

    /**
     * Bind route information to the view
     *
     * @param routeInfo The route information containing distance and duration
     */
    fun bind(routeInfo: StepRouteInfo) {
        // Set walking icon - always show walking icon as per design
        binding.ivTransportIcon.setImageResource(R.drawable.ic_itinerary_walk)

        val distanceFormat = TRPCore.core.miscRepository.getLanguageValueForKey(LanguageConst.TIMELINE_FORMAT_DISTANCE)
            .takeIf { it.isNotEmpty() } ?: "%d min (%@ km)"
        // Format route info text using the language format
        binding.tvRouteInfo.text = routeInfo.formatWithTemplate(distanceFormat)
    }
}
