package com.tripian.trpcore.ui.timeline.adapter

import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.R
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.databinding.ItemTimelineStepRouteSeparatorBinding
import com.tripian.trpcore.domain.model.timeline.StepRouteInfo
import com.tripian.trpcore.util.LanguageConst

/**
 * StepRouteSeparatorVH
 * ViewHolder for displaying route information between steps
 * Shows the transport-mode icon and duration/distance text with separator line
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
        binding.ivTransportIcon.setImageResource(routeInfo.transportIconRes)

        val distanceFormat = TRPCore.core.miscRepository.getLanguageValueForKey(LanguageConst.TIMELINE_FORMAT_DISTANCE)
            .takeIf { it.isNotEmpty() } ?: "%d min (%@ km)"
        binding.tvRouteInfo.text = routeInfo.formatWithTemplate(distanceFormat)
    }
}

/** Icon matching the routing profile the leg was calculated with. */
internal val StepRouteInfo.transportIconRes: Int
    @DrawableRes get() = if (isWalking) R.drawable.trp_icon_distance else R.drawable.trp_icon_distance_car
