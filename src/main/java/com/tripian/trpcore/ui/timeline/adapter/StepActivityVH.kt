package com.tripian.trpcore.ui.timeline.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tripian.one.api.timeline.model.TimelineStep
import com.tripian.trpcore.R
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.databinding.ItemTimelineStepActivityBinding
import com.tripian.trpcore.domain.model.timeline.toDate
import com.tripian.trpcore.util.FormatUtils
import com.tripian.trpcore.util.LanguageConst
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * StepActivityVH
 * ViewHolder for Activity type steps within Recommendations
 * Shows: order-time, image, title, rating, duration, price, cancellation, reservation button
 */
class StepActivityVH(
    private val binding: ItemTimelineStepActivityBinding
) : RecyclerView.ViewHolder(binding.root) {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
    // Turkish locale for formatting: comma as decimal separator, dot as thousand separator
    private val turkishLocale = Locale("tr", "TR")
    private val reviewCountFormat = NumberFormat.getNumberInstance(turkishLocale)

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
        step: TimelineStep,
        order: Int,
        onStepClick: ((TimelineStep) -> Unit)?,
        onDeleteClick: ((TimelineStep) -> Unit)?,
        onReservationClick: ((TimelineStep) -> Unit)?
    ) {
        val poi = step.poi

        // Order badge
        binding.tvOrder.text = order.toString()

        // Time (startTime - endTime format)
        val startTime = step.startDateTimes?.toDate()
        val endTime = step.endDateTimes?.toDate()
        if (startTime != null && endTime != null) {
            binding.tvTime.text = "${timeFormat.format(startTime)} - ${timeFormat.format(endTime)}"
            binding.tvTime.visibility = View.VISIBLE
        } else if (startTime != null) {
            binding.tvTime.text = timeFormat.format(startTime)
            binding.tvTime.visibility = View.VISIBLE
        } else {
            binding.tvTime.visibility = View.GONE
        }

        // Title
        binding.tvTitle.text = poi?.name ?: ""

        // Image - 80x80, 4dp corner radius
        poi?.image?.url?.let { url ->
            Glide.with(binding.ivImage)
                .load(url)
                .centerCrop()
                .placeholder(R.drawable.bg_place_holder_image)
                .into(binding.ivImage)
        } ?: run {
            binding.ivImage.setImageResource(R.drawable.bg_place_holder_image)
        }

        // Rating Row - from poi.rating and poi.ratingCount
        // Rating uses comma as decimal separator (4,2), reviewCount uses dot as thousand separator (49.565)
        val rating = poi?.rating
        val reviewCount = poi?.ratingCount
        if (rating != null && rating > 0) {
            binding.tvRating.text = String.format(turkishLocale, "%.1f", rating)

            if (reviewCount != null && reviewCount > 0) {
                val opinionsText = getLanguage(LanguageConst.ADD_PLAN_OPINIONS)
                binding.tvReviewCount.text = "${reviewCountFormat.format(reviewCount)} $opinionsText"
                binding.tvReviewCount.visibility = View.VISIBLE
            } else {
                binding.tvReviewCount.visibility = View.GONE
            }

            binding.llRating.visibility = View.VISIBLE
        } else {
            binding.llRating.visibility = View.GONE
        }

        // Duration Row - from poi.duration (in minutes, convert to hours format)
        val duration = poi?.duration
        if (duration != null && duration > 0) {
            binding.tvDuration.text = formatDuration(duration)
            binding.llDuration.visibility = View.VISIBLE
        } else {
            binding.llDuration.visibility = View.GONE
        }

        // Cancellation - from poi.additionalData.cancellation or default "Free cancellation"
        val cancellation = poi?.additionalData?.cancellation
            ?: getLanguage(LanguageConst.ADD_PLAN_FREE_CANCELLATION)
        binding.tvCancellation.text = cancellation

        // Price - from poi.additionalData.price and poi.additionalData.currency
        val price = poi?.additionalData?.price ?: poi?.price?.toDouble()
        val currency = poi?.additionalData?.currency ?: "EUR"
        if (price != null && price > 0) {
            binding.tvFromLabel.text = getLanguage(LanguageConst.FROM) + " "
            binding.tvPrice.text = FormatUtils.formatPriceWithCurrency(price, currency)
            binding.llPriceRow.visibility = View.VISIBLE
        } else {
            binding.llPriceRow.visibility = View.GONE
        }

        // Reservation button text
        binding.btnReservation.text = getLanguage(LanguageConst.RESERVATION)

        // Click listeners
        binding.root.setOnClickListener {
            onStepClick?.invoke(step)
        }

        binding.btnDelete.setOnClickListener {
            onDeleteClick?.invoke(step)
        }

        binding.btnReservation.setOnClickListener {
            onReservationClick?.invoke(step)
        }
    }

    /**
     * Format duration from minutes to hours/minutes display
     * Example: 210 minutes -> "3h 30m"
     */
    private fun formatDuration(durationInMinutes: Int): String {
        val hours = durationInMinutes / 60
        val minutes = durationInMinutes % 60

        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            else -> "${minutes}m"
        }
    }
}
