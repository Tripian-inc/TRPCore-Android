@file:Suppress("OVERLOADS_WITHOUT_DEFAULT_ARGUMENTS")

package com.tripian.trpcore.util.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import com.tripian.trpcore.R

class CheckView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet?
) : LinearLayout(context, attrs) {

    var text: String? = null
        set(value) {
            tvText.text = value

            field = value
        }

    private var inflater: LayoutInflater = context!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    private var root: View
    private var tvText: TextView
    private var isChecked = false

    private var onCheckedListener: ((Boolean) -> Unit)? = null

    init {
        inflater.inflate(R.layout.view_check, this, true)

        root = findViewById(R.id.root)
        tvText = findViewById(R.id.tvText)

        if (attrs != null) {
            context!!.withStyledAttributes(attrs, R.styleable.CheckView) {

                getString(R.styleable.CheckView_text).let {
                    tvText.text = it
                }

            }
        }

        root.setOnClickListener { check(!isChecked) }
    }

    fun check(isChecked: Boolean, slient: Boolean = false) {
        this.isChecked = isChecked

        if (!slient) {
            onCheckedListener?.invoke(isChecked)
        }

        if (isChecked) {
            root.setBackgroundResource(R.drawable.bg_oval_purple_large_radius)
            tvText.setTextColor(ContextCompat.getColor(context, R.color.white))
        } else {
            root.setBackgroundResource(R.drawable.bg_oval_gray_large_radius)
            tvText.setTextColor(ContextCompat.getColor(context, R.color.body))
        }
    }

    fun setOnCheckListener(task: ((Boolean) -> Unit)?) {
        onCheckedListener = task
    }
}