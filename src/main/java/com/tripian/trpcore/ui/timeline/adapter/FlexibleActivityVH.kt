package com.tripian.trpcore.ui.timeline.adapter

import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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
        // Order-time row content
        binding.tvFlexibleTitle.text =
            getLanguage(LanguageConst.TIMELINE_FLEXIBLE_TITLE).ifBlank { "Flexible entry" }
        binding.tvFlexibleSubtitle.text =
            getLanguage(LanguageConst.TIMELINE_FLEXIBLE_SUBTITLE).ifBlank { "Check the timetable" }
        applyOrderRowStyle(muted = isPastDayMode)

        // Title
        binding.tvTitle.text = item.title

        // Activity badge — always shown on FlexibleActivity cells (these cells
        // exist precisely because the segment is a flexible-time activity).
        binding.tvActivityBadge.text =
            TRPCore.core.miscRepository.getLanguageValueForKey(LanguageConst.TIMELINE_LABEL_ACTIVITY_BADGE)

        // Image
        item.imageUrl?.let { url ->
            Glide.with(binding.ivImage)
                .load(url)
                .centerCrop()
                .placeholder(R.drawable.trp_bg_place_holder_image)
                .into(binding.ivImage)
        } ?: run {
            binding.ivImage.setImageResource(R.drawable.trp_bg_place_holder_image)
        }

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

        // No-Location badge (Theme 6) — visible only when segment carries the flag.
        binding.noLocationBadge.llNoLocationBadge.visibility = if (item.isNoLocation) {
            binding.noLocationBadge.tvNoLocationLabel.text = TRPCore.core.miscRepository
                .getLanguageValueForKey(LanguageConst.TIMELINE_NO_EXACT_LOCATION)
                .ifBlank { "No exact location" }
            View.VISIBLE
        } else {
            View.GONE
        }

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
