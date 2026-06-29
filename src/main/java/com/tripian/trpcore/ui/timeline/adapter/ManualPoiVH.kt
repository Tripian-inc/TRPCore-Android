package com.tripian.trpcore.ui.timeline.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.R
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.databinding.ItemTimelineManualPoiBinding
import com.tripian.trpcore.domain.model.timeline.TimelineDisplayItem
import com.tripian.trpcore.util.LanguageConst
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * ManualPoiVH
 * ViewHolder for manually added POI
 */
class ManualPoiVH(
    private val binding: ItemTimelineManualPoiBinding
) : RecyclerView.ViewHolder(binding.root) {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.US)

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
        item: TimelineDisplayItem.ManualPoi,
        onItemClick: (TimelineDisplayItem) -> Unit,
        onChangeTimeClick: ((TimelineDisplayItem.ManualPoi) -> Unit)? = null,
        onDeleteClick: (TimelineDisplayItem, Int?) -> Unit
    ) {
        // Order badge — Theme 14: order 0 (or negative) renders as U+2212.
        binding.tvOrder.text = if (item.order <= 0)
            com.tripian.trpcore.util.extensions.MINUS_SIGN
        else item.order.toString()

        // Apply conflict styling
        if (item.hasConflict) {
            binding.orderTimeContainer.setBackgroundResource(R.drawable.trp_bg_order_time_container_conflict)
            binding.tvOrder.setBackgroundResource(R.drawable.trp_bg_step_order_conflict)
        } else {
            binding.orderTimeContainer.setBackgroundResource(R.drawable.trp_bg_order_time_container)
            binding.tvOrder.setBackgroundResource(R.drawable.trp_bg_step_order_new)
        }

        // Title - semibold 16px primaryText
        binding.tvTitle.text = item.title

        // Time (startTime - endTime format)
        val startTime = item.startTime
        val endTime = item.endTime
        if (startTime != null && endTime != null) {
            val timeText = "${timeFormat.format(startTime)} - ${timeFormat.format(endTime)}"
            binding.tvTime.text = if (item.showTimeOverlapText) {
                val overlapText = getLanguage(LanguageConst.TIME_OVERLAP)
                TimeOverlapTextBuilder.build(binding.tvTime.context, timeText, overlapText)
            } else {
                timeText
            }
            binding.tvTime.visibility = View.VISIBLE
        } else if (startTime != null) {
            binding.tvTime.text = timeFormat.format(startTime)
            binding.tvTime.visibility = View.VISIBLE
        } else {
            binding.tvTime.visibility = View.GONE
        }

        // Image - 80x80, 4dp corner radius
        TimelineCellBinder.loadPoiImage(binding.ivImage, item.imageUrl)

        // Manual POIs never show rating — the user added these themselves and
        // the rating is not meaningful in that context.
        binding.llRating.visibility = View.GONE

        // Category Badge - same style as confirmed badge (green bg, green text)
        item.categoryName?.let { category ->
            binding.tvCategory.text = category
            binding.tvCategory.visibility = View.VISIBLE
        } ?: run {
            binding.tvCategory.visibility = View.GONE
        }

        // Click listeners
        binding.root.setOnClickListener {
            onItemClick(item)
        }

        // Change Time button
        binding.btnChangeTime.setOnClickListener {
            onChangeTimeClick?.invoke(item)
        }

        // Delete/Remove Step button
        binding.btnDelete.setOnClickListener {
            onDeleteClick(item, item.segmentIndex)
        }
    }
}
