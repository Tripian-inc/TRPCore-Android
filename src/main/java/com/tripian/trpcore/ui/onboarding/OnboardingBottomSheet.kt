package com.tripian.trpcore.ui.onboarding

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tripian.trpcore.R
import com.tripian.trpcore.base.BaseBottomDialogFragment
import com.tripian.trpcore.databinding.BottomSheetOnboardingBinding
import com.tripian.trpcore.util.LanguageConst

class OnboardingBottomSheet : BaseBottomDialogFragment<BottomSheetOnboardingBinding, OnboardingVM>(
    BottomSheetOnboardingBinding::inflate
) {

    private var onCompleteListener: (() -> Unit)? = null
    private var onCompleteListenerCalled = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        isCancelable = false
        return dialog
    }

    override fun isDragEnable() = false

    override fun setListeners() {
        super.setListeners()

        binding.ivClose.setOnClickListener {
            viewModel.didTapDismiss()
            invokeCompleteListener()
            dismiss()
        }

        binding.btnContinue.setOnClickListener {
            viewModel.didTapContinue()
            invokeCompleteListener()
            dismiss()
        }

        binding.btnSkip.setOnClickListener {
            viewModel.didTapDismiss()
            invokeCompleteListener()
            dismiss()
        }
    }

    /**
     * Invokes the complete listener at most once, guarding against double calls
     * from button clicks and onDismiss.
     */
    private fun invokeCompleteListener() {
        if (!onCompleteListenerCalled) {
            onCompleteListenerCalled = true
            onCompleteListener?.invoke()
        }
    }

    /** Fallback in case the listener wasn't called from a button click (e.g. after configuration change). */
    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        invokeCompleteListener()
    }

    override fun setReceivers() {
        super.setReceivers()
        setupUI()
    }

    private fun setupUI() {
        binding.tvTitle.text = getLanguageForKey(LanguageConst.ONBOARDING_TITLE)

        binding.tvBetaBadge.text = getLanguageForKey(LanguageConst.ONBOARDING_BADGE_BETA)

        binding.tvFeature1.text = createFeatureSpannable(
            getLanguageForKey(LanguageConst.ONBOARDING_FEATURE1_TITLE),
            getLanguageForKey(LanguageConst.ONBOARDING_FEATURE1_DESC)
        )

        binding.tvFeature2.text = createFeatureSpannable(
            getLanguageForKey(LanguageConst.ONBOARDING_FEATURE2_TITLE),
            getLanguageForKey(LanguageConst.ONBOARDING_FEATURE2_DESC)
        )

        binding.tvFeature3.text = createFeatureSpannable(
            getLanguageForKey(LanguageConst.ONBOARDING_FEATURE3_TITLE),
            getLanguageForKey(LanguageConst.ONBOARDING_FEATURE3_DESC)
        )

        binding.tvFooterLine1.text = getLanguageForKey(LanguageConst.ONBOARDING_FOOTER_LINE1)
        binding.tvFooterLine2.text = getLanguageForKey(LanguageConst.ONBOARDING_FOOTER_LINE2)

        binding.btnContinue.text = getLanguageForKey(LanguageConst.ONBOARDING_BUTTON_CONTINUE)
        binding.btnSkip.text = getLanguageForKey(LanguageConst.ONBOARDING_BUTTON_SKIP)
    }

    /** Creates a SpannableString with a bold title followed by a regular-weight description. */
    private fun createFeatureSpannable(title: String, description: String): SpannableString {
        val fullText = "$title $description"
        val spannable = SpannableString(fullText)

        val boldTypeface = ResourcesCompat.getFont(requireContext(), R.font.montserrat_bold)

        boldTypeface?.let { typeface ->
            spannable.setSpan(
                CustomTypefaceSpan(typeface),
                0,
                title.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        return spannable
    }

    /** Custom span that applies a specific Typeface to text. */
    private class CustomTypefaceSpan(private val typeface: Typeface) : MetricAffectingSpan() {
        override fun updateDrawState(paint: TextPaint) {
            applyTypeface(paint)
        }

        override fun updateMeasureState(paint: TextPaint) {
            applyTypeface(paint)
        }

        private fun applyTypeface(paint: Paint) {
            paint.typeface = typeface
        }
    }

    fun setOnCompleteListener(listener: () -> Unit) {
        onCompleteListener = listener
    }

    companion object {
        fun newInstance(): OnboardingBottomSheet {
            return OnboardingBottomSheet()
        }
    }
}
