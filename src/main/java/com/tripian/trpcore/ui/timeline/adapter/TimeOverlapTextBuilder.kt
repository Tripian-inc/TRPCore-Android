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
 * `ImageSpan` variant that aligns the drawable's vertical center to the text
 * line's vertical center (using ascent/descent average) instead of the
 * baseline. The stock `ImageSpan.ALIGN_BASELINE` sits a symmetric icon — like
 * the warning triangle — visibly below the middle of the text; the stock
 * `ALIGN_CENTER` only exists from API 29+. This subclass works on every
 * supported API and renders the same on all of them.
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
        // y is the baseline. Text vertical centre = y + (ascent + descent) / 2.
        // Translate so the drawable's vertical centre lands on that point.
        val textCenter = y + (fm.ascent + fm.descent) / 2
        val transY = textCenter - b.bounds.height() / 2
        canvas.save()
        canvas.translate(x, transY.toFloat())
        b.draw(canvas)
        canvas.restore()
    }
}

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

    private const val MIDDLE_DOT = " · "
    private const val ICON_SIZE_DP = 16f

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
        // Square bounds so the vector renders at its native 1:1 aspect ratio.
        // Spacing between the glyph and the label is provided by the trailing
        // space character appended to the SpannableStringBuilder below — baking
        // extra width into the drawable bounds (as the older `+ paddingPx`
        // variant did) horizontally stretches the icon, which is what made the
        // expired pill's warning triangle look noticeably wider than the same
        // glyph in the conflict banner.
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
