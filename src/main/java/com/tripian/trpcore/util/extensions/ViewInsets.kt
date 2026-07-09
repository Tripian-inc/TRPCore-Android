package com.tripian.trpcore.util.extensions

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * Replaces the view's padding on the selected edges with the system bar inset
 * values and consumes the inset chain so child views don't apply it again.
 * Edges not passed as `true` keep their existing padding.
 *
 * @param top apply `systemBars().top` to `paddingTop`
 * @param bottom apply `systemBars().bottom` to `paddingBottom`
 * @param horizontal apply `systemBars().left` / `right` to the side paddings
 */
fun View.consumeSystemBarPadding(
    top: Boolean = false,
    bottom: Boolean = false,
    horizontal: Boolean = false
) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, windowInsets ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.updatePadding(
            left = if (horizontal) insets.left else v.paddingLeft,
            top = if (top) insets.top else v.paddingTop,
            right = if (horizontal) insets.right else v.paddingRight,
            bottom = if (bottom) insets.bottom else v.paddingBottom
        )
        WindowInsetsCompat.CONSUMED
    }
}

/**
 * Stacks the system navigation bar bottom inset onto the view's existing bottom
 * padding so the last item never sits behind the gesture pill / nav bar. The base
 * `paddingBottom` is captured once so repeated inset passes don't compound it; unlike
 * [consumeSystemBarPadding] it is additive and does not consume the inset chain.
 * Pair with `android:clipToPadding="false"` so the inset region stays scrollable.
 */
fun View.applyBottomSystemBarInsetPadding() {
    val basePaddingBottom = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val navBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
        v.setPadding(
            v.paddingLeft,
            v.paddingTop,
            v.paddingRight,
            basePaddingBottom + navBottom
        )
        insets
    }
}
