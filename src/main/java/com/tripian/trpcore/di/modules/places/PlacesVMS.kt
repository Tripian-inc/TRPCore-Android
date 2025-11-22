package com.tripian.trpcore.di.modules.places

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tripian.trpcore.di.ViewModelFactory
import com.tripian.trpcore.di.ViewModelKey
import com.tripian.trpcore.ui.common.FRPoiCategoriesVM
import com.tripian.trpcore.ui.trip.places.ACPlacesVM
import com.tripian.trpcore.ui.trip.places.FRMustTryVM
import com.tripian.trpcore.ui.trip.places.FRPlacesVM
import com.tripian.trpcore.ui.trip.places.FRSearchVM
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

/**
 * Created by Semih Özköroğlu on 29.09.2019
 */
@Module
abstract class PlacesVMS {

    /**
     * PLACES
     */
    @Binds
    @IntoMap
    @ViewModelKey(ACPlacesVM::class)
    abstract fun bindACPlacesVM(repoViewModel: ACPlacesVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FRPlacesVM::class)
    abstract fun bindFRPlacesVM(repoViewModel: FRPlacesVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FRSearchVM::class)
    abstract fun bindFRSearchVM(repoViewModel: FRSearchVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FRMustTryVM::class)
    abstract fun bindFRMustTryVM(repoViewModel: FRMustTryVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FRPoiCategoriesVM::class)
    abstract fun bindFRPoiCategoriesVM(repoViewModel: FRPoiCategoriesVM): ViewModel

    /**
     * ViewModelProvider'in en altta olmasi gerekiyor
     */
    @Binds
    abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory
}