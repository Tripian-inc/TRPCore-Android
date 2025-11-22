package com.tripian.trpcore.util

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.tripian.trpcore.util.fragment.FragmentFactory
import kotlin.reflect.KClass

/**
 * Created by semihozkoroglu on 2019-08-06.
 */
interface ViewListener {

    fun showFragment(factory: FragmentFactory)

    fun showAlert(type: AlertType, message: String)

    fun showLoading()

    fun hideLoading()

    fun goBack()

    fun returnPage(clazz: KClass<out Fragment>)

    fun <T> returnResult(data: T)

    fun startActivity(kClass: KClass<out FragmentActivity>, bundle: Bundle? = null, flags: Int? = null)

    fun finishActivity()

    fun showSnackBarMessage(message: String)

    fun openLogin()
}