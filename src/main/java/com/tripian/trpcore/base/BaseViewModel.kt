package com.tripian.trpcore.base

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import androidx.annotation.CallSuper
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModel
import com.tripian.trpcore.repository.MiscRepository
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.OnBackPressListener
import com.tripian.trpcore.util.Strings
import com.tripian.trpcore.util.ViewListener
import com.tripian.trpcore.util.dialog.DGActionListener
import com.tripian.trpcore.util.extensions.navigateToFragment
import com.tripian.trpcore.util.extensions.setUseCasesListener
import com.tripian.trpcore.util.fragment.FragmentFactory
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import javax.inject.Inject
import kotlin.reflect.KClass
import kotlin.Array as Array1


/**
 * Created by Semih Özköroğlu on 29.09.2019
 */
abstract class BaseViewModel(vararg cases: BaseUseCase<*, *>) : ViewModel(), OnBackPressListener {

    @Inject
    lateinit var eventBus: EventBus

    @Inject
    lateinit var strings: Strings

    @Inject
    lateinit var miscRepository: MiscRepository

    var arguments: Bundle? = null

    /**
     * Use case'lerde request'leri dispose etmek için use case listesini tutuyoruz
     */
    var useCases = arrayListOf(*cases)

    var fragmentManager: FragmentManager? = null
    var viewListener: ViewListener? = null

    open fun onCreate(savedInstanceState: Bundle?) {
    }

    @CallSuper
    open fun onViewCreated(savedInstanceState: Bundle?) {
        if (!eventBus.isRegistered(this)) {
            eventBus.register(this)
        }

        setUseCasesListener()

        viewListener?.hideLoading()
    }

    @CallSuper
    open fun onStart() {
    }

    @CallSuper
    open fun onDestroy() {
        if (eventBus.isRegistered(this)) {
            eventBus.unregister(this)
        }
    }

    @CallSuper
    open fun onResume() {
    }

    @CallSuper
    open fun onPause() {
    }

    @CallSuper
    open fun onSaveInstanceState(outState: Bundle?) {
    }

    /**
     * ViewModel lifecycle'ı sonlandığında çağırılır.
     * RxJava Disposable'larını temizler.
     */
    override fun onCleared() {
        super.onCleared()

        for (useCase in useCases) {
            useCase.clear()
        }
    }

    open fun onDestroyView() {
        for (useCase in useCases) {
            useCase.clear()
        }
    }

    open fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    }

    open fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array1<String>,
        grantResults: IntArray
    ) {
    }

    override fun isBackEnable(): Boolean {
        return false
    }

    override fun onBackPressed(): Boolean {
        return true
    }

    fun startActivity(
        kClass: KClass<out FragmentActivity>,
        bundle: Bundle? = null,
        flags: Int? = null
    ) {
        viewListener?.startActivity(kClass, bundle, flags)
    }

    fun finishActivity() {
        viewListener?.finishActivity()
    }

    @kotlin.jvm.JvmOverloads
    fun showDialog(
        title: String? = getLanguageForKey(LanguageConst.WARNING),
        contentText: String? = "",
        positiveBtn: String? = getLanguageForKey(LanguageConst.CONTINUE),
        negativeBtn: String? = null,
        positive: DGActionListener? = null,
        negative: DGActionListener? = null,
        isCloseEnable: Boolean = true
    ) {
        val fragment =
            FRWarning.newInstance(title, contentText, positiveBtn, negativeBtn, isCloseEnable)
        fragment.positiveListener = object : DGActionListener {
            override fun onClicked(o: Any?) {
                positive?.onClicked(o)
                fragment.dismiss()
            }
        }
        fragment.negativeListener = object : DGActionListener {
            override fun onClicked(o: Any?) {
                negative?.onClicked(o)
                fragment.dismiss()
            }
        }

        navigateToFragment(fragment)
    }

    @Subscribe
    fun onDummyBusEvent(dummy: Unit) {
        // NOTE: Burasi silinmemeli, bu kısım register oldugunda ihtiyac duyuluyor
    }

    fun showFragment(factory: FragmentFactory) {
        viewListener?.showFragment(factory)
    }

    fun showAlert(type: AlertType, message: String?) {
        if (!TextUtils.isEmpty(message)) {
            viewListener?.showAlert(type, message!!)
        }
    }

    fun getLanguageForKey(key: String): String {
        return miscRepository.getLanguageValueForKey(key)
    }
}