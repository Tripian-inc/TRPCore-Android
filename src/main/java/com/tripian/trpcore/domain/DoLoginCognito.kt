package com.tripian.trpcore.domain

//import android.app.Activity
//import android.net.Uri
//import com.tripian.trpcore.base.BaseUseCase
//import com.tripian.aws_auth.AuthUserSession
//import com.tripian.trpcore.repository.authorization.AwsAuthorization
//import javax.inject.Inject

//class DoLoginCognito @Inject constructor(val repository: AwsAuthorization) : BaseUseCase<AuthUserSession, DoLoginCognito.Params>() {
//
//    class Params(val activity: Activity, val provider: AwsAuthorization.Provider)
//
//    override fun on(params: Params?) {
//        addObservable {
//            repository.doLogin(params!!.activity, params.provider)
//        }
//    }
//
//    fun getToken(uri: Uri) {
//        repository.getToken(uri)
//    }
//
//    override fun isRequiredRefreshToken(): Boolean {
//        return false
//    }
//}