package com.tripian.trpcore.repository

import com.tripian.one.api.reactions.model.Reaction
import com.tripian.one.api.reactions.model.ReactionRequest
import com.tripian.one.api.reactions.model.ReactionResponse
import com.tripian.one.api.reactions.model.ReactionsResponse
import com.tripian.one.api.trip.model.DeleteResponse
import com.tripian.trpcore.util.extensions.remove
import io.reactivex.Observable
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class UserReactionRepository @Inject constructor(val service: Service) {

    var reactions: ArrayList<Reaction>? = null

    fun deleteUserReaction(reactionId: Int): Observable<DeleteResponse> {
        return service.deleteReaction(reactionId).map {
            reactions?.remove { it.id == reactionId }

            it
        }
    }

    fun getUserReactions(tripHash: String): Observable<ReactionsResponse> {
        return if (reactions == null) {
            service.getUserReactions(tripHash).map {
                if (!it.data.isNullOrEmpty()) {
                    reactions = ArrayList(it.data!!)
                }

                it
            }
        } else {
            Observable.just(ReactionsResponse().apply {
                data = reactions
                status = 200
            })
        }
    }

    fun addReaction(request: ReactionRequest): Observable<ReactionResponse> {
        return service.addReaction(request).map {
            if (reactions == null) {
                reactions = ArrayList()
            }

            if (it.data != null) {
                reactions!!.add(it.data!!)
            }

            it
        }
    }

    fun clearItems() {
        reactions?.clear()
    }
}