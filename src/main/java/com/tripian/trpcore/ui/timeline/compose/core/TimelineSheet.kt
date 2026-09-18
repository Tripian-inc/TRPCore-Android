package com.tripian.trpcore.ui.timeline.compose.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.tripian.trpcore.R
import com.tripian.trpcore.base.BaseViewModel

/** Drag handle plus the gap that keeps a sheet below the status bar, like the View-based sheets. */
private val SHEET_TOP_CLEARANCE = 48.dp

/**
 * Bottom sheet chrome shared by the Compose Timeline sheets: white surface,
 * rounded top, SDK drag handle, content capped to the screen height so a
 * step's own scrolling takes over instead of the sheet overflowing.
 *
 * @param dismissible false keeps the sheet up until [onDismissRequest] is called by content
 * @param drawsOwnBackground true for layouts that paint their own rounded surface: the sheet
 *   container turns transparent and shows no drag handle, as the View-based dialogs did
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimelineSheet(
    onDismissRequest: () -> Unit,
    dismissible: Boolean = true,
    drawsOwnBackground: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val maxContentHeight = rememberMaxSheetContentHeight()
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { dismissible || it != SheetValue.Hidden }
        ),
        containerColor = if (drawsOwnBackground) Color.Transparent else colorResource(R.color.trp_white),
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = if (drawsOwnBackground) null else {
            {
                Box(
                    Modifier
                        .padding(top = 8.dp)
                        .size(width = 32.dp, height = 4.dp)
                        .background(colorResource(R.color.trp_borderActive), RoundedCornerShape(2.dp))
                )
            }
        }
    ) {
        Box(Modifier.heightIn(max = maxContentHeight), content = content)
    }
}

/**
 * Screen height minus system bars and the drag handle, measured in the host
 * window: a sheet's own popup window reports no usable insets.
 */
@Composable
internal fun rememberMaxSheetContentHeight(): Dp {
    val density = LocalDensity.current
    val view = LocalView.current
    val topInset = WindowInsets.statusBars.getTop(density)
    val bottomInset = WindowInsets.navigationBars.getBottom(density)
    return remember(view, topInset, bottomInset) {
        val windowHeight = view.rootView.height.takeIf { it > 0 }
            ?: view.resources.displayMetrics.heightPixels
        with(density) { (windowHeight - topInset - bottomInset).toDp() } - SHEET_TOP_CLEARANCE
    }
}

/**
 * Creates a ViewModel from the SDK's Dagger factory in a store owned by this
 * composition, so a sheet gets a fresh ViewModel each time it is shown and
 * releases it when it leaves the composition.
 */
@Composable
internal inline fun <reified VM : ViewModel> rememberSheetViewModel(): VM {
    val factory = LocalTimelineViewModelFactory.current
    val owner = remember {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    DisposableEffect(owner) {
        onDispose { owner.viewModelStore.clear() }
    }
    return remember(owner, factory) { ViewModelProvider(owner, factory)[VM::class.java] }
}

/**
 * Binds a [ComposeViewListener] to [viewModel] for as long as the caller is
 * composed, so alerts and warning dialogs it raises reach the Compose host.
 */
@Composable
internal fun BindViewListener(viewModel: BaseViewModel, onExit: () -> Unit = {}) {
    val context = LocalContext.current
    val overlays = LocalTimelineOverlays.current
    val currentOnExit by rememberUpdatedState(onExit)
    DisposableEffect(viewModel, context, overlays) {
        val listener = ComposeViewListener(context, overlays) { currentOnExit() }
        viewModel.viewListener = listener
        onDispose {
            if (viewModel.viewListener === listener) {
                viewModel.viewListener = null
            }
        }
    }
}
