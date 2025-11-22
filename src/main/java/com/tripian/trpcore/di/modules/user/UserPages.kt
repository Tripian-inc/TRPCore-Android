package com.tripian.trpcore.di.modules.user

import com.tripian.trpcore.ui.user.FRChangePassword
import com.tripian.trpcore.ui.user.FREditProfile
import dagger.Module
import dagger.android.ContributesAndroidInjector

/**
 * Created by semihozkoroglu on 17.05.2020.
 */
@Module
abstract class UserPages {

    @ContributesAndroidInjector
    abstract fun bindFREditProfile(): FREditProfile

    @ContributesAndroidInjector
    abstract fun bindFRChangePassword(): FRChangePassword
}