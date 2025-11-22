package com.tripian.trpcore.ui.user

import android.os.Bundle
import com.tripian.trpcore.R
import com.tripian.trpcore.base.BaseActivity
import com.tripian.trpcore.databinding.AcEditProfileBinding
import com.tripian.trpcore.util.extensions.observe

/**
 * Created by semihozkoroglu on 24.08.2020.
 */
class ACEditProfile : BaseActivity<AcEditProfileBinding, ACEditProfileVM>() {
    override fun getViewBinding(): AcEditProfileBinding {
        return AcEditProfileBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        overridePendingTransition(R.anim.anim_slide_in_up, R.anim.anim_slide_out_up)
    }

    override fun onPause() {
        super.onPause()

        overridePendingTransition(R.anim.anim_slide_in_down, R.anim.anim_slide_out_down)
    }

    override fun setListeners() {
        binding.imNavigation.setOnClickListener { viewModel.onClickedClose() }
    }

    override fun setReceivers() {
        observe(viewModel.onSetTitleListener) {
            binding.tvTitle.text = it
        }
    }
}