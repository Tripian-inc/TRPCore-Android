package com.tripian.trpcore.ui.timeline.compose.sheets

import android.graphics.Paint
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.content.res.ResourcesCompat
import com.tripian.trpcore.R
import com.tripian.trpcore.databinding.BottomSheetOnboardingBinding
import com.tripian.trpcore.ui.onboarding.OnboardingBottomSheet
import com.tripian.trpcore.ui.onboarding.OnboardingVM
import com.tripian.trpcore.ui.timeline.compose.core.BindViewListener
import com.tripian.trpcore.ui.timeline.compose.core.BindingHost
import com.tripian.trpcore.ui.timeline.compose.core.TimelineSheet
import com.tripian.trpcore.ui.timeline.compose.core.rememberSheetViewModel
import com.tripian.trpcore.util.LanguageConst

/**
 * Compose counterpart of [OnboardingBottomSheet]: the first-run feature sheet.
 * It cannot be swiped or tapped away; every exit records the choice on
 * [OnboardingVM] and calls [onComplete] exactly once before [onDismiss].
 */
@Composable
internal fun OnboardingSheet(
    onComplete: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: OnboardingVM = rememberSheetViewModel()
) {
    BindViewListener(viewModel)
    val completion = remember { OnboardingCompletion(onComplete, onDismiss) }

    TimelineSheet(
        onDismissRequest = { completion.finish() },
        dismissible = false,
        drawsOwnBackground = true
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            BindingHost(
                inflate = BottomSheetOnboardingBinding::inflate,
                modifier = Modifier.fillMaxWidth()
            ) { binding, _, _ ->
                binding.applyTexts(viewModel)
                binding.ivClose.setOnClickListener {
                    viewModel.didTapDismiss()
                    completion.finish()
                }
                binding.btnContinue.setOnClickListener {
                    viewModel.didTapContinue()
                    completion.finish()
                }
                binding.btnSkip.setOnClickListener {
                    viewModel.didTapDismiss()
                    completion.finish()
                }
            }
        }
    }
}

private class OnboardingCompletion(
    private val onComplete: () -> Unit,
    private val onDismiss: () -> Unit
) {
    private var completed = false

    fun finish() {
        if (!completed) {
            completed = true
            onComplete()
        }
        onDismiss()
    }
}

private fun BottomSheetOnboardingBinding.applyTexts(viewModel: OnboardingVM) {
    tvTitle.text = viewModel.getLanguageForKey(LanguageConst.ONBOARDING_TITLE)
    tvBetaBadge.text = viewModel.getLanguageForKey(LanguageConst.ONBOARDING_BADGE_BETA)
    tvFeature1.text = featureSpannable(
        viewModel.getLanguageForKey(LanguageConst.ONBOARDING_FEATURE1_TITLE),
        viewModel.getLanguageForKey(LanguageConst.ONBOARDING_FEATURE1_DESC)
    )
    tvFeature2.text = featureSpannable(
        viewModel.getLanguageForKey(LanguageConst.ONBOARDING_FEATURE2_TITLE),
        viewModel.getLanguageForKey(LanguageConst.ONBOARDING_FEATURE2_DESC)
    )
    tvFeature3.text = featureSpannable(
        viewModel.getLanguageForKey(LanguageConst.ONBOARDING_FEATURE3_TITLE),
        viewModel.getLanguageForKey(LanguageConst.ONBOARDING_FEATURE3_DESC)
    )
    tvFooterLine1.text = viewModel.getLanguageForKey(LanguageConst.ONBOARDING_FOOTER_LINE1)
    tvFooterLine2.text = viewModel.getLanguageForKey(LanguageConst.ONBOARDING_FOOTER_LINE2)
    btnContinue.text = viewModel.getLanguageForKey(LanguageConst.ONBOARDING_BUTTON_CONTINUE)
    btnSkip.text = viewModel.getLanguageForKey(LanguageConst.ONBOARDING_BUTTON_SKIP)
}

/** Bold title followed by the regular description, as one feature line. */
private fun BottomSheetOnboardingBinding.featureSpannable(title: String, description: String): SpannableString {
    val spannable = SpannableString("$title $description")
    ResourcesCompat.getFont(root.context, R.font.montserrat_bold)?.let { typeface ->
        spannable.setSpan(TypefaceSpan(typeface), 0, title.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    return spannable
}

private class TypefaceSpan(private val typeface: Typeface) : MetricAffectingSpan() {
    override fun updateDrawState(paint: TextPaint) = applyTypeface(paint)

    override fun updateMeasureState(paint: TextPaint) = applyTypeface(paint)

    private fun applyTypeface(paint: Paint) {
        paint.typeface = typeface
    }
}
