package com.tripian.trpcore.ui.timeline.compose.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.tripian.trpcore.base.BaseViewModel

/**
 * Per-screen scaffold for Compose Timeline screens: binds a
 * [ComposeViewListener] to [viewModel] for alerts and legacy Activity
 * launches, and renders the SDK Lottie loader above [content] while the
 * ViewModel reports loading.
 */
@Composable
fun TimelineComposeScreen(
    viewModel: BaseViewModel,
    onExit: () -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val currentOnExit by rememberUpdatedState(onExit)
    DisposableEffect(viewModel, context) {
        val listener = ComposeViewListener(context) { currentOnExit() }
        viewModel.viewListener = listener
        onDispose {
            if (viewModel.viewListener === listener) {
                viewModel.viewListener = null
            }
        }
    }
    val loaderEvent by viewModel.lottieLoadingEvent.observeAsState()
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        content()
        loaderEvent?.takeIf { it.show }?.let { TimelineLoaderOverlay(it.text) }
    }
}
