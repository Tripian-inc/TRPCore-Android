package com.tripian.trpcore.base

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.TextView
import com.tripian.trpcore.R
import com.tripian.trpcore.databinding.FrWarningBinding
import com.tripian.trpcore.util.dialog.DGActionListener
import com.tripian.trpcore.util.dialog.DGContent
import com.tripian.trpcore.util.extensions.observe

/**
 * Created by semihozkoroglu on 11.03.2020.
 */
class FRWarning : BaseDialogFragment<FrWarningBinding, FRWarningDialogVM>(FrWarningBinding::inflate) {

    var positiveListener: DGActionListener? = null
    var negativeListener: DGActionListener? = null

    companion object {
        fun newInstance(
            title: String? = "", contentText: String? = "",
            positiveBtn: String? = null, negativeBtn: String? = null,
            isCloseEnable: Boolean
        ): FRWarning {
            val fragment = FRWarning()

            val data = Bundle()

            if (!TextUtils.isEmpty(title)) {
                data.putString("title", title)
            }

            if (!TextUtils.isEmpty(positiveBtn)) {
                data.putString("positiveBtn", positiveBtn)
            }

            if (!TextUtils.isEmpty(negativeBtn)) {
                data.putString("negativeBtn", negativeBtn)
            }

            data.putString("contentText", contentText)
            data.putBoolean("isCloseEnable", isCloseEnable)

            fragment.arguments = data

            return fragment
        }
    }

    private fun getDialogContent(): DGContent? {
        var title = arguments?.getString("title")

        if (TextUtils.isEmpty(title)) {
            title = viewModel.getLanguageForKey("warning")
        }

        val positiveBtn = arguments?.getString("positiveBtn")
        val negativeBtn = arguments?.getString("negativeBtn")

        return DGContent(
            title = title,
            positiveBtn = positiveBtn,
            negativeBtn = negativeBtn,
            positiveListener = positiveListener,
            negativeListener = negativeListener
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dgContent = getDialogContent()

        if (dgContent != null) {
            if (!TextUtils.isEmpty(dgContent.title)) {
                binding.tvTitle.text = dgContent.title
//                mContainer.findViewById<TextView>(R.id.tvTitle).text = dgContent.title
            }

            if (!TextUtils.isEmpty(dgContent.negativeBtn)) {
                binding.btnNegative.visibility = View.VISIBLE
                binding.btnNegative.text = dgContent.negativeBtn
                binding.btnNegative.setOnClickListener {

                    if (dgContent.negativeListener == null) {
                        dismiss()
                    }

                    dgContent.negativeListener?.onClicked(this)
                }
            } else {
                binding.btnNegative.visibility = View.GONE
            }

            if (!TextUtils.isEmpty(dgContent.positiveBtn)) {
                binding.btnPositive.visibility = View.VISIBLE
                binding.btnPositive.text = dgContent.positiveBtn
                binding.btnPositive.setOnClickListener {
                    if (dgContent.positiveListener == null) {
                        dismiss()
                    }

                    dgContent.positiveListener?.onClicked(this)
                }
            } else {
                binding.btnPositive.visibility = View.GONE
            }

            isCancelable = false
        }
    }

    override fun setReceivers() {
        super.setReceivers()

        observe(viewModel.onSetTitleListener) {
            binding.tvTitle.text = it

            if (TextUtils.isEmpty(it)) {
                binding.tvTitle.visibility = View.GONE
            }
        }

        observe(viewModel.onSetDescriptionListener) {
            binding.tvDescription.text = it
        }
    }
}