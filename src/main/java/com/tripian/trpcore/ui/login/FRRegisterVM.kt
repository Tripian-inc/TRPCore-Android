package com.tripian.trpcore.ui.login

import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import com.tripian.trpcore.base.BaseViewModel
import com.tripian.trpcore.domain.DoLogin
import com.tripian.trpcore.domain.DoRegister
import com.tripian.trpcore.ui.common.ACWebPage
import com.tripian.trpcore.ui.mytrip.ACMyTrip
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.extensions.getDateFromComponents
import com.tripian.trpcore.util.extensions.goBack
import com.tripian.trpcore.util.extensions.hideLoading
import com.tripian.trpcore.util.extensions.isValidEmail
import com.tripian.trpcore.util.extensions.showLoading
import java.util.Calendar
import javax.inject.Inject

class FRRegisterVM @Inject constructor(val doRegister: DoRegister, val doLogin: DoLogin) : BaseViewModel(doRegister, doLogin) {

    var onSetDateListener = MutableLiveData<String>()

    var isToeChecked = false

    var selectedDate: Long = 0

    fun onClickedRegister(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        password2: String,
        dateOfBirth: String?
    ) {
        if (!checkValidations(firstName, lastName, email, password, password2)) return
        showLoading()
        val dateOfBirthNullable = if (dateOfBirth.isNullOrEmpty()) null else dateOfBirth

        doRegister.on(
            DoRegister.Params(
                firstName,
                lastName,
                email,
                password,
                password2,
                dateOfBirthNullable,
                isToeChecked
            ), success = {
                hideLoading()

                startActivity(ACMyTrip::class, bundleOf(Pair("editProfile", true)))
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

    fun login(email: String, password: String) {
        doLogin.on(DoLogin.Params(email, password), success = {
            hideLoading()

            startActivity(ACMyTrip::class)
        }, error = {
            hideLoading()

            if (it.type == AlertType.DIALOG) {
                showDialog(contentText = it.errorDesc)
            } else {
                showAlert(AlertType.ERROR, it.errorDesc)
            }
        })
    }

    fun onClickedBack() {
        goBack()
    }

    fun onBirthDaySelected(dayOfMonth: Int, monthOfYear: Int, year: Int) {
        val birthDate = getDateFromComponents(year, monthOfYear, dayOfMonth)

        val calendar = Calendar.getInstance()

        calendar[Calendar.YEAR] = year
        calendar[Calendar.MONTH] = monthOfYear
        calendar[Calendar.DAY_OF_MONTH] = dayOfMonth

        selectedDate = calendar.timeInMillis

        onSetDateListener.postValue(birthDate)
    }

    fun openToeWebPage() {
        startActivity(
            ACWebPage::class,
            bundleOf(Pair("url", "https://www.tripian.com/terms-conditions.html"))
        )
    }

    private fun checkValidations(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        password2: String
    ): Boolean {
        if (firstName.isEmpty()) {
            showAlert(AlertType.ERROR, "Please enter First Name")
            return false
        }
        if (lastName.isEmpty()) {
            showAlert(AlertType.ERROR, "Please enter Last Name")
            return false
        }
        if (!isValidEmail(email)) {
            showAlert(AlertType.ERROR, "Please enter valid email address")
            return false
        }
        if (password.isNotEmpty() && password != password2) {
            showAlert(AlertType.ERROR, "Passwords do not match")
            return false
        }
        return true
    }
}
