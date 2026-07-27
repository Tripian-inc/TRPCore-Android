package com.tripian.trpcore.ui.timeline.compose.core

import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.tripian.trpcore.R
import com.tripian.trpcore.ui.common.loader.LottieLoading
import com.tripian.trpcore.ui.common.loader.LottieLoadingText
import kotlinx.coroutines.delay

private const val ROTATION_INTERVAL_MS = 3_500L

/**
 * Full-screen Lottie loader for Compose screens. Reuses the View-based
 * dialog_lottie_full_screen layout so visuals match the Activity flow, and
 * consumes all touch input while visible.
 */
@Composable
internal fun TimelineLoaderOverlay(
    text: LottieLoadingText,
    modifier: Modifier = Modifier
) {
    val displayText = loaderText(text)
    AndroidView(
        factory = { ctx ->
            FrameLayout(ctx).also { frame ->
                LayoutInflater.from(ctx).inflate(R.layout.dialog_lottie_full_screen, frame, true)
                LottieLoading.tintLoader(frame)
            }
        },
        update = { frame ->
            frame.findViewById<TextView>(R.id.tvLoadingText)?.apply {
                visibility = if (displayText.isBlank()) View.GONE else View.VISIBLE
                this.text = displayText
            }
        },
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    )
}

@Composable
private fun loaderText(text: LottieLoadingText): String = when (text) {
    is LottieLoadingText.None -> ""
    is LottieLoadingText.Single -> text.text
    is LottieLoadingText.Rotating -> {
        val texts = remember(text) { text.texts.filter { it.isNotBlank() } }
        var index by remember(texts) { mutableIntStateOf(0) }
        LaunchedEffect(texts) {
            while (index < texts.lastIndex) {
                delay(ROTATION_INTERVAL_MS)
                index += 1
            }
        }
        texts.getOrElse(index) { "" }
    }
}
