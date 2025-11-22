package com.tripian.trpcore.di.modules.places

import com.tripian.trpcore.ui.common.FRPoiCategories
import com.tripian.trpcore.ui.trip.places.FRMustTry
import com.tripian.trpcore.ui.trip.places.FRPlaces
import com.tripian.trpcore.ui.trip.places.FRSearch
import dagger.Module
import dagger.android.ContributesAndroidInjector

/**
 * Created by semihozkoroglu on 17.05.2020.
 */
@Module
abstract class PlacesPages {

    @ContributesAndroidInjector
    abstract fun bindFRPlaces(): FRPlaces

    @ContributesAndroidInjector
    abstract fun bindFRSearch(): FRSearch

    @ContributesAndroidInjector
    abstract fun bindFRMustTry(): FRMustTry

    @ContributesAndroidInjector
    abstract fun bindFRPoiCategories(): FRPoiCategories
}