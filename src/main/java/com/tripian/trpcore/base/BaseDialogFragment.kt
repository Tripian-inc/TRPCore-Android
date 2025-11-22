package com.tripian.trpcore.base

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProviders
import androidx.viewbinding.ViewBinding
import com.tripian.trpcore.R
import com.tripian.trpcore.di.ViewModelFactory
import com.tripian.trpcore.util.extensions.setViewListener
import dagger.android.support.AndroidSupportInjection
import java.lang.reflect.ParameterizedType
import javax.inject.Inject

abstract class BaseDialogFragment<VB: ViewBinding,VM : BaseViewModel>(private val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> VB) : DialogFragment() {

    private var _binding: VB? = null
    protected val binding get() = _binding!!

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    lateinit var viewModel: VM

//    lateinit var mContainer: View

    open fun dismissEnable(): Boolean {
        return true
    }

    open fun setListeners() {}

    open fun setReceivers() {}

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)

        if (!::viewModel.isInitialized) {
            viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get((javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[1] as Class<VM>)
        }

        super.onAttach(context)
    }

    override fun getTheme(): Int {
        return R.style.AppTheme_Transparent
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog: Dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnKeyListener { _, keyCode, keyEvent ->
            if (keyCode == KeyEvent.KEYCODE_BACK && keyEvent.action == KeyEvent.ACTION_UP) {
                if (dismissEnable()) {
                    dismiss()
                }

                true
            } else {
                false
            }
        }

        return dialog
    }

    override fun dismiss() {
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
//        mContainer = inflater.inflate(getLayoutId(), container, false)
        viewModel.fragmentManager = childFragmentManager
        _binding = bindingInflater(inflater, container, false)

        return binding.root
    }

    override fun onPause() {
        hideKeyboard()
        super.onPause()
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