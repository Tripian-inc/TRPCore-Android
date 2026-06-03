package com.tripian.trpcore.base

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.viewbinding.ViewBinding
import com.airbnb.lottie.LottieCompositionFactory
import com.tripian.trpcore.R
import com.tripian.trpcore.di.ViewModelFactory
import com.tripian.trpcore.ui.common.loader.LottieLoading
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.widget.BottomToast
import com.tripian.trpcore.util.OnBackPressListener
import com.tripian.trpcore.util.ToolbarProperties
import com.tripian.trpcore.util.dialog.DGLockScreen
import com.tripian.trpcore.util.extensions.consumeSystemBarPadding
import com.tripian.trpcore.util.extensions.setViewListener
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import java.lang.reflect.ParameterizedType
import javax.inject.Inject


/**
 * Created by Semih Özköroğlu on 29.09.2019
 */
abstract class BaseActivity<VB : ViewBinding, VM : BaseViewModel> : AppCompatActivity(),
    HasSupportFragmentInjector {

    private var _binding: VB? = null
    protected val binding get() = _binding!!

    @Inject
    lateinit var actInjector: DispatchingAndroidInjector<Activity>

    @Inject
    lateinit var fragmentInjector: DispatchingAndroidInjector<Fragment>

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

        // Apply window insets to handle status bar
        binding.root.consumeSystemBarPadding(top = true)

        viewModel = ViewModelProviders.of(this, viewModelFactory)
            .get((javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[1] as Class<VM>)

        viewModel.fragmentManager = supportFragmentManager

        /**
         * ViewModel'e listener setlenir
         */
        setViewListener()

        viewModel.arguments = intent.extras
        viewModel.onViewCreated(savedInstanceState)

        viewModel.lottieLoadingEvent.observe(this) { event ->
            if (event == null) return@observe
            if (event.show) {
                // Force-hide the legacy DGLockScreen spinner so the two loaders
                // never stack on top of each other.
                hideLoading()
                LottieLoading.show(this, event.presentation, event.text)
            } else {
                LottieLoading.hide(this)
                hideLoading()
            }
            // Force the DialogFragment transaction to run now so the loader
            // attaches in the same frame as the activity's first draw — without
            // this the dialog commit waits for the FragmentManager's next idle
            // pass and the empty screen flashes briefly.
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

    override fun supportFragmentInjector(): AndroidInjector<Fragment> {
        return fragmentInjector
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        viewModel.onSaveInstanceState(outState)
    }

    /**
     * Legacy DGLockScreen spinner. Disabled SDK-wide — all callers route through
     * `showLottieLoading()` / `hideLottieLoading()` (full-screen Lottie) or
     * `showBottomSheetLoader()` (bottom-sheet Lottie) instead. Kept as a no-op
     * so existing call sites don't need to be rewritten. Any DGLockScreen that
     * was previously surfaced (older builds, defensive) is dismissed here too.
     */
    fun hideLoading() {
        dgLockScreen?.dismiss()
        dgLockScreen = null
    }

    fun showLoading() {
        // no-op — see [hideLoading] doc. If anything previously inflated the
        // legacy lock screen, dismiss it so the SDK is loader-uniform.
        dgLockScreen?.dismiss()
        dgLockScreen = null
    }

    open fun backPressed() {
//        super.onBackPressed()
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

//    override fun onBackPressed() {
//        if (viewModel.isBackEnable()) {
//            if (viewModel.onBackPressed()) {
//                super.onBackPressed()
//            }
//        } else {
//            if (onBackPressListener != null &&
//                onBackPressListener!!.isBackEnable()
//            ) {
//                if (onBackPressListener!!.onBackPressed()) {
//                    super.onBackPressed()
//                }
//            } else {
//                super.onBackPressed()
//            }
//        }
//    }

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
            duration = 3000L
        )
    }

    open fun setToolbarProperties(properties: ToolbarProperties) {}

    fun getLanguageForKey(key: String) = viewModel.getLanguageForKey(key)
}