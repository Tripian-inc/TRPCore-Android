package com.tripian.trpcore.base

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.viewbinding.ViewBinding
import com.tripian.trpcore.di.ViewModelFactory
import com.tripian.trpcore.util.OnBackPressListener
import com.tripian.trpcore.util.ToolbarProperties
import com.tripian.trpcore.util.extensions.setViewListener
import dagger.android.support.AndroidSupportInjection
import java.lang.reflect.ParameterizedType
import javax.inject.Inject

/**
 * Created by Semih Özköroğlu on 29.09.2019
 */
abstract class BaseFragment<VB: ViewBinding,VM : BaseViewModel>(private val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> VB) : Fragment(), OnBackPressListener {

    private var _binding: VB? = null
    protected val binding get() = _binding!!

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    lateinit var viewModel: VM

    var container: ViewGroup? = null

    private var isViewCreated = false
    private var mView: View? = null

    protected open fun setListeners() {
    }

    protected open fun setReceivers() {
    }

    open fun getToolbarProperties(): ToolbarProperties? {
        return null
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)

        if (!::viewModel.isInitialized) {
            viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get((javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[1] as Class<VM>)
        }

        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)

        this.container = container

        _binding = bindingInflater(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!isViewCreated) {
            isViewCreated = false

            /**
             * ViewModel'e listener setlenir
             */
            setViewListener()

            viewModel.onViewCreated(savedInstanceState)

            setListeners()

            setReceivers()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.arguments = arguments

        viewModel.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()

        viewModel.onStart()

        getToolbarProperties()?.let { (activity as BaseActivity<*, *>).setToolbarProperties(it) }
    }

    override fun onDestroy() {
        viewModel.onDestroy()

        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()

        if (activity is BaseActivity<*, *>) {
            /**
             * Activity'nin onBackPress methoduna register olunur.
             *
             * @see BaseActivity.onBackPressed
             */
            if (isBackEnable()) {
                (activity as BaseActivity<*, *>).onBackPressListener = this
            }
        }
        viewModel.onResume()
    }

    override fun onPause() {
        if (activity is BaseActivity<*, *>) {
            if (isBackEnable()) {
                (activity as BaseActivity<*, *>).onBackPressListener = null
            }
        }
        viewModel.onPause()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        viewModel.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        viewModel.onDestroyView()

        super.onDestroyView()
    }

    @CallSuper
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        viewModel.onActivityResult(requestCode, resultCode, data)
    }

    @CallSuper
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        viewModel.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
    }

    override fun isBackEnable(): Boolean {
        return viewModel.isBackEnable()
    }

    override fun onBackPressed(): Boolean {
        return viewModel.onBackPressed()
    }

    fun getLanguageForKey(key: String) = viewModel.getLanguageForKey(key)
}