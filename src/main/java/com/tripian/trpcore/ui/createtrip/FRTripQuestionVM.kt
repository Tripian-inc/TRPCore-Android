package com.tripian.trpcore.ui.createtrip

import android.os.Bundle
import android.text.TextUtils
import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import com.tripian.one.api.trip.model.Question
import com.tripian.trpcore.R
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.CreateTrip
import com.tripian.trpcore.domain.GetPace
import com.tripian.trpcore.domain.Questions
import com.tripian.trpcore.domain.UpdateTrip
import com.tripian.trpcore.domain.model.Pace
import com.tripian.trpcore.ui.overview.ACOverView
import com.tripian.trpcore.ui.trip.ACTripMode
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.QuestionType
import com.tripian.trpcore.util.dialog.DGActionListener
import com.tripian.trpcore.util.event.EventConstants
import com.tripian.trpcore.util.event.EventMessage
import com.tripian.trpcore.util.extensions.hideLoading
import com.tripian.trpcore.util.extensions.showLoading
import javax.inject.Inject

class FRTripQuestionVM @Inject constructor(val questions: Questions, val getPace: GetPace, val createTrip: CreateTrip, val updateTrip: UpdateTrip) :
    BaseViewModel(questions, getPace, createTrip, updateTrip) {

    @Inject
    lateinit var pageData: PageData

    var onSetQuestionsListener = MutableLiveData<Pair<List<Question>, List<Int>?>>()
    var onSetPaceListener = MutableLiveData<Pair<List<Pace>, String>>()

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

        showLoading()

        questions.on(Questions.Params(QuestionType.TRIP), success = {
            onSetQuestionsListener.postValue(Pair(it.data!!, pageData.answers))

            getPace.on(success = { paceRes ->
                if (TextUtils.isEmpty(pageData.pace)) {
                    pageData.pace = paceRes.data!![1].paceSymbol
                }

//                onSetPaceListener.postValue(Pair(paceRes.data!!, pageData.pace!!))

                hideLoading()
            })
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
        showAlert(AlertType.ERROR, strings.getString(R.string.please_select_field))
    }

    fun onPaceSelected(pace: Pace) {
        pageData.pace = pace.paceSymbol
    }

    fun onClickedNext() {
        if (pageData.trip == null) {
            showLoading()

            createTrip.on(
                CreateTrip.Params(
                pageData.city,
                pageData.place,
                pageData.adult,
                pageData.child,
                pageData.arrivalDate,
                pageData.departureDate,
                pageData.arrivalTime,
                pageData.departureTime,
                pageData.pace,
                pageData.companions,
                pageData.answers
            ), success = {
                hideLoading()

                eventBus.post(EventMessage(EventConstants.UpdateTrip, Unit))

//                startActivity(ACButterfly::class, bundleOf(Pair("trip", it.data)))
                startActivity(ACOverView::class, bundleOf(Pair("trip", it.data)))
                finishActivity()
            }, error = {
                hideLoading()

                if (it.type == AlertType.DIALOG) {
                    showDialog(contentText = it.errorDesc)
                } else {
                    showAlert(AlertType.ERROR, it.errorDesc)
                }
            })
        } else {
            val params = UpdateTrip.Params(
                pageData.trip,
                pageData.city,
                pageData.place,
                pageData.adult,
                pageData.child,
                pageData.arrivalDate,
                pageData.departureDate,
                pageData.arrivalTime,
                pageData.departureTime,
                pageData.pace,
                pageData.companions,
                pageData.answers
            )

            if (updateTrip.doNotGenerate(params) == 0) {
                showDialog(
                    title = getLanguageForKey(LanguageConst.WARNING),
                    contentText = getLanguageForKey(LanguageConst.TRIP_WILL_UPDATE),
                    negativeBtn = getLanguageForKey(LanguageConst.CANCEL),
                    positiveBtn = getLanguageForKey(LanguageConst.CONTINUE),
                    positive = object : DGActionListener {
                        override fun onClicked(o: Any?) {
                            sendUpdateRequest(params)
                        }
                    })
            } else {
                sendUpdateRequest(params)
            }
        }
    }

    private fun sendUpdateRequest(params: UpdateTrip.Params) {
        showLoading()

        updateTrip.on(params, success = {
            hideLoading()

            eventBus.post(EventMessage(EventConstants.UpdateTrip, Unit))

            startActivity(ACTripMode::class, bundleOf(Pair("trip", it.data)))
            finishActivity()
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