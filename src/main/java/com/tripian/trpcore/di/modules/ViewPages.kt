package com.tripian.trpcore.di.modules

import com.tripian.trpcore.base.FRWarning
import com.tripian.trpcore.di.modules.timeline.TimelineModule
import com.tripian.trpcore.di.modules.timeline.TimelineScope
import com.tripian.trpcore.ui.common.ACWebPage
import com.tripian.trpcore.ui.createtrip.ACCitySelection
import com.tripian.trpcore.ui.createtrip.ACDateSelection
import com.tripian.trpcore.ui.createtrip.ACMyTrips
import com.tripian.trpcore.ui.onboarding.OnboardingBottomSheet
import com.tripian.trpcore.ui.splash.ACSplash
import com.tripian.trpcore.ui.timeline.ACTimeline
import com.tripian.trpcore.ui.timeline.activity.ACActivityListing
import com.tripian.trpcore.ui.timeline.activity.ActivityTimeSelectionBottomSheet
import com.tripian.trpcore.ui.timeline.addplan.ACStartingPointSelection
import com.tripian.trpcore.ui.timeline.addplan.AddPlanContainerBottomSheet
import com.tripian.trpcore.ui.timeline.poi.ACPOISelection
import com.tripian.trpcore.ui.timeline.poidetail.ACPOIDetail
import com.tripian.trpcore.ui.timeline.poilisting.ACPOIListing
import com.tripian.trpcore.ui.timeline.savedplans.ACSavedPlans
import dagger.Module
import dagger.android.AndroidInjectionModule
import dagger.android.ContributesAndroidInjector

@Module(includes = [AndroidInjectionModule::class])
abstract class ViewPages {

    /**
     * COMMON
     */
    @ContributesAndroidInjector
    abstract fun bindFRWarningDialog(): FRWarning

    @ContributesAndroidInjector
    abstract fun bindACWebPage(): ACWebPage

    @ContributesAndroidInjector
    abstract fun bindACSplash(): ACSplash

    /**
     * CREATE TRIP (no-reservations flow)
     */
    @ContributesAndroidInjector
    abstract fun bindACCitySelection(): ACCitySelection

    @ContributesAndroidInjector
    abstract fun bindACDateSelection(): ACDateSelection

    @ContributesAndroidInjector
    abstract fun bindACMyTrips(): ACMyTrips

    /**
     * ONBOARDING
     */
    @ContributesAndroidInjector
    abstract fun bindOnboardingBottomSheet(): OnboardingBottomSheet

    /**
     * TIMELINE
     */
    @TimelineScope
    @ContributesAndroidInjector(modules = [TimelineModule::class])
    abstract fun bindACTimeline(): ACTimeline

    @TimelineScope
    @ContributesAndroidInjector(modules = [TimelineModule::class])
    abstract fun bindAddPlanContainerBottomSheet(): AddPlanContainerBottomSheet

    @TimelineScope
    @ContributesAndroidInjector(modules = [TimelineModule::class])
    abstract fun bindACPOISelection(): ACPOISelection

    @TimelineScope
    @ContributesAndroidInjector(modules = [TimelineModule::class])
    abstract fun bindACStartingPointSelection(): ACStartingPointSelection

    @TimelineScope
    @ContributesAndroidInjector(modules = [TimelineModule::class])
    abstract fun bindACActivityListing(): ACActivityListing

    @TimelineScope
    @ContributesAndroidInjector(modules = [TimelineModule::class])
    abstract fun bindACPOIListing(): ACPOIListing

    @TimelineScope
    @ContributesAndroidInjector(modules = [TimelineModule::class])
    abstract fun bindACSavedPlans(): ACSavedPlans

    @TimelineScope
    @ContributesAndroidInjector(modules = [TimelineModule::class])
    abstract fun bindActivityTimeSelectionBottomSheet(): ActivityTimeSelectionBottomSheet

    @TimelineScope
    @ContributesAndroidInjector(modules = [TimelineModule::class])
    abstract fun bindACPOIDetail(): ACPOIDetail
}
