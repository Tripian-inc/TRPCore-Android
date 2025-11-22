package com.tripian.trpcore.repository

import com.tripian.one.api.offers.model.*
import com.tripian.one.api.pois.model.Poi
import com.tripian.one.api.pois.model.PoisResponse
import com.tripian.one.api.trip.model.DeleteResponse
import com.tripian.trpcore.util.OfferType
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class OfferRepository @Inject constructor(val service: Service) {

    private lateinit var myOffers: ArrayList<Offer>
    private lateinit var myOffersPois: ArrayList<Poi>
    private lateinit var offers: ArrayList<Offer>

    private var myOfferEmitter = PublishSubject.create<ArrayList<Poi>>()

    fun getOffersWithBoundary(
        dateFrom: String,
        dateTo: String,
        boundary: String
    ): Observable<OffersResponse> {
        return service.getOffers(dateFrom = dateFrom, dateTo = dateTo, boundary = boundary)
    }

    fun getPoiOffersWithBoundary(
        dateFrom: String,
        dateTo: String,
        boundary: String
    ): Observable<PoisResponse> {
        return service.getPoisWithOffer(dateFrom = dateFrom, dateTo = dateTo, boundary = boundary)
    }

    fun getMyOffers(
        dateFrom: String? = null,
        dateTo: String? = null,
        isClear: Boolean = false
    ): Observable<ArrayList<Poi>> {
        return if (isClear || !::myOffersPois.isInitialized || myOffersPois.isEmpty()) {
            service.getMyOffers(dateFrom = dateFrom, dateTo = dateTo).map {
                if (::myOffersPois.isInitialized) {
                    myOffersPois.clear()
                } else {
                    myOffersPois = ArrayList()
                }

                it.data?.let {
                    myOffersPois.addAll(it)
                }

                myOfferEmitter.onNext(myOffersPois)
                myOffersPois
            }
        } else {
            PublishSubject.create {
                it.onNext(myOffersPois)
            }
        }
    }

//    fun getOffer(offerId: Long): Observable<OfferDetailResponse> {
//        return service.getOffer(offerId)
//    }

//    fun getOfferHistory(): Observable<OffersResponse> {
//        return service.myOffers()
//    }

    fun updateMyOffers(
        dateFrom: String? = null,
        dateTo: String? = null
    ): Observable<Unit> {
        return getMyOffers(dateFrom = dateFrom, dateTo = dateTo, isClear = true).map {
            myOfferEmitter.onNext(it)
        }
    }

    fun addOffer(id: Int, optInDate: String): Observable<OfferResponse> {
        return service.addUserOffer(
            id,
            AddOfferRequest().apply { this.optInDate = optInDate })
    }

    fun removeOffer(id: Int): Observable<DeleteResponse> {
        return service.deleteUserOffer(id)
    }

    fun getAllOffers(): ArrayList<Offer> {
        val offers: ArrayList<Offer> = arrayListOf()
        if (::myOffersPois.isInitialized) {
            myOffersPois.forEach { poi ->
                poi.offers?.let { poiOffers -> offers.addAll(poiOffers) }
            }
        }
        return offers
    }

    // eger opt in yapildiginda itemin kaldirilmasini istenirse yorumlari kaldirmalisin
//    fun updateOffers(id: Long, type: OfferType?, isOptIn: Boolean = true): Observable<Unit> {
//        return PublishSubject.create {
//            if (isOptIn) {
//                when (type) {
//                    OfferType.FOOD -> removeOffer(id, OfferType.FOOD)
//                    OfferType.GROCERY -> removeOffer(id, OfferType.GROCERY)
//                    OfferType.DRINK -> removeOffer(id, OfferType.DRINK)
//                    else -> {
//                        when {
//                            foods.any { it.id == id } -> removeOffer(id, OfferType.FOOD)
//                            grocery.any { it.id == id } -> removeOffer(id, OfferType.GROCERY)
//                            drinks.any { it.id == id } -> removeOffer(id, OfferType.DRINK)
//                        }
//                    }
//                }
//            }
//
//            it.onNext(Unit)
//        }
//    }

//    private fun removeOffer(id: Long, type: OfferType) {
//        when (type) {
//            OfferType.FOOD -> {
//                // remove comment edilir ise listede gozukmeye devam eder
//                foods.remove { it.id == id }
//
//                foodEmitter.onNext(foods)
//            }
//            OfferType.GROCERY -> {
//                // remove comment edilir ise listede gozukmeye devam eder
//                grocery.remove { it.id == id }
//
//                groceryEmitter.onNext(grocery)
//            }
//            OfferType.DRINK -> {
//                // remove comment edilir ise listede gozukmeye devam eder
//                drinks.remove { it.id == id }
//
//                drinkEmitter.onNext(drinks)
//            }
//            else -> {
//            }
//        }
//    }

//    fun getFoodListener(): Observable<ArrayList<OfferModel>> {
//        return foodEmitter
//    }
//
//    fun getDrinkListener(): Observable<ArrayList<OfferModel>> {
//        return drinkEmitter
//    }
//
//    fun getGroceryListener(): Observable<ArrayList<OfferModel>> {
//        return groceryEmitter
//    }

    fun getMyOffersListener(): Observable<ArrayList<Poi>> {
        return myOfferEmitter
    }
}