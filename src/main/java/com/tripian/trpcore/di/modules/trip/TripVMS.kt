package com.tripian.trpcore.di.modules.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tripian.trpcore.di.ViewModelFactory
import com.tripian.trpcore.di.ViewModelKey
import com.tripian.trpcore.ui.common.FRPoiCategoriesVM
import com.tripian.trpcore.ui.createtrip.FRTimePickerVM
import com.tripian.trpcore.ui.trip.ACTripModeVM
import com.tripian.trpcore.ui.trip.FRChangeTimePickerVM
import com.tripian.trpcore.ui.trip.FRChangeTimeVM
import com.tripian.trpcore.ui.trip.FRDaySelectVM
import com.tripian.trpcore.ui.trip.FRItineraryVM
import com.tripian.trpcore.ui.trip.FRPoiViewVM
import com.tripian.trpcore.ui.trip.FRSearchCategoryVM
import com.tripian.trpcore.ui.trip.FRStepAlternativesVM
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

/**
 * Created by Semih Özköroğlu on 29.09.2019
 */
@Module
abstract class TripVMS {

    /**
     * TRIP MODE
     */
    @Binds
    @IntoMap
    @ViewModelKey(ACTripModeVM::class)
    abstract fun bindACTripModeVM(repoViewModel: ACTripModeVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FRDaySelectVM::class)
    abstract fun bindFRDaySelectVM(repoViewModel: FRDaySelectVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FRChangeTimeVM::class)
    abstract fun bindFRChangeTimeVM(repoViewModel: FRChangeTimeVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FRTimePickerVM::class)
    abstract fun bindFRTimePickerVM(repoViewModel: FRTimePickerVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FRPoiViewVM::class)
    abstract fun bindFRPoiViewVM(repoViewModel: FRPoiViewVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FRItineraryVM::class)
    abstract fun bindFRItineraryVM(repoViewModel: FRItineraryVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FRStepAlternativesVM::class)
    abstract fun bindFRStepAlternativesVM(repoViewModel: FRStepAlternativesVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FRSearchCategoryVM::class)
    abstract fun bindFRSearchCategoryVM(repoViewModel: FRSearchCategoryVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FRPoiCategoriesVM::class)
    abstract fun bindFRPoiCategoriesVM(repoViewModel: FRPoiCategoriesVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FRChangeTimePickerVM::class)
    abstract fun bindFRChangeTimePickerVM(repoViewModel: FRChangeTimePickerVM): ViewModel

    /**
     * ViewModelProvider'in en altta olmasi gerekiyor
     */
    @Binds
    abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory
}