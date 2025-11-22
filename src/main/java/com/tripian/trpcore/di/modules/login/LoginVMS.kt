package com.tripian.trpcore.di.modules.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tripian.trpcore.di.ViewModelFactory
import com.tripian.trpcore.di.ViewModelKey
import com.tripian.trpcore.ui.login.FRForgotPasswordVM
import com.tripian.trpcore.ui.login.FRLoginVM
import com.tripian.trpcore.ui.login.FRRegisterVM
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

/**
 * Created by Semih Özköroğlu on 29.09.2019
 */
@Module
abstract class LoginVMS {

    /**
     * HOME
     */
    @Binds
    @IntoMap
    @ViewModelKey(FRLoginVM::class)
    abstract fun bindFRLoginVM(repoViewModel: FRLoginVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FRRegisterVM::class)
    abstract fun bindFRRegisterVM(repoViewModel: FRRegisterVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FRForgotPasswordVM::class)
    abstract fun bindFRForgotPasswordVM(repoViewModel: FRForgotPasswordVM): ViewModel

    /**
     * ViewModelProvider'in en altta olmasi gerekiyor
     */
    @Binds
    abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory
}