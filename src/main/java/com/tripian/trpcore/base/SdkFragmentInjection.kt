package com.tripian.trpcore.base

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.fragment.app.Fragment
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection

/**
 * Injects an SDK fragment. Activity-hosted flows keep going through the host's
 * dagger-android injector; a Compose host has no injector in the view
 * hierarchy, so the fragment is injected from the SDK's own graph instead.
 */
internal fun injectSdkFragment(fragment: Fragment, context: Context) {
    if (fragment.hasAndroidInjector(context)) {
        AndroidSupportInjection.inject(fragment)
    } else {
        TRPCore.core.androidInjector.inject(fragment)
    }
}

private fun Fragment.hasAndroidInjector(context: Context): Boolean {
    var parent = parentFragment
    while (parent != null) {
        if (parent is HasAndroidInjector) return true
        parent = parent.parentFragment
    }
    val hostActivity = activity ?: context.findHostActivity() ?: return false
    return hostActivity is HasAndroidInjector || hostActivity.application is HasAndroidInjector
}

private tailrec fun Context.findHostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findHostActivity()
    else -> null
}
