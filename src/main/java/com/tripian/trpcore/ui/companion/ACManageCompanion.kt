package com.tripian.trpcore.ui.companion

import android.os.Bundle
import com.tripian.trpcore.R
import com.tripian.trpcore.base.BaseActivity
import com.tripian.trpcore.databinding.AcManageCompanionBinding
import com.tripian.trpcore.util.LanguageConst

/**
 * Created by semihozkoroglu on 24.08.2020.
 */
enum class CompanionMode {
    PROFILE,
    TRIP,
    CREATETRIP
}

class ACManageCompanion : BaseActivity<AcManageCompanionBinding, ACManageCompanionVM>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent.extras?.getSerializable("mode")?.let {
            if (it == CompanionMode.PROFILE) {
                binding.tvTitle.text = getLanguageForKey(LanguageConst.TRAVEL_COMPANIONS)
            }
        }

        overridePendingTransition(R.anim.anim_slide_in_up, R.anim.anim_slide_out_up)
    }

    override fun onPause() {
        super.onPause()

        overridePendingTransition(R.anim.anim_slide_in_down, R.anim.anim_slide_out_down)
    }

    override fun getViewBinding(): AcManageCompanionBinding {
        return AcManageCompanionBinding.inflate(layoutInflater)
    }

    override fun setListeners() {
        binding.imNavigation.setOnClickListener { viewModel.onClickedClose() }
        binding.btnNewCompanion.setOnClickListener { viewModel.onClickedAdd() }
    }

    override fun setReceivers() {
    }
}