package com.tripian.trpcore.ui.login

import android.graphics.Paint
import com.tripian.trpcore.base.BaseFragment
import com.tripian.trpcore.databinding.FrLoginBinding
import com.tripian.trpcore.repository.authorization.AwsAuthorization
import com.tripian.trpcore.util.LanguageConst

/**
 * Created by semihozkoroglu on 29.05.2021.
 */
class FRLogin : BaseFragment<FrLoginBinding, FRLoginVM>(FrLoginBinding::inflate) {

    companion object {
        fun newInstance(): FRLogin {
            return FRLogin()
        }
    }

    override fun onResume() {
        super.onResume()
//        setViews()
    }

    private fun setViews() {
        binding.tvLogin.text = getLanguageForKey(LanguageConst.LOGIN_TITLE)
        binding.tvForgotPassword.text = getLanguageForKey(LanguageConst.FORGOT_PASS)
        binding.btnLogin.text = getLanguageForKey(LanguageConst.LOGIN)
        binding.tvTitleOr.text = getLanguageForKey(LanguageConst.OR)
        binding.tvGoogle.text = getLanguageForKey(LanguageConst.GOOGLE_LOGIN)
        binding.tvDontHaveAccount.text = getLanguageForKey(LanguageConst.DONT_HAVE_ACCOUNT)
        binding.btnRegister.text = getLanguageForKey(LanguageConst.REGISTER_NOW)
//        binding.inputEmail.setPlaceholder(getLanguageForKey(LanguageConst.EMAIL_PLACEHOLDER))
//        binding.inputPassword.setPlaceholder(getLanguageForKey(LanguageConst.ENTER_PASSWORD))
    }

    override fun setListeners() {
        super.setListeners()

        setViews()

//        binding.inputEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
//        binding.inputPassword.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD)

        binding.tvForgotPassword.apply {
            paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
        }
        binding.tvForgotPassword.setOnClickListener {
            viewModel.onClickedForgotPassword()
        }
        binding.btnLogin.setOnClickListener {
//            viewModel.onClickedLogin(
//                binding.inputEmail.text.toString(),
//                binding.inputPassword.text.toString()
//            )
        }
        binding.btnRegister.setOnClickListener { viewModel.onClickedRegister() }
        binding.btnGoogle.setOnClickListener {
            (requireActivity() as ACLogin).doSocialLogin(
                AwsAuthorization.Provider.GOOGLE
            )
        }
    }
}