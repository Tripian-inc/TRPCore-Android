package com.tripian.trpcore.repository

import com.tripian.one.api.companion.model.CompanionRequest
import com.tripian.one.api.companion.model.CompanionResponse
import com.tripian.one.api.companion.model.CompanionsResponse
import com.tripian.one.api.trip.model.DeleteResponse
import com.tripian.trpcore.util.extensions.remove
import com.tripian.trpcore.util.extensions.replace
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class CompanionRepository @Inject constructor(val service: Service) {

    private var companionEmitter = PublishSubject.create<CompanionsResponse>()

    private var companionResponse: CompanionsResponse? = null

    fun getUserCompanions(): Observable<CompanionsResponse> {
        return if (companionResponse == null) {
            service.getUserCompanions(null, null).map {
                companionResponse = it

                companionEmitter.onNext(companionResponse!!)

                it
            }
        } else {
            Observable.just(companionResponse)
        }
    }

    fun getUserCompanionListener(): Observable<CompanionsResponse> {
        return companionEmitter
    }

    fun addCompanion(name: String, title: String, age: Int, answers: Array<Int>? = null): Observable<CompanionResponse> {
        val request = CompanionRequest()
        request.name = name
        request.title = title
        request.age = age
        request.answers = answers

        return service.addCompanion(request).map {
            if (it.data != null) {
                companionResponse?.data?.add(0, it.data!!)

                companionEmitter.onNext(companionResponse!!)
            }

            it
        }
    }

    fun updateCompanion(
        companionId: Int,
        name: String,
        title: String,
        age: Int,
        answers: Array<Int>? = null
    ): Observable<CompanionResponse> {
        val request = CompanionRequest()
        request.name = name
        request.title = title
        request.age = age
        request.answers = answers

        return service.updateCompanion(companionId, request).map {
            if (it.data != null) {
                companionResponse?.data?.replace { c -> if (c.id == it.data!!.id) it.data!! else c }

                companionEmitter.onNext(companionResponse!!)
            }

            it
        }
    }

    fun deleteCompanion(companionId: Int): Observable<DeleteResponse> {
        return service.deleteCompanion(companionId).map {
            if (it.data != null) {
                companionResponse?.data?.remove { c -> c.id == it.data!!.recordId }

                companionEmitter.onNext(companionResponse!!)
            }

            it
        }
    }
}