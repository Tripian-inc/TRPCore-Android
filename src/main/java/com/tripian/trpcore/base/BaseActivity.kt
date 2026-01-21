package com.tripian.trpcore.base

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.viewbinding.ViewBinding
import com.tripian.trpcore.R
import com.tripian.trpcore.di.ViewModelFactory
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.widget.BottomToast
import com.tripian.trpcore.util.OnBackPressListener
import com.tripian.trpcore.util.ToolbarProperties
import com.tripian.trpcore.util.dialog.DGLockScreen
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

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        super.onCreate(savedInstanceState)
        _binding = getViewBinding()
        setContentView(binding.root)

        // Apply window insets to handle status bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = insets.top)
            WindowInsetsCompat.CONSUMED
        }

        viewModel = ViewModelProviders.of(this, viewModelFactory)
            .get((javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[1] as Class<VM>)

        viewModel.fragmentManager = supportFragmentManager

        /**
         * ViewModel'e listener setlenir
         */
        setViewListener()

        viewModel.arguments = intent.extras
        viewModel.onViewCreated(savedInstanceState)

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

    fun hideLoading() {
        dgLockScreen?.dismiss()
    }

    fun showLoading() {
        if (dgLockScreen == null) {
            dgLockScreen = DGLockScreen(this)
        }

        dgLockScreen?.let {
            if (!it.isShowing) {
                it.show()
            }
        }
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