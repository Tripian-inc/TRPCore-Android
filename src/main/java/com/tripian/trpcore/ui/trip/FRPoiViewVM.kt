package com.tripian.trpcore.ui.trip

import android.app.Application
import android.os.Bundle
import android.text.TextUtils
import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.AddStep
import com.tripian.trpcore.domain.DeleteStep
import com.tripian.trpcore.domain.GetDailyPlan
import com.tripian.trpcore.domain.StepListener
import com.tripian.trpcore.domain.model.MapStep
import com.tripian.trpcore.ui.trip_detail.ACTripDetail
import com.tripian.trpcore.ui.trip_detail.TripDetail
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.event.EventConstants
import com.tripian.trpcore.util.event.EventMessage
import com.tripian.trpcore.util.extensions.hideLoading
import com.tripian.trpcore.util.extensions.showLoading
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import javax.inject.Inject

class FRPoiViewVM @Inject constructor(
    val app: Application, val addStep: AddStep, val deleteStep: DeleteStep,
    val stepListener: StepListener, val getDailyPlan: GetDailyPlan
) : BaseViewModel(addStep, deleteStep, getDailyPlan, stepListener) {

    var onSetMapStepListener = MutableLiveData<MapStep>()
    var onShowAddListener = MutableLiveData<Boolean>()
    var onDismissListener = MutableLiveData<Unit>()

    lateinit var mapStep: MapStep

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

        mapStep = arguments!!.getSerializable("mapStep") as MapStep

        update()

        stepListener.on(success = {
            if (it.stepId == mapStep.stepId || it.poiId == mapStep.poiId) {
                mapStep.group = it.group

                // Add step durumunda step id degisiyor
                mapStep.stepId = it.stepId

                update()
            }
        })
    }

    fun onClickedItem() {
        startActivity(ACTripDetail::class, bundleOf(Pair("detail", TripDetail().apply {
            poiId = mapStep.poiId
            stepId = mapStep.stepId
        })))
    }

    private fun update() {
        onSetMapStepListener.postValue(mapStep)
        onShowAddListener.postValue(TextUtils.equals(mapStep.group, "alternative"))
    }

    fun onClickedAction() {
        showLoading()

        if (TextUtils.equals(mapStep.group, "alternative")) {
            // Add state
            addStep.on(AddStep.Params(poiId = mapStep.poiId), success = {
                hideLoading()
            }, error = {
                hideLoading()

                if (it.type == AlertType.DIALOG) {
                    showDialog(contentText = it.errorDesc)
                } else {
                    showAlert(AlertType.ERROR, it.errorDesc)
                }
            })
        } else {
            // Remove state
            deleteStep.on(DeleteStep.Params(mapStep.stepId), success = {
                hideLoading()
                onDismissListener.postValue(Unit)
            }, error = {
                hideLoading()

                if (it.type == AlertType.DIALOG) {
                    showDialog(contentText = it.errorDesc)
                } else {
                    showAlert(AlertType.ERROR, it.errorDesc)
                }
            })
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public fun onLocationRouteEvent(item: EventMessage<MapStep>) {
        if (item.tag == EventConstants.LocationRedirect) {
            onDismissListener.postValue(Unit)
        }
    }
}
