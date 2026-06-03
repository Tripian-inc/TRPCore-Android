package com.tripian.trpcore.util.extensions

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * Replaces the view's padding on the selected edges with the system bar
 * inset values and consumes the inset chain so child views don't try to
 * apply it again. Use for top-level / root views that need to absorb the
 * status bar or navigation bar (BaseActivity content root,
 * BaseBottomDialogFragment / BaseSimpleBottomSheet roots, etc.). Edges not
 * passed as `true` keep their existing padding.
 *
 * @param top apply `systemBars().top` to `paddingTop`
 * @param bottom apply `systemBars().bottom` to `paddingBottom`
 * @param horizontal apply `systemBars().left` / `right` to the side
 *   paddings (edge-to-edge bottom sheets need this when the device has
 *   side gesture insets).
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
 * Stacks the system navigation bar bottom inset onto the view's existing
 * bottom padding so the last item never sits behind the device's gesture
 * pill or 3-button bar.
 *
 * Captures the view's current `paddingBottom` once and adds the system bar
 * bottom inset to it on every emission, so re-attaching or rotating the
 * device doesn't compound the padding. Designed for scrollable content
 * inside a window (RecyclerView, NestedScrollView…) where the XML's base
 * padding should be preserved. Pair with `android:clipToPadding="false"`
 * in the XML so the inset region remains scrollable.
 *
 * Differs from [consumeSystemBarPadding] in two ways: it is additive (XML
 * padding is preserved) and it does not consume the inset chain.
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
