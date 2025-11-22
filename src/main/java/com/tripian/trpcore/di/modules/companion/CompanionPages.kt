package com.tripian.trpcore.di.modules.companion

import com.tripian.trpcore.ui.companion.FRCompanions
import com.tripian.trpcore.ui.companion.FRNewCompanion
import dagger.Module
import dagger.android.ContributesAndroidInjector

/**
 * Created by semihozkoroglu on 17.05.2020.
 */
@Module
abstract class CompanionPages {

    @ContributesAndroidInjector
    abstract fun bindFRCompanions(): FRCompanions

    @ContributesAndroidInjector
    abstract fun bindFRNewCompanion(): FRNewCompanion
}