package com.tripian.trpcore.domain

import android.text.TextUtils
import com.tripian.one.api.users.model.LoginResponse
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.base.ErrorModel
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.LanguageConst
import com.tripian.trpcore.util.extensions.isValidEmail
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class DoRegister @Inject constructor() : BaseUseCase<LoginResponse, DoRegister.Params>() {

    class Params(
        val firstName: String,
        val lastName: String,
        val email: String,
        val password: String,
        val password2: String,
        val dateOfBirth: String?,
        val termsChecked: Boolean
    )

    override fun on(params: Params?) {
        when {
            TextUtils.isEmpty(params!!.firstName) -> {
                onSendError(
                    ErrorModel(
                        miscRepository.getLanguageValueForKey(LanguageConst.ERROR_ENTER_FIRST_NAME),
                        type = AlertType.ERROR
                    )
                )
            }
            TextUtils.isEmpty(params.lastName) -> {
                onSendError(
                    ErrorModel(
                        miscRepository.getLanguageValueForKey(LanguageConst.ERROR_ENTER_LAST_NAME),
                        type = AlertType.ERROR
                    )
                )
            }
            TextUtils.isEmpty(params.email) -> {
                onSendError(
                    ErrorModel(
                        miscRepository.getLanguageValueForKey(LanguageConst.ERROR_ENTER_EMAIL),
                        type = AlertType.ERROR
                    )
                )
            }
            !isValidEmail(params.email) -> {
                onSendError(
                    ErrorModel(
                        miscRepository.getLanguageValueForKey(LanguageConst.ERROR_EMAIL_FORMAT),
                        type = AlertType.ERROR
                    )
                )
            }
            params.password.length < 6 -> {
                onSendError(
                    ErrorModel(
                        miscRepository.getLanguageValueForKey(LanguageConst.ERROR_PASSWORD),
                        type = AlertType.ERROR
                    )
                )
            }
            TextUtils.equals(params.password, params.password2).not() -> {
                onSendError(
                    ErrorModel(
                        miscRepository.getLanguageValueForKey(LanguageConst.ERROR_PASSWORD_EQUAL),
                        type = AlertType.ERROR
                    )
                )
            }
            !params.termsChecked -> {
                onSendError(
                    ErrorModel(
                        miscRepository.getLanguageValueForKey(LanguageConst.ERROR_TOE),
                        type = AlertType.ERROR
                    )
                )
            }
            else -> {
                addObservable {
                    tripianUserRepository.register(
                        params.firstName,
                        params.lastName,
                        params.email,
                        params.password,
                        params.dateOfBirth
                    )
                }
            }
        }
    }

    override fun isRequiredRefreshToken(): Boolean {
        return false
    }
}