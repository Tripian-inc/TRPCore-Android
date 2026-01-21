package com.tripian.trpcore.domain.model.itinerary

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Activity price information
 */
@Parcelize
data class SegmentActivityPrice(
    val currency: String,     // "EUR", "USD", "GBP", "TRY", etc.
    val value: Double         // Numeric value (e.g., 83.50)
) : Parcelable
