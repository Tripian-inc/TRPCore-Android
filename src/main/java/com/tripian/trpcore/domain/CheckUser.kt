package com.tripian.trpcore.domain

import com.tripian.trpcore.base.BaseUseCase
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class CheckUser @Inject constructor(
    getUser: GetUser,
//    checkCognito: CheckCognito
) : BaseUseCase<Boolean, Unit>() {

    override fun on(params: Unit?) {

//        if (pref)
//        val provider = Preferences.getString(Preferences.Keys.SOCIAL_PROVIDER)
//
//        if (provider == null) {
//            onSendError(ErrorModel())
//        } else {
//            addObservable {
//
//                repository.getSession(if (provider == AwsAuthorization.Provider.GOOGLE.identity) AwsAuthorization.Provider.GOOGLE else AwsAuthorization.Provider.FACEBOOK)
//            }
//        }
    }

    override fun isRequiredRefreshToken(): Boolean {
        return false
    }
}