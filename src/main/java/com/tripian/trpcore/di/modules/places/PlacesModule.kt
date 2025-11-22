package com.tripian.trpcore.di.modules.places

import com.tripian.trpcore.ui.trip.places.PageData
import dagger.Module
import dagger.Provides

/**
 * Created by semihozkoroglu on 17.05.2020.
 */
@Module(includes = [PlacesPages::class, PlacesVMS::class])
class PlacesModule {

    @PlacesScope
    @Provides
    fun providePageDate(): PageData = PageData()
}