package com.tripian.trpcore.ui.common.loader

import android.app.Dialog
import android.content.Context
import android.content.ContextWrapper
import android.graphics.ColorFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.SimpleColorFilter
import com.airbnb.lottie.model.KeyPath
import com.airbnb.lottie.value.LottieValueCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tripian.trpcore.R
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.databinding.DialogLottieBottomSheetBinding
import com.tripian.trpcore.util.LanguageConst

/**
 * Presentation modes for the Lottie loader:
 * [FULL_SCREEN] is an overlay attached to the activity's content frame,
 * [BOTTOM_SHEET] is a modal bottom sheet with a single text and drag disabled.
 */
enum class LottieLoadingPresentation {
    FULL_SCREEN,
    BOTTOM_SHEET,
    /**
     * Inline overlay rendered inside an existing bottom sheet's own view tree,
     * resolved by [com.tripian.trpcore.base.BaseBottomDialogFragment]'s loader
     * observer. No-op when the host isn't a bottom sheet.
     */
    INLINE_SHEET
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
    private const val TAG_INLINE_VIEW = "lottie_loading_inline_view"

    private const val ROTATION_INTERVAL_MS = 3_500L
    private const val OVERLAY_ELEVATION_DP = 32f

    /** One rotation handle per activity; weak keys so finished activities aren't pinned. */
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
            LottieLoadingPresentation.INLINE_SHEET -> Unit
        }
    }

    @JvmStatic
    fun hide(activity: FragmentActivity) {
        hideFullScreenView(activity)
        hideBottomSheet(activity)
    }

    // ------------------------------------------------------------------
    // INLINE_SHEET — view-attached overlay inside an arbitrary host
    // ------------------------------------------------------------------

    /**
     * Attaches the loader as a child view of [host]; idempotent (re-showing just
     * refreshes the text). Rotation text only animates when [host]'s context
     * resolves to a [FragmentActivity]. Remove with [hideInline].
     *
     * [host] is the bottom sheet's `design_bottom_sheet` frame, a sibling of the
     * sheet's own rounded-top-corner root view rather than a child of it — the
     * overlay is given a matching rounded-top background so it doesn't square off
     * the sheet's corners while it covers the content underneath.
     */
    @JvmStatic
    fun showInline(host: ViewGroup, text: LottieLoadingText) {
        val activity = host.context.findFragmentActivity()
        val existing = host.findViewWithTag<View>(TAG_INLINE_VIEW)
        if (existing != null) {
            activity?.let { applyText(it, existing, text) }
            return
        }
        val overlay = LayoutInflater.from(host.context)
            .inflate(R.layout.dialog_lottie_full_screen, host, false)
        overlay.tag = TAG_INLINE_VIEW
        overlay.isClickable = true
        overlay.isFocusable = true
        overlay.elevation = OVERLAY_ELEVATION_DP * host.resources.displayMetrics.density
        overlay.setBackgroundResource(R.drawable.trp_bg_bottom_sheet)
        tintLoader(overlay)
        activity?.let { applyText(it, overlay, text) }
        host.addView(overlay)
    }

    @JvmStatic
    fun hideInline(host: ViewGroup) {
        host.context.findFragmentActivity()?.let { cancelRotation(it) }
        val overlay = host.findViewWithTag<View>(TAG_INLINE_VIEW) ?: return
        host.removeView(overlay)
    }

    private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
        is FragmentActivity -> this
        is ContextWrapper -> baseContext.findFragmentActivity()
        else -> null
    }

    /** Recolor the loader animation to the primary brand color. */
    internal fun tintLoader(root: View) {
        val lottie = root.findViewById<LottieAnimationView>(R.id.lottieView) ?: return
        val color = ContextCompat.getColor(root.context, R.color.trp_primary)
        lottie.addValueCallback(
            KeyPath("**"),
            LottieProperty.COLOR_FILTER,
            LottieValueCallback<ColorFilter>(SimpleColorFilter(color))
        )
    }

    // ------------------------------------------------------------------
    // FULL_SCREEN — view-attached overlay
    // ------------------------------------------------------------------

    /**
     * Attaches the overlay to the window's decor view (not android.R.id.content):
     * the content frame stops above the system bars on decor-fitted screens, which
     * would leave the status/nav bar strips uncovered.
     */
    private fun showFullScreenView(activity: FragmentActivity, text: LottieLoadingText) {
        val decor = activity.window.decorView as? ViewGroup ?: return
        val existing = decor.findViewWithTag<View>(TAG_FULL_SCREEN_VIEW)
        if (existing != null) {
            applyText(activity, existing, text)
            return
        }
        val overlay = LayoutInflater.from(activity)
            .inflate(R.layout.dialog_lottie_full_screen, decor, false)
        overlay.tag = TAG_FULL_SCREEN_VIEW
        overlay.isClickable = true
        overlay.isFocusable = true
        overlay.elevation = OVERLAY_ELEVATION_DP * activity.resources.displayMetrics.density
        tintLoader(overlay)
        applyText(activity, overlay, text)
        decor.addView(overlay)
    }

    private fun hideFullScreenView(activity: FragmentActivity) {
        cancelRotation(activity)
        val decor = activity.window.decorView as? ViewGroup ?: return
        val overlay = decor.findViewWithTag<View>(TAG_FULL_SCREEN_VIEW) ?: return
        decor.removeView(overlay)
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
                if (index >= texts.size) return
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
    // BOTTOM_SHEET — DialogFragment
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
            window?.let { WindowCompat.setDecorFitsSystemWindows(it, false) }
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
        LottieLoading.tintLoader(binding.root)
        val text = arguments?.getString(ARG_TEXT).orEmpty()
        if (text.isBlank()) {
            binding.tvLoadingText.visibility = View.GONE
        } else {
            binding.tvLoadingText.text = text
        }
    }

    /** Material's design_bottom_sheet background must be cleared so the root view's rounded corners stay visible. */
    override fun onStart() {
        super.onStart()
        val bsDialog = dialog as? BottomSheetDialog
        bsDialog?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            isDraggable = false
            skipCollapsed = true
        }
        bsDialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { sheet ->
            sheet.setBackgroundResource(android.R.color.transparent)
            ViewCompat.setOnApplyWindowInsetsListener(sheet) { _, insets ->
                val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                _binding?.root?.updatePadding(
                    left = sysBars.left,
                    right = sysBars.right,
                    bottom = sysBars.bottom
                )
                insets
            }
            ViewCompat.requestApplyInsets(sheet)
        }
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
