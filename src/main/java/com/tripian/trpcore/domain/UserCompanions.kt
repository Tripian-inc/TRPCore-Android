package com.tripian.trpcore.domain

import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.CompanionRepository
import com.tripian.one.api.companion.model.CompanionsResponse
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class UserCompanions @Inject constructor(val repository: CompanionRepository) : BaseUseCase<CompanionsResponse, Unit>() {

    override fun on(params: Unit?) {
        addObservable { repository.getUserCompanions() }
    }
}