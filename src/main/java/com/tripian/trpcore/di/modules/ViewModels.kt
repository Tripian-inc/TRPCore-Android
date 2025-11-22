package com.tripian.trpcore.di.modules

import androidx.lifecycle.ViewModel
import com.tripian.trpcore.base.FRWarningDialogVM
import com.tripian.trpcore.di.ViewModelKey
import com.tripian.trpcore.ui.common.ACWebPageVM
import com.tripian.trpcore.ui.createtrip.ACSearchAddressVM
import com.tripian.trpcore.ui.login.ACLoginVM
import com.tripian.trpcore.ui.profile.ACProfileVM
import com.tripian.trpcore.ui.profile.change_langugae.FRLanguageSelectVM
import com.tripian.trpcore.ui.splash.ACSplashVM
import com.tripian.trpcore.ui.trip.booking.ACBookingVM
import com.tripian.trpcore.ui.trip.favorite.ACFavoriteVM
import com.tripian.trpcore.ui.trip.my_offers.ACMyOffersVM
import com.tripian.trpcore.ui.trip.places.ACMustTryVM
import com.tripian.trpcore.ui.trip_detail.ACTripDetailVM
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

/**
 * Created by Semih Özköroğlu on 29.09.2019
 */
@Module
abstract class ViewModels {

    /**
     * SEARCH ADDRESS
     */
    @Binds
    @IntoMap
    @ViewModelKey(ACSearchAddressVM::class)
    abstract fun bindACSearchAddressVM(repoViewModel: ACSearchAddressVM): ViewModel

    /**
     * FAVORITE
     */
    @Binds
    @IntoMap
    @ViewModelKey(ACFavoriteVM::class)
    abstract fun bindACFavoriteVM(repoViewModel: ACFavoriteVM): ViewModel

    /**
     * BOOKING
     */
    @Binds
    @IntoMap
    @ViewModelKey(ACBookingVM::class)
    abstract fun bindACBookingVM(repoViewModel: ACBookingVM): ViewModel

    /**
     * TRIP DETAIL
     */
    @Binds
    @IntoMap
    @ViewModelKey(ACTripDetailVM::class)
    abstract fun bindACTripDetailVM(repoViewModel: ACTripDetailVM): ViewModel

    /**
     * MUST TRY
     */
    @Binds
    @IntoMap
    @ViewModelKey(ACMustTryVM::class)
    abstract fun bindACMustTryVM(repoViewModel: ACMustTryVM): ViewModel

    /**
     * COMMON
     */
    @Binds
    @IntoMap
    @ViewModelKey(FRWarningDialogVM::class)
    abstract fun bindFRWarningDialogVM(repoViewModel: FRWarningDialogVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ACWebPageVM::class)
    abstract fun bindACWebPageVM(repoViewModel: ACWebPageVM): ViewModel

    /**
     * SPLASH
     */
    @Binds
    @IntoMap
    @ViewModelKey(ACSplashVM::class)
    abstract fun bindACSplashVM(repoViewModel: ACSplashVM): ViewModel

    /**
     * LOGIN
     */
    @Binds
    @IntoMap
    @ViewModelKey(ACLoginVM::class)
    abstract fun bindACLoginVM(repoViewModel: ACLoginVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ACProfileVM::class)
    abstract fun bindACProfileVM(repoViewModel: ACProfileVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FRLanguageSelectVM::class)
    abstract fun bindFRLanguageSelectVM(repoViewModel: FRLanguageSelectVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ACMyOffersVM::class)
    abstract fun bindACMyOffersVM(repoViewModel: ACMyOffersVM): ViewModel
}