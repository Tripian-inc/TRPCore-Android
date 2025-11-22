package com.tripian.trpcore.domain

import com.tripian.trpcore.base.BaseUseCase
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class LogoutUser @Inject constructor() : BaseUseCase<Unit, Unit>() {

    override fun on(params: Unit?) {
        tripianUserRepository.logout()

        onSendSuccess(Unit)
    }
}