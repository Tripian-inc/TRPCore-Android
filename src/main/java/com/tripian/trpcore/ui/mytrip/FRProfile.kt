package com.tripian.trpcore.ui.mytrip

import com.tripian.trpcore.base.BaseFragment
import com.tripian.trpcore.databinding.FrProfileBinding
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.capitalizeFirstChar
import com.tripian.trpcore.util.extensions.observe

/**
 * Created by semihozkoroglu on 15.08.2020.
 */
class FRProfile : BaseFragment<FrProfileBinding, FRProfileVM>(FrProfileBinding::inflate) {

    companion object {
        fun newInstance(): FRProfile {
            return FRProfile()
        }
    }

    override fun setListeners() {
        super.setListeners()

        binding.llEditProfile.setOnClickListener { viewModel.onClickedEditProfile() }
        binding.llCompanion.setOnClickListener { viewModel.onClickedCompanion() }
        binding.llChangePassword.setOnClickListener { viewModel.onClickedChangePassword() }
        binding.llHelp.setOnClickListener { viewModel.onClickedHelp() }
        binding.llAbout.setOnClickListener { viewModel.onClickedAbout() }
        binding.llLogout.setOnClickListener { viewModel.onClickedLogout() }
    }

    override fun setReceivers() {
        super.setReceivers()

        observe(viewModel.onSetUserListener) {
            val text =
                getLanguageForKey(LanguageConst.HELLO) + ", " + "${it?.firstName?.capitalizeFirstChar()} ${it?.lastName?.capitalizeFirstChar()}"
            binding.tvWelcome.text = text
        }
    }
}