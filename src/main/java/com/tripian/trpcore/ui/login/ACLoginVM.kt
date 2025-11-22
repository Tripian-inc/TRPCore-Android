package com.tripian.trpcore.ui.login

//import com.tripian.trpcore.repository.authorization.AuthRepository
import android.net.Uri
import android.os.Bundle
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.DoSocialLogin
import com.tripian.trpcore.util.extensions.navigateToFragment
import com.tripian.trpcore.util.extensions.showLoading
import javax.inject.Inject

class ACLoginVM @Inject constructor(
    val doSocialLogin: DoSocialLogin,
) : BaseViewModel(doSocialLogin) {

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

        navigateToFragment(FRLogin.newInstance(), addToBackStack = true)
    }

    fun parseToken(uri: Uri) {
        showLoading()

//        doLoginCognito.getToken(uri)
    }

//    fun doSocialLogin(acLogin: ACLogin, provider: AwsAuthorization.Provider) {
//        doLoginCognito.on(DoLoginCognito.Params(acLogin, provider), success = {
//            doSocialLogin.on(Unit, success = {
//                hideLoading()
//
//                startActivity(ACMyTrip::class)
//
//                finishActivity()
//            }, error = {
//                hideLoading()
//
//                if (it.type == AlertType.DIALOG) {
//                    showDialog(contentText = it.errorDesc)
//                } else {
//                    showAlert(it.type, it.errorDesc)
//                }
//            })
//        }, error = {
//            hideLoading()
//            doLogoutCognito.on()
//            if (it.type == AlertType.DIALOG) {
//                showDialog(contentText = it.errorDesc)
//            } else {
//                showAlert(it.type, it.errorDesc)
//            }
//        })
//    }
}