package com.tripian.trpcore.di.modules.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tripian.trpcore.di.ViewModelFactory
import com.tripian.trpcore.di.ViewModelKey
import com.tripian.trpcore.ui.timeline.ACTimelineVM
import com.tripian.trpcore.ui.timeline.activity.ACActivityListingVM
import com.tripian.trpcore.ui.timeline.activity.ActivityTimeSelectionVM
import com.tripian.trpcore.ui.timeline.addplan.ACStartingPointSelectionVM
import com.tripian.trpcore.ui.timeline.addplan.AddPlanContainerVM
import com.tripian.trpcore.ui.timeline.poi.ACPOISelectionVM
import com.tripian.trpcore.ui.timeline.poilisting.ACPOIListingVM
import com.tripian.trpcore.ui.timeline.poidetail.ACPOIDetailVM
import com.tripian.trpcore.ui.timeline.savedplans.ACSavedPlansVM
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

/**
 * TimelineVMS
 * Dagger bindings for ViewModels in Timeline module
 */
@Module
abstract class TimelineVMS {

    @Binds
    @IntoMap
    @ViewModelKey(ACTimelineVM::class)
    abstract fun bindACTimelineVM(viewModel: ACTimelineVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AddPlanContainerVM::class)
    abstract fun bindAddPlanContainerVM(viewModel: AddPlanContainerVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ACPOISelectionVM::class)
    abstract fun bindACPOISelectionVM(viewModel: ACPOISelectionVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ACStartingPointSelectionVM::class)
    abstract fun bindACStartingPointSelectionVM(viewModel: ACStartingPointSelectionVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ACActivityListingVM::class)
    abstract fun bindACActivityListingVM(viewModel: ACActivityListingVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ActivityTimeSelectionVM::class)
    abstract fun bindActivityTimeSelectionVM(viewModel: ActivityTimeSelectionVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ACPOIListingVM::class)
    abstract fun bindACPOIListingVM(viewModel: ACPOIListingVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ACSavedPlansVM::class)
    abstract fun bindACSavedPlansVM(viewModel: ACSavedPlansVM): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ACPOIDetailVM::class)
    abstract fun bindACPOIDetailVM(viewModel: ACPOIDetailVM): ViewModel

    @Binds
    abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory
}
