package com.tripian.trpcore.ui.trip

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.ChangeDailyPlan
import com.tripian.trpcore.domain.GetTripPlans
import com.tripian.trpcore.util.extensions.formatDate
import com.tripian.one.api.trip.model.Plan
import com.tripian.trpcore.util.LanguageConst
import javax.inject.Inject

class FRDaySelectVM @Inject constructor(val getTripPlans: GetTripPlans, val changeDailyPlan: ChangeDailyPlan) : BaseViewModel(getTripPlans, changeDailyPlan) {

    var onSetDaysListener = MutableLiveData<List<DayItem>>()
    var onDismissListener = MutableLiveData<Unit>()

    private var plans: List<Plan>? = null

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

        getTripPlans.on(success = {
            plans = it.second

            val selectedDayIndex = it.first

            var dayCount = 0

            onSetDaysListener.postValue(
                it.second.flatMap {
                    ArrayList<DayItem>().apply { add(DayItem("${getLanguageForKey(LanguageConst.DAY)} ${dayCount + 1} - ${it.date?.formatDate()}", selectedDayIndex == dayCount++)) }
                }
            )
        })
    }

    fun onClickedItem(position: Int) {
        changeDailyPlan.on(ChangeDailyPlan.Params(plans!![position].id), success = {
            onDismissListener.postValue(Unit)
        })
    }
}
