package com.tripian.trpcore.util.extensions

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.google.android.material.snackbar.Snackbar
import com.tripian.trpcore.R
import com.tripian.trpcore.util.fragment.AnimationType
import com.tripian.trpcore.util.fragment.FragmentFactory
import com.tripian.trpcore.util.fragment.TransitionType
import java.io.Serializable
import kotlin.reflect.KClass

/**
 * Created by semihozkoroglu on 2019-12-06.
 */
fun FragmentActivity.showFragment(builder: FragmentFactory?) {
    if (builder == null) {
        throw NullPointerException("Builder can't be null")
    }

    var fm: FragmentManager? = supportFragmentManager

    if (builder.getManager() != null) {
        fm = builder.getManager()
    }

    if (builder.isClearBackStack()) {
        for (i in 0 until fm!!.backStackEntryCount) {
            val tag = fm.getBackStackEntryAt(0).name
            fm.popBackStackImmediate(tag, 0)

//            val fragment = fm.findFragmentByTag(tag)
//            fragment?.let {
//                fm.beginTransaction().remove(it).commit()
//            }
        }
    }

    val ft = fm!!.beginTransaction()

    if (builder.isDialog()) {
        builder.getDialogFragment()!!.show(fm, null)
    } else {
        val fragment = builder.getFragment() ?: return

        val tag = builder.getTag()

        val containerId = builder.getViewId()

        if (builder.getAnimationType() !== AnimationType.NO_ANIM) {
            val anim = AnimationType.getAnimation(builder.getAnimationType())

            ft.setCustomAnimations(anim[0], anim[1], anim[2], anim[3])
        }

        if (builder.addToBackStack()) {
            ft.addToBackStack(tag)
        }

        when (builder.getTransitionType()) {
            TransitionType.ADD -> ft.add(containerId, fragment, tag)
            TransitionType.SHOW -> ft.show(fragment)
            TransitionType.HIDE -> ft.hide(fragment)
            else -> ft.replace(containerId, fragment, tag)
        }

        /**
         * commit() changed to commitAllowingStateLoss() for "Can not perform this action after onSaveInstanceState"
         */
        ft.commitAllowingStateLoss()
    }
}

fun FragmentActivity.returnPage(clazz: KClass<out Fragment>) {
    val fm = supportFragmentManager
    fm.popBackStack(clazz.java.simpleName, 0)
}

fun <T> FragmentActivity.returnResult(data: T) {
    val i = Intent()

    val bundle = Bundle()
    bundle.putSerializable("data", data as Serializable)

    i.putExtras(bundle)

    setResult(Activity.RESULT_OK, i)
    finish()
}

fun FragmentActivity.startActivity(kClass: KClass<out FragmentActivity>, bundle: Bundle?, flags: Int?) {
    val intent = Intent(this, kClass.java)

    flags?.let { intent.flags = flags }

    bundle?.let { intent.putExtras(bundle) }

    startActivity(intent)
}

fun FragmentActivity.showSnackBarMessage(message: String) {
    val container = findViewById<View>(R.id.container)

    container?.let {
        val snackbar = Snackbar.make(container, message, Snackbar.LENGTH_LONG)
        val snackBarView = snackbar.view as FrameLayout

        val params = snackBarView.getChildAt(0).layoutParams as FrameLayout.LayoutParams

        snackBarView.getChildAt(0).layoutParams = params
        snackbar.show()
    }
}