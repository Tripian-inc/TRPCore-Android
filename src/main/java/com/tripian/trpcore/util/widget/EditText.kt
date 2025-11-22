package com.tripian.trpcore.util.widget

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText

/**
 * Created by semihozkoroglu on 2019-08-06.
 */
open class EditText @JvmOverloads constructor(context: Context, attrs: AttributeSet?) : AppCompatEditText(context, attrs) {
    init {
        this.isFocusable = true
        this.isFocusableInTouchMode = true
    }
}