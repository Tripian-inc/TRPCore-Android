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

    /** Binds the cell. Manual POIs never show a rating row. */
    fun bind(
        item: TimelineDisplayItem.ManualPoi,
        onItemClick: (TimelineDisplayItem) -> Unit,
        onChangeTimeClick: ((TimelineDisplayItem.ManualPoi) -> Unit)? = null,
        onDeleteClick: (TimelineDisplayItem, Int?) -> Unit
    ) {
        binding.tvOrder.text = if (item.order <= 0)
            com.tripian.trpcore.util.extensions.MINUS_SIGN
        else item.order.toString()

        if (item.hasConflict) {
            binding.orderTimeContainer.setBackgroundResource(R.drawable.trp_bg_order_time_container_conflict)
            binding.tvOrder.setBackgroundResource(R.drawable.trp_bg_step_order_conflict)
        } else {
            binding.orderTimeContainer.setBackgroundResource(R.drawable.trp_bg_order_time_container)
            binding.tvOrder.setBackgroundResource(R.drawable.trp_bg_step_order_new)
        }

        binding.tvTitle.text = item.title

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

        TimelineCellBinder.loadPoiImage(binding.ivImage, item.imageUrl)

        binding.llRating.visibility = View.GONE

        item.categoryName?.let { category ->
            binding.tvCategory.text = category
            binding.tvCategory.visibility = View.VISIBLE
        } ?: run {
            binding.tvCategory.visibility = View.GONE
        }

        binding.root.setOnClickListener {
            onItemClick(item)
        }

        binding.btnChangeTime.setOnClickListener {
            onChangeTimeClick?.invoke(item)
        }

        binding.btnDelete.setOnClickListener {
            onDeleteClick(item, item.segmentIndex)
        }
    }
}
