package com.tripian.trpcore.di.modules

import android.app.Application
import com.google.gson.Gson
import com.tripian.trpcore.util.Preferences
import com.tripian.trpcore.repository.MiscRepository
import com.tripian.trpcore.repository.PoiRepository
import com.tripian.trpcore.repository.ServiceWrapper
import com.tripian.trpcore.repository.TripRepository
import com.tripian.trpcore.repository.TimelineRepository
import com.tripian.trpcore.repository.TourRepository
import com.tripian.trpcore.repository.TripianUserRepository
import com.tripian.one.TRPRest
import com.tripian.trpcore.util.Strings
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class RepositoryModule {

    @Provides
    @Singleton
    fun providesTripianUserRepository(
        app: Application,
        pref: Preferences,
        gson: Gson,
        strings: Strings
    ): TripianUserRepository {
        return TripianUserRepository(app, pref, gson, strings)
    }

    @Provides
    @Singleton
    fun providesTripRepository(service: ServiceWrapper, preferences: Preferences): TripRepository {
        return TripRepository(service, preferences)
    }

    @Provides
    @Singleton
    fun providesPoiRepository(service: ServiceWrapper): PoiRepository {
        return PoiRepository(service)
    }

    @Provides
    @Singleton
    fun providesMiscRepository(
        app: Application,
        pref: Preferences
    ): MiscRepository {
        return MiscRepository(app = app, preferences = pref)
    }

    @Provides
    @Singleton
    fun providesTimelineRepository(trpRest: TRPRest): TimelineRepository {
        return TimelineRepository(trpRest)
    }

    @Provides
    @Singleton
    fun providesTourRepository(trpRest: TRPRest): TourRepository {
        return TourRepository(trpRest)
    }
}
