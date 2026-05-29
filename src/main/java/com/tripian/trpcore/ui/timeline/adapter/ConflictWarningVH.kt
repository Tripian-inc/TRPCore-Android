package com.tripian.trpcore.ui.timeline.adapter

import androidx.recyclerview.widget.RecyclerView
import com.tripian.trpcore.ui.timeline.views.ConflictWarningView

/**
 * Renders the conflict warning banner inline with the timeline list so it scrolls
 * away with the content. The banner's tap/dismiss callbacks are wired per bind.
 */
class ConflictWarningVH(
    private val view: ConflictWarningView
) : RecyclerView.ViewHolder(view) {

    fun bind(onTap: () -> Unit, onDismiss: () -> Unit) {
        view.onTap = onTap
        view.onDismiss = onDismiss
    }
}
