package com.tripian.trpcore.domain

import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.CompanionRepository
import com.tripian.one.api.trip.model.DeleteResponse
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class DeleteCompanion @Inject constructor(val repository: CompanionRepository) : BaseUseCase<DeleteResponse, DeleteCompanion.Params>() {

    class Params(val companionId: Int)

    override fun on(params: Params?) {
        addObservable {
            repository.deleteCompanion(params!!.companionId)
        }
    }
}