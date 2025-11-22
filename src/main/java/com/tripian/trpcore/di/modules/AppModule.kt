package com.tripian.trpcore.di.modules

import android.app.Application
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.tripian.trpcore.util.Preferences
import com.tripian.trpcore.repository.authorization.AwsAuthorization
import com.tripian.trpcore.util.Strings
import dagger.Module
import dagger.Provides
import org.greenrobot.eventbus.EventBus
import javax.inject.Singleton

/**
 * Created by Semih Özköroğlu on 26.09.2019
 */
@Module
class AppModule {

    @Provides
    @Singleton
    fun providesGson(): Gson {
        return GsonBuilder().create()
    }

    @Provides
    @Singleton
    fun providesStrings(app: Application): Strings {
        return Strings(app.applicationContext)
    }

    @Provides
    @Singleton
    fun preferences(app: Application): Preferences {
        return Preferences(app.applicationContext)
    }

    @Provides
    @Singleton
    fun providesEventBus(): EventBus {
        return EventBus.getDefault()
    }

    @Provides
    @Singleton
    fun providesAwsAuthorization(app: Application, preferences: Preferences): AwsAuthorization {
        return AwsAuthorization(app, preferences)
    }
}