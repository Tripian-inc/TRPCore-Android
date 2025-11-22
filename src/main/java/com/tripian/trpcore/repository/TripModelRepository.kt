package com.tripian.trpcore.repository

import com.tripian.one.api.trip.model.Plan
import com.tripian.one.api.trip.model.Trip
import com.tripian.trpcore.util.extensions.remove
import com.tripian.trpcore.util.extensions.replace
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 19.09.2020.
 */
class TripModelRepository @Inject constructor() {
    var trip: Trip? = null
        set(value) {
            field = value

            if (field != null) {
                tripEmitter.onNext(field!!)

                if (!field!!.plans.isNullOrEmpty()) {
                    dailyPlan = field!!.plans!![0]
                }
            }
        }

    var dailyPlan: Plan? = null
        set(value) {
            field = value

            if (field != null) {
                trip?.plans?.replace { if (it.id == field!!.id) field!! else it }

                dailyPlanEmitter.onNext(field!!)
                tripEmitter.onNext(trip!!)
            }
        }

    private var tripEmitter = PublishSubject.create<Trip>()
    private var dailyPlanEmitter = PublishSubject.create<Plan>()

    fun getTripEmitter(): Observable<Trip> {
        return tripEmitter
    }

    fun getDailyPlanEmitter(): Observable<Plan> {
        return dailyPlanEmitter
    }

    fun deleteStep(stepId: Int?) {
        trip!!.plans?.forEach {
            it.steps?.remove { step -> step.id == stepId }

            if (dailyPlan?.id == it.id) {
                dailyPlan = it
            }
        }

        tripEmitter.onNext(trip!!)
    }

    fun getStepId(poiId: String): Int {
        trip?.plans?.forEach {
            val step = it.steps?.firstOrNull { it.poi?.id == poiId }

            if (step != null) {
                return step.id
            }
        }

        return -1
    }

    fun updateDailyEvent() {
        dailyPlanEmitter.onNext(dailyPlan!!)
    }
}