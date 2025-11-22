package com.tripian.trpcore.domain

import android.text.TextUtils
import com.tripian.one.api.cities.model.City
import com.tripian.one.api.companion.model.Companion
import com.tripian.one.api.trip.model.Accommodation
import com.tripian.one.api.trip.model.Trip
import com.tripian.one.api.trip.model.TripRequest
import com.tripian.one.api.trip.model.TripResponse
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.TripModelRepository
import com.tripian.trpcore.repository.TripRepository
import com.tripian.trpcore.repository.UserReactionRepository
import com.tripian.trpcore.util.extensions.checkEquality
import com.tripian.trpcore.util.extensions.formatDateString
import java.util.Arrays
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class UpdateTrip @Inject constructor(val repository: TripRepository, val tripModelRepository: TripModelRepository, val userReactionRepository: UserReactionRepository) : BaseUseCase<TripResponse, UpdateTrip.Params>() {

    class Params(
        var trip: Trip? = null,
        var city: City? = null,
        var place: Accommodation? = null,
        var adult: Int,
        var child: Int,
        var arrivalDate: Long,
        var departureDate: Long,
        var arrivalTime: Long,
        var departureTime: Long,
        var pace: String? = null,
        var companions: List<Companion>? = null,
        var answers: List<Int>? = null,
//        var doNotGenerate: Int = 1
    )

    override fun on(params: Params?) {
        addObservable {
            val builder = getBuilder(params!!)
            builder.apply {
                doNotGenerate = doNotGenerate(params)
            }

            repository.updateTrip(params.trip!!.tripHash!!, builder)
        }
    }

    private fun getBuilder(params: Params): TripRequest {
        val answers = ArrayList<Int>()

        if (params.answers != null) {
            answers.addAll(params.answers!!)
        }

        tripianUserRepository.user?.answers?.forEach { u ->
            if (!answers.contains(u)) {
                answers.add(u)
            }
        }

        return TripRequest(
            params.city!!.id,
            formatDateString(params.arrivalDate, params.arrivalTime),
            formatDateString(params.departureDate, params.departureTime),
            params.adult, params.child, params.place, answers, params.companions?.map { c -> c.id!! }, theme = null,
        )
    }

    fun doNotGenerate(params: Params): Int {
        val tripParam = getBuilder(params)

        var isDoNotGenerate = 1

        val tripProfile = params.trip!!.tripProfile

        if (!TextUtils.equals(tripProfile?.arrivalDatetime, tripParam.arrivalDatetime)) {
            isDoNotGenerate = 0
        }

        if (!TextUtils.equals(tripProfile?.departureDatetime, tripParam.departureDatetime)) {
            isDoNotGenerate = 0
        }

        if (tripProfile?.numberOfAdults != tripParam.numberOfAdults) {
            isDoNotGenerate = 0
        }

        if (tripProfile?.numberOfChildren != tripParam.numberOfChildren) {
            isDoNotGenerate = 0
        }

        var companionArrayPre = arrayOfNulls<Int>(0)
        var companionArrayAfter = arrayOfNulls<Int>(0)

        if (tripProfile?.companionIds != null && tripProfile.companionIds!!.isNotEmpty()) {
            companionArrayPre = tripProfile.companionIds!!.toTypedArray()
            Arrays.sort(companionArrayPre) { o1, o2 -> o1!!.compareTo(o2!!) }
        }

        if (tripParam.companionIds != null && tripParam.companionIds!!.isNotEmpty()) {
            companionArrayAfter = tripParam.companionIds!!.toTypedArray()
            Arrays.sort(companionArrayAfter) { o1, o2 -> o1!!.compareTo(o2!!) }
        }

        if (!checkEquality(companionArrayPre, companionArrayAfter)) {
            isDoNotGenerate = 0
        }

        var tripAnswersArrayPre = arrayOfNulls<Int>(0)
        var tripAnswersArrayAfter = arrayOfNulls<Int>(0)

        if (tripProfile!!.answers != null) {
            tripAnswersArrayPre = tripProfile.answers!!.toTypedArray()
            Arrays.sort(tripAnswersArrayPre) { o1, o2 -> o1!!.compareTo(o2!!) }
        }

        if (tripParam.answers != null && tripParam.answers!!.isNotEmpty()) {
            tripAnswersArrayAfter = tripParam.answers!!.toTypedArray()
            Arrays.sort(tripAnswersArrayAfter) { o1, o2 -> o1!!.compareTo(o2!!) }
        }

        if (!checkEquality(tripAnswersArrayPre, tripAnswersArrayAfter)) {
            isDoNotGenerate = 0
        }

        return isDoNotGenerate
    }

    override fun onSendSuccess(t: TripResponse) {
        // TODO: shared object olayi cozulecek
        tripModelRepository.trip = null
        tripModelRepository.dailyPlan = null
        userReactionRepository.reactions = null

        super.onSendSuccess(t)
    }
}