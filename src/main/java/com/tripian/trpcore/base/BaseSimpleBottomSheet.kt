package com.tripian.trpcore.base

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tripian.trpcore.R
import com.tripian.trpcore.ui.common.loader.LottieLoading
import com.tripian.trpcore.ui.common.loader.LottieLoadingText

/**
 * Base class for simple BottomSheets that don't require ViewModel or DI.
 * Provides ViewBinding, drag configuration, fullscreen support and
 * edge-to-edge insets handling.
 */
abstract class BaseSimpleBottomSheet<VB : ViewBinding>(
    private val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> VB
) : BottomSheetDialogFragment() {

    private var _binding: VB? = null
    protected val binding get() = _binding!!

    override fun getTheme(): Int = R.style.TrpAppTheme_BottomSheetDialog

    /**
     * Override to disable drag-to-dismiss behavior.
     * Default is true (draggable).
     */
    open fun isDragEnable(): Boolean = true

    /**
     * Override to make the BottomSheet fullscreen.
     * Default is false.
     */
    open fun isFullscreen(): Boolean = false

    /**
     * Runs the sheet edge-to-edge: system bar insets are forwarded from the sheet
     * frame to the content root's padding so the sheet background reaches the
     * screen bottom while content stays above the nav bar. When the sheet settles
     * into the expanded state after its content resized mid-animation, the stale
     * expanded offset leaves a gap below the sheet, so a layout is re-requested
     * once the settle completes with the sheet bottom off the parent edge.
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bottomSheetDialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        isCancelable = true

        bottomSheetDialog.window?.let { WindowCompat.setDecorFitsSystemWindows(it, false) }

        bottomSheetDialog.setOnShowListener { dialog ->
            val dg = dialog as BottomSheetDialog
            val bottomSheet = dg.findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.isDraggable = isDragEnable()
                behavior.skipCollapsed = true
                behavior.isFitToContents = true
                if (isFullscreen()) {
                    setupFullHeight(it)
                }
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.addBottomSheetCallback(object :
                    BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(sheet: View, newState: Int) {
                        if (newState == BottomSheetBehavior.STATE_EXPANDED &&
                            sheet.bottom != (sheet.parent as View).height
                        ) {
                            sheet.requestLayout()
                        }
                    }

                    override fun onSlide(sheet: View, slideOffset: Float) {}
                })

                ViewCompat.setOnApplyWindowInsetsListener(it) { _, insets ->
                    val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                    _binding?.root?.updatePadding(
                        left = sysBars.left,
                        right = sysBars.right,
                        bottom = sysBars.bottom
                    )
                    insets
                }
                ViewCompat.requestApplyInsets(it)
            }
        }
        return bottomSheetDialog
    }

    private fun setupFullHeight(bottomSheet: View) {
        val layoutParams = bottomSheet.layoutParams
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
        bottomSheet.layoutParams = layoutParams
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        container?.setBackgroundColor(Color.TRANSPARENT)
        _binding = bindingInflater(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    override fun onPause() {
        hideKeyboard()
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    protected fun hideKeyboard() {
        val inputManager = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val v = requireActivity().currentFocus ?: return
        inputManager.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
    }

    /**
     * Surfaces a loader over this sheet's own view tree (no separate window/dialog)
     * while the host performs an operation. Resolves [languageKey] via the shared
     * language repository since this sheet has no ViewModel of its own.
     */
    fun showInSheetLoadingOverlay(languageKey: String, fallback: String) {
        val host = dialog?.findViewById<ViewGroup>(
            com.google.android.material.R.id.design_bottom_sheet
        ) ?: return
        val text = TRPCore.core.miscRepository.getLanguageValueForKey(languageKey).ifBlank { fallback }
        LottieLoading.showInline(host, LottieLoadingText.Single(text))
    }

    /** Hides the inline loading overlay (e.g. on failure/retry). */
    fun hideInSheetLoadingOverlay() {
        val host = dialog?.findViewById<ViewGroup>(
            com.google.android.material.R.id.design_bottom_sheet
        ) ?: return
        LottieLoading.hideInline(host)
    }
}
