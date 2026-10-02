package com.tripian.trpcore.ui.timeline.compose.core

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tripian.trpcore.R
import com.tripian.trpcore.util.WarningDialogRequest

/**
 * Flow-wide overlay state of the Compose Timeline: the warning dialog a
 * ViewModel asked for through [com.tripian.trpcore.base.BaseViewModel.showDialog].
 */
class TimelineOverlayState {
    var dialog by mutableStateOf<WarningDialogRequest?>(null)
        private set

    fun showDialog(request: WarningDialogRequest) {
        dialog = request
    }

    fun dismissDialog() {
        dialog = null
    }
}

val LocalTimelineOverlays = staticCompositionLocalOf<TimelineOverlayState> {
    error("LocalTimelineOverlays is not provided. Timeline screens must run inside TimelineNexus.")
}

/** Compose counterpart of the FRWarning dialog fragment, rendered from [overlays]. */
@Composable
internal fun TimelineWarningDialogHost(overlays: TimelineOverlayState) {
    val request = overlays.dialog ?: return
    Dialog(
        onDismissRequest = { if (request.isCloseEnable) overlays.dismissDialog() },
        properties = DialogProperties(
            dismissOnBackPress = request.isCloseEnable,
            dismissOnClickOutside = request.isCloseEnable
        )
    ) {
        Column(
            modifier = Modifier
                .width(320.dp)
                .background(colorResource(R.color.trp_white), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp)
                .padding(top = 20.dp, bottom = 14.dp)
        ) {
            request.title?.takeIf { it.isNotBlank() }?.let { title ->
                Text(
                    text = title,
                    color = colorResource(R.color.trp_head),
                    fontFamily = TimelineFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            request.message?.takeIf { it.isNotBlank() }?.let { message ->
                Text(
                    text = message,
                    color = colorResource(R.color.trp_head),
                    fontFamily = TimelineFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                request.negativeText?.let { text ->
                    OutlinedButton(
                        onClick = {
                            overlays.dismissDialog()
                            request.onNegative()
                        },
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, colorResource(R.color.trp_primary)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = colorResource(R.color.trp_white),
                            contentColor = colorResource(R.color.trp_primary)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(text, fontFamily = TimelineFontFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                    }
                }
                request.positiveText?.let { text ->
                    Button(
                        onClick = {
                            overlays.dismissDialog()
                            request.onPositive()
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.trp_primary),
                            contentColor = colorResource(R.color.trp_white)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(text, fontFamily = TimelineFontFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
