package com.tripian.trpcore.util.extensions

import androidx.fragment.app.Fragment
import com.tripian.trpcore.R
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.util.fragment.AnimationType
import com.tripian.trpcore.util.fragment.FragmentFactory
import com.tripian.trpcore.util.fragment.TransitionType
import kotlin.reflect.KClass

/**
 * Navigates to [fragment] via [BaseViewModel.fragmentManager] (supportFragmentManager
 * on Activities, childFragmentManager on Fragments). For inner fragments without
 * backstack set [fragmentManagerEnable] to true; backPress must then be overridden for pop.
 */
fun BaseViewModel.navigateToFragment(
    fragment: Fragment,
    addToBackStack: Boolean = true,
    fragmentManagerEnable: Boolean = false,
    clearBackStack: Boolean = false,
    viewId: Int = R.id.container,
    transitionType: TransitionType = TransitionType.REPLACE,
    animation: AnimationType = AnimationType.NO_ANIM
) {

    val factory = FragmentFactory.Builder(fragment)
        .addToBackStack(addToBackStack)
        .setFragmentManager(if (fragmentManagerEnable) fragmentManager else null)
        .setClearBackStack(clearBackStack)
        .setViewId(viewId)
        .setTransitionType(transitionType)
        .setAnimation(animation)
        .build()

    viewListener?.showFragment(factory)
}

/**
 * Shows the full-screen Lottie loader with the default rotating texts. Callers
 * that want the bottom-sheet variant must use [BaseViewModel.showBottomSheetLoader]
 * (or `showLottieLoading(BOTTOM_SHEET, …)`) directly.
 */
fun BaseViewModel.showLoading() {
    showLottieLoading()
    viewListener?.showLoading()
}

fun BaseViewModel.hideLoading() {
    hideLottieLoading()
    viewListener?.hideLoading()
}

fun BaseViewModel.goBack() {
    viewListener?.goBack()
}

fun BaseViewModel.returnPage(clazz: KClass<out Fragment>) {
    viewListener?.returnPage(clazz)
}

fun <T> BaseViewModel.returnResult(data: T) {
    viewListener?.returnResult(data)
}

fun BaseViewModel.showSnackBarMessage(message: String) {
    viewListener?.showSnackBarMessage(message)
}