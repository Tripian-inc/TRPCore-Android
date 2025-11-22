package com.tripian.trpcore.di.modules.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tripian.trpcore.di.ViewModelFactory
import com.tripian.trpcore.di.ViewModelKey
import com.tripian.trpcore.ui.user.ACEditProfileVM
import com.tripian.trpcore.ui.user.FRChangePasswordVM
import com.tripian.trpcore.ui.user.FREditProfileVM
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

/**
 * Created by Semih Özköroğlu on 29.09.2019
 */
@Module
abstract class UserVMS {

    /**
     * USER
     */
    @Binds
    @IntoMap
    @ViewModelKey(ACEditProfileVM::class)
    abstract fun bindACEditProfile(repoViewModel: ACEditProfileVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FREditProfileVM::class)
    abstract fun bindFREditProfileVM(repoViewModel: FREditProfileVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FRChangePasswordVM::class)
    abstract fun bindFRChangePasswordVM(repoViewModel: FRChangePasswordVM): ViewModel

    /**
     * ViewModelProvider'in en altta olmasi gerekiyor
     */
    @Binds
    abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory
}