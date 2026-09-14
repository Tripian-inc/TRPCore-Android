package com.tripian.trpcore.base

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import com.airbnb.lottie.LottieCompositionFactory
import com.tripian.trpcore.R
import com.tripian.trpcore.di.ViewModelFactory
import com.tripian.trpcore.ui.common.loader.LottieLoading
import com.tripian.trpcore.ui.common.loader.LottieLoadingPresentation
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.OnBackPressListener
import com.tripian.trpcore.util.ToolbarProperties
import com.tripian.trpcore.util.dialog.DGLockScreen
import com.tripian.trpcore.util.extensions.consumeSystemBarPadding
import com.tripian.trpcore.util.extensions.setViewListener
import com.tripian.trpcore.util.widget.BottomToast
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import java.lang.reflect.ParameterizedType
import javax.inject.Inject


/**
 * Created by Semih Özköroğlu on 29.09.2019
 */
abstract class BaseActivity<VB : ViewBinding, VM : BaseViewModel> : AppCompatActivity(),
    HasAndroidInjector {

    private var _binding: VB? = null
    protected val binding get() = _binding!!

    @Inject
    lateinit var actInjector: DispatchingAndroidInjector<Activity>

    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    lateinit var viewModel: VM

    var onBackPressListener: OnBackPressListener? = null

    /**
     * Servis request'lerinde kullanilmaktadir
     */
    private var dgLockScreen: DGLockScreen? = null

    abstract fun setListeners()

    abstract fun setReceivers()

    abstract fun getViewBinding(): VB

    override fun onCreate(savedInstanceState: Bundle?) {
        TRPCore.inject(this)
        TRPCore.registerActivity(this)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        LottieCompositionFactory.fromRawRes(applicationContext, R.raw.loader)

        super.onCreate(savedInstanceState)
        _binding = getViewBinding()
        setContentView(binding.root)

        binding.root.consumeSystemBarPadding(top = true)

        viewModel = ViewModelProvider(this, viewModelFactory)
            .get((javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[1] as Class<VM>)

        viewModel.fragmentManager = supportFragmentManager

        setViewListener()

        viewModel.arguments = intent.extras
        viewModel.onViewCreated(savedInstanceState)

        viewModel.lottieLoadingEvent.observe(this) { event ->
            if (event == null) return@observe
            if (event.presentation == LottieLoadingPresentation.INLINE_SHEET) return@observe
            if (event.show) {
                hideLoading()
                LottieLoading.show(this, event.presentation, event.text)
            } else {
                LottieLoading.hide(this)
                hideLoading()
            }
            if (!supportFragmentManager.isStateSaved) {
                supportFragmentManager.executePendingTransactions()
            }
        }

        setListeners()
        setReceivers()
    }

    override fun onResume() {
        super.onResume()

        viewModel.onResume()
    }

    override fun onPause() {
        viewModel.onPause()

        super.onPause()
    }

    override fun androidInjector(): AndroidInjector<Any> = androidInjector

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        viewModel.onSaveInstanceState(outState)
    }

    /**
     * Legacy DGLockScreen spinner, kept as a no-op; loaders route through the
     * Lottie loading helpers instead.
     */
    fun hideLoading() {
        dgLockScreen?.dismiss()
        dgLockScreen = null
    }

    fun showLoading() {
        dgLockScreen?.dismiss()
        dgLockScreen = null
    }

    open fun backPressed() {
        if (viewModel.isBackEnable()) {
            if (viewModel.onBackPressed()) {
                onBackPressedDispatcher.onBackPressed()
            }
        } else {
            if (onBackPressListener != null &&
                onBackPressListener!!.isBackEnable()
            ) {
                if (onBackPressListener!!.onBackPressed()) {
                    onBackPressedDispatcher.onBackPressed()
                }
            } else {
                onBackPressedDispatcher.onBackPressed()
            }
        }
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

    @CallSuper
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        viewModel.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroy() {
        TRPCore.unregisterActivity(this)
        viewModel.onDestroy()

        super.onDestroy()
    }

    fun showAlert(type: AlertType, message: String) {
        BottomToast.show(
            activity = this,
            message = message,
            alertType = type,
            duration = 3000L,
            parent = alertParent()
        )
    }

    /**
     * Where alerts are attached. The activity's content view sits behind any open
     * dialog, so a screen holding bottom sheets returns the topmost sheet's decor
     * view — otherwise its failures are drawn out of sight.
     */
    open fun alertParent(): ViewGroup? = null

    open fun setToolbarProperties(properties: ToolbarProperties) {}

    fun getLanguageForKey(key: String) = viewModel.getLanguageForKey(key)
}