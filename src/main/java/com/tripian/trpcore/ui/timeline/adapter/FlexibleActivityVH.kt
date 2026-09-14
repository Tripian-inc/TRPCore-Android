package com.tripian.trpcore.ui.timeline.adapter

import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.R
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.databinding.ItemTimelineFlexibleActivityBinding
import com.tripian.trpcore.domain.model.timeline.TimelineDisplayItem
import com.tripian.trpcore.util.FormatUtils
import com.tripian.trpcore.util.LanguageConst
import java.text.NumberFormat
import java.util.Locale

/**
 * ViewHolder for Flexible-Time Activities. Same layout as [ReservedActivityVH] but the
 * order-time container uses a dashed border and shows "Flexible entry / Check the
 * timetable" instead of a time range. Flexible items are excluded from conflict
 * detection, show no duration row, and hide the change-time button (their
 * 00:00–23:59 start/end are placeholders, not a real interval).
 */
class FlexibleActivityVH(
    private val binding: ItemTimelineFlexibleActivityBinding
) : RecyclerView.ViewHolder(binding.root) {

    /** When true, click handlers no-op and styles mute. */
    var isPastDayMode: Boolean = false

    private val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())

    private fun getLanguage(key: String): String = try {
        TRPCore.core.miscRepository.getLanguageValueForKey(key)
    } catch (e: Exception) {
        key
    }

    /**
     * Binds the cell. The flexible-title typeface is set on every bind because a
     * recycled holder may keep the expired state's medium weight.
     */
    fun bind(
        item: TimelineDisplayItem.FlexibleActivity,
        onItemClick: (TimelineDisplayItem) -> Unit,
        onChangeTimeClick: (TimelineDisplayItem.FlexibleActivity) -> Unit,
        onDeleteClick: (TimelineDisplayItem, Int?) -> Unit,
        onReservationClick: (TimelineDisplayItem.FlexibleActivity) -> Unit
    ) {
        if (item.isAvailabilityExpired) {
            val flexibleLabel = getLanguage(LanguageConst.TIMELINE_FLEXIBLE_TITLE)
                .ifBlank { "Flexible entry" }
            val notAvailableLabel = getLanguage(LanguageConst.TIMELINE_LABEL_NOT_AVAILABLE)
                .ifBlank { "Not available" }
            binding.orderTimeContainer
                .setBackgroundResource(R.drawable.trp_bg_order_time_container_expired)
            binding.tvOrder.setBackgroundResource(R.drawable.trp_bg_step_order_expired)
            binding.tvFlexibleTitle.typeface =
                androidx.core.content.res.ResourcesCompat.getFont(
                    binding.tvFlexibleTitle.context,
                    R.font.medium
                )
            binding.tvFlexibleTitle.text = TimeOverlapTextBuilder.build(
                binding.tvFlexibleTitle.context,
                timeText = flexibleLabel,
                statusLabel = notAvailableLabel,
                status = TimeBadgeStatus.EXPIRED
            )
            binding.tvFlexibleSubtitle.visibility = View.GONE
            applyOrderRowStyle(muted = isPastDayMode)
        } else {
            binding.orderTimeContainer
                .setBackgroundResource(R.drawable.trp_bg_order_time_container_flexible)
            binding.tvOrder.setBackgroundResource(R.drawable.trp_bg_step_order_new)
            binding.tvFlexibleTitle.typeface =
                androidx.core.content.res.ResourcesCompat.getFont(
                    binding.tvFlexibleTitle.context,
                    R.font.semibold
                )
            binding.tvFlexibleTitle.text =
                getLanguage(LanguageConst.TIMELINE_FLEXIBLE_TITLE).ifBlank { "Flexible entry" }
            binding.tvFlexibleSubtitle.text =
                getLanguage(LanguageConst.TIMELINE_FLEXIBLE_SUBTITLE).ifBlank { "Check the timetable" }
            binding.tvFlexibleSubtitle.visibility = View.VISIBLE
            applyOrderRowStyle(muted = isPastDayMode)
        }

        binding.tvTitle.text = item.title

        binding.tvActivityBadge.text =
            TRPCore.core.miscRepository.getLanguageValueForKey(LanguageConst.TIMELINE_LABEL_ACTIVITY_BADGE)

        TimelineCellBinder.loadActivityImage(binding.ivImage, item.imageUrl)

        val rating = item.rating
        val reviewCount = item.reviewCount
        if (rating != null && rating > 0) {
            binding.tvRating.text = String.format(Locale.getDefault(), "%.1f", rating)
            if (reviewCount != null && reviewCount > 0) {
                val opinionsText = TRPCore.core.miscRepository
                    .getLanguageValueForKey(LanguageConst.ADD_PLAN_OPINIONS)
                binding.tvReviewCount.text = "${numberFormat.format(reviewCount)} $opinionsText"
                binding.tvReviewCount.visibility = View.VISIBLE
            } else {
                binding.tvReviewCount.visibility = View.GONE
            }
            binding.llRating.visibility = View.VISIBLE
        } else {
            binding.llRating.visibility = View.GONE
        }

        TimelineCellBinder.bindNoLocationBadge(binding.noLocationBadge, item.isNoLocation)

        val cancellationText = item.cancellation?.takeIf { it.isNotBlank() }
            ?: TRPCore.core.miscRepository.getLanguageValueForKey(LanguageConst.ADD_PLAN_FREE_CANCELLATION)
        binding.tvCancellation.text = cancellationText

        TimelineCellBinder.bindPrice(
            priceRow = binding.llPriceRow,
            fromLabel = binding.tvFromLabel,
            priceView = binding.tvPrice,
            price = item.price,
            currency = item.currency
        )

        binding.btnReservation.text =
            TRPCore.core.miscRepository.getLanguageValueForKey(LanguageConst.RESERVATION)
        binding.btnReservation.visibility = if (isPastDayMode) View.GONE else View.VISIBLE

        if (isPastDayMode) {
            applyPastDayStyle()
        }

        binding.btnChangeTime.visibility = if (isPastDayMode) View.GONE else View.VISIBLE

        binding.root.setOnClickListener {
            if (isPastDayMode) return@setOnClickListener
            onItemClick(item)
        }
        binding.btnChangeTime.setOnClickListener {
            if (isPastDayMode) return@setOnClickListener
            onChangeTimeClick(item)
        }
        binding.btnDelete.setOnClickListener {
            if (isPastDayMode) return@setOnClickListener
            onDeleteClick(item, item.segmentIndex)
        }
        binding.btnReservation.setOnClickListener {
            if (isPastDayMode) return@setOnClickListener
            onReservationClick(item)
        }
    }


    private fun applyOrderRowStyle(muted: Boolean) {
        val ctx = binding.root.context
        val titleColor = ContextCompat.getColor(
            ctx,
            if (muted) R.color.trp_fgWeak else R.color.trp_text_primary
        )
        val subtitleColor = ContextCompat.getColor(
            ctx,
            if (muted) R.color.trp_fgWeak else R.color.trp_text_secondary
        )
        binding.tvFlexibleTitle.setTextColor(titleColor)
        binding.tvFlexibleSubtitle.setTextColor(subtitleColor)
    }

    private fun applyPastDayStyle() {
        val muted = ContextCompat.getColor(binding.root.context, R.color.trp_fgWeak)
        binding.tvTitle.setTextColor(muted)
        binding.tvCancellation.setTextColor(muted)
        binding.tvRating.setTextColor(muted)
        binding.tvReviewCount.setTextColor(muted)
    }
}
