package com.tripian.trpcore.util.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.tripian.trpcore.R
import com.tripian.trpcore.databinding.ViewBottomToastBinding
import com.tripian.trpcore.util.AlertType

/**
 * BottomToast - A custom toast that slides up from the bottom of the screen
 * Similar to iOS-style toast notifications
 */
class BottomToast private constructor(
    private val activity: Activity,
    private val message: String,
    private val alertType: AlertType,
    private val duration: Long,
    private val parentOverride: ViewGroup?
) {

    private var binding: ViewBottomToastBinding? = null
    private var toastView: View? = null

    companion object {
        private const val DEFAULT_DURATION = 3000L
        private const val ANIMATION_DURATION = 300L

        /**
         * @param parent optional attach target. Pass a dialog's decor view to
         *               surface the toast on top of that dialog instead of the
         *               activity's content view (which sits behind any open
         *               dialogs).
         */
        fun show(
            activity: Activity,
            message: String,
            alertType: AlertType = AlertType.SUCCESS,
            duration: Long = DEFAULT_DURATION,
            parent: ViewGroup? = null
        ) {
            BottomToast(activity, message, alertType, duration, parent).display()
        }
    }

    private fun resolveParent(): ViewGroup =
        parentOverride ?: activity.window.decorView.findViewById(android.R.id.content)

    private fun display() {
        removeExistingToast()

        binding = ViewBottomToastBinding.inflate(LayoutInflater.from(activity))
        toastView = binding?.root

        binding?.tvToastMessage?.text = message

        val iconRes = when (alertType) {
            AlertType.SUCCESS -> R.drawable.trp_ic_success
            AlertType.WARNING -> R.drawable.trp_ic_info
            AlertType.ERROR -> R.drawable.trp_ic_close
            AlertType.INFO -> R.drawable.trp_ic_info
            else -> R.drawable.trp_ic_success
        }
        binding?.ivToastIcon?.setImageResource(iconRes)
        when (alertType) {
            AlertType.SUCCESS -> binding?.ivToastIcon?.clearColorFilter()
            AlertType.WARNING -> binding?.ivToastIcon?.setColorFilter(
                ContextCompat.getColor(activity, R.color.trp_warning_message)
            )
            AlertType.ERROR -> binding?.ivToastIcon?.setColorFilter(
                ContextCompat.getColor(activity, R.color.trp_error_message)
            )
            AlertType.INFO -> binding?.ivToastIcon?.setColorFilter(
                ContextCompat.getColor(activity, R.color.trp_info_message)
            )
            else -> binding?.ivToastIcon?.clearColorFilter()
        }

        if (alertType == AlertType.ERROR) {
            binding?.cardToast?.radius = 8 * activity.resources.displayMetrics.density
            binding?.cardToast?.setCardBackgroundColor(
                ContextCompat.getColor(activity, R.color.trp_error_bg)
            )
            binding?.llToastContainer?.setBackgroundResource(R.drawable.trp_bg_alert_error)
            binding?.ivToastIcon?.visibility = View.GONE
            binding?.ivToastClose?.visibility = View.VISIBLE
            binding?.ivToastClose?.setOnClickListener { dismiss() }
        }

        val rootView = resolveParent()
        val density = activity.resources.displayMetrics.density
        val horizontalMargin = (16 * density).toInt()
        val bottomMargin = (16 * density).toInt() + navBarOverlap(rootView)

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = android.view.Gravity.BOTTOM
            setMargins(horizontalMargin, 0, horizontalMargin, bottomMargin)
        }

        toastView?.let { view ->
            view.tag = "bottom_toast"
            rootView.addView(view, params)

            view.translationY = 500f

            ObjectAnimator.ofFloat(view, "translationY", 500f, 0f).apply {
                this.duration = ANIMATION_DURATION
                start()
            }

            if (alertType != AlertType.ERROR) {
                view.postDelayed({
                    dismiss()
                }, duration)
            }
        }
    }

    /**
     * Returns how far [rootView]'s bottom edge actually extends under the
     * navigation bar. A decor-fitted parent already ends above the nav bar, so
     * adding the full inset there would double the gap; an edge-to-edge parent
     * needs the full inset. Falls back to the full inset before layout.
     */
    private fun navBarOverlap(rootView: ViewGroup): Int {
        val inset = ViewCompat.getRootWindowInsets(rootView)
            ?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
        if (inset == 0) return 0
        val windowHeight = rootView.rootView.height
        if (windowHeight == 0 || rootView.height == 0) return inset
        val location = IntArray(2)
        rootView.getLocationInWindow(location)
        val rootBottom = location[1] + rootView.height
        return (rootBottom - (windowHeight - inset)).coerceIn(0, inset)
    }

    private fun dismiss() {
        toastView?.let { view ->
            ObjectAnimator.ofFloat(view, "translationY", 0f, 500f).apply {
                this.duration = ANIMATION_DURATION
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        removeView()
                    }
                })
                start()
            }
        }
    }

    private fun removeView() {
        try {
            toastView?.let { view ->
                resolveParent().removeView(view)
            }
            binding = null
            toastView = null
        } catch (e: Exception) {
        }
    }

    private fun removeExistingToast() {
        try {
            val rootView = resolveParent()
            val existingToast = rootView.findViewWithTag<View>("bottom_toast")
            existingToast?.let {
                rootView.removeView(it)
            }
        } catch (e: Exception) {
        }
    }
}
