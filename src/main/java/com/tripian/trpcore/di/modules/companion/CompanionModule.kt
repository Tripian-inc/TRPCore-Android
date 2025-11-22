package com.tripian.trpcore.di.modules.companion

import dagger.Module

/**
 * Created by semihozkoroglu on 17.05.2020.
 */
@Module(includes = [CompanionPages::class, CompanionVMS::class])
class CompanionModule {

//    @CompanionScope
//    @Provides
//    fun providePageDate(): PageData = PageData()
}