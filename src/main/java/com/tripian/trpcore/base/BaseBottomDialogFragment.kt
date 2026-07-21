package com.tripian.trpcore.base

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
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
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tripian.trpcore.R
import com.tripian.trpcore.di.ViewModelFactory
import com.tripian.trpcore.ui.common.loader.LottieLoading
import com.tripian.trpcore.ui.common.loader.LottieLoadingPresentation
import com.tripian.trpcore.util.extensions.consumeSystemBarPadding
import com.tripian.trpcore.util.extensions.hideLoading
import com.tripian.trpcore.util.extensions.setViewListener
import dagger.android.support.AndroidSupportInjection
import java.lang.reflect.ParameterizedType
import javax.inject.Inject


abstract class BaseBottomDialogFragment<VB : ViewBinding, VM : BaseViewModel>(private val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> VB) :
    BottomSheetDialogFragment() {

    private var _binding: VB? = null
    protected val binding get() = _binding!!

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    lateinit var viewModel: VM

    private var mBehavior: BottomSheetBehavior<FrameLayout>? = null

    /** Last presentation this sheet's VM showed, so hide only tears down loaders this sheet owns. */
    private var lastInSheetPresentation: LottieLoadingPresentation? = null

    open fun setListeners() {


        binding.root.consumeSystemBarPadding(horizontal = true, bottom = true)
    }

    open fun setReceivers() {}

    open fun isDragEnable(): Boolean {
        return true
    }

    open fun isFullscreen(): Boolean {
        return false
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)

        if (!::viewModel.isInitialized) {
            viewModel = ViewModelProvider(this, viewModelFactory)
                .get((javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[1] as Class<VM>)
        }

        super.onAttach(context)
    }

    /**
     * System bar insets are applied in a single place: forwarded from the sheet
     * frame to the content root, since BottomSheetBehavior may swallow them
     * before the root's own inset listener runs. When the sheet settles into
     * the expanded state after its content resized mid-animation, the stale
     * expanded offset leaves a gap below the sheet, so a layout is re-requested
     * once the settle completes with the sheet bottom off the parent edge.
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bottomSheetDialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        isCancelable = true

        bottomSheetDialog.window?.let { WindowCompat.setDecorFitsSystemWindows(it, false) }

        bottomSheetDialog.setOnShowListener { dialog: DialogInterface ->
            val dg = dialog as BottomSheetDialog
            val bottomSheet =
                dg.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val bottomSheetBehavior: BottomSheetBehavior<*> =
                    BottomSheetBehavior.from(bottomSheet)
                bottomSheetBehavior.isDraggable = isDragEnable()
                bottomSheetBehavior.skipCollapsed = true
                bottomSheetBehavior.isFitToContents = true
                if (isFullscreen()) {
                    setupFullHeight(it)
                }
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED)
                bottomSheetBehavior.addBottomSheetCallback(object :
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

    override fun dismiss() {
        viewModel.hideLoading()
        viewModel.onDestroy()
        super.dismiss()
    }

    override fun onStart() {
        super.onStart()
        if (dialog != null) {
            dialog!!.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        viewModel.onStart()
    }

    override fun onPause() {
        viewModel.hideLoading()
        hideKeyboard()
        super.onPause()
    }

    override fun getTheme(): Int {
        return R.style.TrpAppTheme_BottomSheetDialog
    }

    private fun hideKeyboard() {
        val inputManager =
            requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        val v = requireActivity().currentFocus ?: return

        inputManager.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setViewListener()

        viewModel.arguments = arguments
        viewModel.onViewCreated(savedInstanceState)

        viewModel.lottieLoadingEvent.observe(viewLifecycleOwner) { event ->
            if (event == null) return@observe
            val host = activity as? FragmentActivity ?: return@observe
            val inlineHost = dialog?.findViewById<ViewGroup>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            if (!event.show) {
                when (lastInSheetPresentation) {
                    LottieLoadingPresentation.INLINE_SHEET ->
                        inlineHost?.let { LottieLoading.hideInline(it) }
                    LottieLoadingPresentation.FULL_SCREEN,
                    LottieLoadingPresentation.BOTTOM_SHEET ->
                        LottieLoading.hide(host)
                    null -> Unit
                }
                lastInSheetPresentation = null
                return@observe
            }
            lastInSheetPresentation = event.presentation
            if (event.presentation == LottieLoadingPresentation.INLINE_SHEET) {
                inlineHost?.let { LottieLoading.showInline(it, event.text) }
                return@observe
            }
            LottieLoading.show(host, event.presentation, event.text)
        }

        setListeners()
        setReceivers()
    }

    override fun onResume() {
        super.onResume()

        viewModel.onResume()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        viewModel.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        viewModel.onDestroyView()

        super.onDestroyView()
    }

    override fun show(manager: FragmentManager, tag: String?) {
        try {
            val ft = manager.beginTransaction()
            ft.add(this, tag)
            ft.commitAllowingStateLoss()
        } catch (ignored: IllegalStateException) {

        }
    }

    fun getLanguageForKey(key: String) = viewModel.getLanguageForKey(key)
}