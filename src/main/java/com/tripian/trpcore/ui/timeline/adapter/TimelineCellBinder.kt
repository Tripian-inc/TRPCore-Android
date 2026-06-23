package com.tripian.trpcore.ui.timeline.adapter

import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.tripian.trpcore.R
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.databinding.IncludeNoLocationBadgeBinding
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

    /**
     * Activity image. The no-image / load-error fallback is a per-host policy
     * (HostStrategy.activityImageFallback) — default generic placeholder, a
     * branded host (e.g. Nexus) supplies its own logo.
     */
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
