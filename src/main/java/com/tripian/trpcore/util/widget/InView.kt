package com.tripian.trpcore.util.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.tripian.trpcore.R
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.util.LanguageConst

class InView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet?
) : LinearLayout(context, attrs) {

    private var inflater: LayoutInflater = context!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    private var root: View
    private var tvText: TextView
//    private var imIcon: ImageView

    init {
        inflater.inflate(R.layout.view_i_am_in, this, true)

        root = findViewById(R.id.root)
        tvText = findViewById(R.id.tvText)
//        imIcon = findViewById(R.id.imIcon)
    }

    fun offerEnable(enable: Boolean, optOutEnable: Boolean = false) {
        if (enable) {
//            if (optOutEnable) {
                root.setBackgroundResource(R.drawable.bg_i_am_in_enable)
//            } else {
//                root.setBackgroundResource(R.drawable.bg_i_am_in_disable)
//            }
//            imIcon.setImageResource(R.drawable.ic_i_am_in_disable)
            tvText.text = TRPCore.core.miscRepository.getLanguageValueForKey(LanguageConst.I_AM_OUT)
        } else {
            root.setBackgroundResource(R.drawable.bg_i_am_in_enable)
//            imIcon.setImageResource(R.drawable.ic_i_am_in_enable)
            tvText.text = TRPCore.core.miscRepository.getLanguageValueForKey(LanguageConst.I_AM_IN)
        }
    }
}