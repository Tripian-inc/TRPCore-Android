package com.tripian.trpcore.ui.timeline.views

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import com.tripian.trpcore.R
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.databinding.ViewConflictWarningBinding
import com.tripian.trpcore.util.LanguageConst

/**
 * Sticky banner shown above the timeline list when the current day contains
 * overlapping activities. The user can dismiss it (per-day); switching to a
 * different day re-evaluates visibility from scratch.
 *
 * Wired up from `ACTimeline` via [updateVisibility] / [onTap] / [onDismiss].
 */
class ConflictWarningView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: ViewConflictWarningBinding =
        ViewConflictWarningBinding.inflate(
            android.view.LayoutInflater.from(context),
            this
        )

    var onTap: (() -> Unit)? = null
    var onDismiss: (() -> Unit)? = null

    init {
        background = androidx.core.content.ContextCompat.getDrawable(
            context, R.drawable.bg_conflict_warning
        )
        val padding = (4f * resources.displayMetrics.density).toInt()
        setPadding(padding, padding, padding, padding)

        binding.tvConflictText.text = TRPCore.core.miscRepository
            .getLanguageValueForKey(LanguageConst.TIMELINE_CONFLICT_BANNER)
            .ifBlank { "You have overlapping activities on this day. Tap to review." }

        binding.btnConflictClose.setOnClickListener {
            onDismiss?.invoke()
        }
        setOnClickListener {
            onTap?.invoke()
        }
    }
}
