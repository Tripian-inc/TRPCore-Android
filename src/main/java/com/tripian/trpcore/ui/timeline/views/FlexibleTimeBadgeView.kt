package com.tripian.trpcore.ui.timeline.views

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.tripian.trpcore.R
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.databinding.ViewFlexibleTimeBadgeBinding
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.MINUS_SIGN

/**
 * Custom view used by [FlexibleActivityVH]: dashed-circle order chip showing a
 * U+2212 minus, plus "Flexible entry / Check the timetable" labels. The character
 * inside the chip is the unicode minus, not an ASCII hyphen — vertical centering
 * relies on this.
 *
 * Past-day muting is handled via [applyMutedStyle]; the recursive label helper
 * doesn't cover the dashed [GradientDrawable] stroke colour so we update it
 * explicitly.
 */
class FlexibleTimeBadgeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: ViewFlexibleTimeBadgeBinding =
        ViewFlexibleTimeBadgeBinding.inflate(
            android.view.LayoutInflater.from(context),
            this
        )

    init {
        binding.tvOrderChip.text = MINUS_SIGN
        binding.tvFlexibleTitle.text =
            TRPCore.core.miscRepository.getLanguageValueForKey(LanguageConst.TIMELINE_FLEXIBLE_TITLE)
                .ifBlank { "Flexible entry" }
        binding.tvFlexibleSubtitle.text =
            TRPCore.core.miscRepository.getLanguageValueForKey(LanguageConst.TIMELINE_FLEXIBLE_SUBTITLE)
                .ifBlank { "Check the timetable" }
    }

    /** Apply the past-day muted styling: tint chip border, title and subtitle. */
    fun applyMutedStyle(@ColorInt color: Int) {
        binding.tvOrderChip.setTextColor(color)
        (binding.tvOrderChip.background as? GradientDrawable)?.setStroke(
            (1.5f * resources.displayMetrics.density).toInt(),
            color,
            (4f * resources.displayMetrics.density),
            (3f * resources.displayMetrics.density)
        )
        binding.tvFlexibleTitle.setTextColor(color)
        binding.tvFlexibleSubtitle.setTextColor(color)
    }

    /** Restore the default (non-muted) styling. */
    fun applyDefaultStyle() {
        val secondary = ContextCompat.getColor(context, R.color.trp_text_secondary)
        val primary = ContextCompat.getColor(context, R.color.trp_text_primary)
        binding.tvOrderChip.setTextColor(secondary)
        (binding.tvOrderChip.background as? GradientDrawable)?.setStroke(
            (1.5f * resources.displayMetrics.density).toInt(),
            secondary,
            (4f * resources.displayMetrics.density),
            (3f * resources.displayMetrics.density)
        )
        binding.tvFlexibleTitle.setTextColor(primary)
        binding.tvFlexibleSubtitle.setTextColor(secondary)
    }
}
