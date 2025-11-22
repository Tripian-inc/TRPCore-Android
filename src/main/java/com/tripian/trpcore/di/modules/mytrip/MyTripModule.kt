package com.tripian.trpcore.di.modules.mytrip

import dagger.Module

/**
 * Created by semihozkoroglu on 17.05.2020.
 */
@Module(includes = [MyTripPages::class, MyTripVMS::class])
class MyTripModule {

//    @ProviderScope
//    @Provides
//    fun providePageDate(): PageData = PageData()
}