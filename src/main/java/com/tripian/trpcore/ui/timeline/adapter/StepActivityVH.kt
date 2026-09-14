package com.tripian.trpcore.ui.timeline.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
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

    /** Yields comma as decimal separator, dot as thousand separator. */
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

    /**
     * Binds the step. Badge/status precedence: expired > conflict/overlap > normal.
     * A null or (0.0, 0.0) POI coordinate is the backend's no-location placeholder
     * (online tours / audio guides) and shows the no-location badge.
     */
    fun bind(
        step: TimelineStep,
        order: Int,
        onStepClick: ((TimelineStep) -> Unit)?,
        onChangeTimeClick: ((TimelineStep) -> Unit)?,
        onDeleteClick: ((TimelineStep) -> Unit)?,
        onReservationClick: ((TimelineStep) -> Unit)?,
        hasConflict: Boolean = false,
        showTimeOverlapText: Boolean = false,
        isAvailabilityExpired: Boolean = false
    ) {
        val poi = step.poi

        binding.tvOrder.text = order.toString()

        when {
            isAvailabilityExpired -> {
                binding.orderTimeContainer
                    .setBackgroundResource(R.drawable.trp_bg_order_time_container_expired)
                binding.tvOrder.setBackgroundResource(R.drawable.trp_bg_step_order_expired)
            }
            hasConflict -> {
                binding.orderTimeContainer
                    .setBackgroundResource(R.drawable.trp_bg_order_time_container_conflict)
                binding.tvOrder.setBackgroundResource(R.drawable.trp_bg_step_order_conflict)
            }
            else -> {
                binding.orderTimeContainer
                    .setBackgroundResource(R.drawable.trp_bg_order_time_container)
                binding.tvOrder.setBackgroundResource(R.drawable.trp_bg_step_order_new)
            }
        }

        val startTime = step.startDateTimes?.toDate()
        val endTime = step.endDateTimes?.toDate()
        if (startTime != null && endTime != null) {
            val timeText = "${timeFormat.format(startTime)} - ${timeFormat.format(endTime)}"
            binding.tvTime.text = when {
                isAvailabilityExpired -> {
                    val label = getLanguage(LanguageConst.TIMELINE_LABEL_NOT_AVAILABLE)
                        .ifBlank { "Not available" }
                    TimeOverlapTextBuilder.build(
                        binding.tvTime.context,
                        timeText,
                        label,
                        TimeBadgeStatus.EXPIRED
                    )
                }
                showTimeOverlapText -> {
                    val overlapText = getLanguage(LanguageConst.TIME_OVERLAP)
                    TimeOverlapTextBuilder.build(binding.tvTime.context, timeText, overlapText)
                }
                else -> timeText
            }
            binding.tvTime.visibility = View.VISIBLE
        } else if (startTime != null) {
            binding.tvTime.text = timeFormat.format(startTime)
            binding.tvTime.visibility = View.VISIBLE
        } else {
            binding.tvTime.visibility = View.GONE
        }

        binding.tvTitle.text = poi?.name ?: ""

        if (step.stepType == "activity") {
            binding.tvActivityBadge.text = getLanguage(LanguageConst.TIMELINE_LABEL_ACTIVITY_BADGE)
            binding.tvActivityBadge.visibility = View.VISIBLE
        } else {
            binding.tvActivityBadge.visibility = View.GONE
        }

        val coord = poi?.coordinate
        val isNoLocation = coord == null || (coord.lat == 0.0 && coord.lng == 0.0)
        TimelineCellBinder.bindNoLocationBadge(binding.noLocationBadge, isNoLocation)

        TimelineCellBinder.loadPoiImage(binding.ivImage, poi?.image?.url)

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

        val duration = poi?.duration
        if (duration != null && duration > 0) {
            binding.tvDuration.text = FormatUtils.formatDuration(duration)
            binding.tvDuration.visibility = View.VISIBLE
        } else {
            binding.tvDuration.visibility = View.GONE
        }

        val cancellation = poi?.additionalData?.cancellation
            ?: getLanguage(LanguageConst.ADD_PLAN_FREE_CANCELLATION)
        binding.tvCancellation.text = cancellation

        TimelineCellBinder.bindPrice(
            priceRow = binding.llPriceRow,
            fromLabel = binding.tvFromLabel,
            priceView = binding.tvPrice,
            price = poi?.additionalData?.price ?: poi?.price?.toDouble(),
            currency = poi?.additionalData?.currency
        )

        binding.btnReservation.text = getLanguage(LanguageConst.RESERVATION)

        binding.root.setOnClickListener {
            onStepClick?.invoke(step)
        }

        binding.btnChangeTime.setOnClickListener {
            onChangeTimeClick?.invoke(step)
        }

        binding.btnDelete.setOnClickListener {
            onDeleteClick?.invoke(step)
        }

        binding.btnReservation.setOnClickListener {
            onReservationClick?.invoke(step)
        }
    }
}
