package com.tripian.trpcore.ui.trip

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.GetDirectionRoutes
import com.tripian.trpcore.domain.GetMapStep
import com.tripian.trpcore.domain.GetMapStepAlternatives
import com.tripian.trpcore.domain.model.MapStep
import com.tripian.trpcore.ui.trip_detail.ACTripDetail
import com.tripian.trpcore.ui.trip_detail.TripDetail
import javax.inject.Inject

class FRStepAlternativesVM @Inject constructor(
    val getMapStep: GetMapStep, val getMapStepAlternatives: GetMapStepAlternatives,
    val getDirectionRoutes: GetDirectionRoutes
) : BaseViewModel(getMapStep, getMapStepAlternatives, getDirectionRoutes) {

    var onSetStepListener = MutableLiveData<List<MapStep>>()
    var onShowProgressListener = MutableLiveData<Unit>()
    var onHideProgressListener = MutableLiveData<Unit>()

    private lateinit var mapStep: MapStep

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

        mapStep = arguments!!.getSerializable("step") as MapStep

        setAdapter()

        getMapStep.on(success = { steps ->
            steps.forEach {
                if (it.stepId == mapStep.stepId) {
                    mapStep = it

                    setAdapter()

                    return@forEach
                }
            }
        })
    }

    fun onClickedAlternatives(step: MapStep) {
        // Itinerary ile ayni adapter kullanildigi icin var, kullanilmiyor.
    }

    private fun setAdapter() {
        onShowProgressListener.postValue(Unit)

        getMapStepAlternatives.on(GetMapStepAlternatives.Params(mapStep.alternatives), success = { steps ->
            onSetStepListener.postValue(steps)
            onHideProgressListener.postValue(Unit)
        })
    }

    fun onClickedItem(clickedStep: MapStep) {
        startActivity(ACTripDetail::class, bundleOf(Pair("detail", TripDetail().apply {
            poiId = clickedStep.poiId
            stepId = mapStep.stepId
            order = mapStep.order
        })))
    }
}
