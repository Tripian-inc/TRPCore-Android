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
    private val duration: Long
) {

    private var binding: ViewBottomToastBinding? = null
    private var toastView: View? = null

    companion object {
        private const val DEFAULT_DURATION = 3000L
        private const val ANIMATION_DURATION = 300L

        fun show(
            activity: Activity,
            message: String,
            alertType: AlertType = AlertType.SUCCESS,
            duration: Long = DEFAULT_DURATION
        ) {
            BottomToast(activity, message, alertType, duration).display()
        }
    }

    private fun display() {
        // Remove any existing toast first
        removeExistingToast()

        // Inflate the toast layout
        binding = ViewBottomToastBinding.inflate(LayoutInflater.from(activity))
        toastView = binding?.root

        // Set message
        binding?.tvToastMessage?.text = message

        // Set accent border color based on alert type
        val accentColor = when (alertType) {
            AlertType.SUCCESS -> ContextCompat.getColor(activity, R.color.trp_success_message)
            AlertType.WARNING -> ContextCompat.getColor(activity, R.color.trp_warning_message)
            AlertType.ERROR -> ContextCompat.getColor(activity, R.color.trp_error_message)
            AlertType.INFO -> ContextCompat.getColor(activity, R.color.trp_info_message)
            else -> ContextCompat.getColor(activity, R.color.trp_success_message)
        }
        binding?.viewAccentBorder?.setBackgroundColor(accentColor)

        // Set icon based on alert type
        val iconRes = when (alertType) {
            AlertType.SUCCESS -> R.drawable.ic_check_circle_green
            AlertType.WARNING -> R.drawable.ic_info
            AlertType.ERROR -> R.drawable.ic_close
            AlertType.INFO -> R.drawable.ic_info
            else -> R.drawable.ic_check_circle_green
        }
        binding?.ivToastIcon?.setImageResource(iconRes)
        // Set icon tint to match accent color
        binding?.ivToastIcon?.setColorFilter(accentColor)

        // Add to the activity's root view
        val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
        val density = activity.resources.displayMetrics.density
        val horizontalMargin = (32 * density).toInt()
        val bottomMargin = (45 * density).toInt()

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

            // Schedule removal
            view.postDelayed({
                dismiss()
            }, duration)
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
                val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
                rootView.removeView(view)
            }
            binding = null
            toastView = null
        } catch (e: Exception) {
            // Ignore if activity is destroyed
        }
    }

    private fun removeExistingToast() {
        try {
            val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
            val existingToast = rootView.findViewWithTag<View>("bottom_toast")
            existingToast?.let {
                rootView.removeView(it)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }
}
