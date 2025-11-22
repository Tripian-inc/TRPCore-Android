package com.tripian.trpcore.util.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import android.view.WindowManager
import com.tripian.trpcore.R

/**
 * Created by semihozkoroglu on 2019-08-31.
 */
class DGLockScreen : Dialog {

    constructor(context: Context) : super(context) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        if (window != null) {
            window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window!!.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }

        setCanceledOnTouchOutside(false)
        setCancelable(true)

        setContentView(R.layout.dg_lock_screen)
    }
}