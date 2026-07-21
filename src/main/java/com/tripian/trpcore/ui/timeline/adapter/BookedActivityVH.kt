package com.tripian.trpcore.ui.timeline.adapter

import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.R
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.databinding.ItemTimelineBookedActivityBinding
import com.tripian.trpcore.domain.model.timeline.TimelineDisplayItem
import com.tripian.trpcore.domain.model.timeline.toDate
import com.tripian.trpcore.util.LanguageConst
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * BookedActivityVH
 * Display for confirmed activity
 */
class BookedActivityVH(
    private val binding: ItemTimelineBookedActivityBinding
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
        item: TimelineDisplayItem.BookedActivity,
        onItemClick: (TimelineDisplayItem) -> Unit,
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

        val startTime = item.startDateTime?.toDate()
        val endTime = item.endDateTime?.toDate()
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

        TimelineCellBinder.loadActivityImage(binding.ivImage, item.imageUrl)

        binding.tvBadge.text = getLanguage(LanguageConst.CONFIRMED)
        binding.tvBadge.setBackgroundResource(R.drawable.trp_bg_confirmed_badge)
        binding.tvBadge.setTextColor(binding.root.context.getColor(R.color.trp_confirmed_badge_text))

        TimelineCellBinder.bindNoLocationBadge(binding.noLocationBadge, item.isNoLocation)

        val travelers = mutableListOf<String>()
        if (item.adults > 0) {
            val adultsText = getLanguage(LanguageConst.ADULTS)
            travelers.add("${item.adults} $adultsText")
        }
        if (item.children > 0) {
            val childrenText = getLanguage(LanguageConst.CHILDREN)
            travelers.add("${item.children} $childrenText")
        }
        if (travelers.isNotEmpty()) {
            binding.tvTravelers.text = travelers.joinToString(", ")
            binding.llTravelers.visibility = View.VISIBLE
        } else {
            binding.llTravelers.visibility = View.GONE
        }

        val cancellationText = item.cancellation?.takeIf { it.isNotBlank() }
            ?: getLanguage(LanguageConst.ADD_PLAN_FREE_CANCELLATION)
        binding.tvCancellation.text = cancellationText
        binding.tvCancellation.isVisible = cancellationText.isNotEmpty()

        binding.root.setOnClickListener {
            onItemClick(item)
        }
    }
}
