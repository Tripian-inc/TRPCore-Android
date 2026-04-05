package com.tripian.trpcore.ui.common

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

/**
 * A FrameLayout that supports maxHeight constraint.
 * When content exceeds maxHeight, the view is capped and content scrolls internally.
 */
class MaxHeightFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var maxHeight: Int = Int.MAX_VALUE
        set(value) {
            field = value
            requestLayout()
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var heightSpec = heightMeasureSpec

        if (maxHeight < Int.MAX_VALUE) {
            val maxHeightSpec = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST)
            heightSpec = maxHeightSpec
        }

        super.onMeasure(widthMeasureSpec, heightSpec)
    }
}
