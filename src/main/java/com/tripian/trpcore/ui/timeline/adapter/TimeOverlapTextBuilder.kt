package com.tripian.trpcore.ui.timeline.adapter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.tripian.trpcore.R
import com.tripian.trpcore.ui.timeline.adapter.TimeBadgeStatus.EXPIRED
import com.tripian.trpcore.ui.timeline.adapter.TimeBadgeStatus.OVERLAP

/**
 * Visual states for the time badge: [OVERLAP] renders the orange warning icon,
 * [EXPIRED] the red one (`trp_expired_fg`). Only the icon tint differs — the
 * label always inherits the TextView's default color.
 */
enum class TimeBadgeStatus { OVERLAP, EXPIRED }

/**
 * `ImageSpan` variant that aligns the drawable's vertical center to the text
 * line's vertical center (ascent/descent average) instead of the baseline.
 * Works on every supported API, unlike `ALIGN_CENTER` (API 29+).
 */
private class CenteredImageSpan(d: Drawable) : ImageSpan(d, ALIGN_BASELINE) {
    override fun draw(
        canvas: Canvas,
        text: CharSequence?,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        val b = drawable
        val fm = paint.fontMetricsInt
        val textCenter = y + (fm.ascent + fm.descent) / 2
        val transY = textCenter - b.bounds.height() / 2
        canvas.save()
        canvas.translate(x, transY.toFloat())
        b.draw(canvas)
        canvas.restore()
    }
}

/**
 * Builds the status label rendered inline in time-bearing timeline cells:
 * "HH:mm - HH:mm · <warning icon> <localized status label>", or icon + label
 * only when [timeText] is null (e.g. flexible activities). [TimeBadgeStatus]
 * picks the colors so callers don't recompute them.
 */
object TimeOverlapTextBuilder {

    private const val MIDDLE_DOT = " · "
    private const val ICON_SIZE_DP = 16f

    /**
     * Build the spanned label. Falls back to a plain string if the warning drawable
     * cannot be resolved. The icon keeps square bounds; spacing to the label comes
     * from the trailing space character, not from widened drawable bounds.
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
        icon.setBounds(0, 0, sizePx, sizePx)

        if (status == TimeBadgeStatus.EXPIRED) {
            val tint = ContextCompat.getColor(context, R.color.trp_expired_fg)
            DrawableCompat.setTint(icon.mutate(), tint)
        }

        val builder = SpannableStringBuilder(prefix)
        val iconStart = builder.length
        builder.append(" ")
        builder.setSpan(
            CenteredImageSpan(icon),
            iconStart,
            iconStart + 1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        builder.append(" ")
        builder.append(statusLabel)
        return builder
    }
}
