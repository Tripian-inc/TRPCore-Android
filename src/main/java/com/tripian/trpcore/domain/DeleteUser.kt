package com.tripian.trpcore.domain

import com.tripian.one.api.users.model.EmptyResponse
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.TripModelRepository
import com.tripian.trpcore.repository.TripianUserRepository
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class DeleteUser @Inject constructor() : BaseUseCase<EmptyResponse, Unit>() {

    override fun on(params: Unit?) {
        addObservable {
            tripianUserRepository.deleteAccount()
        }

    }

    override fun onSendSuccess(t: EmptyResponse) {
        tripianUserRepository.deleteUser()
        super.onSendSuccess(t)
    }
}