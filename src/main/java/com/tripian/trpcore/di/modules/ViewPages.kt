package com.tripian.trpcore.di.modules

import com.tripian.trpcore.base.FRWarning
import com.tripian.trpcore.di.modules.butterfly.ButterFlyModule
import com.tripian.trpcore.di.modules.companion.CompanionModule
import com.tripian.trpcore.di.modules.createtrip.CreateTripModule
import com.tripian.trpcore.di.modules.createtrip.CreateTripScope
import com.tripian.trpcore.di.modules.login.LoginModule
import com.tripian.trpcore.di.modules.login.LoginScope
import com.tripian.trpcore.di.modules.mytrip.MyTripModule
import com.tripian.trpcore.di.modules.mytrip.MyTripScope
import com.tripian.trpcore.di.modules.overview.OverViewModule
import com.tripian.trpcore.di.modules.places.PlacesModule
import com.tripian.trpcore.di.modules.places.PlacesScope
import com.tripian.trpcore.di.modules.trip.TripModule
import com.tripian.trpcore.di.modules.trip.TripScope
import com.tripian.trpcore.di.modules.user.UserModule
import com.tripian.trpcore.di.modules.user.UserScope
import com.tripian.trpcore.ui.butterfly.ACButterfly
import com.tripian.trpcore.ui.common.ACWebPage
import com.tripian.trpcore.ui.companion.ACManageCompanion
import com.tripian.trpcore.ui.createtrip.ACCreateTrip
import com.tripian.trpcore.ui.createtrip.ACSearchAddress
import com.tripian.trpcore.ui.login.ACLogin
import com.tripian.trpcore.ui.mytrip.ACMyTrip
import com.tripian.trpcore.ui.overview.ACOverView
import com.tripian.trpcore.ui.profile.ACProfile
import com.tripian.trpcore.ui.profile.change_langugae.FRLanguageSelect
import com.tripian.trpcore.ui.splash.ACSplash
import com.tripian.trpcore.ui.trip.ACTripMode
import com.tripian.trpcore.ui.trip.booking.ACBooking
import com.tripian.trpcore.ui.trip.favorite.ACFavorite
import com.tripian.trpcore.ui.trip.my_offers.ACMyOffers
import com.tripian.trpcore.ui.trip.places.ACMustTry
import com.tripian.trpcore.ui.trip.places.ACPlaces
import com.tripian.trpcore.ui.trip_detail.ACTripDetail
import com.tripian.trpcore.ui.user.ACEditProfile
import dagger.Module
import dagger.android.AndroidInjectionModule
import dagger.android.ContributesAndroidInjector

/**
 * Created by Semih Özköroğlu on 22.07.
 */
@Module(includes = [AndroidInjectionModule::class])
abstract class ViewPages {

    /**
     * MY TRIP
     */
    @MyTripScope
    @ContributesAndroidInjector(modules = [MyTripModule::class])
    abstract fun bindACMyTrip(): ACMyTrip

    /**
     * CREATE TRIP
     */
    @CreateTripScope
    @ContributesAndroidInjector(modules = [CreateTripModule::class])
    abstract fun bindACCreateTrip(): ACCreateTrip

    /**
     * SEARCH ADDRESS
     */
    @ContributesAndroidInjector
    abstract fun bindACSearchAddress(): ACSearchAddress

    /**
     * MANAGE COMPANION
     */
    @ContributesAndroidInjector(modules = [CompanionModule::class])
    abstract fun bindACManageCompanion(): ACManageCompanion

    /**
     * BUTTERFLY
     */
    @ContributesAndroidInjector(modules = [ButterFlyModule::class])
    abstract fun bindACButterfly(): ACButterfly

    /**
     * OVERVIEW
     */
    @ContributesAndroidInjector(modules = [OverViewModule::class])
    abstract fun bindACOverView(): ACOverView

    /**
     * TRIP MODE
     */
    @TripScope
    @ContributesAndroidInjector(modules = [TripModule::class])
    abstract fun bindACTripMode(): ACTripMode

    /**
     * PLACES
     */
    @PlacesScope
    @ContributesAndroidInjector(modules = [PlacesModule::class])
    abstract fun bindACPlaces(): ACPlaces

    /**
     * FAVORITE
     */
    @ContributesAndroidInjector
    abstract fun bindACFavorite(): ACFavorite

    /**
     * BOOKING
     */
    @ContributesAndroidInjector
    abstract fun bindACBooking(): ACBooking

    /**
     * TRIP DETAIL
     */
    @ContributesAndroidInjector
    abstract fun bindACTripDetail(): ACTripDetail

    /**
     * USER
     */
    @UserScope
    @ContributesAndroidInjector(modules = [UserModule::class])
    abstract fun bindACEditProfile(): ACEditProfile

    /**
     * MUST TRY
     */
    @ContributesAndroidInjector
    abstract fun bindACMustTry(): ACMustTry

    /**
     * COMMON
     */
    @ContributesAndroidInjector
    abstract fun bindFRWarningDialog(): FRWarning

    @ContributesAndroidInjector
    abstract fun bindACWebPage(): ACWebPage

    /**
     * SPLASH
     */
    @ContributesAndroidInjector
    abstract fun bindACSplash(): ACSplash

    /**
     * LOGIN
     */
    @LoginScope
    @ContributesAndroidInjector(modules = [LoginModule::class])
    abstract fun bindACLogin(): ACLogin

    /**
     * PROFILE
     */
    @ContributesAndroidInjector
    abstract fun bindACProfile(): ACProfile

    @ContributesAndroidInjector
    abstract fun bindFRLanguageSelect(): FRLanguageSelect

    /**
     * MY OFFERS
     */
    @ContributesAndroidInjector
    abstract fun bindACMyOffers(): ACMyOffers
}