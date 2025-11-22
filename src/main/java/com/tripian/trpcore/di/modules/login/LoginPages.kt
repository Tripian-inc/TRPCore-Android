package com.tripian.trpcore.di.modules.login

import com.tripian.trpcore.ui.login.FRForgotPassword
import com.tripian.trpcore.ui.login.FRLogin
import com.tripian.trpcore.ui.login.FRRegister
import dagger.Module
import dagger.android.ContributesAndroidInjector

/**
 * Created by semihozkoroglu on 17.05.2020.
 */
@Module
abstract class LoginPages {

    @ContributesAndroidInjector
    abstract fun bindFRLogin(): FRLogin

    @ContributesAndroidInjector
    abstract fun bindFRRegister(): FRRegister

    @ContributesAndroidInjector
    abstract fun bindFRForgotPassword(): FRForgotPassword
}