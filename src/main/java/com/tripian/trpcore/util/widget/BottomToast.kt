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
        parentOverride ?: activity.findViewById(android.R.id.content)

    private fun display() {
        // Remove any existing toast first
        removeExistingToast()

        // Inflate the toast layout
        binding = ViewBottomToastBinding.inflate(LayoutInflater.from(activity))
        toastView = binding?.root

        // Set message
        binding?.tvToastMessage?.text = message

        // Set icon based on alert type
        val iconRes = when (alertType) {
            AlertType.SUCCESS -> R.drawable.trp_ic_success
            AlertType.WARNING -> R.drawable.trp_ic_info
            AlertType.ERROR -> R.drawable.trp_ic_close
            AlertType.INFO -> R.drawable.trp_ic_info
            else -> R.drawable.trp_ic_success
        }
        binding?.ivToastIcon?.setImageResource(iconRes)
        // Tint non-success icons to match the alert type; the success
        // drawable already carries its own brand color.
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

        // Attach to the override parent when provided, otherwise the activity's
        // root view. Using a dialog's decor view as the parent surfaces the
        // toast on top of that dialog.
        val rootView = resolveParent()
        val density = activity.resources.displayMetrics.density
        val horizontalMargin = (16 * density).toInt()
        // 16dp above the navigation bar (3-button or gesture) so the toast keeps a
        // 16dp gap and never sits under the device buttons.
        val navBarInset = ViewCompat.getRootWindowInsets(rootView)
            ?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
        val bottomMargin = (16 * density).toInt() + navBarInset

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

            // Start off-screen (below)
            view.translationY = 500f

            // Animate in (slide up)
            ObjectAnimator.ofFloat(view, "translationY", 500f, 0f).apply {
                this.duration = ANIMATION_DURATION
                start()
            }

            // Error alerts persist until the user taps the close icon; all other
            // types auto-dismiss after [duration].
            if (alertType != AlertType.ERROR) {
                view.postDelayed({
                    dismiss()
                }, duration)
            }
        }
    }

    private fun dismiss() {
        toastView?.let { view ->
            // Animate out (slide down)
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
            // Ignore if activity is destroyed
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
            // Ignore
        }
    }
}
