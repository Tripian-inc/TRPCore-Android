package com.tripian.trpcore.ui.timeline.compose.addplan

import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import com.tripian.trpcore.R
import com.tripian.trpcore.domain.model.timeline.AddPlanData
import com.tripian.trpcore.domain.model.timeline.AddPlanStep
import com.tripian.trpcore.domain.model.timeline.ManualCategory
import com.tripian.trpcore.ui.timeline.activity.ACActivityListing
import com.tripian.trpcore.ui.timeline.addplan.AddPlanContainerVM
import com.tripian.trpcore.ui.timeline.compose.core.TimelineLoaderOverlay
import com.tripian.trpcore.ui.timeline.poilisting.ACPOIListing
import com.tripian.trpcore.ui.timeline.poilisting.POIListingType
import com.tripian.trpcore.util.LanguageConst

/**
 * Compose AddPlan wizard container — replaces AddPlanContainerBottomSheet for
 * Compose hosts, sharing AddPlanContainerVM. Steps are driven by the VM's
 * currentStep; the footer Continue/Clear behavior, manual listing launches and
 * the in-sheet Lottie loader mirror the Fragment-based sheet.
 *
 * @param errorMessage segment-create error surfaced over the sheet; cleared via [onErrorDismiss]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlanSheet(
    viewModel: AddPlanContainerVM,
    errorMessage: String?,
    onErrorDismiss: () -> Unit,
    onDismissRequest: () -> Unit,
    onAddPlanComplete: (AddPlanData) -> Unit,
    onSegmentCreated: (Int) -> Unit
) {
    val context = LocalContext.current
    val currentStep by viewModel.currentStep.observeAsState(AddPlanStep.SELECT_DAY_AND_CITY)
    val titleKey by viewModel.titleKey.observeAsState()
    val continueEnabled by viewModel.continueButtonEnabled.observeAsState(false)
    val continueTextKey by viewModel.continueButtonTextKey.observeAsState()
    val showClearSelection by viewModel.showClearSelection.observeAsState(false)
    val loaderEvent by viewModel.lottieLoadingEvent.observeAsState()

    val manualListingLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedDayIndex = result.data?.getIntExtra(
                ACActivityListing.RESULT_SELECTED_DAY_INDEX,
                viewModel.planData.selectedDayIndex
            ) ?: 0
            onSegmentCreated(selectedDayIndex)
        }
    }

    val openManualListing by viewModel.openManualListing.observeAsState()
    LaunchedEffect(openManualListing) {
        openManualListing?.let { category ->
            launchManualListing(context, viewModel, category, manualListingLauncher)
            viewModel.clearOpenManualListing()
        }
    }

    val dismissEvent by viewModel.dismissSheet.observeAsState()
    LaunchedEffect(dismissEvent) {
        if (dismissEvent == true) onDismissRequest()
    }

    val completeEvent by viewModel.onComplete.observeAsState()
    LaunchedEffect(completeEvent) {
        completeEvent?.let { onAddPlanComplete(it) }
    }

    val navigateBack by viewModel.navigateBack.observeAsState()
    LaunchedEffect(navigateBack) {
        if (navigateBack == true) viewModel.clearNavigateBack()
    }

    val resetEvent by viewModel.resetToFirstStep.observeAsState()
    LaunchedEffect(resetEvent) {
        if (resetEvent == true) viewModel.clearResetToFirstStep()
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { it != SheetValue.Hidden }
        ),
        containerColor = colorResource(R.color.trp_white),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 8.dp)
                    .size(width = 32.dp, height = 4.dp)
                    .background(colorResource(R.color.trp_bgDisabled), RoundedCornerShape(2.dp))
            )
        }
    ) {
        Box {
            Column {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 20.dp)
                        .padding(vertical = 12.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.trp_ic_back),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .size(24.dp)
                            .align(Alignment.CenterStart)
                            .clickable {
                                if (viewModel.currentStep.value == AddPlanStep.SELECT_DAY_AND_CITY) {
                                    onDismissRequest()
                                } else {
                                    viewModel.goToPreviousStep()
                                }
                            }
                    )
                    Text(
                        text = titleKey?.let { viewModel.getLanguageForKey(it) }.orEmpty(),
                        color = colorResource(R.color.trp_text_primary),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    Image(
                        painter = painterResource(R.drawable.trp_ic_close),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(24.dp)
                            .padding(4.dp)
                            .align(Alignment.CenterEnd)
                            .clickable { onDismissRequest() }
                    )
                }

                Box(Modifier.weight(1f, fill = false)) {
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            if (targetState.ordinal >= initialState.ordinal) {
                                slideInHorizontally(tween(300)) { it } togetherWith
                                    slideOutHorizontally(tween(300)) { -it }
                            } else {
                                slideInHorizontally(tween(300)) { -it } togetherWith
                                    slideOutHorizontally(tween(300)) { it }
                            }
                        },
                        label = "addPlanStep"
                    ) { step ->
                        when (step) {
                            AddPlanStep.SELECT_DAY_AND_CITY -> AddPlanSelectDayStep(viewModel)
                            AddPlanStep.TIME_AND_TRAVELERS -> AddPlanTimeTravelersStep(viewModel)
                            AddPlanStep.CATEGORY_SELECTION -> AddPlanCategorySelectionStep(viewModel)
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(colorResource(R.color.trp_white))
                        .padding(horizontal = 16.dp)
                ) {
                    if (showClearSelection) {
                        Text(
                            text = viewModel.getLanguageForKey(LanguageConst.ADD_PLAN_CLEAR_SELECTION),
                            color = colorResource(R.color.trp_fgWeak),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.clearSelection() }
                                .padding(horizontal = 16.dp)
                        )
                    }
                    Button(
                        onClick = { viewModel.goToNextStep() },
                        enabled = continueEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.trp_primary),
                            contentColor = colorResource(R.color.trp_white),
                            disabledContainerColor = colorResource(R.color.trp_bgDisabled),
                            disabledContentColor = colorResource(R.color.trp_fgWeak)
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(
                            text = continueTextKey?.let { viewModel.getLanguageForKey(it) }.orEmpty(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            loaderEvent?.takeIf { it.show }?.let {
                TimelineLoaderOverlay(it.text, Modifier.matchParentSize())
            }

            errorMessage?.let { message ->
                AddPlanErrorToast(
                    message = message,
                    onDismiss = onErrorDismiss,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun AddPlanErrorToast(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(colorResource(R.color.trp_error_bg), RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = message,
            color = colorResource(R.color.trp_error_message),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        Image(
            painter = painterResource(R.drawable.trp_ic_close),
            contentDescription = null,
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                colorResource(R.color.trp_error_message)
            ),
            modifier = Modifier
                .size(20.dp)
                .clickable { onDismiss() }
        )
    }
}

private fun launchManualListing(
    context: Context,
    viewModel: AddPlanContainerVM,
    category: ManualCategory,
    launcher: ActivityResultLauncher<Intent>
) {
    val planData = viewModel.getValidPlanData() ?: return
    val tripHash = viewModel.getTripHash() ?: return
    val intent = when (category) {
        ManualCategory.ACTIVITIES -> ACActivityListing.launch(
            context = context,
            planData = planData,
            tripHash = tripHash
        )
        ManualCategory.PLACES_OF_INTEREST -> ACPOIListing.launch(
            context = context,
            planData = planData,
            tripHash = tripHash,
            listingType = POIListingType.PLACES_OF_INTEREST
        )
        ManualCategory.EAT_AND_DRINK -> ACPOIListing.launch(
            context = context,
            planData = planData,
            tripHash = tripHash,
            listingType = POIListingType.EAT_AND_DRINK
        )
    }
    launcher.launch(intent)
}
