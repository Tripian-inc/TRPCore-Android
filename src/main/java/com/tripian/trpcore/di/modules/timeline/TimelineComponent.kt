package com.tripian.trpcore.di.modules.timeline

import com.tripian.trpcore.di.ViewModelFactory
import dagger.Subcomponent

/**
 * Timeline graph for the Compose entry point. The View-based flow gets the
 * same bindings through `@ContributesAndroidInjector`, but Compose screens
 * have no Activity to inject, so they resolve their ViewModels from this
 * subcomponent instead.
 */
@TimelineScope
@Subcomponent(modules = [TimelineModule::class])
interface TimelineComponent {

    fun viewModelFactory(): ViewModelFactory
}
