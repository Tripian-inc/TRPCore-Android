package com.tripian.trpcore.ui.timeline.common

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.tripian.trpcore.R
import com.tripian.trpcore.databinding.ItemActivityCardBinding
import com.tripian.trpcore.util.FormatUtils
import com.tripian.trpcore.util.LanguageConst
import java.text.NumberFormat
import java.util.Locale

/**
 * Shared ViewHolder for activity cards
 * Used by both ActivityListing and SavedPlans screens
 */
class ActivityCardViewHolder(
    private val binding: ItemActivityCardBinding,
    private val getLanguage: (String) -> String,
    private val onAddClicked: (ActivityCardData) -> Unit,
    private val onItemClicked: ((ActivityCardData) -> Unit)? = null
) : RecyclerView.ViewHolder(binding.root) {

    private var currentData: ActivityCardData? = null

    fun bind(data: ActivityCardData) {
        currentData = data

        // Title
        binding.tvTitle.text = data.title

        // Image
        bindImage(data.imageUrl)

        // Rating
        bindRating(data.rating, data.ratingCount)

        // Duration
        bindDuration(data.duration)

        // Free Cancellation
        bindCancellation(data.isRefundable)

        // Price
        bindPrice(data.price, data.currency)

        // Add button click
        binding.btnAdd.setOnClickListener {
            currentData?.let { onAddClicked(it) }
        }

        // Item click (whole card)
        binding.root.setOnClickListener {
            currentData?.let { onItemClicked?.invoke(it) }
        }
    }

    private fun bindImage(imageUrl: String?) {
        if (!imageUrl.isNullOrEmpty()) {
            val cornerRadius = binding.root.context.resources.getDimensionPixelSize(R.dimen.corner_radius_8dp)
            Glide.with(binding.ivActivityImage)
                .load(imageUrl)
                .transform(CenterCrop(), RoundedCorners(cornerRadius))
                .placeholder(R.color.trp_grey_10)
                .into(binding.ivActivityImage)
        } else {
            binding.ivActivityImage.setImageResource(R.color.trp_grey_10)
        }
    }

    private fun bindRating(rating: Double?, ratingCount: Int?) {
        if (rating != null && rating > 0) {
            binding.ivStar.visibility = View.VISIBLE
            binding.tvRating.visibility = View.VISIBLE
            binding.tvRating.text = formatRating(rating)

            if (ratingCount != null && ratingCount > 0) {
                binding.tvRatingCount.visibility = View.VISIBLE
                val formattedCount = formatReviewCount(ratingCount)
                val opinionsLabel = getLanguage(LanguageConst.ADD_PLAN_OPINIONS)
                binding.tvRatingCount.text = "$formattedCount $opinionsLabel"
            } else {
                binding.tvRatingCount.visibility = View.GONE
            }
        } else {
            binding.ivStar.visibility = View.GONE
            binding.tvRating.visibility = View.GONE
            binding.tvRatingCount.visibility = View.GONE
        }
    }

    private fun bindDuration(duration: Double?) {
        if (duration != null && duration > 0) {
            binding.llDurationRow.visibility = View.VISIBLE
            binding.tvDuration.text = FormatUtils.formatDuration(duration)
        } else {
            binding.llDurationRow.visibility = View.GONE
        }
    }

    private fun bindCancellation(isRefundable: Boolean) {
        if (isRefundable) {
            binding.tvFreeCancellation.visibility = View.VISIBLE
            binding.tvFreeCancellation.text = getLanguage(LanguageConst.ADD_PLAN_FREE_CANCELLATION)
        } else {
            binding.tvFreeCancellation.visibility = View.GONE
        }
    }

    private fun bindPrice(price: Double?, currency: String) {
        when {
            price == null -> {
                binding.llPriceRow.visibility = View.GONE
            }
            price == 0.0 -> {
                binding.tvFromLabel.visibility = View.GONE
                binding.tvPrice.text = getLanguage(LanguageConst.FREE)
                binding.tvPrice.setTypeface(null, Typeface.BOLD)
                binding.llPriceRow.visibility = View.VISIBLE
            }
            else -> {
                binding.tvFromLabel.visibility = View.VISIBLE
                binding.tvFromLabel.text = getLanguage(LanguageConst.FROM) + " "
                binding.tvPrice.text = FormatUtils.formatPriceWithCurrency(price, currency)
                binding.llPriceRow.visibility = View.VISIBLE
            }
        }
    }

    companion object {
        /**
         * Create ViewHolder from parent ViewGroup
         */
        fun create(
            parent: ViewGroup,
            getLanguage: (String) -> String,
            onAddClicked: (ActivityCardData) -> Unit,
            onItemClicked: ((ActivityCardData) -> Unit)? = null
        ): ActivityCardViewHolder {
            val binding = ItemActivityCardBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ActivityCardViewHolder(binding, getLanguage, onAddClicked, onItemClicked)
        }

        /**
         * Format rating with comma as decimal separator
         * Example: 4.5 → "4,5"
         */
        fun formatRating(rating: Double): String {
            return String.format(Locale.GERMAN, "%.1f", rating)
        }

        /**
         * Format review count with dot as thousand separator
         * Example: 1796 → "1.796"
         */
        fun formatReviewCount(number: Int): String {
            val format = NumberFormat.getNumberInstance(Locale.GERMAN)
            return format.format(number)
        }
    }
}
