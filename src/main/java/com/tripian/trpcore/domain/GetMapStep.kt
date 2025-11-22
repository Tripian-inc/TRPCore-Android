package com.tripian.trpcore.domain

import com.tripian.one.api.pois.model.Coordinate
import com.tripian.one.api.reactions.model.Reaction
import com.tripian.one.api.trip.model.Plan
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.domain.model.MapStep
import com.tripian.trpcore.repository.TripModelRepository
import com.tripian.trpcore.repository.UserReactionRepository
import com.tripian.trpcore.util.extensions.mapIcons
import com.tripian.trpcore.util.extensions.step2MapStep
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class GetMapStep @Inject constructor(
    val tripModelRepository: TripModelRepository,
    val userReactionRepository: UserReactionRepository
) : BaseUseCase<List<MapStep>, Unit>() {

    override fun on(params: Unit?) {
        addObservable {
            tripModelRepository.getDailyPlanEmitter().flatMap { plan ->
                tripModelRepository.trip!!.tripHash?.let {
                    userReactionRepository.getUserReactions(it).map { reactionRes ->
                        return@map plan2step(reactionRes.data, plan)
                    }
                }
            }
        }
    }

    private fun plan2step(reactions: List<Reaction>? = null, plan: Plan): List<MapStep> {
        val items = ArrayList<MapStep>()

        var order = 0
        var position = 1

        plan.steps?.forEach { step ->
            items.add(step2MapStep(step, alternative = false).apply {
                this.position = position++
                this.order = order++
                this.reaction = reactions?.find { it.stepId == step.id && it.poiId == step.poi?.id }
                this.planDate = plan.date
            })
        }

        tripModelRepository.trip?.tripProfile?.accommodation?.let { accommodation ->
            val item = MapStep()

            item.homeBase = true
            item.group = "step"
            item.poiId = "Homebase"
            item.name = accommodation.name
            item.image = "Homebase"
            item.markerIcon = mapIcons["Homebase"] ?: -1

//            item.position = position++
            item.coordinate = Coordinate().apply {
                lat = accommodation.coordinate?.lat ?: -1.0
                lng = accommodation.coordinate?.lng ?: -1.0
            }

            items.add(index = 0, item)

            items.add(item)
        }

        return items
    }
}