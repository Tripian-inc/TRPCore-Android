package com.tripian.trpcore.domain

import com.tripian.one.api.users.model.LoginResponse
import com.tripian.trpcore.base.SuspendUseCase
import com.tripian.trpcore.util.Preferences
import com.tripian.trpcore.util.extensions.getDeviceId
import javax.inject.Inject

class DoLightLogin @Inject constructor(
    val pref: Preferences
) : SuspendUseCase<LoginResponse, DoLightLogin.Params>() {

    class Params(
        val firstName: String? = null,
        val lastName: String? = null,
        val uniqueId: String? = null,
    )

    override suspend fun execute(params: Params): LoginResponse {
        val resolvedUniqueId = if (params.uniqueId.isNullOrEmpty()) {
            "${getDeviceId(pref)}@tripianguest.com"
        } else {
            params.uniqueId
        }
        return tripianUserRepository.lightLoginAsync(
            uniqueId = resolvedUniqueId,
            firstName = params.firstName,
            lastName = params.lastName
        )
    }
}
