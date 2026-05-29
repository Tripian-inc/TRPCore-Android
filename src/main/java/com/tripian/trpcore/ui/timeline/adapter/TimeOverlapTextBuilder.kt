package com.tripian.trpcore.ui.timeline.adapter

import android.content.Context
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import androidx.appcompat.content.res.AppCompatResources
import com.tripian.trpcore.R

/**
 * Builds the time-overlap status label rendered in time-bearing timeline cells.
 *
 * Format: "HH:mm - HH:mm · <warning icon> <localized overlap label>"
 *
 * The middle dot separates the time range from the warning. The warning glyph
 * is the project's `trp_ic_warning` vector — its own fillColor (#D6771A) gives
 * it the orange warning tint, so no tinting is needed here.
 */
object TimeOverlapTextBuilder {

    private const val MIDDLE_DOT = "·"
    private const val ICON_SIZE_DP = 14f
    private const val ICON_PADDING_DP = 2f

    /**
     * Build the spanned label. Falls back to a plain string if the warning drawable
     * cannot be resolved (extremely unlikely — vector is bundled).
     */
    fun build(context: Context, timeText: String, overlapLabel: String): CharSequence {
        val icon = AppCompatResources.getDrawable(context, R.drawable.trp_ic_warning)
            ?: return "$timeText $MIDDLE_DOT $overlapLabel"

        val density = context.resources.displayMetrics.density
        val sizePx = (ICON_SIZE_DP * density).toInt()
        val paddingPx = (ICON_PADDING_DP * density).toInt()
        // Add a touch of right padding so the icon doesn't kiss the label text.
        icon.setBounds(0, 0, sizePx + paddingPx, sizePx)

        val builder = SpannableStringBuilder("$timeText $MIDDLE_DOT ")
        val iconStart = builder.length
        builder.append(" ")
        builder.setSpan(
            ImageSpan(icon, ImageSpan.ALIGN_BASELINE),
            iconStart,
            iconStart + 1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        builder.append(" ")
        builder.append(overlapLabel)
        return builder
    }
}
