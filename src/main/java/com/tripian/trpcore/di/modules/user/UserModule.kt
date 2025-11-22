package com.tripian.trpcore.di.modules.user

import dagger.Module

/**
 * Created by semihozkoroglu on 17.05.2020.
 */
@Module(includes = [UserPages::class, UserVMS::class])
class UserModule {
}