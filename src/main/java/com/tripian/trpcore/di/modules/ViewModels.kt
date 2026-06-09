package com.tripian.trpcore.di.modules

import androidx.lifecycle.ViewModel
import com.tripian.trpcore.base.FRWarningDialogVM
import com.tripian.trpcore.di.ViewModelKey
import com.tripian.trpcore.ui.common.ACWebPageVM
import com.tripian.trpcore.ui.onboarding.OnboardingVM
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

    /**
     * ONBOARDING
     */
    @Binds
    @IntoMap
    @ViewModelKey(OnboardingVM::class)
    abstract fun bindOnboardingVM(repoViewModel: OnboardingVM): ViewModel
}
