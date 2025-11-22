package com.tripian.trpcore.ui.trip

import android.os.Bundle
import android.text.TextUtils
import androidx.lifecycle.MutableLiveData
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.GetDailyPlan
import com.tripian.trpcore.domain.UpdateDailyPlan
import com.tripian.trpcore.ui.createtrip.FRTimePicker
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.event.EventConstants
import com.tripian.trpcore.util.event.EventMessage
import com.tripian.trpcore.util.extensions.hideLoading
import com.tripian.trpcore.util.extensions.hours
import com.tripian.trpcore.util.extensions.navigateToFragment
import com.tripian.trpcore.util.extensions.showLoading
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import javax.inject.Inject

class FRChangeTimeVM @Inject constructor(
    private val getDailyPlan: GetDailyPlan,
    private val updateDailyPlan: UpdateDailyPlan
) : BaseViewModel(getDailyPlan, updateDailyPlan) {

    var onSetStartTimeListener = MutableLiveData<String>()
    var onSetEndTimeListener = MutableLiveData<String>()
    var onDismissListener = MutableLiveData<Unit>()

    private var startTimePos = -1
    private var startTime = ""
    private var endTimePos = -1
    private var endTime = ""

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

        getDailyPlan.on(success = { plan ->
            startTime = plan.startTime ?: ""
            endTime = plan.endTime ?: ""

            startTimePos = hours.indexOfFirst { it == startTime }
            endTimePos = hours.indexOfFirst { it == endTime }

            onSetStartTimeListener.postValue(plan.startTime)
            onSetEndTimeListener.postValue(plan.endTime)
        })
    }

    fun onClickedStartTime() {
        navigateToFragment(
            FRTimePicker.newInstance(
                "startTime",
                getLanguageForKey(LanguageConst.SELECT_ARRIVAL_HOUR),
                "09:00"
            )
        )

//        navigateToFragment(FRTimePicker.newInstance("startTime"))
    }

    fun onClickedEndTime() {
//        navigateToFragment(FRTimePicker.newInstance("endTime"))
        navigateToFragment(
            FRTimePicker.newInstance(
                "endTime",
                getLanguageForKey(LanguageConst.SELECT_DEPARTURE_HOUR),
                "21:00"
            )
        )
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public fun onTimeSelected(item: EventMessage<Pair<String, Int>>) {
        if (item.tag == EventConstants.TimePicker) {
            if (TextUtils.equals(item.value!!.first, "startTime")) {
                startTimePos = item.value!!.second

                if (startTimePos >= endTimePos) {
                    if (startTimePos == hours.size - 1) {
                        // 23:00 ise endTime 23:59 olmali

                        endTimePos = startTimePos

                        endTime = "23:59"
                    } else {
                        endTimePos = startTimePos + 1

                        endTime = hours[endTimePos]
                    }

                    onSetEndTimeListener.postValue(endTime)
                }

                startTime = hours[startTimePos]
                onSetStartTimeListener.postValue(startTime)
            } else if (TextUtils.equals(item.value!!.first, "endTime")) {
                endTimePos = item.value!!.second

                if (endTimePos <= startTimePos) {
                    if (endTimePos == 0) {
                        startTimePos = 0

                        endTimePos = startTimePos + 1
                    } else {
                        startTimePos = endTimePos - 1
                    }

                    startTime = hours[startTimePos]
                    onSetStartTimeListener.postValue(startTime)
                }

                endTime = hours[endTimePos]
                onSetEndTimeListener.postValue(endTime)
            }
        }
    }

    fun onClickedOk() {
        showLoading()

        updateDailyPlan.on(UpdateDailyPlan.Params(startTime, endTime), success = {
            onDismissListener.postValue(Unit)

            hideLoading()
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
