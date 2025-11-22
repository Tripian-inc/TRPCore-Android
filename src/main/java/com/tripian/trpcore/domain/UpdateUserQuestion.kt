package com.tripian.trpcore.domain

import com.tripian.trpcore.base.BaseUseCase
import com.tripian.one.api.users.model.UserResponse
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class UpdateUserQuestion @Inject constructor() : BaseUseCase<UserResponse, UpdateUserQuestion.Params>() {

    class Params(val answers: Array<Int>? = null)

    override fun on(params: Params?) {
        addObservable {
            tripianUserRepository.update(answers = params!!.answers)
        }
    }
}