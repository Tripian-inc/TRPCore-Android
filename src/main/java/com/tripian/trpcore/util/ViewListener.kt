package com.tripian.trpcore.util

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.tripian.trpcore.util.fragment.FragmentFactory
import kotlin.reflect.KClass

/** A warning dialog the ViewModel asked for; buttons are hidden when their text is null. */
data class WarningDialogRequest(
    val title: String?,
    val message: String?,
    val positiveText: String?,
    val negativeText: String?,
    val isCloseEnable: Boolean,
    val onPositive: () -> Unit,
    val onNegative: () -> Unit
)

interface ViewListener {

    fun showFragment(factory: FragmentFactory)

    /** Returns true when the host rendered [request] itself instead of the FRWarning fragment. */
    fun showWarningDialog(request: WarningDialogRequest): Boolean = false

    fun showAlert(type: AlertType, message: String)

    fun showLoading()

    fun hideLoading()

    fun goBack()

    fun returnPage(clazz: KClass<out Fragment>)

    fun <T> returnResult(data: T)

    fun startActivity(kClass: KClass<out FragmentActivity>, bundle: Bundle? = null, flags: Int? = null)

    fun finishActivity()

    fun showSnackBarMessage(message: String)
}