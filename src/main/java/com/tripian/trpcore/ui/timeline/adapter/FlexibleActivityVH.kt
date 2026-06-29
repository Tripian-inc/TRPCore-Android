package com.tripian.trpcore.ui.timeline.adapter

import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.R
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.databinding.ItemTimelineFlexibleActivityBinding
import com.tripian.trpcore.domain.model.timeline.TimelineDisplayItem
import com.tripian.trpcore.util.LanguageConst
import java.text.NumberFormat
import java.util.Locale

/**
 * ViewHolder for Flexible-Time Activities.
 *
 * The layout now mirrors [ReservedActivityVH] — same order-time container, vertical
 * line and content row — but the container uses a dashed border and its text track
 * shows "Flexible entry / Check the timetable" instead of a time range.
 *
 *  - No conflict / time-overlap styling — flexible items are excluded from conflict
 *    detection (the 00:00–23:59 envelope is a placeholder, not a real interval).
 *  - No duration row — flexible items carry `additionalData.duration == -1` which is
 *    a sentinel, not a real duration.
 */
class FlexibleActivityVH(
    private val binding: ItemTimelineFlexibleActivityBinding
) : RecyclerView.ViewHolder(binding.root) {

    /** Past-day mode (Theme 3). When true, click handlers no-op and styles mute. */
    var isPastDayMode: Boolean = false

    private val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())

    private fun getLanguage(key: String): String = try {
        TRPCore.core.miscRepository.getLanguageValueForKey(key)
    } catch (e: Exception) {
        key
    }

    fun bind(
        item: TimelineDisplayItem.FlexibleActivity,
        onItemClick: (TimelineDisplayItem) -> Unit,
        onChangeTimeClick: (TimelineDisplayItem.FlexibleActivity) -> Unit,
        onDeleteClick: (TimelineDisplayItem, Int?) -> Unit,
        onReservationClick: (TimelineDisplayItem.FlexibleActivity) -> Unit
    ) {
        // Order-time row content. When the activity is no longer available,
        // the "Flexible entry / Check the timetable" track is replaced with
        // a single composite line that keeps the "Flexible entry" prefix in
        // place of the timed cells' "HH:mm - HH:mm", followed by the same
        // middle-dot + red icon + "Not available" suffix the timed cells
        // use. The container also swaps to the solid red-bordered expired
        // pill so every activity cell shares the same expired treatment.
        if (item.isAvailabilityExpired) {
            val flexibleLabel = getLanguage(LanguageConst.TIMELINE_FLEXIBLE_TITLE)
                .ifBlank { "Flexible entry" }
            val notAvailableLabel = getLanguage(LanguageConst.TIMELINE_LABEL_NOT_AVAILABLE)
                .ifBlank { "Not available" }
            binding.orderTimeContainer
                .setBackgroundResource(R.drawable.trp_bg_order_time_container_expired)
            binding.tvOrder.setBackgroundResource(R.drawable.trp_bg_step_order_expired)
            // Match the timed cells' badge text style — XML keeps tvFlexibleTitle
            // semibold for the normal "Flexible entry" title, but inside the
            // expired pill the whole spannable should render in the same medium
            // weight that `tvTime` uses on Reserved/Step activity cells so the
            // badge looks consistent across all activity types.
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
            // Restore the XML default (semibold) so a recycled VH that just
            // rendered the expired state doesn't keep its medium typeface for
            // the normal "Flexible entry" title.
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

        // Title
        binding.tvTitle.text = item.title

        // Activity badge — always shown on FlexibleActivity cells (these cells
        // exist precisely because the segment is a flexible-time activity).
        binding.tvActivityBadge.text =
            TRPCore.core.miscRepository.getLanguageValueForKey(LanguageConst.TIMELINE_LABEL_ACTIVITY_BADGE)

        // Image (Nexus default logo fallback handled by the shared binder).
        TimelineCellBinder.loadActivityImage(binding.ivImage, item.imageUrl)

        // Rating row (hide if no rating)
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

        // No-Location badge (Theme 6).
        TimelineCellBinder.bindNoLocationBadge(binding.noLocationBadge, item.isNoLocation)

        // Cancellation
        val cancellationText = item.cancellation
            ?: TRPCore.core.miscRepository.getLanguageValueForKey(LanguageConst.ADD_PLAN_FREE_CANCELLATION)
        binding.tvCancellation.text = cancellationText

        // Reservation CTA — hidden in past-day mode
        binding.btnReservation.text =
            TRPCore.core.miscRepository.getLanguageValueForKey(LanguageConst.RESERVATION)
        binding.btnReservation.visibility = if (isPastDayMode) View.GONE else View.VISIBLE

        // Past-day style for non-order-row text.
        if (isPastDayMode) {
            applyPastDayStyle()
        }

        // Change-time is hidden for flexible items: their start/end are 00:00–23:59
        // placeholders, so picking a real time would conceptually convert the item
        // into a non-flexible reserved activity.
        binding.btnChangeTime.visibility = View.GONE

        // Click handlers — no-op when past-day.
        binding.root.setOnClickListener {
            if (isPastDayMode) return@setOnClickListener
            onItemClick(item)
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
