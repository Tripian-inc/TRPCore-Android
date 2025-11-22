package com.tripian.trpcore.domain

import android.text.TextUtils
import com.tripian.one.api.users.model.UserResponse
import com.tripian.trpcore.base.BaseUseCase
import com.tripian.trpcore.repository.base.ErrorModel
import com.tripian.trpcore.util.AlertType
import com.tripian.trpcore.util.LanguageConst
import javax.inject.Inject

/**
 * Created by semihozkoroglu on 13.08.2020.
 */
class UpdateUser @Inject constructor() : BaseUseCase<UserResponse, UpdateUser.Params>() {

    class Params(
        val firstName: String?,
        val lastName: String?,
        val dateOfBirth: String? = null,
        val answers: Array<Int>? = null
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
//            params.password != null && params.password.isEmpty() -> {
//                onSendError(
//                    ErrorModel(
//                        miscRepository.getLanguageValueForKey(LanguageConst.ERROR_PASSWORD),
//                        type = AlertType.ERROR
//                    )
//                )
//            }
//            TextUtils.isEmpty(params.dateOfBirth) -> {
//                onSendError(
//                    ErrorModel(
//                        strings.getString(R.string.invalid_age),
//                        type = AlertType.ERROR
//                    )
//                )
//            }
            else -> {
                addObservable {
                    tripianUserRepository.update(
                        firstName = params.firstName,
                        lastName = params.lastName,
                        dateOfBirth = params.dateOfBirth,
                        answers = params.answers
                    )
                }
            }
        }
    }
}