package com.tripian.trpcore.ui.timeline.adapter

import android.graphics.Typeface
import android.view.View
import androidx.recyclerview.widget.RecyclerView
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
        onChangeTimeClick: (TimelineDisplayItem.BookedActivity) -> Unit,
        onDeleteClick: (TimelineDisplayItem, Int?) -> Unit,
        onReservationClick: (TimelineDisplayItem.BookedActivity) -> Unit
    ) {
        // Order badge — Theme 14: order 0 (or negative) renders as U+2212.
        binding.tvOrder.text = if (item.order <= 0)
            com.tripian.trpcore.util.extensions.MINUS_SIGN
        else item.order.toString()

        // Badge styling precedence (Theme 7): expired > conflict > normal.
        // Expired wins over conflict so the red "Not available" cue is never
        // hidden by a time-overlap badge on the same item.
        when {
            item.isAvailabilityExpired -> {
                binding.orderTimeContainer
                    .setBackgroundResource(R.drawable.trp_bg_order_time_container_expired)
                binding.tvOrder.setBackgroundResource(R.drawable.trp_bg_step_order_expired)
            }
            item.hasConflict -> {
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

        // Title - semibold 16px
        binding.tvTitle.text = item.title

        // Activity badge — always shown on ReservedActivity cells.
        binding.tvActivityBadge.text =
            TRPCore.core.miscRepository.getLanguageValueForKey(LanguageConst.TIMELINE_LABEL_ACTIVITY_BADGE)

        // Time (startTime - endTime format)
        // Status suffix precedence (Theme 7): "Not available" > "Time Overlap" > none
        val startTime = item.startDateTime?.toDate()
        val endTime = item.endDateTime?.toDate()
        if (startTime != null && endTime != null) {
            val timeText = "${timeFormat.format(startTime)} - ${timeFormat.format(endTime)}"
            binding.tvTime.text = when {
                item.isAvailabilityExpired -> {
                    val label = getLanguage(LanguageConst.TIMELINE_LABEL_NOT_AVAILABLE)
                        .ifBlank { "Not available" }
                    TimeOverlapTextBuilder.build(
                        binding.tvTime.context,
                        timeText,
                        label,
                        TimeBadgeStatus.EXPIRED
                    )
                }
                item.showTimeOverlapText -> {
                    val label = getLanguage(LanguageConst.TIME_OVERLAP)
                    TimeOverlapTextBuilder.build(binding.tvTime.context, timeText, label)
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

        // Image - 80x80, 4dp corner radius (Nexus logo fallback via shared binder).
        TimelineCellBinder.loadActivityImage(binding.ivImage, item.imageUrl)

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
            binding.tvDuration.text = FormatUtils.formatDuration(duration)
            binding.llDuration.visibility = View.VISIBLE
        } else {
            binding.llDuration.visibility = View.GONE
        }

        // No-Location badge (Theme 6).
        TimelineCellBinder.bindNoLocationBadge(binding.noLocationBadge, item.isNoLocation)

        // Cancellation text - default "Free cancellation"
        val cancellationText = item.cancellation?.takeIf { it.isNotBlank() }
            ?: TRPCore.core.miscRepository.getLanguageValueForKey(LanguageConst.ADD_PLAN_FREE_CANCELLATION)
        binding.tvCancellation.text = cancellationText

        // Price - "From €XX" format or "FREE" when price is 0
        val price = item.price
        val currency = item.currency ?: "EUR"
        when {
            price == null -> {
                binding.llPriceRow.visibility = View.GONE
            }
            price == 0.0 -> {
                binding.tvFromLabel.visibility = View.GONE
                binding.tvPrice.text = TRPCore.core.miscRepository.getLanguageValueForKey(LanguageConst.FREE)
                binding.tvPrice.setTypeface(null, Typeface.BOLD)
                binding.llPriceRow.visibility = View.VISIBLE
            }
            else -> {
                binding.tvFromLabel.visibility = View.VISIBLE
                binding.tvFromLabel.text = TRPCore.core.miscRepository.getLanguageValueForKey(LanguageConst.FROM) + " "
                binding.tvPrice.text = FormatUtils.formatPriceWithCurrency(price, currency)
                binding.llPriceRow.visibility = View.VISIBLE
            }
        }

        // Reservation button text - use language service
        binding.btnReservation.text = TRPCore.core.miscRepository.getLanguageValueForKey(LanguageConst.RESERVATION)

        // Click listeners
        binding.root.setOnClickListener {
            onItemClick(item)
        }

        binding.btnChangeTime.setOnClickListener {
            onChangeTimeClick(item)
        }

        binding.btnDelete.setOnClickListener {
            onDeleteClick(item, item.segmentIndex)
        }

        binding.btnReservation.setOnClickListener {
            onReservationClick(item)
        }
    }
}
