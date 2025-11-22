package com.tripian.trpcore.domain

import com.tripian.trpcore.base.BaseUseCase
import com.tripian.one.api.users.model.EmptyResponse
import com.tripian.one.api.users.model.LoginResponse
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class DoSocialLogin @Inject constructor() : BaseUseCase<EmptyResponse, Unit>() {

    override fun on(params: Unit?) {
        addObservable {
            tripianUserRepository.socialLogin()
        }
    }

    override fun isRequiredRefreshToken(): Boolean {
        return false
    }
}