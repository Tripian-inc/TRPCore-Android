package com.tripian.trpcore.util.extensions

import android.view.View
import android.widget.TextView
import com.tripian.trpcore.R
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.util.TimeFieldError

/**
 * Paints this time field with the error border and shows [error]'s message in
 * [label], or restores the normal border and hides [label] when there is none.
 */
fun View.applyTimeFieldError(label: TextView, error: TimeFieldError?) {
    setBackgroundResource(
        if (error != null) R.drawable.trp_bg_selection_field_error else R.drawable.trp_bg_selection_field
    )
    label.text = error?.let { TRPCore.core.miscRepository.getLanguageValueForKey(it.languageKey) }.orEmpty()
    label.visibility = if (error != null) View.VISIBLE else View.GONE
}
