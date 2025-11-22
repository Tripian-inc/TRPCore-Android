package com.tripian.trpcore.domain

import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.authorization.AwsAuthorization
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class DoLogoutCognito @Inject constructor(val repository: AwsAuthorization) : BaseUseCase<Unit, Unit>() {

    override fun on(params: Unit?) {
        addObservable {

            PublishSubject.create {
                it.onNext(repository.doLogout())
            }
        }
    }

    override fun isRequiredRefreshToken(): Boolean {
        return false
    }
}