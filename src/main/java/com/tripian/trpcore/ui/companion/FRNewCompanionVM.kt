package com.tripian.trpcore.ui.companion

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import com.tripian.one.api.companion.model.Companion
import com.tripian.one.api.trip.model.Question
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.CreateCompanion
import com.tripian.trpcore.domain.DeleteCompanion
import com.tripian.trpcore.domain.Questions
import com.tripian.trpcore.domain.UpdateCompanion
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.QuestionType
import com.tripian.trpcore.util.dialog.DGActionListener
import com.tripian.trpcore.util.event.EventConstants
import com.tripian.trpcore.util.event.EventMessage
import com.tripian.trpcore.util.extensions.goBack
import com.tripian.trpcore.util.extensions.hideLoading
import com.tripian.trpcore.util.extensions.showLoading
import javax.inject.Inject

class FRNewCompanionVM @Inject constructor(
    val questions: Questions,
    val createCompanion: CreateCompanion,
    val updateCompanion: UpdateCompanion,
    val deleteCompanion: DeleteCompanion
) :
    BaseViewModel(questions, createCompanion, updateCompanion, deleteCompanion) {

    var onSetNameListener = MutableLiveData<String>()
    var onSetAgeListener = MutableLiveData<String>()
    var onShowDeleteListener = MutableLiveData<Unit>()

    var onSetQuestionsListener = MutableLiveData<Pair<List<Question>, List<Int>?>>()
    var onShowQuestionLoading = MutableLiveData<Boolean>()

    var onSetTitleListener = MutableLiveData<Int>()

    var title = ""
    lateinit var titles: ArrayList<String>

    var companion: Companion? = null

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

        titles = ArrayList()
        // 0
        titles.add(getLanguageForKey(LanguageConst.FAMILY_MEMBER))
        // 1
        titles.add(getLanguageForKey(LanguageConst.WORK_COLLEAGUE))
        // 2
        titles.add(getLanguageForKey(LanguageConst.FRIEND))

        if (arguments != null && arguments!!.containsKey("companion")) {
            companion = arguments!!.getSerializable("companion") as Companion

            title = companion!!.title ?: ""

            onSetNameListener.postValue(companion!!.name)
            onSetAgeListener.postValue(companion!!.age.toString())
            onShowDeleteListener.postValue(Unit)

            val titlePos = titles.indexOf(title)
            if (titlePos != -1) {
                onSetTitleListener.postValue(titlePos)
            }
        }

        onShowQuestionLoading.postValue(true)

        questions.on(Questions.Params(QuestionType.COMPANION), success = {
            onSetQuestionsListener.postValue(Pair(it.data!!, companion?.answers))

            onShowQuestionLoading.postValue(false)
        }, error = {
            onShowQuestionLoading.postValue(false)

            if (it.type == AlertType.DIALOG) {
                showDialog(contentText = it.errorDesc)
            } else {
                showAlert(AlertType.ERROR, it.errorDesc)
            }
        })
    }

    fun onTitleSelected(pos: Int, isSelected: Boolean) {
        title = if (isSelected) titles[pos] else ""
    }

    fun onClickedCreate(name: String, age: String, items: Array<Int>?) {
        showLoading()

        if (companion == null) {
            createCompanion.on(CreateCompanion.Params(name, title, age, items), success = {
                hideLoading()
                it.data?.let { companion ->
                    publishCreateCompanionEvent(companion)
                }
                goBack()
            }, error = {
                hideLoading()

//                if (it.type == AlertType.DIALOG) {
                    showDialog(contentText = it.errorDesc)
//                } else {
//                    showAlert(AlertType.ERROR, it.errorDesc)
//                }
            })
        } else {
            updateCompanion.on(
                UpdateCompanion.Params(companion!!.id!!, name, title, age, items),
                success = {
                    hideLoading()

                    goBack()
                },
                error = {
                    hideLoading()

//                    if (it.type == AlertType.DIALOG) {
                        showDialog(contentText = it.errorDesc)
//                    } else {
//                        showAlert(AlertType.ERROR, it.errorDesc)
//                    }
                })
        }
    }

    private fun publishCreateCompanionEvent(companion: Companion) {
        eventBus.post(EventMessage(EventConstants.CompanionCreate, companion))
    }

    fun onClickedDelete() {
        showDialog(
            contentText = getLanguageForKey(LanguageConst.DOU_YOU_CONFIRM),
            positiveBtn = getLanguageForKey(LanguageConst.DELETE),
            negativeBtn = getLanguageForKey(LanguageConst.CANCEL),
            positive = object : DGActionListener {
                override fun onClicked(o: Any?) {
                    showLoading()

                    deleteCompanion.on(DeleteCompanion.Params(companion!!.id!!), success = {
                        hideLoading()

                        goBack()
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
        )
    }
}