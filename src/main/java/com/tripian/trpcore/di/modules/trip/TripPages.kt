package com.tripian.trpcore.di.modules.trip

import com.tripian.trpcore.ui.common.FRPoiCategories
import com.tripian.trpcore.ui.createtrip.FRTimePicker
import com.tripian.trpcore.ui.trip.*
import dagger.Module
import dagger.android.ContributesAndroidInjector

/**
 * Created by semihozkoroglu on 17.05.2020.
 */
@Module
abstract class TripPages {

    @ContributesAndroidInjector
    abstract fun bindFRDaySelect(): FRDaySelect

    @ContributesAndroidInjector
    abstract fun bindFRChangeTime(): FRChangeTime

    @ContributesAndroidInjector
    abstract fun bindFRTimePicker(): FRTimePicker

    @ContributesAndroidInjector
    abstract fun bindFRPoiView(): FRPoiView

    @ContributesAndroidInjector
    abstract fun bindFRItinerary(): FRItinerary

    @ContributesAndroidInjector
    abstract fun bindFRStepAlternatives(): FRStepAlternatives

    @ContributesAndroidInjector
    abstract fun bindFRSearchCategory(): FRSearchCategory

    @ContributesAndroidInjector
    abstract fun bindFRPoiCategories(): FRPoiCategories

    @ContributesAndroidInjector
    abstract fun bindFRChangeTimePicker(): FRChangeTimePicker
}