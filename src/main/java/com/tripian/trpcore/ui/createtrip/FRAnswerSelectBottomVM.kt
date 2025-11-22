package com.tripian.trpcore.ui.createtrip

import android.os.Bundle
import android.text.TextUtils
import androidx.lifecycle.MutableLiveData
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.GetCities
import com.tripian.trpcore.domain.SearchCity
import com.tripian.trpcore.domain.model.CitySelect
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.extensions.hideLoading
import com.tripian.trpcore.util.extensions.navigateToFragment
import com.tripian.trpcore.util.extensions.showLoading
import com.tripian.trpcore.util.fragment.AnimationType
import com.tripian.one.api.cities.model.City
import com.tripian.one.api.companion.model.Companion
import com.tripian.one.api.trip.model.Answer
import com.tripian.one.api.trip.model.Question
import com.tripian.trpcore.domain.ChangeDailyPlan
import com.tripian.trpcore.util.event.EventConstants
import com.tripian.trpcore.util.event.EventMessage
import javax.inject.Inject

class FRAnswerSelectBottomVM @Inject constructor() : BaseViewModel() {


    var onSetAnswersListener = MutableLiveData<List<Answer>>()
    var onSetTitleListener = MutableLiveData<String>()
    var onDismissListener = MutableLiveData<Unit>()

    private var question: Question? = null

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

        question = if (arguments != null && arguments!!.containsKey("question")) {
            arguments?.getSerializable("question") as Question
        } else {
            null
        }

        question?.let {
            onSetTitleListener.postValue(it.name)
            onSetAnswersListener.postValue(it.answerList)
        }

    }

    fun onClickedItem(answer: Answer) {
        eventBus.post(EventMessage(EventConstants.SpinnerAnswerSelect, Pair(question, answer)))
        onDismissListener.postValue(Unit)
    }
}
