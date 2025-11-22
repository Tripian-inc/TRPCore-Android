package com.tripian.trpcore.di.modules.companion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tripian.trpcore.di.ViewModelFactory
import com.tripian.trpcore.di.ViewModelKey
import com.tripian.trpcore.ui.companion.ACManageCompanionVM
import com.tripian.trpcore.ui.companion.FRCompanionsVM
import com.tripian.trpcore.ui.companion.FRNewCompanionVM
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

/**
 * Created by Semih Özköroğlu on 29.09.2019
 */
@Module
abstract class CompanionVMS {

    /**
     * MANAGE COMPANION
     */
    @Binds
    @IntoMap
    @ViewModelKey(ACManageCompanionVM::class)
    abstract fun bindACManageCompanionVM(repoViewModel: ACManageCompanionVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FRCompanionsVM::class)
    abstract fun bindFRCompanionsVM(repoViewModel: FRCompanionsVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FRNewCompanionVM::class)
    abstract fun bindFRNewCompanionVM(repoViewModel: FRNewCompanionVM): ViewModel

    /**
     * ViewModelProvider'in en altta olmasi gerekiyor
     */
    @Binds
    abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory
}