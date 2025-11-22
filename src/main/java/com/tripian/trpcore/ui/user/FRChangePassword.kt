package com.tripian.trpcore.ui.user

import com.tripian.trpcore.base.BaseBottomDialogFragment
import com.tripian.trpcore.databinding.FrChangePasswordBinding
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.observe

/**
 * Created by semihozkoroglu on 24.08.2020.
 */
class FRChangePassword :
    BaseBottomDialogFragment<FrChangePasswordBinding, FRChangePasswordVM>(FrChangePasswordBinding::inflate) {

    companion object {
        fun newInstance(): FRChangePassword {
            return FRChangePassword()
        }
    }

    override fun setListeners() {
        super.setListeners()
        binding.tvTitle.text = getLanguageForKey(LanguageConst.CHANGE_PSW)
        binding.etOldPassword.hint = getLanguageForKey(LanguageConst.ENTER_PASSWORD)
        binding.etPassword.hint = getLanguageForKey(LanguageConst.ENTER_NEW_PASSWORD)
        binding.etPasswordConfirm.hint = getLanguageForKey(LanguageConst.RETYPE_NEW_PASSWORD)
        binding.btnCreate.text = getLanguageForKey(LanguageConst.UPDATE)

        binding.btnCreate.setOnClickListener {
            viewModel.onClickedUpdate(
                binding.etOldPassword.text.toString(),
                binding.etPassword.text.toString(),
                binding.etPasswordConfirm.text.toString()
            )
        }
    }

    override fun setReceivers() {
        super.setReceivers()

        observe(viewModel.onClearPasswordListener) {
            binding.etPassword.setText("")
        }
    }
}