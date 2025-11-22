package com.tripian.trpcore.di.modules.butterfly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tripian.trpcore.di.ViewModelFactory
import com.tripian.trpcore.di.ViewModelKey
import com.tripian.trpcore.ui.butterfly.ACButterflyVM
import com.tripian.trpcore.ui.butterfly.FRTellUsVM
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

/**
 * Created by Semih Özköroğlu on 29.09.2019
 */
@Module
abstract class ButterFlyVMS {

    /**
     * BUTTERFLY
     */
    @Binds
    @IntoMap
    @ViewModelKey(ACButterflyVM::class)
    abstract fun bindACButterflyVM(repoViewModel: ACButterflyVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FRTellUsVM::class)
    abstract fun bindFRTellUsVM(repoViewModel: FRTellUsVM): ViewModel

    /**
     * ViewModelProvider'in en altta olmasi gerekiyor
     */
    @Binds
    abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory
}