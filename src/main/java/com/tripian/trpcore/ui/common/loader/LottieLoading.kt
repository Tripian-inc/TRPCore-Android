package com.tripian.trpcore.ui.common.loader

import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tripian.trpcore.R
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.databinding.DialogLottieBottomSheetBinding
import com.tripian.trpcore.util.LanguageConst

/**
 * Lottie loading API.
 *
 * Two presentation modes:
 *  - [LottieLoadingPresentation.FULL_SCREEN] — view-attached overlay added
 *    directly to the activity's content frame (android.R.id.content). This
 *    avoids the one-frame delay that a DialogFragment incurs from its
 *    asynchronous WindowManager.addView() — the loader is part of the
 *    activity's own window and therefore renders on the first frame the
 *    activity is drawn.
 *  - [LottieLoadingPresentation.BOTTOM_SHEET] — modal bottom sheet. Single
 *    text only, drag disabled. Still a DialogFragment because its
 *    slide-from-bottom semantics need a separate window.
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
 * Singleton helper to show/hide Lottie loaders. Prevents double-show by checking
 * existing tags / fragments.
 */
object LottieLoading {

    private const val TAG_FULL_SCREEN_VIEW = "lottie_loading_full_screen_view"
    private const val TAG_BOTTOM_SHEET = "lottie_loading_bottom_sheet"

    private const val ROTATION_INTERVAL_MS = 3_500L
    private const val OVERLAY_ELEVATION_DP = 32f

    // One rotation handle per activity — WeakHashMap so we don't pin the
    // FragmentActivity once it's gone.
    private val rotations = java.util.WeakHashMap<FragmentActivity, RotationHandle>()

    private data class RotationHandle(val handler: Handler, val runnable: Runnable)

    @JvmStatic
    fun show(
        activity: FragmentActivity,
        presentation: LottieLoadingPresentation,
        text: LottieLoadingText
    ) {
        when (presentation) {
            LottieLoadingPresentation.FULL_SCREEN -> showFullScreenView(activity, text)
            LottieLoadingPresentation.BOTTOM_SHEET -> showBottomSheet(activity, text)
        }
    }

    @JvmStatic
    fun hide(activity: FragmentActivity) {
        hideFullScreenView(activity)
        hideBottomSheet(activity)
    }

    // ------------------------------------------------------------------
    // FULL_SCREEN — view-attached overlay
    // ------------------------------------------------------------------

    private fun showFullScreenView(activity: FragmentActivity, text: LottieLoadingText) {
        val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        val existing = content.findViewWithTag<View>(TAG_FULL_SCREEN_VIEW)
        if (existing != null) {
            // Already attached — just refresh the text.
            applyText(activity, existing, text)
            return
        }
        val overlay = LayoutInflater.from(activity)
            .inflate(R.layout.dialog_lottie_full_screen, content, false)
        overlay.tag = TAG_FULL_SCREEN_VIEW
        overlay.isClickable = true
        overlay.isFocusable = true
        overlay.elevation = OVERLAY_ELEVATION_DP * activity.resources.displayMetrics.density
        applyText(activity, overlay, text)
        content.addView(overlay)
    }

    private fun hideFullScreenView(activity: FragmentActivity) {
        cancelRotation(activity)
        val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        val overlay = content.findViewWithTag<View>(TAG_FULL_SCREEN_VIEW) ?: return
        content.removeView(overlay)
    }

    private fun applyText(activity: FragmentActivity, overlay: View, text: LottieLoadingText) {
        val tv = overlay.findViewById<TextView>(R.id.tvLoadingText) ?: return
        cancelRotation(activity)
        when (text) {
            is LottieLoadingText.None -> tv.visibility = View.GONE
            is LottieLoadingText.Single -> {
                tv.text = text.text
                tv.visibility = View.VISIBLE
            }
            is LottieLoadingText.Rotating -> {
                val texts = text.texts.filter { it.isNotBlank() }
                if (texts.isEmpty()) {
                    tv.visibility = View.GONE
                } else {
                    tv.visibility = View.VISIBLE
                    tv.text = texts[0]
                    startRotation(activity, tv, texts)
                }
            }
        }
    }

    private fun startRotation(activity: FragmentActivity, tv: TextView, texts: List<String>) {
        if (texts.size <= 1) return
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            private var index = 0
            override fun run() {
                index += 1
                if (index >= texts.size) return // Last text stays visible.
                tv.text = texts[index]
                if (index < texts.size - 1) {
                    handler.postDelayed(this, ROTATION_INTERVAL_MS)
                }
            }
        }
        rotations[activity] = RotationHandle(handler, runnable)
        handler.postDelayed(runnable, ROTATION_INTERVAL_MS)
    }

    private fun cancelRotation(activity: FragmentActivity) {
        val handle = rotations.remove(activity) ?: return
        handle.handler.removeCallbacks(handle.runnable)
    }

    // ------------------------------------------------------------------
    // BOTTOM_SHEET — DialogFragment (semantics need a separate window)
    // ------------------------------------------------------------------

    private fun showBottomSheet(activity: FragmentActivity, text: LottieLoadingText) {
        val fm = activity.supportFragmentManager
        if (fm.isStateSaved || fm.isDestroyed) return
        if (fm.findFragmentByTag(TAG_BOTTOM_SHEET) != null) return
        LottieBottomSheetDialog.newInstance(text).show(fm, TAG_BOTTOM_SHEET)
    }

    private fun hideBottomSheet(activity: FragmentActivity) {
        val fm = activity.supportFragmentManager
        if (fm.isStateSaved || fm.isDestroyed) return
        (fm.findFragmentByTag(TAG_BOTTOM_SHEET) as? DialogFragment)?.dismissAllowingStateLoss()
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
