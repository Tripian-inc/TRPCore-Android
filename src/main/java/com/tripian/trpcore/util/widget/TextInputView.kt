@file:Suppress("OVERLOADS_WITHOUT_DEFAULT_ARGUMENTS")

package com.tripian.trpcore.util.widget

import android.content.Context
import android.text.InputType
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.core.content.withStyledAttributes
import com.tripian.trpcore.R
import com.tripian.trpcore.databinding.ViewTextInputBinding

class TextInputView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {


    private val binding = ViewTextInputBinding.inflate(LayoutInflater.from(context), this, true)

    val textInputLayout: TextInputLayout get() = binding.til
    val editText get() = binding.et
    val textView get() = binding.tvLabel

    init {
        orientation = VERTICAL

        // Optional: still read XML attrs, but programmatic calls override them
        context?.withStyledAttributes(attrs, R.styleable.PillInputView) {
            setLabel(getString(R.styleable.PillInputView_labelText))
            setPlaceholder(getString(R.styleable.PillInputView_placeholderText))

            val inputType = getInt(R.styleable.PillInputView_android_inputType, InputType.TYPE_CLASS_TEXT)
            setInputType(inputType)

            val toggle = getBoolean(R.styleable.PillInputView_isPasswordToggleEnabled, false)
            setPasswordToggleEnabled(toggle)

            getResourceId(R.styleable.PillInputView_labelTextAppearance, 0).takeIf { it != 0 }?.let {
                binding.tvLabel.setTextAppearance(it)
            }
            getResourceId(R.styleable.PillInputView_fieldTextAppearance, 0).takeIf { it != 0 }?.let {
                binding.et.setTextAppearance(it)
            }
            getResourceId(R.styleable.PillInputView_placeholderTextAppearance, 0).takeIf { it != 0 }?.let {
                binding.til.setPlaceholderTextAppearance(it)
            }
        }
    }

    fun setLabel(text: CharSequence?) {
        // accessibility: announce label as the field label
        binding.et.hint = null // avoid double announcement
        binding.et.contentDescription = null
        binding.til.hint = null
    }

    fun setLabel(@StringRes resId: Int) = setLabel(context.getString(resId))

    /** Shows when field is empty, stays inside the pill */
    fun setPlaceholder(text: CharSequence?) {
        binding.tvLabel.text = text ?: ""
        binding.til.placeholderText = text
    }

    fun setPlaceholder(@StringRes resId: Int) = setPlaceholder(context.getString(resId))

    fun setPasswordToggleEnabled(enabled: Boolean) {
        binding.til.endIconMode = if (enabled)
            com.google.android.material.textfield.TextInputLayout.END_ICON_PASSWORD_TOGGLE
        else
            com.google.android.material.textfield.TextInputLayout.END_ICON_NONE
        // If you enable toggle, make sure inputType has password variation
        if (enabled && (binding.et.inputType and InputType.TYPE_TEXT_VARIATION_PASSWORD) == 0) {
            setInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        }
        binding.et.transformationMethod = if (enabled) PasswordTransformationMethod.getInstance() else HideReturnsTransformationMethod.getInstance()
    }

    fun setInputType(type: Int) {
        binding.et.inputType = type
    }

    /* ---------- Convenience ---------- */
    var text: CharSequence?
        get() = binding.et.text
        set(value) { binding.et.setText(value) }

    fun setError(message: CharSequence?) { textInputLayout.error = message }
}