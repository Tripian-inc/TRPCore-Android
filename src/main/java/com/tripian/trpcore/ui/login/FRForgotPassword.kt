package com.tripian.trpcore.ui.login

import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import androidx.core.view.isVisible
import com.tripian.trpcore.base.BaseFragment
import com.tripian.trpcore.databinding.FrForgotPasswordBinding
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.observe

/**
 * Created by semihozkoroglu on 29.05.2021.
 */
class FRForgotPassword :
    BaseFragment<FrForgotPasswordBinding, FRForgotPasswordVM>(FrForgotPasswordBinding::inflate) {

    companion object {
        fun newInstance(): FRForgotPassword {
            return FRForgotPassword()
        }
    }

    override fun setListeners() {
        super.setListeners()

        binding.tvTitle.text = getLanguageForKey(LanguageConst.FORGOT_PASS)
        binding.tvDescription.text = getLanguageForKey(LanguageConst.FORGOT_PASS_DESC)
        binding.etMail.hint = getLanguageForKey(LanguageConst.EMAIL_PLACEHOLDER)
        binding.etHash.hint = getLanguageForKey(LanguageConst.CODE)
        binding.etPassword.hint = getLanguageForKey(LanguageConst.ENTER_NEW_PASSWORD)
        binding.btnSendEmail.text = getLanguageForKey(LanguageConst.SEND_MAIL)
        binding.btnBackToEmail.text = getLanguageForKey(LanguageConst.SEND_MAIL)
        binding.btnResetPassword.text = getLanguageForKey(LanguageConst.RESET_PSW)

        binding.imNavigation.setOnClickListener { viewModel.onClickedBack() }

        binding.btnSendEmail.setOnClickListener {
            viewModel.onClickedSendMail(binding.etMail.text.toString().trim())
        }

        binding.btnResetPassword.setOnClickListener {
            viewModel.onClickedResetPassword(
                binding.etPassword.text.toString().trim(),
                binding.etHash.text.toString().trim()
            )
        }

        binding.imShowPassword.setOnClickListener {
            if (binding.imShowPassword.tag == null || binding.imShowPassword.tag == false) {
                binding.etPassword.transformationMethod =
                    HideReturnsTransformationMethod.getInstance()
                binding.imShowPassword.tag = true
            } else {
                binding.etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                binding.imShowPassword.tag = false
            }
        }
        binding.btnBackToEmail.setOnClickListener {
            showHideResetPassword(false)
        }
    }

    override fun setReceivers() {
        super.setReceivers()

        observe(viewModel.onShowPasswordListener) {
            showHideResetPassword(true)
        }
    }

    private fun showHideResetPassword(show: Boolean) {

        binding.etMail.isVisible = !show
        binding.rlPassword.isVisible = show
        binding.etHash.isVisible = show
        binding.btnSendEmail.isVisible = !show
        binding.llResetPassword.isVisible = show
        if (show) {
            binding.tvDescription.text = getLanguageForKey(LanguageConst.ENTER_NEW_PSW)
        } else {
            binding.tvDescription.text = getLanguageForKey(LanguageConst.ENTER_EMAIL_RESET)
        }
    }
}