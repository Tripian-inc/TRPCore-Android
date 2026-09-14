package com.tripian.trpcore.ui.timeline.compose.core

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.ViewListener
import com.tripian.trpcore.util.WarningDialogRequest
import com.tripian.trpcore.util.fragment.FragmentFactory
import com.tripian.trpcore.util.widget.BottomToast
import kotlin.reflect.KClass

/**
 * [ViewListener] adapter for Compose screens. Alerts surface through
 * [BottomToast], warning dialogs through [TimelineOverlayState]; back/finish
 * requests route to [onExit].
 */
internal class ComposeViewListener(
    private val context: Context,
    private val overlays: TimelineOverlayState,
    private val onExit: () -> Unit
) : ViewListener {

    override fun showFragment(factory: FragmentFactory) = Unit

    override fun showWarningDialog(request: WarningDialogRequest): Boolean {
        overlays.showDialog(request)
        return true
    }

    override fun showAlert(type: AlertType, message: String) {
        context.findActivity()?.let { BottomToast.show(it, message, type) }
    }

    override fun showLoading() = Unit

    override fun hideLoading() = Unit

    override fun goBack() = onExit()

    override fun returnPage(clazz: KClass<out Fragment>) = Unit

    override fun <T> returnResult(data: T) = Unit

    override fun startActivity(kClass: KClass<out FragmentActivity>, bundle: Bundle?, flags: Int?) {
        val intent = Intent(context, kClass.java)
        bundle?.let { intent.putExtras(it) }
        flags?.let { intent.addFlags(it) }
        if (context.findActivity() == null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    override fun finishActivity() = onExit()

    override fun showSnackBarMessage(message: String) {
        context.findActivity()?.let { BottomToast.show(it, message, AlertType.INFO) }
    }
}

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
