@file:Suppress("OVERLOADS_WITHOUT_DEFAULT_ARGUMENTS")

package com.tripian.trpcore.util.widget

import android.content.Context
import android.graphics.Typeface
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import androidx.core.content.res.ResourcesCompat
import com.tripian.trpcore.R
import com.tripian.trpcore.databinding.ViewSearchBarBinding

/**
 * Custom SearchBar component for AddPlan flow
 *
 * Specs:
 * - Height: 48dp
 * - Border: 0.5dp #CFCFCF
 * - Icon: left 18dp padding
 * - Placeholder: Light 14sp, primaryText color
 * - Text: Medium 14sp, primaryText color
 * - When text entered: search icon hides, clear (X) icon shows on right
 */
class SearchBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding = ViewSearchBarBinding.inflate(LayoutInflater.from(context), this, true)

    private var lightFont: Typeface? = null
    private var mediumFont: Typeface? = null

    private var onTextChangedListener: ((String) -> Unit)? = null
    private var onSearchActionListener: ((String) -> Unit)? = null

    val editText get() = binding.etInput

    init {
        // Load fonts
        lightFont = ResourcesCompat.getFont(context, R.font.light)
        mediumFont = ResourcesCompat.getFont(context, R.font.medium)

        setupTextWatcher()
        setupClearButton()
        setupSearchAction()

        // Initial state - placeholder font (light)
        binding.etInput.typeface = lightFont
    }

    private fun setupTextWatcher() {
        binding.etInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s?.toString() ?: ""
                updateUIState(text.isNotEmpty())
                onTextChangedListener?.invoke(text)
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupClearButton() {
        binding.ivClear.setOnClickListener {
            binding.etInput.text?.clear()
            binding.etInput.requestFocus()
        }
    }

    private fun setupSearchAction() {
        binding.etInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                onSearchActionListener?.invoke(binding.etInput.text?.toString() ?: "")
                true
            } else {
                false
            }
        }
    }

    private fun updateUIState(hasText: Boolean) {
        val density = resources.displayMetrics.density

        if (hasText) {
            // Text entered - hide search icon, show clear icon, use medium font
            binding.ivSearch.visibility = View.GONE
            binding.ivClear.visibility = View.VISIBLE
            binding.etInput.typeface = mediumFont

            // Adjust left margin when search icon is hidden (18dp)
            val params = binding.etInput.layoutParams as MarginLayoutParams
            params.marginStart = (18 * density).toInt()
            binding.etInput.layoutParams = params
        } else {
            // No text - show search icon, hide clear icon, use light font (for placeholder)
            binding.ivSearch.visibility = View.VISIBLE
            binding.ivClear.visibility = View.GONE
            binding.etInput.typeface = lightFont

            // Restore left margin for search icon (18dp icon margin + 24dp icon + 8dp gap = 50dp)
            val params = binding.etInput.layoutParams as MarginLayoutParams
            params.marginStart = (50 * density).toInt()
            binding.etInput.layoutParams = params
        }
    }

    /**
     * Set placeholder/hint text
     */
    fun setHint(hint: String) {
        binding.etInput.hint = hint
    }

    /**
     * Set placeholder/hint text from resource
     */
    fun setHint(resId: Int) {
        binding.etInput.setHint(resId)
    }

    /**
     * Get current text
     */
    fun getText(): String {
        return binding.etInput.text?.toString() ?: ""
    }

    /**
     * Set text programmatically
     */
    fun setText(text: String) {
        binding.etInput.setText(text)
    }

    /**
     * Clear the search text
     */
    fun clear() {
        binding.etInput.text?.clear()
    }

    /**
     * Set listener for text changes (debounce should be handled by caller)
     */
    fun setOnTextChangedListener(listener: (String) -> Unit) {
        onTextChangedListener = listener
    }

    /**
     * Set listener for search action (keyboard search button)
     */
    fun setOnSearchActionListener(listener: (String) -> Unit) {
        onSearchActionListener = listener
    }

    /**
     * Check if search bar has text
     */
    fun hasText(): Boolean = binding.etInput.text?.isNotEmpty() == true
}
