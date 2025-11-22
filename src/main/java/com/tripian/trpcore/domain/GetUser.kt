package com.tripian.trpcore.domain

import com.tripian.trpcore.base.BaseUseCase
import com.tripian.one.api.users.model.User
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class GetUser @Inject constructor() : BaseUseCase<User, Unit>() {

    override fun on(params: Unit?) {
        addObservable {
            tripianUserRepository.getUser().map {
                addObservable {
                    tripianUserRepository.getUserEmitter()
                }

                it.data
            }
        }
    }
}