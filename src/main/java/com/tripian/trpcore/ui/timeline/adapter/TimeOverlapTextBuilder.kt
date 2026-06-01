package com.tripian.trpcore.ui.timeline.adapter

import android.content.Context
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.tripian.trpcore.R

/**
 * Two visual states the badge can take:
 *  - [OVERLAP]: orange warning (the vector's own fillColor #D6771A).
 *  - [EXPIRED]: red warning (`trp_expired_fg`).
 *
 * Label text always inherits the TextView's default color — only the icon
 * tint differs between the two states. The surrounding pill (container
 * background, order chip) carries the rest of the state's color cue.
 */
enum class TimeBadgeStatus { OVERLAP, EXPIRED }

/**
 * Builds the status label rendered inline in time-bearing timeline cells.
 *
 * Format with [timeText]:    "HH:mm - HH:mm · <warning icon> <localized status label>"
 * Format without [timeText]: "<warning icon> <localized status label>"
 *
 * The same layout is shared between the time-overlap and availability-expired
 * states; [TimeBadgeStatus] picks the colors so callers don't recompute them.
 * Cells without a real time range (flexible activity) pass [timeText] = null
 * so only the icon + label render, with no leading time prefix or middle dot.
 */
object TimeOverlapTextBuilder {

    private const val MIDDLE_DOT = "·"
    private const val ICON_SIZE_DP = 14f
    private const val ICON_PADDING_DP = 2f

    /**
     * Build the spanned label. Falls back to a plain string if the warning drawable
     * cannot be resolved (extremely unlikely — vector is bundled).
     */
    fun build(
        context: Context,
        timeText: String?,
        statusLabel: String,
        status: TimeBadgeStatus = TimeBadgeStatus.OVERLAP
    ): CharSequence {
        val prefix = if (timeText.isNullOrEmpty()) "" else "$timeText $MIDDLE_DOT "
        val icon = AppCompatResources.getDrawable(context, R.drawable.trp_ic_warning)
            ?: return "$prefix$statusLabel"

        val density = context.resources.displayMetrics.density
        val sizePx = (ICON_SIZE_DP * density).toInt()
        val paddingPx = (ICON_PADDING_DP * density).toInt()
        // Add a touch of right padding so the icon doesn't kiss the label text.
        icon.setBounds(0, 0, sizePx + paddingPx, sizePx)

        if (status == TimeBadgeStatus.EXPIRED) {
            val tint = ContextCompat.getColor(context, R.color.trp_expired_fg)
            DrawableCompat.setTint(icon.mutate(), tint)
        }

        val builder = SpannableStringBuilder(prefix)
        val iconStart = builder.length
        builder.append(" ")
        builder.setSpan(
            ImageSpan(icon, ImageSpan.ALIGN_BASELINE),
            iconStart,
            iconStart + 1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        builder.append(" ")
        builder.append(statusLabel)
        return builder
    }
}
