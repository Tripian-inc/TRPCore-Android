package com.tripian.trpcore.domain

import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.TripModelRepository
import com.tripian.trpcore.repository.UserReactionRepository
import com.tripian.trpcore.util.ReactionType
import com.tripian.one.api.reactions.model.ReactionRequest
import com.tripian.one.api.reactions.model.ReactionResponse
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class AddReaction @Inject constructor(val repository: UserReactionRepository, val tripModelRepository: TripModelRepository) : BaseUseCase<ReactionResponse, AddReaction.Params>() {

    class Params(val poiId: String, val stepId: Int, val reaction: ReactionType)

    override fun on(params: Params?) {
        addObservable {
            repository.addReaction(ReactionRequest().apply {
                poiId = params!!.poiId
                stepId = params.stepId
                reaction = params.reaction.type
            })
        }
    }

    override fun onSendSuccess(t: ReactionResponse) {
//        tripModelRepository.updateDailyEvent()

        super.onSendSuccess(t)
    }
}