package com.tripian.trpcore.util.extensions

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.tripian.trpcore.base.BaseActivity
import com.tripian.trpcore.base.BaseBottomDialogFragment
import com.tripian.trpcore.base.BaseDialogFragment
import com.tripian.trpcore.base.BaseFragment
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.ui.splash.ACSplash
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.UseCaseListener
import com.tripian.trpcore.util.ViewListener
import com.tripian.trpcore.util.dialog.DGContent
import com.tripian.trpcore.util.fragment.FragmentFactory
import javax.inject.Inject
import kotlin.reflect.KClass

/**
 * Created by semihozkoroglu on 2019-12-06.
 */
fun BaseViewModel.setUseCasesListener() {
    val viewModel = this

    this::class.java.fields.forEach {
        if (it != null && it.isAnnotationPresent(Inject::class.java)) {
            val obj = it.get(this)
            if (obj is BaseUseCase<*, *>) {
                useCases.add(obj)
            }
        }
    }

    for (useCase in useCases) {
        useCase.useCaseListener = object : UseCaseListener {
            override fun showSnackBarMessage(message: String) {
                viewListener?.showSnackBarMessage(message)
            }

            override fun showDialog(dgContent: DGContent) {
                viewModel.showDialog(
                    title = dgContent.title,
                    contentText = dgContent.content,
                    positiveBtn = dgContent.positiveBtn,
                    negativeBtn = dgContent.negativeBtn,
                    positive = dgContent.positiveListener,
                    negative = dgContent.negativeListener
                )
            }

            override fun refreshTokenError() {
                viewListener?.openLogin()
            }
        }
    }
}

fun BaseActivity<*, *>.setViewListener() {
    val baseActivity = this

    viewModel.viewListener = object : ViewListener {
        override fun finishActivity() {
            baseActivity.finish()
        }

        override fun showSnackBarMessage(message: String) {
            baseActivity.showSnackBarMessage(message)
        }

        override fun startActivity(
            kClass: KClass<out FragmentActivity>,
            bundle: Bundle?,
            flags: Int?
        ) {
            baseActivity.startActivity(kClass, bundle, flags)
        }

        override fun returnPage(clazz: KClass<out Fragment>) {
            baseActivity.returnPage(clazz)
        }

        override fun <T> returnResult(data: T) {
            baseActivity.returnResult(data)
        }

        override fun goBack() {
            backPressed()
        }

        override fun showLoading() {
            baseActivity.showLoading()
        }

        override fun hideLoading() {
            baseActivity.hideLoading()
        }

        override fun showFragment(factory: FragmentFactory) {
            baseActivity.showFragment(factory)
        }

        override fun showAlert(type: AlertType, message: String) {
            baseActivity.showAlert(type, message)
        }

        override fun openLogin() {
            startActivity(ACSplash::class)

            finishActivity()
        }
    }
}

fun BaseFragment<*,*>.setViewListener() {
    val baseFragment = this
    viewModel.viewListener = object : ViewListener {
        override fun finishActivity() {
            activity?.finish()
        }

        override fun showSnackBarMessage(message: String) {
            activity?.showSnackBarMessage(message)
        }

        override fun startActivity(
            kClass: KClass<out FragmentActivity>,
            bundle: Bundle?,
            flags: Int?
        ) {
            activity?.startActivity(kClass, bundle, flags)
        }

        override fun returnPage(clazz: KClass<out Fragment>) {
            activity?.returnPage(clazz)
        }

        override fun <T> returnResult(data: T) {
            activity?.returnResult(data)
        }

        override fun goBack() {
            activity?.onBackPressed()
        }

        override fun showLoading() {
            if (activity != null) {
                (activity as BaseActivity<*, *>).showLoading()
            }
        }

        override fun hideLoading() {
            if (activity != null) {
                (activity as BaseActivity<*, *>).hideLoading()
            }
        }

        override fun showFragment(factory: FragmentFactory) {
            if (factory.mViewId == -1) {
                factory.mViewId = container?.id!!
            }

            activity?.showFragment(factory)
        }

        override fun showAlert(type: AlertType, message: String) {
            (activity as BaseActivity<*, *>).showAlert(type, message)
        }

        override fun openLogin() {
            startActivity(ACSplash::class)

            finishActivity()
        }
    }
}

fun BaseDialogFragment<*, *>.setViewListener() {
    viewModel.viewListener = object : ViewListener {
        override fun finishActivity() {
            activity?.finish()
        }

        override fun showSnackBarMessage(message: String) {
            activity?.showSnackBarMessage(message)
        }

        override fun startActivity(
            kClass: KClass<out FragmentActivity>,
            bundle: Bundle?,
            flags: Int?
        ) {
            activity?.startActivity(kClass, bundle, flags)
        }

        override fun returnPage(clazz: KClass<out Fragment>) {

        }

        override fun <T> returnResult(data: T) {
            activity?.returnResult(data)
        }

        override fun goBack() {
            dismiss()
        }

        override fun showLoading() {
            (activity as BaseActivity<*, *>).showLoading()
        }

        override fun hideLoading() {
            (activity as BaseActivity<*, *>).hideLoading()
        }

        override fun showFragment(factory: FragmentFactory) {
            activity?.showFragment(factory)
        }

        override fun showAlert(type: AlertType, message: String) {
            (activity as BaseActivity<*, *>).showAlert(type, message)
        }

        override fun openLogin() {
            startActivity(ACSplash::class)

            finishActivity()
        }
    }
}

fun BaseBottomDialogFragment<*, *>.setViewListener() {
    viewModel.viewListener = object : ViewListener {
        override fun finishActivity() {
            activity?.finish()
        }

        override fun showSnackBarMessage(message: String) {
            activity?.showSnackBarMessage(message)
        }

        override fun startActivity(
            kClass: KClass<out FragmentActivity>,
            bundle: Bundle?,
            flags: Int?
        ) {
            activity?.startActivity(kClass, bundle, flags)
        }

        override fun returnPage(clazz: KClass<out Fragment>) {

        }

        override fun <T> returnResult(data: T) {
            activity?.returnResult(data)
        }

        override fun goBack() {
            dismiss()
        }

        override fun showLoading() {
            (activity as BaseActivity<*, *>).showLoading()
        }

        override fun hideLoading() {
            (activity as BaseActivity<*, *>).hideLoading()
        }

        override fun showFragment(factory: FragmentFactory) {
            activity?.showFragment(factory)
        }

        override fun showAlert(type: AlertType, message: String) {
            (activity as BaseActivity<*, *>).showAlert(type, message)
        }

        override fun openLogin() {
            startActivity(ACSplash::class)

            finishActivity()
        }
    }
}