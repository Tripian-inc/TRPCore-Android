package com.tripian.trpcore.di.modules.butterfly

import com.tripian.trpcore.ui.butterfly.FRTellUs
import dagger.Module
import dagger.android.ContributesAndroidInjector

/**
 * Created by semihozkoroglu on 17.05.2020.
 */
@Module
abstract class ButterFlyPages {

    @ContributesAndroidInjector
    abstract fun bindFRTellUs(): FRTellUs
}