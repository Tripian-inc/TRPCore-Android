package com.tripian.trpcore.di.modules.mytrip

import com.tripian.trpcore.ui.mytrip.FRMoreSelection
import com.tripian.trpcore.ui.mytrip.FRPastTrip
import com.tripian.trpcore.ui.mytrip.FRProfile
import com.tripian.trpcore.ui.mytrip.FRUpComingsTrip
import dagger.Module
import dagger.android.ContributesAndroidInjector

/**
 * Created by semihozkoroglu on 17.05.2020.
 */
@Module
abstract class MyTripPages {

    @ContributesAndroidInjector
    abstract fun bindFRProfile(): FRProfile

    @ContributesAndroidInjector
    abstract fun bindFRUpComingsTrip(): FRUpComingsTrip

    @ContributesAndroidInjector
    abstract fun bindFRPastTrip(): FRPastTrip

    @ContributesAndroidInjector
    abstract fun bindFRMoreSelection(): FRMoreSelection

}