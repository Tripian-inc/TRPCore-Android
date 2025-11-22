package com.tripian.trpcore.domain

import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.TripModelRepository
import com.tripian.trpcore.repository.UserReactionRepository
import com.tripian.one.api.trip.model.DeleteResponse
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class DeleteReaction @Inject constructor(val repository: UserReactionRepository, val tripModelRepository: TripModelRepository) : BaseUseCase<DeleteResponse, DeleteReaction.Params>() {

    class Params(val reactionId: Int)

    override fun on(params: Params?) {
        addObservable {
            repository.deleteUserReaction(params!!.reactionId)
        }
    }

    override fun onSendSuccess(t: DeleteResponse) {
//        tripModelRepository.updateDailyEvent()
        super.onSendSuccess(t)
    }
}