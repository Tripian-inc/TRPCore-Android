package com.tripian.trpcore.ui.common.loader

import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tripian.trpcore.R
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.databinding.DialogLottieBottomSheetBinding
import com.tripian.trpcore.databinding.DialogLottieFullScreenBinding
import com.tripian.trpcore.util.LanguageConst

/**
 * Lottie loading API.
 *
 * Two presentation modes:
 *  - [LottieLoadingPresentation.FULL_SCREEN] — overlay on top of current activity. Supports rotating text.
 *  - [LottieLoadingPresentation.BOTTOM_SHEET] — modal bottom sheet. Single text only, drag disabled.
 *
 * Used for long-running operations (timeline create, segment create, step delete/edit, refresh).
 * Existing simple progress (DGLockScreen) is kept for short network calls.
 */
enum class LottieLoadingPresentation {
    FULL_SCREEN,
    BOTTOM_SHEET
}

sealed class LottieLoadingText {
    object None : LottieLoadingText()
    data class Single(val text: String) : LottieLoadingText()
    data class Rotating(val texts: List<String>) : LottieLoadingText() {
        companion object {
            fun default(): Rotating {
                val repo = TRPCore.core.miscRepository
                return Rotating(
                    listOf(
                        repo.getLanguageValueForKey(LanguageConst.LOADING_TEXT_FINDING_ACTIVITIES)
                            .ifBlank { "Finding the best activities in your city" },
                        repo.getLanguageValueForKey(LanguageConst.LOADING_TEXT_TAILORING_RECOMMENDATIONS)
                            .ifBlank { "Tailoring recommendations to your preferences" },
                        repo.getLanguageValueForKey(LanguageConst.LOADING_TEXT_OPTIMIZING_ROUTE)
                            .ifBlank { "Optimizing your route" }
                    )
                )
            }
        }
    }
}

/**
 * Singleton helper to show/hide Lottie loading dialogs. Prevents double-show by checking
 * existing fragment tags.
 */
object LottieLoading {

    private const val TAG_FULL_SCREEN = "lottie_loading_full_screen"
    private const val TAG_BOTTOM_SHEET = "lottie_loading_bottom_sheet"

    @JvmStatic
    fun show(
        activity: FragmentActivity,
        presentation: LottieLoadingPresentation,
        text: LottieLoadingText
    ) {
        val fm = activity.supportFragmentManager
        if (fm.isStateSaved || fm.isDestroyed) return

        when (presentation) {
            LottieLoadingPresentation.FULL_SCREEN -> {
                if (fm.findFragmentByTag(TAG_FULL_SCREEN) != null) return
                LottieFullScreenDialog.newInstance(text).show(fm, TAG_FULL_SCREEN)
            }
            LottieLoadingPresentation.BOTTOM_SHEET -> {
                if (fm.findFragmentByTag(TAG_BOTTOM_SHEET) != null) return
                LottieBottomSheetDialog.newInstance(text).show(fm, TAG_BOTTOM_SHEET)
            }
        }
    }

    @JvmStatic
    fun hide(activity: FragmentActivity) {
        val fm = activity.supportFragmentManager
        if (fm.isStateSaved || fm.isDestroyed) return
        (fm.findFragmentByTag(TAG_FULL_SCREEN) as? DialogFragment)?.dismissAllowingStateLoss()
        (fm.findFragmentByTag(TAG_BOTTOM_SHEET) as? DialogFragment)?.dismissAllowingStateLoss()
    }
}

class LottieFullScreenDialog : DialogFragment() {

    private var _binding: DialogLottieFullScreenBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())
    private var rotateRunnable: Runnable? = null
    private var rotatingTexts: List<String> = emptyList()
    private var rotatingIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.TrpFullScreenTransparentDialog)
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogLottieFullScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mode = argText()
        applyText(mode)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun onDestroyView() {
        stopRotation()
        _binding = null
        super.onDestroyView()
    }

    private fun argText(): LottieLoadingText {
        val mode = arguments?.getString(ARG_MODE) ?: MODE_ROTATING
        return when (mode) {
            MODE_NONE -> LottieLoadingText.None
            MODE_SINGLE -> LottieLoadingText.Single(
                arguments?.getString(ARG_TEXT_SINGLE).orEmpty()
            )
            MODE_ROTATING -> {
                val texts = arguments?.getStringArrayList(ARG_TEXT_LIST).orEmpty()
                if (texts.isEmpty()) LottieLoadingText.Rotating.default()
                else LottieLoadingText.Rotating(texts)
            }
            else -> LottieLoadingText.None
        }
    }

    private fun applyText(text: LottieLoadingText) {
        when (text) {
            is LottieLoadingText.None -> binding.tvLoadingText.visibility = View.GONE
            is LottieLoadingText.Single -> {
                binding.tvLoadingText.text = text.text
                binding.tvLoadingText.visibility = View.VISIBLE
            }
            is LottieLoadingText.Rotating -> {
                rotatingTexts = text.texts.filter { it.isNotBlank() }
                if (rotatingTexts.isEmpty()) {
                    binding.tvLoadingText.visibility = View.GONE
                    return
                }
                binding.tvLoadingText.visibility = View.VISIBLE
                rotatingIndex = 0
                binding.tvLoadingText.text = rotatingTexts[rotatingIndex]
                startRotation()
            }
        }
    }

    private fun startRotation() {
        if (rotatingTexts.size <= 1) return
        rotateRunnable = object : Runnable {
            override fun run() {
                if (_binding == null) return
                rotatingIndex += 1
                if (rotatingIndex >= rotatingTexts.size) {
                    // Last text stays visible
                    return
                }
                binding.tvLoadingText.text = rotatingTexts[rotatingIndex]
                if (rotatingIndex < rotatingTexts.size - 1) {
                    handler.postDelayed(this, ROTATION_INTERVAL_MS)
                }
            }
        }
        handler.postDelayed(rotateRunnable!!, ROTATION_INTERVAL_MS)
    }

    private fun stopRotation() {
        rotateRunnable?.let { handler.removeCallbacks(it) }
        rotateRunnable = null
    }

    companion object {
        private const val ROTATION_INTERVAL_MS = 3_500L
        private const val ARG_MODE = "lottie_text_mode"
        private const val ARG_TEXT_SINGLE = "lottie_text_single"
        private const val ARG_TEXT_LIST = "lottie_text_list"
        private const val MODE_NONE = "none"
        private const val MODE_SINGLE = "single"
        private const val MODE_ROTATING = "rotating"

        fun newInstance(text: LottieLoadingText) = LottieFullScreenDialog().apply {
            arguments = Bundle().apply {
                when (text) {
                    is LottieLoadingText.None -> putString(ARG_MODE, MODE_NONE)
                    is LottieLoadingText.Single -> {
                        putString(ARG_MODE, MODE_SINGLE)
                        putString(ARG_TEXT_SINGLE, text.text)
                    }
                    is LottieLoadingText.Rotating -> {
                        putString(ARG_MODE, MODE_ROTATING)
                        putStringArrayList(ARG_TEXT_LIST, ArrayList(text.texts))
                    }
                }
            }
        }
    }
}

class LottieBottomSheetDialog : BottomSheetDialogFragment() {

    private var _binding: DialogLottieBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), theme).apply {
            setCanceledOnTouchOutside(false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogLottieBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val text = arguments?.getString(ARG_TEXT).orEmpty()
        if (text.isBlank()) {
            binding.tvLoadingText.visibility = View.GONE
        } else {
            binding.tvLoadingText.text = text
        }
    }

    override fun onStart() {
        super.onStart()
        val bsDialog = dialog as? BottomSheetDialog
        // Force expanded + lock drag
        bsDialog?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            isDraggable = false
            skipCollapsed = true
        }
        // Material's design_bottom_sheet ships with a solid white drawable that
        // hides the rounded background on our root view. Clear it so the
        // trp_bg_bottom_sheet drawable's rounded top corners are visible.
        bsDialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.setBackgroundResource(android.R.color.transparent)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_TEXT = "lottie_bottom_text"

        fun newInstance(text: LottieLoadingText) = LottieBottomSheetDialog().apply {
            val resolved = when (text) {
                is LottieLoadingText.Single -> text.text
                is LottieLoadingText.None -> ""
                is LottieLoadingText.Rotating -> text.texts.firstOrNull().orEmpty()
            }
            arguments = Bundle().apply { putString(ARG_TEXT, resolved) }
        }
    }
}
