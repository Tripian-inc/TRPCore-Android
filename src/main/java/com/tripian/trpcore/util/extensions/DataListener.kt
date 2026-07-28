package com.tripian.trpcore.util.extensions

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.tripian.trpcore.base.BaseActivity
import com.tripian.trpcore.base.BaseBottomDialogFragment
import com.tripian.trpcore.base.BaseDialogFragment
import com.tripian.trpcore.base.BaseFragment
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.ViewListener
import com.tripian.trpcore.util.fragment.FragmentFactory
import com.tripian.trpcore.util.widget.BottomToast
import kotlin.reflect.KClass

/**
 * Shows an SDK alert on [host]. Fragments embedded in a Compose host are not
 * attached to a [BaseActivity], so the toast is shown on the plain Activity
 * instead of routing through the base class.
 */
private fun showAlertOnHost(host: FragmentActivity?, type: AlertType, message: String) {
    when (host) {
        null -> Unit
        is BaseActivity<*, *> -> host.showAlert(type, message)
        else -> BottomToast.show(host, message, type)
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
                (activity as? BaseActivity<*, *>)?.showLoading()
            }
        }

        override fun hideLoading() {
            if (activity != null) {
                (activity as? BaseActivity<*, *>)?.hideLoading()
            }
        }

        override fun showFragment(factory: FragmentFactory) {
            if (factory.mViewId == -1) {
                factory.mViewId = container?.id!!
            }

            activity?.showFragment(factory)
        }

        override fun showAlert(type: AlertType, message: String) {
            showAlertOnHost(activity, type, message)
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
            (activity as? BaseActivity<*, *>)?.showLoading()
        }

        override fun hideLoading() {
            (activity as? BaseActivity<*, *>)?.hideLoading()
        }

        override fun showFragment(factory: FragmentFactory) {
            activity?.showFragment(factory)
        }

        override fun showAlert(type: AlertType, message: String) {
            showAlertOnHost(activity, type, message)
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
            (activity as? BaseActivity<*, *>)?.showLoading()
        }

        override fun hideLoading() {
            (activity as? BaseActivity<*, *>)?.hideLoading()
        }

        override fun showFragment(factory: FragmentFactory) {
            activity?.showFragment(factory)
        }

        override fun showAlert(type: AlertType, message: String) {
            showAlertOnHost(activity, type, message)
        }

    }
}