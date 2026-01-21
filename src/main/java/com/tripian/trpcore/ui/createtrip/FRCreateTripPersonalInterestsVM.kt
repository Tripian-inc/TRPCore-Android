package com.tripian.trpcore.ui.createtrip

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import com.tripian.one.api.trip.model.Answer
import com.tripian.one.api.trip.model.Question
import com.tripian.trpcore.R
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.TripQuestions
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.CreateTripSteps
import com.tripian.trpcore.util.QuestionType
import com.tripian.trpcore.util.event.EventConstants
import com.tripian.trpcore.util.event.EventMessage
import com.tripian.trpcore.util.extensions.hideLoading
import com.tripian.trpcore.util.extensions.navigateToFragment
import com.tripian.trpcore.util.extensions.showLoading
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import javax.inject.Inject

class FRCreateTripPersonalInterestsVM @Inject constructor(val questions: TripQuestions) :
    BaseViewModel(questions) {

    @Inject
    lateinit var pageData: PageData

    var onSetQuestionsListener = MutableLiveData<Pair<List<Question>, List<Int>?>>()
    var onSpinnerAnswerSelected = MutableLiveData<List<Int>>()

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

        showLoading()

        questions.on(TripQuestions.Params(CreateTripSteps.PERSONAL_INTERESTS), success = {
            hideLoading()
            onSetQuestionsListener.postValue(Pair(it.data!!, pageData.answers))

        }, error = {
            hideLoading()

            if (it.type == AlertType.DIALOG) {
                showDialog(contentText = it.errorDesc)
            } else {
                showAlert(AlertType.ERROR, it.errorDesc)
            }
        })
    }

    fun onAnswerUpdated(items: Array<Int>) {
        pageData.answers = items.toList()
    }

    fun onShowError() {
        showAlert(AlertType.ERROR, getLanguageForKey(LanguageConst.PLEASE_SELECT_FIELD))
    }

}