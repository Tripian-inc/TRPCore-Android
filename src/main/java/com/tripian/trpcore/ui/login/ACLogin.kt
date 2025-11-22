package com.tripian.trpcore.ui.login

import android.content.Intent
import com.tripian.trpcore.base.BaseActivity
import com.tripian.trpcore.databinding.AcLoginBinding
import com.tripian.trpcore.repository.authorization.AwsAuthorization

/**
 * Created by semihozkoroglu on 15.08.2020.
 */
class ACLogin : BaseActivity<AcLoginBinding, ACLoginVM>() {

    override fun getViewBinding(): AcLoginBinding {
        return AcLoginBinding.inflate(layoutInflater)
    }

    override fun setListeners() {
    }

    override fun setReceivers() {
    }

    fun doSocialLogin(provider: AwsAuthorization.Provider) {
//        viewModel.doSocialLogin(this, provider)
    }
//    override fun onNewIntent(intent: Intent?) {
//        super.onNewIntent(intent)
//
//        if (intent?.data != null) {
//            viewModel.parseToken(intent.data!!)
//        }
//    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (intent.data != null) {
            viewModel.parseToken(intent.data!!)
        }
    }
}