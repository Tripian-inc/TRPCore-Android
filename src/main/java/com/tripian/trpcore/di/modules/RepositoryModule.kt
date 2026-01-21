package com.tripian.trpcore.di.modules

//import com.tripian.trpcore.repository.authorization.AwsAuthorization
import android.app.Application
import com.google.gson.Gson
import com.tripian.trpcore.util.Preferences
import com.tripian.trpcore.repository.CompanionRepository
import com.tripian.trpcore.repository.FavoriteRepository
import com.tripian.trpcore.repository.MiscRepository
import com.tripian.trpcore.repository.OfferRepository
import com.tripian.trpcore.repository.PlanRepository
import com.tripian.trpcore.repository.PoiRepository
import com.tripian.trpcore.repository.QuestionRepository
import com.tripian.trpcore.repository.Service
import com.tripian.trpcore.repository.StepRepository
import com.tripian.trpcore.repository.TripModelRepository
import com.tripian.trpcore.repository.TripRepository
import com.tripian.trpcore.repository.TimelineRepository
import com.tripian.trpcore.repository.TourRepository
import com.tripian.trpcore.repository.TripianUserRepository
import com.tripian.trpcore.repository.UserReactionRepository
import com.tripian.one.TRPRest
import com.tripian.trpcore.repository.authorization.AwsAuthorization
import com.tripian.trpcore.util.Strings
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by Semih Özköroğlu on 26.09.2019
 */
@Module
class RepositoryModule {

    @Provides
    @Singleton
    fun providesTripianUserRepository(
        app: Application,
        service: Service,
        pref: Preferences,
        gson: Gson,
        strings: Strings,
        awsAuthorization: AwsAuthorization
    ): TripianUserRepository {
        return TripianUserRepository(app, service, pref, gson, strings, awsAuthorization)
    }

    @Provides
    @Singleton
    fun providesTripRepository(service: Service): TripRepository {
        return TripRepository(service)
    }

    @Provides
    @Singleton
    fun providesUserReactionRepository(service: Service): UserReactionRepository {
        return UserReactionRepository(service)
    }

    @Provides
    @Singleton
    fun providesCompanionRepository(service: Service): CompanionRepository {
        return CompanionRepository(service)
    }

    @Provides
    @Singleton
    fun providesQuestionRepository(service: Service): QuestionRepository {
        return QuestionRepository(service)
    }

    @Provides
    @Singleton
    fun providesTripModelRepository(): TripModelRepository {
        return TripModelRepository()
    }

    @Provides
    @Singleton
    fun providesPoiRepository(service: Service): PoiRepository {
        return PoiRepository(service)
    }

    @Provides
    @Singleton
    fun providesPlanRepository(service: Service): PlanRepository {
        return PlanRepository(service)
    }

    @Provides
    @Singleton
    fun providesStepRepository(service: Service): StepRepository {
        return StepRepository(service)
    }

    @Provides
    @Singleton
    fun providesFavoriteRepository(service: Service): FavoriteRepository {
        return FavoriteRepository(service)
    }

    @Provides
    @Singleton
    fun providesOfferRepository(service: Service): OfferRepository {
        return OfferRepository(service)
    }

    @Provides
    @Singleton
    fun providesMiscRepository(
        app: Application,
        service: Service,
        pref: Preferences
    ): MiscRepository {
        return MiscRepository(app = app, service = service, preferences = pref)
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