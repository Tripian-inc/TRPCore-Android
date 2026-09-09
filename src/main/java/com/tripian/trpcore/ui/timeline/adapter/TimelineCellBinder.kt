package com.tripian.trpcore.ui.timeline.adapter

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.tripian.trpcore.R
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.databinding.IncludeNoLocationBadgeBinding
import com.tripian.trpcore.util.FormatUtils
import com.tripian.trpcore.util.LanguageConst

/** Resolve a localized string for a language key (falls back to the key itself). */
internal fun String.languageValue(): String =
    try {
        TRPCore.core.miscRepository.getLanguageValueForKey(this)
    } catch (e: Exception) {
        this
    }

/**
 * Shared bind helpers for timeline cells.
 *
 * The cells use different generated ViewBinding types (no common supertype), so
 * rather than a base ViewHolder, the logic that was duplicated across the
 * activity/POI ViewHolders lives here and operates on the concrete views.
 */
internal object TimelineCellBinder {

    private const val DEFAULT_CURRENCY = "EUR"

    /**
     * Single source of truth for the activity price row. The tour API omits the
     * price field on free products instead of sending zero, so a missing or
     * non-positive price reads as "free" rather than "unknown".
     */
    fun bindPrice(
        priceRow: View,
        fromLabel: TextView,
        priceView: TextView,
        price: Double?,
        currency: String?
    ) {
        priceRow.visibility = View.VISIBLE
        if (price == null || price <= 0.0) {
            fromLabel.visibility = View.GONE
            priceView.text = LanguageConst.FREE.languageValue()
            return
        }
        fromLabel.visibility = View.VISIBLE
        fromLabel.text = LanguageConst.FROM.languageValue() + " "
        priceView.text = FormatUtils.formatPriceWithCurrency(
            price,
            currency?.takeIf { it.isNotBlank() } ?: DEFAULT_CURRENCY
        )
    }

    /** Activity image; the no-image / load-error fallback is the host's (HostStrategy.activityImageFallback). */
    fun loadActivityImage(imageView: ImageView, imageUrl: String?) {
        val fallback = TRPCore.host.activityImageFallback
        if (!imageUrl.isNullOrBlank()) {
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            Glide.with(imageView)
                .load(imageUrl)
                .centerCrop()
                .placeholder(R.drawable.trp_bg_place_holder_image)
                .error(fallback)
                .into(imageView)
        } else {
            imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
            imageView.setImageResource(fallback)
        }
    }

    /** POI image — generic placeholder fallback (no brand logo). */
    fun loadPoiImage(imageView: ImageView, imageUrl: String?) {
        if (!imageUrl.isNullOrBlank()) {
            Glide.with(imageView)
                .load(imageUrl)
                .centerCrop()
                .placeholder(R.drawable.trp_bg_place_holder_image)
                .into(imageView)
        } else {
            imageView.setImageResource(R.drawable.trp_bg_place_holder_image)
        }
    }

    /** Toggle the "No exact location" badge and set its localized label. */
    fun bindNoLocationBadge(badge: IncludeNoLocationBadgeBinding, isNoLocation: Boolean) {
        badge.llNoLocationBadge.visibility = if (isNoLocation) {
            badge.tvNoLocationLabel.text =
                LanguageConst.TIMELINE_NO_EXACT_LOCATION.languageValue()
                    .ifBlank { "No exact location" }
            View.VISIBLE
        } else {
            View.GONE
        }
    }
}
