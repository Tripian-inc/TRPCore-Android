package com.tripian.trpcore.ui.timeline.compose.core

import android.app.Activity
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.widget.BottomToast

/**
 * Places Timeline toasts above the topmost open sheet. Sheets register their
 * window roots, most recent last; a non-error toast still on screen when its sheet
 * closes is shown again on the next sheet, or on the activity when none is left.
 * Error toasts close together with their sheet.
 */
internal class TimelineToastAnchor {
    private val roots = mutableListOf<ViewGroup>()
    private var anchoredToast: AnchoredToast? = null

    val current: ViewGroup?
        get() = roots.lastOrNull()

    fun register(root: ViewGroup) {
        roots.remove(root)
        roots.add(root)
    }

    fun unregister(root: ViewGroup) {
        roots.remove(root)
        val toast = anchoredToast?.takeIf { it.root === root } ?: return
        anchoredToast = null
        if (toast.type != AlertType.ERROR && BottomToast.isShowing(root)) {
            show(toast.activity, toast.message, toast.type)
        }
    }

    fun show(activity: Activity, message: String, type: AlertType) {
        val root = current
        BottomToast.show(activity, message, type, parent = root)
        anchoredToast = root?.let { AnchoredToast(activity, message, type, it) }
    }

    private class AnchoredToast(
        val activity: Activity,
        val message: String,
        val type: AlertType,
        val root: ViewGroup
    )
}

/** Registers the calling composition's window root with [anchor] while it stays composed. */
@Composable
internal fun RegisterToastAnchor(anchor: TimelineToastAnchor) {
    val view = LocalView.current
    DisposableEffect(anchor, view) {
        val root = view.rootView as? ViewGroup
        root?.let(anchor::register)
        onDispose { root?.let(anchor::unregister) }
    }
}
