package com.tripian.trpcore.repository

import android.app.Application
import com.tripian.one.TRPRest
import com.tripian.one.api.bookings.model.ReservationRequest
import com.tripian.one.api.bookings.model.ReservationResponse
import com.tripian.one.api.bookings.model.ReservationsResponse
import com.tripian.one.api.cities.model.GetCitiesResponse
import com.tripian.one.api.cities.model.GetCityResponse
import com.tripian.one.api.companion.model.CompanionRequest
import com.tripian.one.api.companion.model.CompanionResponse
import com.tripian.one.api.companion.model.CompanionsResponse
import com.tripian.one.api.favorites.model.FavoriteRequest
import com.tripian.one.api.favorites.model.FavoriteResponse
import com.tripian.one.api.favorites.model.FavoritesResponse
import com.tripian.one.api.misc.model.ConfigListResponse
import com.tripian.one.api.offers.model.AddOfferRequest
import com.tripian.one.api.offers.model.OfferResponse
import com.tripian.one.api.offers.model.OffersResponse
import com.tripian.one.api.pois.model.PoiCategoriesResponse
import com.tripian.one.api.pois.model.PoiResponse
import com.tripian.one.api.pois.model.PoisResponse
import com.tripian.one.api.reactions.model.ReactionRequest
import com.tripian.one.api.reactions.model.ReactionResponse
import com.tripian.one.api.reactions.model.ReactionsResponse
import com.tripian.one.api.trip.model.AddCustomPoiStepRequest
import com.tripian.one.api.trip.model.AddStepRequest
import com.tripian.one.api.trip.model.DeleteResponse
import com.tripian.one.api.trip.model.ExportPlanRequest
import com.tripian.one.api.trip.model.ExportPlanResponse
import com.tripian.one.api.trip.model.PlanResponse
import com.tripian.one.api.trip.model.QuestionsResponse
import com.tripian.one.api.trip.model.StepAlternativesResponse
import com.tripian.one.api.trip.model.StepResponse
import com.tripian.one.api.trip.model.TripRequest
import com.tripian.one.api.trip.model.TripResponse
import com.tripian.one.api.trip.model.TripsResponse
import com.tripian.one.api.trip.model.UpdatePlanRequest
import com.tripian.one.api.trip.model.UpdateStepRequest
import com.tripian.one.api.trip.model.UpdateStepTimeRequest
import com.tripian.one.api.users.model.EmptyResponse
import com.tripian.one.api.users.model.ForgotPasswordRequest
import com.tripian.one.api.users.model.GuestLoginRequest
import com.tripian.one.api.users.model.LightLoginRequest
import com.tripian.one.api.users.model.LoginRequest
import com.tripian.one.api.users.model.LoginResponse
import com.tripian.one.api.users.model.RegisterRequest
import com.tripian.one.api.users.model.UpdateUserRequest
import com.tripian.one.api.users.model.UserResponse
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import okhttp3.ResponseBody
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class ServiceWrapper @Inject constructor(val app: Application, val tone: TRPRest) : Service {

    override fun login(request: LoginRequest): Observable<LoginResponse> {
        return PublishSubject.create {
            tone.login(request, success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun socialLogin(): Observable<EmptyResponse> {
        return PublishSubject.create {
            tone.socialLogin(success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun guestLogin(request: GuestLoginRequest): Observable<LoginResponse> {
        return PublishSubject.create {
            tone.guestLogin(request, success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun lightLogin(request: LightLoginRequest): Observable<LoginResponse> {
        return PublishSubject.create {
            tone.lightLogin(request, success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun register(request: RegisterRequest): Observable<LoginResponse> {
        return PublishSubject.create {
            tone.register(request, success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun logout(): Observable<EmptyResponse> {
        return PublishSubject.create {
            tone.logout(success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun deleteUser(): Observable<EmptyResponse> {
        return PublishSubject.create {
            tone.deleteUser(success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun sendMail(request: ForgotPasswordRequest): Observable<EmptyResponse> {
        return PublishSubject.create {
            tone.sendMail(request, success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun resetPassword(request: ForgotPasswordRequest): Observable<EmptyResponse> {
        return PublishSubject.create {
            tone.resetPassword(request, success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun updateUser(request: UpdateUserRequest): Observable<UserResponse> {
        return PublishSubject.create {
            tone.updateUser(request, success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun getUser(): Observable<UserResponse> {
        return PublishSubject.create {
            tone.getUser(success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun fetchPlan(planId: Int): Observable<PlanResponse> {
        return PublishSubject.create {
            tone.plan(planId, success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun exportPlan(request: ExportPlanRequest): Observable<ExportPlanResponse> {
        return PublishSubject.create {
            tone.exportPlan(request, success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun updatePlan(planId: Int, request: UpdatePlanRequest): Observable<PlanResponse> {
        return PublishSubject.create {
            tone.updatePlan(planId, request, success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun getUserTrip(
        from: String?,
        to: String?,
        limit: Int,
        page: Int?
    ): Observable<TripsResponse> {
        return PublishSubject.create {
            tone.trips(from, to, page, limit, success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun fetchTrip(tripHash: String): Observable<TripResponse> {
        return PublishSubject.create {
            tone.trip(tripHash, success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun createTrip(request: TripRequest): Observable<TripResponse> {
        return PublishSubject.create {
            tone.createTrip(request, success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun updateTrip(tripHash: String, request: TripRequest): Observable<TripResponse> {
        return PublishSubject.create {
            tone.updateTrip(tripHash, request, success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun deleteTrip(tripHash: String): Observable<DeleteResponse> {
        return PublishSubject.create {
            tone.deleteTrip(tripHash, success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun getUserCompanions(limit: Int?, page: Int?): Observable<CompanionsResponse> {
        return PublishSubject.create {
            tone.companions(page, limit, success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun addCompanion(request: CompanionRequest): Observable<CompanionResponse> {
        return PublishSubject.create {
            tone.addCompanion(request, success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun updateCompanion(
        companionId: Int,
        request: CompanionRequest
    ): Observable<CompanionResponse> {
        return PublishSubject.create {
            tone.updateCompanion(companionId, request, success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun deleteCompanion(companionId: Int): Observable<DeleteResponse> {
        return PublishSubject.create {
            tone.deleteCompanion(companionId, success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun getCities(search: String?, limit: Int, page: Int?): Observable<GetCitiesResponse> {
        return PublishSubject.create {
            tone.cities(
                autoPagination = true,
                search,
                countryCode = null,
                page,
                limit,
                success = { res ->
                    if (!it.isDisposed) {
                        it.onNext(res)
                    }
                },
                error = { thr ->
                    if (!it.isDisposed) {
                        it.onError(thr ?: Throwable("Unexpected error code -1"))
                    }
                })
        }
    }

    override fun getCity(cityId: Int): Observable<GetCityResponse> {
        return PublishSubject.create {
            tone.city(cityId, success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun getPoi(
        poiIds: Array<out String>?,
        limit: Int?,
        page: Int?,
        coordinate: Array<out String>?,
        boundary: String?,
        distance: Double?,
        categoryId: Int?,
        categoryIds: Array<out Int>?,
        nextUrl: String?,
        search: String?,
        cityId: Int?,
        mustTryIds: Int?,
        isAutoPagination: Boolean,
        sort: String?,
        order: String?,
        price: String?
    ): Observable<PoisResponse> {
        return PublishSubject.create {
            tone.getPoi(
                isAutoPagination,
                cityId = cityId,
                search = search,
                coordinate = coordinate,
                poiIds = poiIds,
                mustTryIds = mustTryIds,
                categoryIds = categoryIds,
                distance = distance,
                boundary = boundary,
                sort = sort,
                order = order,
                price = price,
                rating = null,
                page = page,
                limit = limit, success = { res ->
                    if (!it.isDisposed) {
                        it.onNext(res)
                    }
                }, error = { thr ->
                    if (!it.isDisposed) {
                        it.onError(thr ?: Throwable("Unexpected error code -1"))
                    }
                })
        }
    }

    override fun getPoiInfo(poiId: String): Observable<PoiResponse> {
        return PublishSubject.create {
            tone.getPoiDetail(poiId, success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun addReaction(request: ReactionRequest): Observable<ReactionResponse> {
        return PublishSubject.create {
            tone.addReaction(request, success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun deleteReaction(reactionId: Int): Observable<DeleteResponse> {
        return PublishSubject.create {
            tone.deleteReaction(reactionId, success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun getUserReactions(tripHash: String): Observable<ReactionsResponse> {
        return PublishSubject.create {
            tone.reactions(tripHash = tripHash, success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun updateReaction(
        reactionId: Int,
        request: ReactionRequest
    ): Observable<ReactionResponse> {
        return PublishSubject.create {
            tone.updateReaction(reactionId, request, success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun getUserFavorites(
        cityId: Int,
        limit: Int?,
        page: Int?
    ): Observable<FavoritesResponse> {
        return PublishSubject.create {
            tone.favorites(cityId = cityId, limit = limit, page = page, success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun addUserFavorites(request: FavoriteRequest): Observable<FavoriteResponse> {
        return PublishSubject.create {
            tone.addFavorite(request, success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun deleteUserFavorites(favoriteId: Int): Observable<DeleteResponse> {
        return PublishSubject.create {
            tone.deleteFavorite(favoriteId, success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun getStepAlternatives(
        tripHash: String?,
        planId: Int?,
        stepId: Int?
    ): Observable<StepAlternativesResponse> {
        return PublishSubject.create {
            tone.stepAlternatives(planId!!, stepId!!, tripHash!!, success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun deleteStep(stepId: Int): Observable<DeleteResponse> {
        return PublishSubject.create {
            tone.deleteStep(stepId, success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun addStep(request: AddStepRequest): Observable<StepResponse> {
        return PublishSubject.create {
            tone.addStep(request, success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun addCustomPoiStep(request: AddCustomPoiStepRequest): Observable<StepResponse> {
        return PublishSubject.create {
            tone.addCustomPoiStep(request, success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun updateStep(stepId: Int, request: UpdateStepRequest): Observable<StepResponse> {
        return PublishSubject.create {
            tone.updateStep(stepId, request, success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun updateStepTime(stepId: Int, request: UpdateStepTimeRequest): Observable<StepResponse> {
        return PublishSubject.create {
            tone.updateStepTime(stepId, request, success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun getUserReservation(cityId: String): Observable<ReservationsResponse> {
        return PublishSubject.create {
            tone.bookings(cityId = Integer.parseInt(cityId), success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun deleteUserReservation(reservationId: Int): Observable<DeleteResponse> {
        return PublishSubject.create {
            tone.deleteBookings(reservationId, success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun saveUserReservation(request: ReservationRequest): Observable<ReservationResponse> {
        return PublishSubject.create {
            tone.addBookings(request, success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun getQuestions(
        cityId: Int?,
        category: String,
        languageCode: String?
    ): Observable<QuestionsResponse> {
        return PublishSubject.create {
            tone.questions(cityId, category, languageCode, success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }


    override fun getOffers(
        dateFrom: String?,
        dateTo: String?,
        poiIds: String?,
        typeId: String?,
        boundary: String?,
        excludeOptIn: Int?
    ): Observable<OffersResponse> {
        return PublishSubject.create {
            tone.getOffers(
                dateFrom,
                dateTo,
                poiIds,
                typeId,
                boundary,
                excludeOptIn,
                success = { res ->
                    if (!it.isDisposed) {
                        it.onNext(res)
                    }
                },
                error = { thr ->
                    if (!it.isDisposed) {
                        it.onError(thr ?: Throwable("Unexpected error code -1"))
                    }
                })
        }
    }

    override fun deleteUserOffer(offerId: Int): Observable<DeleteResponse> {
        return PublishSubject.create {
            tone.deleteOffer(offerId, success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun addUserOffer(offerId: Int, request: AddOfferRequest): Observable<OfferResponse> {
        return PublishSubject.create {
            tone.addOffer(offerId, request, success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun getMyOffers(
        dateFrom: String?,
        dateTo: String?
    ): Observable<PoisResponse> {
        return PublishSubject.create {
            tone.getMyOffers(dateFrom, dateTo, success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun getPoisWithOffer(
        dateFrom: String?,
        dateTo: String?,
        boundary: String?
    ): Observable<PoisResponse> {
        return PublishSubject.create {
            tone.getPoisWithOffer(dateFrom, dateTo, boundary, success = { res ->
                if (!it.isDisposed) {
                    it.onNext(res)
                }
            }, error = { thr ->
                if (!it.isDisposed) {
                    it.onError(thr ?: Throwable("Unexpected error code -1"))
                }
            })
        }
    }

    override fun setLanguage(lang: String) {
        tone.setLanguage(lang)
    }

    override fun getLanguage(): String {
        return tone.getLanguage()
    }

    override fun getLanguageValues(): Observable<ResponseBody> {
        return PublishSubject.create {
            tone.getLanguageValues(
                success = { res ->
                    if (!it.isDisposed) {
                        it.onNext(res)
                    }
                }, error = { thr ->
                    if (!it.isDisposed) {
                        it.onError(thr ?: Throwable("Unexpected error code -1"))
                    }
                }
            )
        }
    }

    override fun getConfigList(): Observable<ConfigListResponse> {
        return PublishSubject.create {
            tone.getConfigList(
                success = { res ->
                    if (!it.isDisposed) {
                        it.onNext(res)
                    }
                }, error = { thr ->
                    if (!it.isDisposed) {
                        it.onError(thr ?: Throwable("Unexpected error code -1"))
                    }
                }
            )
        }
    }

    override fun getPoiCategories(): Observable<PoiCategoriesResponse> {
        return PublishSubject.create {
            tone.getPoiCategories(
                success = { res ->
                    if (!it.isDisposed) {
                        it.onNext(res)
                    }
                }, error = { thr ->
                    if (!it.isDisposed) {
                        it.onError(thr ?: Throwable("Unexpected error code -1"))
                    }
                }
            )
        }
    }
}