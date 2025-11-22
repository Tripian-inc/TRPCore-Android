package com.tripian.trpcore.ui.user

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import com.tripian.one.api.trip.model.Question
import com.tripian.one.api.users.model.User
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.DeleteUser
import com.tripian.trpcore.domain.GetUser
import com.tripian.trpcore.domain.Questions
import com.tripian.trpcore.domain.UpdateUser
import com.tripian.trpcore.ui.splash.ACSplash
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.QuestionType
import com.tripian.trpcore.util.dialog.DGActionListener
import com.tripian.trpcore.util.extensions.goBack
import com.tripian.trpcore.util.extensions.hideLoading
import com.tripian.trpcore.util.extensions.navigateToFragment
import com.tripian.trpcore.util.extensions.showLoading
import java.util.Calendar
import javax.inject.Inject

class FREditProfileVM @Inject constructor(
    val questions: Questions,
    private val updateUser: UpdateUser,
    val getUser: GetUser,
    val deleteUser: DeleteUser
) :
    BaseViewModel(questions, updateUser, getUser, deleteUser) {

    var onSetFirstNameListener = MutableLiveData<String>()
    var onSetLastNameListener = MutableLiveData<String>()
    var onSetAgeListener = MutableLiveData<String>()
    var onSetEmailListener = MutableLiveData<String>()

    var onSetQuestionsListener = MutableLiveData<Pair<List<Question>, List<Int>?>>()

    var user: User? = null

    override fun onViewCreated(savedInstanceState: Bundle?) {
        super.onViewCreated(savedInstanceState)

        getUser.on(success = {
            user = it

            user?.let { user1 ->
                onSetFirstNameListener.postValue(user1.firstName ?: "")
                onSetLastNameListener.postValue(user1.lastName ?: "")
                onSetEmailListener.postValue(user1.email ?: "")
                getUserQuestions()

                onSetAgeListener.postValue(user1.dateOfBirth ?: "")
            }
        })

        showLoading()
    }

    private fun getUserQuestions() {

        questions.on(Questions.Params(QuestionType.PROFILE), success = {
            onSetQuestionsListener.postValue(
                Pair(
                    it.data!!,
                    user!!.answers
                )
            )

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

    fun onClickedUpdate(
        firstName: String? = null,
        lastName: String? = null,
        dateOfBirth: String? = null,
        answers: Array<Int>? = null
    ) {
        showLoading()

        updateUser.on(
            UpdateUser.Params(
                firstName = firstName,
                lastName = lastName,
                dateOfBirth = dateOfBirth,
                answers = answers
            ), success = {
                hideLoading()

                showDialog(
                    contentText = getLanguageForKey(LanguageConst.PROFILE_UPDATED),
                    positive = object : DGActionListener {
                        override fun onClicked(o: Any?) {
                            goBack()
                        }
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

    fun onClickedChangePassword() {
        navigateToFragment(fragment = FRChangePassword.newInstance())
    }

    fun onBirthDaySelected(dayOfMonth: Int, monthOfYear: Int, year: Int) {
        val calendar = Calendar.getInstance()

        calendar[Calendar.YEAR] = year
        calendar[Calendar.MONTH] = monthOfYear
        calendar[Calendar.DAY_OF_MONTH] = dayOfMonth

        val month = if ((monthOfYear + 1) < 10) {
            "0${monthOfYear + 1}"
        } else {
            "${monthOfYear + 1}"
        }

        val day = if ((dayOfMonth) < 10) {
            "0$dayOfMonth"
        } else {
            "$dayOfMonth"
        }

        onSetAgeListener.postValue("$year-$month-$day")
    }

    fun onClickedDeleteUser() {
        showDialog(
            contentText = getLanguageForKey(LanguageConst.DELETE_USER),
            positiveBtn = getLanguageForKey(LanguageConst.DELETE),
            negativeBtn = getLanguageForKey(LanguageConst.CANCEL),
            positive = object : DGActionListener {
                override fun onClicked(o: Any?) {
                    deleteUser.on(success = {
                        startActivity(
                            ACSplash::class,
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        )

                        finishActivity()
                    })
                }
            }
        )
    }
}