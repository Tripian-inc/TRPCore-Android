package com.tripian.trpcore.ui.timeline.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tripian.trpcore.R
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.databinding.ItemTimelineManualPoiBinding
import com.tripian.trpcore.domain.model.timeline.TimelineDisplayItem
import com.tripian.trpcore.util.LanguageConst
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * ManualPoiVH
 * ViewHolder for manually added POI
 */
class ManualPoiVH(
    private val binding: ItemTimelineManualPoiBinding
) : RecyclerView.ViewHolder(binding.root) {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
    // Turkish locale for formatting: comma as decimal separator, dot as thousand separator
    private val turkishLocale = Locale("tr", "TR")
    private val reviewCountFormat = NumberFormat.getNumberInstance(turkishLocale)

    fun bind(
        item: TimelineDisplayItem.ManualPoi,
        onItemClick: (TimelineDisplayItem) -> Unit,
        onChangeTimeClick: ((TimelineDisplayItem.ManualPoi) -> Unit)? = null,
        onDeleteClick: (TimelineDisplayItem, Int?) -> Unit
    ) {
        // Order badge
        binding.tvOrder.text = item.order.toString()

        // Title - semibold 16px primaryText
        binding.tvTitle.text = item.title

        // Time (startTime - endTime format)
        val startTime = item.startTime
        val endTime = item.endTime
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

        // Rating Row - Bold 14px rating, star icon 12x12, Light 14px fgWeak review count
        // Rating uses comma as decimal separator (4,2), reviewCount uses dot as thousand separator (49.565)
        val rating = item.rating
        val reviewCount = item.reviewCount
        if (rating != null && rating > 0) {
            binding.tvRating.text = String.format(turkishLocale, "%.1f", rating)

            if (reviewCount != null && reviewCount > 0) {
                val opinionsText = TRPCore.core.miscRepository.getLanguageValueForKey(LanguageConst.ADD_PLAN_OPINIONS)
                binding.tvReviewCount.text = "${reviewCountFormat.format(reviewCount)} $opinionsText"
                binding.tvReviewCount.visibility = View.VISIBLE
            } else {
                binding.tvReviewCount.visibility = View.GONE
            }

            binding.llRating.visibility = View.VISIBLE
        } else {
            binding.llRating.visibility = View.GONE
        }

        // Category Badge - same style as confirmed badge (green bg, green text)
        item.categoryName?.let { category ->
            binding.tvCategory.text = category
            binding.tvCategory.visibility = View.VISIBLE
        } ?: run {
            binding.tvCategory.visibility = View.GONE
        }

        // Click listeners
        binding.root.setOnClickListener {
            onItemClick(item)
        }

        // Change Time button
        binding.btnChangeTime.setOnClickListener {
            onChangeTimeClick?.invoke(item)
        }

        // Delete/Remove Step button
        binding.btnDelete.setOnClickListener {
            onDeleteClick(item, item.segmentIndex)
        }
    }
}
