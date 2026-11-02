package com.tripian.trpcore.ui.timeline.compose.core

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.ui.platform.LocalView

/**
 * Window roots of the Timeline sheets currently on screen, most recent last.
 * Toasts attach to [current] so they draw above an open sheet's dialog window
 * instead of the activity content underneath it.
 */
@Stable
internal class TimelineToastAnchor {
    private val roots = mutableListOf<ViewGroup>()

    val current: ViewGroup?
        get() = roots.lastOrNull()

    fun register(root: ViewGroup) {
        roots.remove(root)
        roots.add(root)
    }

    fun unregister(root: ViewGroup) {
        roots.remove(root)
    }
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
