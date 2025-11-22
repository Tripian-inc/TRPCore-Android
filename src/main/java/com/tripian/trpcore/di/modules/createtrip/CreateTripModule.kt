package com.tripian.trpcore.di.modules.createtrip

import com.tripian.trpcore.ui.createtrip.PageData
import dagger.Module
import dagger.Provides

/**
 * Created by semihozkoroglu on 17.05.2020.
 */
@Module(includes = [CreateTripPages::class, CreateTripVMS::class])
class CreateTripModule {

    @CreateTripScope
    @Provides
    fun providePageDate(): PageData = PageData()
}