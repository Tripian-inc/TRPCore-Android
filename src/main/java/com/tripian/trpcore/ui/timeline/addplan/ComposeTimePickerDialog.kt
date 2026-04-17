package com.tripian.trpcore.ui.timeline.addplan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerColors
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.tripian.trpcore.R
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.util.LanguageConst

/**
 * Compose-based Time Picker Dialog
 * Displayed in center of screen (not bottom sheet) with custom buttons
 */
class ComposeTimePickerDialog(
    private val initialHour: Int = 10,
    private val initialMinute: Int = 0,
    private val cancelText: String,
    private val selectText: String,
    private val onTimeSelected: (hour: Int, minute: Int) -> Unit,
    private val onDismiss: () -> Unit = {}
) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                TimePickerDialogContent(
                    initialHour = initialHour,
                    initialMinute = initialMinute,
                    cancelText = cancelText,
                    selectText = selectText,
                    onConfirm = { hour, minute ->
                        onTimeSelected(hour, minute)
                        dismiss()
                    },
                    onCancel = {
                        onDismiss()
                        dismiss()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialogContent(
    initialHour: Int,
    initialMinute: Int,
    cancelText: String,
    selectText: String,
    onConfirm: (Int, Int) -> Unit,
    onCancel: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )

    // Load custom fonts
    val mediumFont = FontFamily(
        Font(R.font.medium, FontWeight.Medium)
    )

    // Load colors
    val backgroundColor = colorResource(id = android.R.color.white)
    val textPrimary = colorResource(id = R.color.trp_text_primary)
    val primary = colorResource(id = R.color.trp_primary)
    val bgCloudy = colorResource(id = R.color.trp_bgCloudy)
    val lineWeak = colorResource(id = R.color.trp_bgDisabled)

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = backgroundColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // TimePicker with custom colors
                MaterialTheme(
                    colorScheme = MaterialTheme.colorScheme.copy(
                        primary = textPrimary,  // Clock hand and selected time background
                        onPrimary = Color.White,  // Selected time text
                        onSurface = textPrimary,  // Unselected time text and clock numbers
                        primaryContainer = bgCloudy,  // AM/PM chip background
                        onPrimaryContainer = textPrimary,  // AM/PM text
                        surface = backgroundColor,  // Dialog background
                        secondaryContainer = bgCloudy  // Clock dial background
                    )
                ) {
                    TimePicker(
                        state = timePickerState,
                        modifier = Modifier.padding(vertical = 16.dp),
                        colors = TimePickerColors(
                            clockDialColor = bgCloudy,
                            selectorColor = textPrimary,
                            containerColor = bgCloudy,
                            periodSelectorBorderColor = lineWeak,
                            clockDialSelectedContentColor = Color.White,
                            clockDialUnselectedContentColor = textPrimary,
                            periodSelectorSelectedContainerColor = bgCloudy,
                            periodSelectorUnselectedContainerColor = Color.White,
                            periodSelectorSelectedContentColor = textPrimary,
                            periodSelectorUnselectedContentColor = textPrimary,
                            timeSelectorSelectedContainerColor = bgCloudy,
                            timeSelectorUnselectedContainerColor = Color.White,
                            timeSelectorSelectedContentColor = textPrimary,
                            timeSelectorUnselectedContentColor = textPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Custom Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cancel Button (Text Button)
                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(
                            text = cancelText,
                            color = textPrimary,
                            fontSize = 16.sp,
                            fontFamily = mediumFont,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Confirm Button (Filled Button - TrpButtonPrimary style)
                    Button(
                        onClick = {
                            onConfirm(timePickerState.hour, timePickerState.minute)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(
                            text = selectText,
                            fontSize = 16.sp,
                            fontFamily = mediumFont,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Helper function to show the dialog from Fragment
 */
fun Fragment.showComposeTimePicker(
    initialTime: String? = null,
    onTimeSelected: (hour: Int, minute: Int) -> Unit
) {
    val (hour, minute) = if (initialTime != null) {
        MaterialTimePickerHelper.parseTime24h(initialTime)
    } else {
        Pair(10, 0)
    }

    // Get localized button texts
    val cancelText = TRPCore.core.miscRepository.getLanguageValueForKey(LanguageConst.ADD_PLAN_CANCEL)
    val selectText = TRPCore.core.miscRepository.getLanguageValueForKey(LanguageConst.ADD_PLAN_SELECT)

    val dialog = ComposeTimePickerDialog(
        initialHour = hour,
        initialMinute = minute,
        cancelText = cancelText,
        selectText = selectText,
        onTimeSelected = onTimeSelected
    )

    dialog.show(childFragmentManager, "compose_time_picker")
}
