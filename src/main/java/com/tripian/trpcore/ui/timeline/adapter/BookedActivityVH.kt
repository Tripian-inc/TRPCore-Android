package com.tripian.trpcore.ui.timeline.adapter

import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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
        // Order badge
        binding.tvOrder.text = item.order.toString()

        // Apply conflict styling (no "Time Overlap" text for BookedActivity)
        if (item.hasConflict) {
            binding.orderTimeContainer.setBackgroundResource(R.drawable.bg_order_time_container_conflict)
            binding.tvOrder.setBackgroundResource(R.drawable.bg_step_order_conflict)
        } else {
            binding.orderTimeContainer.setBackgroundResource(R.drawable.bg_order_time_container)
            binding.tvOrder.setBackgroundResource(R.drawable.bg_step_order_new)
        }

        // Title
        binding.tvTitle.text = item.title

        // Time (startTime - endTime format)
        // Reserved activity CAN show "Time Overlap" text
        // Booked activity NEVER shows "Time Overlap" text
        val startTime = item.startDateTime?.toDate()
        val endTime = item.endDateTime?.toDate()
        if (startTime != null && endTime != null) {
            val timeText = "${timeFormat.format(startTime)} - ${timeFormat.format(endTime)}"
            if (item.showTimeOverlapText) {
                val overlapText = getLanguage(LanguageConst.TIME_OVERLAP)
                binding.tvTime.text = "$timeText $overlapText"
            } else {
                binding.tvTime.text = timeText
            }
            binding.tvTime.visibility = View.VISIBLE
        } else if (startTime != null) {
            binding.tvTime.text = timeFormat.format(startTime)
            binding.tvTime.visibility = View.VISIBLE
        } else {
            binding.tvTime.visibility = View.GONE
        }

        // Image
        item.imageUrl?.let { url ->
            Glide.with(binding.ivImage)
                .load(url)
                .centerCrop()
                .placeholder(R.drawable.bg_place_holder_image)
                .into(binding.ivImage)
        } ?: run {
            binding.ivImage.setImageResource(R.drawable.bg_place_holder_image)
        }

        // Badge - Confirmed style (green bg, green text)
        binding.tvBadge.text = getLanguage(LanguageConst.CONFIRMED)
        binding.tvBadge.setBackgroundResource(R.drawable.bg_confirmed_badge)
        binding.tvBadge.setTextColor(binding.root.context.getColor(R.color.trp_confirmed_badge_text))

        // Travelers - "X Adults, Y Children" format
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

        // Cancellation info - advantage color
        val cancellationText = item.cancellation ?: getLanguage(LanguageConst.ADD_PLAN_FREE_CANCELLATION)
        binding.tvCancellation.text = cancellationText
        binding.tvCancellation.isVisible = cancellationText.isNotEmpty()

        // Click listener
        binding.root.setOnClickListener {
            onItemClick(item)
        }
    }
}
