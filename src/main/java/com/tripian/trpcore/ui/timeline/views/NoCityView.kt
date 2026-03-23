package com.tripian.trpcore.ui.timeline.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.tripian.trpcore.databinding.ViewNoCityBinding

/**
 * NoCityView
 * Displays when all destination cities are not available/supported.
 * Shows a message and a button to go back to the host app.
 */
class NoCityView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    interface Listener {
        fun onGoToMyTripClicked()
    }

    var listener: Listener? = null
    private val binding: ViewNoCityBinding

    init {
        binding = ViewNoCityBinding.inflate(LayoutInflater.from(context), this, true)
        binding.btnGoToMyTrip.setOnClickListener {
            listener?.onGoToMyTripClicked()
        }
    }

    /**
     * Setup the view with localized strings.
     */
    fun setup(
        title: String,
        description: String,
        buttonText: String
    ) {
        binding.tvTitle.text = title
        binding.tvDescription.text = description
        binding.btnGoToMyTrip.text = buttonText
    }
}
