package com.tripian.trpcore.domain

import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.Service
import com.tripian.trpcore.util.ReactionType
import com.tripian.one.api.reactions.model.ReactionRequest
import com.tripian.one.api.reactions.model.ReactionResponse
import com.tripian.one.api.trip.model.Step
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class UpdateReaction @Inject constructor(val repository: Service) : BaseUseCase<ReactionResponse, UpdateReaction.Params>() {

    class Params(val reactionId: Int, val step: Step, val reaction: ReactionType, val comment: String)

    override fun on(params: Params?) {
        addObservable {
            repository.updateReaction(params!!.reactionId, ReactionRequest().apply {
                poiId = params.step.poi?.id
                stepId = params.step.id
                reaction = params.reaction.type
                comment = params.comment
            })
        }
    }
}