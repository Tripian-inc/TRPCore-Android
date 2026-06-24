package com.tripian.trpcore.di.modules

import androidx.lifecycle.ViewModel
import com.tripian.trpcore.base.FRWarningDialogVM
import com.tripian.trpcore.di.ViewModelKey
import com.tripian.trpcore.ui.common.ACWebPageVM
import com.tripian.trpcore.ui.onboarding.OnboardingVM
import com.tripian.trpcore.ui.createtrip.ACCitySelectionVM
import com.tripian.trpcore.ui.createtrip.ACDateSelectionVM
import com.tripian.trpcore.ui.splash.ACSplashVM
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class ViewModels {

    /**
     * COMMON
     */
    @Binds
    @IntoMap
    @ViewModelKey(FRWarningDialogVM::class)
    abstract fun bindFRWarningDialogVM(repoViewModel: FRWarningDialogVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ACWebPageVM::class)
    abstract fun bindACWebPageVM(repoViewModel: ACWebPageVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ACSplashVM::class)
    abstract fun bindACSplashVM(repoViewModel: ACSplashVM): ViewModel

    /**
     * CREATE TRIP (no-reservations flow)
     */
    @Binds
    @IntoMap
    @ViewModelKey(ACCitySelectionVM::class)
    abstract fun bindACCitySelectionVM(viewModel: ACCitySelectionVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ACDateSelectionVM::class)
    abstract fun bindACDateSelectionVM(viewModel: ACDateSelectionVM): ViewModel

    /**
     * ONBOARDING
     */
    @Binds
    @IntoMap
    @ViewModelKey(OnboardingVM::class)
    abstract fun bindOnboardingVM(repoViewModel: OnboardingVM): ViewModel
}
