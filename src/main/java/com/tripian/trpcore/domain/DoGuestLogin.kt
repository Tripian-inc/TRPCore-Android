package com.tripian.trpcore.domain

import com.tripian.one.api.users.model.LoginResponse
import com.tripian.trpcore.util.Preferences
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.util.extensions.getDeviceId
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class DoGuestLogin @Inject constructor(val pref: Preferences) : BaseUseCase<LoginResponse, Unit>() {

    override fun on(params: Unit?) {
        val firstName = "Guest"
        val deviceId = getDeviceId(pref)
        val lastName = "User"
        val email = "$deviceId@tripianguest.com"
        val psw = "Tripian1234"
        addObservable {
            tripianUserRepository.guestLogin(
                firstName = firstName,
                lastName = lastName,
                email = email,
                password = psw
            )
        }
    }

    override fun isRequiredRefreshToken(): Boolean {
        return false
    }
}