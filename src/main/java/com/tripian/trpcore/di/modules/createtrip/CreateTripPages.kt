package com.tripian.trpcore.di.modules.createtrip

import com.tripian.trpcore.ui.companion.FRCompanionSelect
import com.tripian.trpcore.ui.companion.FRCompanions
import com.tripian.trpcore.ui.companion.FRNewCompanion
import com.tripian.trpcore.ui.createtrip.FRAnswerSelectBottom
import com.tripian.trpcore.ui.createtrip.FRCitySelect
import com.tripian.trpcore.ui.createtrip.FRCreateTripDestination
import com.tripian.trpcore.ui.createtrip.FRCreateTripItineraryProfile
import com.tripian.trpcore.ui.createtrip.FRCreateTripPersonalInterests
import com.tripian.trpcore.ui.createtrip.FRCreateTripTravelerInfo
import com.tripian.trpcore.ui.createtrip.FRPropertiesSelect
import com.tripian.trpcore.ui.createtrip.FRSearchAddress
import com.tripian.trpcore.ui.createtrip.FRTimePicker
import com.tripian.trpcore.ui.createtrip.FRTripQuestion
import dagger.Module
import dagger.android.ContributesAndroidInjector

/**
 * Created by semihozkoroglu on 17.05.2020.
 */
@Module
abstract class CreateTripPages {

    @ContributesAndroidInjector
    abstract fun bindFRCitySelect(): FRCitySelect

    @ContributesAndroidInjector
    abstract fun bindFRSearchAddress(): FRSearchAddress

    @ContributesAndroidInjector
    abstract fun bindFRCreateTripDestination(): FRCreateTripDestination

    @ContributesAndroidInjector
    abstract fun bindFRCreateTripTravelerInfo(): FRCreateTripTravelerInfo

    @ContributesAndroidInjector
    abstract fun bindFRCreateTripItineraryProfile(): FRCreateTripItineraryProfile

    @ContributesAndroidInjector
    abstract fun bindFRCreateTripPersonalInterests(): FRCreateTripPersonalInterests

    @ContributesAndroidInjector
    abstract fun bindFRPropertiesSelect(): FRPropertiesSelect

    @ContributesAndroidInjector
    abstract fun bindFRTripQuestion(): FRTripQuestion

    @ContributesAndroidInjector
    abstract fun bindFRCompanionSelect(): FRCompanionSelect

    @ContributesAndroidInjector
    abstract fun bindFRTimePicker(): FRTimePicker

    @ContributesAndroidInjector
    abstract fun bindFRCompanions(): FRCompanions

    @ContributesAndroidInjector
    abstract fun bindFRNewCompanion(): FRNewCompanion

    @ContributesAndroidInjector
    abstract fun bindFRAnswerSelectBottom(): FRAnswerSelectBottom
}