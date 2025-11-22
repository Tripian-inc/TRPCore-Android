package com.tripian.trpcore.domain

import com.tripian.one.api.users.model.LoginResponse
import com.tripian.trpcore.util.Preferences
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.util.extensions.getDeviceId
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class DoLightLogin @Inject constructor(val pref: Preferences) : BaseUseCase<LoginResponse, DoLightLogin.Params>() {

    class Params(
        val firstName: String? = null,
        val lastName: String? = null,
        val uniqueId: String? = null,
    )

    override fun on(params: Params?) {
        var uniqueId = params?.uniqueId
        if (uniqueId.isNullOrEmpty()) {
            val deviceId = getDeviceId(pref)
            uniqueId = "$deviceId@tripianguest.com"
        }
        addObservable {
            tripianUserRepository.lightLogin(
                firstName = params?.firstName,
                lastName = params?.lastName,
                uniqueId = uniqueId,
            )
        }
    }

    override fun isRequiredRefreshToken(): Boolean {
        return false
    }
}