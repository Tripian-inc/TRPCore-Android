package com.tripian.trpcore.ui.timeline.adapter

import android.view.View
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
 * Differences vs [ReservedActivityVH]:
 *  - Dashed-circle order chip with U+2212 minus instead of a numeric order.
 *  - "Flexible entry / Check the timetable" labels (FlexibleTimeBadgeView).
 *  - No duration row — flexible items carry `additionalData.duration == -1` which is
 *    a sentinel, not a real duration.
 *  - No conflict / time-overlap styling — flexible items are excluded from conflict
 *    detection (the 00:00–23:59 envelope is a placeholder, not a real interval).
 */
class FlexibleActivityVH(
    private val binding: ItemTimelineFlexibleActivityBinding
) : RecyclerView.ViewHolder(binding.root) {

    /** Past-day mode (Theme 3). When true, click handlers no-op and styles mute. */
    var isPastDayMode: Boolean = false

    private val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())

    fun bind(
        item: TimelineDisplayItem.FlexibleActivity,
        onItemClick: (TimelineDisplayItem) -> Unit,
        onChangeTimeClick: (TimelineDisplayItem.FlexibleActivity) -> Unit,
        onDeleteClick: (TimelineDisplayItem, Int?) -> Unit,
        onReservationClick: (TimelineDisplayItem.FlexibleActivity) -> Unit
    ) {
        // Title
        binding.tvTitle.text = item.title

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

        // Past-day style (Theme 3 will wire this from the adapter).
        if (isPastDayMode) {
            applyPastDayStyle()
        } else {
            binding.flexibleTimeBadge.applyDefaultStyle()
        }

        // Change-time is hidden for flexible items: their start/end are 00:00–23:59
        // placeholders, so picking a real time would conceptually convert the item
        // into a non-flexible reserved activity. Keep the icon out of the layout
        // for past-day mode as well.
        binding.btnChangeTime.visibility = if (isPastDayMode) View.GONE else View.VISIBLE

        // Click handlers — no-op when past-day.
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

    private fun applyPastDayStyle() {
        val muted = androidx.core.content.ContextCompat.getColor(
            binding.root.context,
            R.color.trp_fgWeak
        )
        binding.flexibleTimeBadge.applyMutedStyle(muted)
        binding.tvTitle.setTextColor(muted)
        binding.tvCancellation.setTextColor(muted)
        binding.tvRating.setTextColor(muted)
        binding.tvReviewCount.setTextColor(muted)
    }
}
