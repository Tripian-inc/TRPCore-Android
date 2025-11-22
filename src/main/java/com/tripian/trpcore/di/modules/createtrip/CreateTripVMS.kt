package com.tripian.trpcore.di.modules.createtrip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tripian.trpcore.di.ViewModelFactory
import com.tripian.trpcore.di.ViewModelKey
import com.tripian.trpcore.ui.companion.FRCompanionSelectVM
import com.tripian.trpcore.ui.companion.FRCompanionsVM
import com.tripian.trpcore.ui.companion.FRNewCompanionVM
import com.tripian.trpcore.ui.createtrip.ACCreateTripVM
import com.tripian.trpcore.ui.createtrip.FRAnswerSelectBottomVM
import com.tripian.trpcore.ui.createtrip.FRCitySelectVM
import com.tripian.trpcore.ui.createtrip.FRCompanionCountVM
import com.tripian.trpcore.ui.createtrip.FRCreateTripDestinationVM
import com.tripian.trpcore.ui.createtrip.FRCreateTripItineraryProfileVM
import com.tripian.trpcore.ui.createtrip.FRCreateTripPersonalInterestsVM
import com.tripian.trpcore.ui.createtrip.FRCreateTripTravelerInfoVM
import com.tripian.trpcore.ui.createtrip.FRPropertiesSelectVM
import com.tripian.trpcore.ui.createtrip.FRSearchAddressVM
import com.tripian.trpcore.ui.createtrip.FRTimePickerVM
import com.tripian.trpcore.ui.createtrip.FRTripQuestionVM
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

/**
 * Created by Semih Özköroğlu on 29.09.2019
 */
@Module
abstract class CreateTripVMS {

    /**
     * CREATE TRIP
     */
    @Binds
    @IntoMap
    @ViewModelKey(ACCreateTripVM::class)
    abstract fun bindACCreateTripVM(repoViewModel: ACCreateTripVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FRCitySelectVM::class)
    abstract fun bindFRCitySelectVM(repoViewModel: FRCitySelectVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FRSearchAddressVM::class)
    abstract fun bindFRSearchAddressVM(repoViewModel: FRSearchAddressVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FRCreateTripDestinationVM::class)
    abstract fun bindFRCreateTripDestinationVM(repoViewModel: FRCreateTripDestinationVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FRCreateTripTravelerInfoVM::class)
    abstract fun bindFRCreateTripTravelerInfoVM(repoViewModel: FRCreateTripTravelerInfoVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FRCreateTripItineraryProfileVM::class)
    abstract fun bindFRCreateTripItineraryProfileVM(repoViewModel: FRCreateTripItineraryProfileVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FRCreateTripPersonalInterestsVM::class)
    abstract fun bindFRCreateTripPersonalInterestsVM(repoViewModel: FRCreateTripPersonalInterestsVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FRPropertiesSelectVM::class)
    abstract fun bindFRPropertiesSelectVM(repoViewModel: FRPropertiesSelectVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FRTripQuestionVM::class)
    abstract fun bindFRTripQuestionVM(repoViewModel: FRTripQuestionVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FRCompanionSelectVM::class)
    abstract fun bindFRCompanionSelectVM(repoViewModel: FRCompanionSelectVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FRTimePickerVM::class)
    abstract fun bindFRTimePickerVM(repoViewModel: FRTimePickerVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FRCompanionCountVM::class)
    abstract fun bindFRCompanionCountVM(repoViewModel: FRCompanionCountVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FRCompanionsVM::class)
    abstract fun bindFRCompanionsVM(repoViewModel: FRCompanionsVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FRNewCompanionVM::class)
    abstract fun bindFRNewCompanionVM(repoViewModel: FRNewCompanionVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FRAnswerSelectBottomVM::class)
    abstract fun bindFRAnswerSelectBottomVM(repoViewModel: FRAnswerSelectBottomVM): ViewModel

    /**
     * ViewModelProvider'in en altta olmasi gerekiyor
     */
    @Binds
    abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory
}