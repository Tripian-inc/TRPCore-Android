package com.tripian.trpcore.di.modules

import androidx.lifecycle.ViewModelProvider
import com.tripian.trpcore.di.ViewModelFactory
import dagger.Binds
import dagger.Module
import dagger.android.AndroidInjectionModule

/**
 * Created by semihozkoroglu on 15.05.2021.
 */
@Module(includes = [AndroidInjectionModule::class])
abstract class BindsModule {

    @Binds
    abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory
}
