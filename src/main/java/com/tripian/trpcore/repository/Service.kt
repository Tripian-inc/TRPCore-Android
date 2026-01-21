package com.tripian.trpcore.repository

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
import okhttp3.ResponseBody

interface Service {

    fun login(request: LoginRequest): Observable<LoginResponse>

    fun guestLogin(request: GuestLoginRequest): Observable<LoginResponse>

    fun lightLogin(request: LightLoginRequest): Observable<LoginResponse>

    fun sendMail(request: ForgotPasswordRequest): Observable<EmptyResponse>

    fun resetPassword(request: ForgotPasswordRequest): Observable<EmptyResponse>

    fun socialLogin(): Observable<EmptyResponse>

    fun logout(): Observable<EmptyResponse>

    fun deleteUser(): Observable<EmptyResponse>

    fun register(request: RegisterRequest): Observable<LoginResponse>

    fun updateUser(request: UpdateUserRequest): Observable<UserResponse>

    fun getUser(): Observable<UserResponse>

    fun fetchPlan(planId: Int): Observable<PlanResponse>

    fun exportPlan(request: ExportPlanRequest): Observable<ExportPlanResponse>

    fun updatePlan(planId: Int, request: UpdatePlanRequest): Observable<PlanResponse>

    fun getUserTrip(from: String?, to: String?, limit: Int, page: Int?): Observable<TripsResponse>

    fun fetchTrip(tripHash: String): Observable<TripResponse>

    fun createTrip(request: TripRequest): Observable<TripResponse>

    fun updateTrip(tripHash: String, request: TripRequest): Observable<TripResponse>

    fun deleteTrip(tripHash: String): Observable<DeleteResponse>

    fun getUserCompanions(limit: Int?, page: Int?): Observable<CompanionsResponse>

    fun addCompanion(request: CompanionRequest): Observable<CompanionResponse>

    fun updateCompanion(companionId: Int, request: CompanionRequest): Observable<CompanionResponse>

    fun deleteCompanion(companionId: Int): Observable<DeleteResponse>

    fun getCities(search: String?, limit: Int, page: Int?): Observable<GetCitiesResponse>

    fun getCity(cityId: Int): Observable<GetCityResponse>

    fun getPoi(
        poiIds: Array<out String>? = null,
        limit: Int? = null,
        page: Int? = null,
        coordinate: Array<out String>? = null,
        boundary: String? = null,
        distance: Double? = null,
        categoryId: Int? = null,
        categoryIds: Array<out Int>? = null,
        nextUrl: String? = null,
        search: String? = null,
        cityId: Int? = null,
        mustTryIds: Int? = null,
        isAutoPagination: Boolean = false,
        sort: String? = null,
        order: String? = null,
        price: String? = null
    ): Observable<PoisResponse>

    fun getPoiInfo(poiId: String): Observable<PoiResponse>

    fun addReaction(request: ReactionRequest): Observable<ReactionResponse>

    fun deleteReaction(reactionId: Int): Observable<DeleteResponse>

    fun getUserReactions(tripHash: String): Observable<ReactionsResponse>

    fun updateReaction(reactionId: Int, request: ReactionRequest): Observable<ReactionResponse>

    fun getUserFavorites(cityId: Int, limit: Int?, page: Int?): Observable<FavoritesResponse>

    fun addUserFavorites(request: FavoriteRequest): Observable<FavoriteResponse>

    fun deleteUserFavorites(favoriteId: Int): Observable<DeleteResponse>

    fun getStepAlternatives(
        tripHash: String? = null,
        planId: Int?,
        stepId: Int? = null
    ): Observable<StepAlternativesResponse>

    fun deleteStep(stepId: Int): Observable<DeleteResponse>

    fun addStep(request: AddStepRequest): Observable<StepResponse>

    fun addCustomPoiStep(request: AddCustomPoiStepRequest): Observable<StepResponse>

    fun updateStep(stepId: Int, request: UpdateStepRequest): Observable<StepResponse>

    fun updateStepTime(stepId: Int, request: UpdateStepTimeRequest): Observable<StepResponse>

    fun getUserReservation(cityId: String): Observable<ReservationsResponse>

    fun deleteUserReservation(reservationId: Int): Observable<DeleteResponse>

    fun saveUserReservation(request: ReservationRequest): Observable<ReservationResponse>

    fun getQuestions(
        cityId: Int?,
        category: String,
        languageCode: String?
    ): Observable<QuestionsResponse>


    fun getOffers(
        dateFrom: String? = null,
        dateTo: String? = null,
        poiIds: String? = null,
        typeId: String? = null,
        boundary: String? = null,
        excludeOptIn: Int? = null
    ): Observable<OffersResponse>

    fun addUserOffer(offerId: Int, request: AddOfferRequest): Observable<OfferResponse>

    fun deleteUserOffer(offerId: Int): Observable<DeleteResponse>

    fun getMyOffers(
        dateFrom: String? = null,
        dateTo: String? = null
    ): Observable<PoisResponse>

    fun getPoisWithOffer(
        dateFrom: String? = null,
        dateTo: String? = null,
        boundary: String? = null
    ): Observable<PoisResponse>

    fun setLanguage(lang: String)

    fun getLanguage() : String

    fun getLanguageValues(): Observable<ResponseBody>

    fun getConfigList(): Observable<ConfigListResponse>

    fun getPoiCategories(): Observable<PoiCategoriesResponse>
}