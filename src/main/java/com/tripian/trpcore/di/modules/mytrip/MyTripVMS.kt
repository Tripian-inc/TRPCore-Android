package com.tripian.trpcore.di.modules.mytrip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tripian.trpcore.di.ViewModelFactory
import com.tripian.trpcore.di.ViewModelKey
import com.tripian.trpcore.ui.mytrip.*
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

/**
 * Created by Semih Özköroğlu on 29.09.2019
 */
@Module
abstract class MyTripVMS {

    /**
     * MY TRIP
     */
    @Binds
    @IntoMap
    @ViewModelKey(ACMyTripVM::class)
    abstract fun bindACMyTripVM(repoViewModel: ACMyTripVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FRProfileVM::class)
    abstract fun bindFRProfileVM(repoViewModel: FRProfileVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FRUpComingsVM::class)
    abstract fun bindFRUpComingsVM(repoViewModel: FRUpComingsVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FRPastTripVM::class)
    abstract fun bindFRPastTripVM(repoViewModel: FRPastTripVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FRMoreSelectionVM::class)
    abstract fun bindFRMoreSelectionVM(repoViewModel: FRMoreSelectionVM): ViewModel

    /**
     * ViewModelProvider'in en altta olmasi gerekiyor
     */
    @Binds
    abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory
}