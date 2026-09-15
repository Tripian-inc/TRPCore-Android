package com.tripian.trpcore.ui.timeline.compose.core

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.viewbinding.ViewBinding
import com.tripian.trpcore.R

/**
 * Hosts a ViewBinding layout ported from an SDK Activity inside a Compose
 * route. [bind] runs once per inflated binding with a [LifecycleOwner] that
 * mirrors the route's lifecycle and is destroyed together with the binding, so
 * LiveData observers registered through it never outlive their views.
 * [viewState] survives the route leaving and re-entering composition (a host
 * screen pushed on top of the SDK); [onSaveViewState] fills it on dispose.
 * [update] re-runs whenever the Compose state it reads changes.
 */
@Composable
internal fun <VB : ViewBinding> BindingHost(
    inflate: (LayoutInflater) -> VB,
    modifier: Modifier = Modifier.fillMaxSize(),
    onSaveViewState: (binding: VB, viewState: Bundle) -> Unit = { _, _ -> },
    update: (binding: VB) -> Unit = {},
    bind: (binding: VB, lifecycleOwner: LifecycleOwner, viewState: Bundle) -> Unit
) {
    val parentOwner = LocalLifecycleOwner.current
    val viewState = rememberSaveable { Bundle() }
    val owner = remember { ComposedLifecycleOwner() }
    val holder = remember { BindingHolder<VB>() }
    val currentOnSave by rememberUpdatedState(onSaveViewState)

    DisposableEffect(parentOwner, owner) {
        val mirror = LifecycleEventObserver { _, event -> owner.moveTo(event.targetState) }
        owner.moveTo(parentOwner.lifecycle.currentState)
        parentOwner.lifecycle.addObserver(mirror)
        onDispose {
            parentOwner.lifecycle.removeObserver(mirror)
            holder.binding?.let { currentOnSave(it, viewState) }
            holder.binding = null
            owner.moveTo(Lifecycle.State.DESTROYED)
        }
    }

    AndroidView(
        factory = { ctx ->
            val binding = inflate(LayoutInflater.from(ContextThemeWrapper(ctx, R.style.TrpAppTheme)))
            holder.binding = binding
            bind(binding, owner, viewState)
            binding.root
        },
        update = { holder.binding?.let(update) },
        modifier = modifier
    )
}

private class BindingHolder<VB : ViewBinding> {
    var binding: VB? = null
}

private class ComposedLifecycleOwner : LifecycleOwner {
    private val registry = LifecycleRegistry(this)

    override val lifecycle: Lifecycle get() = registry

    fun moveTo(state: Lifecycle.State) {
        if (registry.currentState == Lifecycle.State.DESTROYED) return
        if (state == Lifecycle.State.DESTROYED && registry.currentState == Lifecycle.State.INITIALIZED) {
            registry.currentState = Lifecycle.State.CREATED
        }
        registry.currentState = state
    }
}
