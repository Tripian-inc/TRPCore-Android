package com.tripian.trpcore.di.modules.trip

import dagger.Module

/**
 * Created by semihozkoroglu on 17.05.2020.
 */
@Module(includes = [TripPages::class, TripVMS::class])
class TripModule {

//    @TripScope
//    @Provides
//    fun providePageDate(): PageData = PageData()
}