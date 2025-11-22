package com.tripian.trpcore.domain

import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.TripianUserRepository
import com.tripian.one.api.users.model.User
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class GetUserListener @Inject constructor(val repository: TripianUserRepository) : BaseUseCase<User, Unit>() {

    override fun on(params: Unit?) {
        addObservable {
            repository.getUserEmitter()
        }
    }
}