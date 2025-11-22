package com.tripian.trpcore.di

import android.app.Application
import com.tripian.trpcore.base.AppConfig
import com.tripian.trpcore.base.TRPCore
import com.tripian.trpcore.di.modules.*
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AppModule::class,
        RepositoryModule::class,
        NetworkModule::class,
        ViewPages::class,
        ViewModels::class,
        BindsModule::class
    ]
)
interface AppComponent {

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun configurations(appConfig: AppConfig): Builder

        @BindsInstance
        fun application(app: Application): Builder

        fun build(): AppComponent
    }

    fun inject(provider: TRPCore)
}