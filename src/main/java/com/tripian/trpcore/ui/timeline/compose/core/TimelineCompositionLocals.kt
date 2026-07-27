package com.tripian.trpcore.ui.timeline.compose.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tripian.trpcore.base.TRPCore

/**
 * The SDK's Dagger multibinding [ViewModelProvider.Factory], provided by
 * [com.tripian.trpcore.ui.timeline.compose.TimelineNexus].
 */
val LocalTimelineViewModelFactory = staticCompositionLocalOf<ViewModelProvider.Factory> {
    error("LocalTimelineViewModelFactory is not provided. Compose Timeline screens must run inside TimelineNexus.")
}

/**
 * Resolves a [ViewModel] from the SDK's Dagger factory, scoped to the current
 * [ViewModelStoreOwner] (the NavBackStackEntry inside the Timeline NavHost).
 */
@Composable
inline fun <reified VM : ViewModel> timelineViewModel(
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner available for timelineViewModel"
    },
    key: String? = null
): VM = viewModel(viewModelStoreOwner, key, factory = LocalTimelineViewModelFactory.current)

/** Returns the localized value for [key] from the SDK language service. */
@Composable
fun trpString(key: String): String {
    return remember(key) { TRPCore.core.miscRepository.getLanguageValueForKey(key) }
}
