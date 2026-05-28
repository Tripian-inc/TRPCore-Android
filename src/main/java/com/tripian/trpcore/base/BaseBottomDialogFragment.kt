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
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProviders
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.fragment.app.FragmentActivity
import com.tripian.trpcore.R
import com.tripian.trpcore.di.ViewModelFactory
import com.tripian.trpcore.ui.common.loader.LottieLoading
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

    open fun setListeners() {


        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(
                left = insets.left,
                top = 0,
                right = insets.right,
                bottom = insets.bottom
            )
            WindowInsetsCompat.CONSUMED
        }
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
            viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get((javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[1] as Class<VM>)
        }

        super.onAttach(context)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bottomSheetDialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        isCancelable = true

        // Why: targetSdk 35 forces dialog windows into edge-to-edge by default, but Material
        // 1.9's BottomSheetDialog doesn't auto-pad the sheet for system bars in that mode
        // (tall sheets like AddPlan end up with the Continue button behind the nav bar).
        // Opt this dialog window back into the legacy "fits system windows" mode so Android
        // applies status/nav bar insets to the decor view itself — sheets sit above the nav
        // bar automatically without any per-screen padding logic.
        bottomSheetDialog.window?.let { WindowCompat.setDecorFitsSystemWindows(it, true) }

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
        viewModel.lottieLoadingEvent.observe(viewLifecycleOwner) { event ->
            if (event == null) return@observe
            val host = activity as? FragmentActivity ?: return@observe
            if (event.show) {
                LottieLoading.show(host, event.presentation, event.text)
            } else {
                LottieLoading.hide(host)
            }
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