package com.tripian.trpcore.domain

import com.tripian.aws_auth.AuthUserSession
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.authorization.AwsAuthorization
import com.tripian.trpcore.repository.base.ErrorModel
import com.tripian.trpcore.util.Preferences
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class CheckCognito @Inject constructor(val repository: AwsAuthorization, val preferences: Preferences) : BaseUseCase<AuthUserSession, Unit>() {

    override fun on(params: Unit?) {
        val provider = preferences.getString(Preferences.Keys.SOCIAL_PROVIDER)

        if (provider == null) {
            onSendError(ErrorModel())
        } else {
            addObservable {
                repository.getSession(if (provider == AwsAuthorization.Provider.GOOGLE.identity) AwsAuthorization.Provider.GOOGLE else AwsAuthorization.Provider.FACEBOOK)
            }
        }
    }

    override fun isRequiredRefreshToken(): Boolean {
        return false
    }
}