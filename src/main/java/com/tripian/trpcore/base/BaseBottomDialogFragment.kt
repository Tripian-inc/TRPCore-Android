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
import com.tripian.trpcore.util.extensions.consumeSystemBarPadding
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.fragment.app.FragmentActivity
import com.tripian.trpcore.R
import com.tripian.trpcore.di.ViewModelFactory
import com.tripian.trpcore.ui.common.loader.LottieLoading
import com.tripian.trpcore.ui.common.loader.LottieLoadingPresentation
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

    // Last presentation this sheet's VM asked us to show — used so that on a
    // subsequent hide we only tear down what THIS sheet actually owns.
    // Without this, the sheet's lifecycle hide (e.g. onPause during dismiss)
    // would call LottieLoading.hide(activity) and wipe an unrelated
    // activity-level loader that the host VM raised separately.
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

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bottomSheetDialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        isCancelable = true

        // Edge-to-edge: keep the dialog window unconstrained so the sheet's
        // background extends all the way to the screen bottom. The previous
        // setDecorFitsSystemWindows(true) opt-out worked on most devices but
        // left a visible gap between the sheet bottom and the nav bar on
        // some Samsung One UI / gesture-nav configurations because the
        // decor offset didn't match the actual nav-bar height. With the
        // decor unconstrained, the sheet fills to screen bottom and we
        // pad the content manually below.
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

                // Forward the system bar insets to the sheet content's bottom
                // padding. Attach on design_bottom_sheet (above our root) —
                // Material 1.9's BottomSheetBehavior can swallow insets before
                // they reach binding.root, in which case the consumeSystemBarPadding
                // listener installed in setListeners() never fires and the
                // Continue button ends up behind the nav bar.
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

        // check if no view has focus:
        val v = requireActivity().currentFocus ?: return

        inputManager.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setViewListener()

        viewModel.arguments = arguments
        viewModel.onViewCreated(savedInstanceState)

        // Bridge the VM's lottie loading events to LottieLoading on the host activity.
        // LottieLoading attaches its own DialogFragment via the activity's FragmentManager,
        // which puts the loader in a window above this bottom sheet — the sheet stays
        // open underneath the loader. Re-usable: any BaseBottomDialogFragment subclass
        // whose VM calls showBottomSheetLoader/showLottieLoading gets this for free.
        // We deliberately skip executePendingTransactions() — onPause fires a hide event
        // while FragmentManager is already executing, and forcing another pass throws.
        //
        // INLINE_SHEET is handled here, not on the activity: the loader is
        // attached as a child of this sheet's root so no new window opens.
        // The activity observer skips this presentation to avoid double-firing.
        viewModel.lottieLoadingEvent.observe(viewLifecycleOwner) { event ->
            if (event == null) return@observe
            val host = activity as? FragmentActivity ?: return@observe
            // Use Material's design_bottom_sheet wrapper (a FrameLayout) as
            // the inline host: a child added there sits on top of the sheet's
            // content in z-order, not appended below it like LinearLayout
            // [binding.root] would do.
            val inlineHost = dialog?.findViewById<ViewGroup>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            if (!event.show) {
                // Only tear down what THIS sheet actually raised. Hiding
                // unconditionally would let the sheet's own lifecycle hide
                // (onPause during dismiss) wipe out an activity-level loader
                // that a different VM started.
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