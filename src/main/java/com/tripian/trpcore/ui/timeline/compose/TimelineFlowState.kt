package com.tripian.trpcore.ui.timeline.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import com.tripian.trpcore.domain.model.timeline.AddPlanData
import com.tripian.trpcore.ui.timeline.addplan.AddPlanContainerVM

/**
 * UI state of the Timeline screen that must outlive its composition: the
 * AddPlan sheet and the plan waiting for a POI pick stay put while a nested
 * sub-screen or a host screen sits on top of the Timeline.
 */
internal class TimelineFlowState : ViewModel() {

    val addPlanStore = ViewModelStore()
    var addPlanVisible by mutableStateOf(false)
    var addPlanViewModel by mutableStateOf<AddPlanContainerVM?>(null)
    var addPlanError by mutableStateOf<String?>(null)
    var pendingAddPlanData: AddPlanData? = null

    fun resetAddPlan() {
        addPlanStore.clear()
        addPlanViewModel = null
        addPlanError = null
        addPlanVisible = false
    }

    override fun onCleared() {
        addPlanStore.clear()
    }
}
