package com.tripian.trpcore.ui.timeline.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tripian.trpcore.R
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.databinding.ItemTimelineReservedActivityBinding
import com.tripian.trpcore.domain.model.timeline.TimelineDisplayItem
import com.tripian.trpcore.domain.model.timeline.toDate
import com.tripian.trpcore.util.FormatUtils
import com.tripian.trpcore.util.LanguageConst
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * ReservedActivityVH
 * ViewHolder for reserved activities (not yet confirmed)
 * Shows rating, duration, price, and reservation button
 */
class ReservedActivityVH(
    private val binding: ItemTimelineReservedActivityBinding
) : RecyclerView.ViewHolder(binding.root) {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
    private val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())

    fun bind(
        item: TimelineDisplayItem.BookedActivity,
        onItemClick: (TimelineDisplayItem) -> Unit,
        onDeleteClick: (TimelineDisplayItem, Int?) -> Unit,
        onReservationClick: (TimelineDisplayItem.BookedActivity) -> Unit
    ) {
        // Order badge
        binding.tvOrder.text = item.order.toString()

        // Title - semibold 16px
        binding.tvTitle.text = item.title

        // Time (startTime - endTime format)
        val startTime = item.startDateTime?.toDate()
        val endTime = item.endDateTime?.toDate()
        if (startTime != null && endTime != null) {
            binding.tvTime.text = "${timeFormat.format(startTime)} - ${timeFormat.format(endTime)}"
            binding.tvTime.visibility = View.VISIBLE
        } else if (startTime != null) {
            binding.tvTime.text = timeFormat.format(startTime)
            binding.tvTime.visibility = View.VISIBLE
        } else {
            binding.tvTime.visibility = View.GONE
        }

        // Image - 80x80, 4dp corner radius
        item.imageUrl?.let { url ->
            Glide.with(binding.ivImage)
                .load(url)
                .centerCrop()
                .placeholder(R.drawable.bg_place_holder_image)
                .into(binding.ivImage)
        } ?: run {
            binding.ivImage.setImageResource(R.drawable.bg_place_holder_image)
        }

        // Rating Row - from additionalData (hide if no data)
        val rating = item.segment.additionalData?.rating
        val reviewCount = item.segment.additionalData?.reviewCount
        if (rating != null && rating > 0) {
            binding.tvRating.text = String.format(Locale.getDefault(), "%.1f", rating)

            if (reviewCount != null && reviewCount > 0) {
                val opinionsText = TRPCore.core.miscRepository.getLanguageValueForKey(LanguageConst.ADD_PLAN_OPINIONS)
                binding.tvReviewCount.text = "${numberFormat.format(reviewCount)} $opinionsText"
                binding.tvReviewCount.visibility = View.VISIBLE
            } else {
                binding.tvReviewCount.visibility = View.GONE
            }

            binding.llRating.visibility = View.VISIBLE
        } else {
            binding.llRating.visibility = View.GONE
        }

        // Duration Row - from additionalData
        val duration = item.duration
        if (duration != null && duration > 0) {
            val durationText = formatDuration(duration)
            binding.tvDuration.text = durationText
            binding.llDuration.visibility = View.VISIBLE
        } else {
            binding.llDuration.visibility = View.GONE
        }

        // Cancellation text - default "Free cancellation"
        val cancellationText = item.cancellation ?: TRPCore.core.miscRepository.getLanguageValueForKey(LanguageConst.ADD_PLAN_FREE_CANCELLATION)
        binding.tvCancellation.text = cancellationText

        // Price - "From €XX" format
        val price = item.price
        val currency = item.currency ?: "EUR"
        if (price != null && price > 0) {
            binding.tvFromLabel.text = TRPCore.core.miscRepository.getLanguageValueForKey(LanguageConst.FROM) + " "
            binding.tvPrice.text = FormatUtils.formatPriceWithCurrency(price, currency)
            binding.llPriceRow.visibility = View.VISIBLE
        } else {
            binding.llPriceRow.visibility = View.GONE
        }

        // Reservation button text - use language service
        binding.btnReservation.text = TRPCore.core.miscRepository.getLanguageValueForKey(LanguageConst.RESERVATION)

        // Click listeners
        binding.root.setOnClickListener {
            onItemClick(item)
        }

        binding.btnDelete.setOnClickListener {
            onDeleteClick(item, item.segmentIndex)
        }

        binding.btnReservation.setOnClickListener {
            onReservationClick(item)
        }
    }

    /**
     * Format duration from minutes to hours/minutes display
     * Example: 210 minutes -> "3h 30m"
     */
    private fun formatDuration(durationInMinutes: Double): String {
        val totalMinutes = durationInMinutes.toInt()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            else -> "${minutes}m"
        }
    }
}
