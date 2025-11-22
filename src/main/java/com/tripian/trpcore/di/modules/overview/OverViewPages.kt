package com.tripian.trpcore.di.modules.overview

import com.tripian.trpcore.ui.butterfly.FRTellUs
import dagger.Module
import dagger.android.ContributesAndroidInjector

/**
 * Created by semihozkoroglu on 17.05.2020.
 */
@Module
abstract class OverViewPages {

    @ContributesAndroidInjector
    abstract fun bindFRTellUs(): FRTellUs
}